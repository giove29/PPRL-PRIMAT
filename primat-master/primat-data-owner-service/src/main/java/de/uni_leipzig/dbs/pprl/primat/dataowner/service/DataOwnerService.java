/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigException;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DbSourceConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.CsvRecordSource;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.JdbcRecordSource;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.RecordSource;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttClientWrapper;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttTopics;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.BrokerCheckRequest;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.CheckVersionCommand;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.ConfigAck;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.ConfigPush;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.ConfigVersionReport;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfCodec;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfPayload;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StartCommand;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StatusMessage;

/**
 * Servizio long-running lato Data Owner: resta in ascolto all'infinito sul
 * proprio topic di comando MQTT. Alla ricezione di uno {@link StartCommand}
 * esegue {@link DataOwnerPipeline} sui dati locali e pubblica il risultato
 * (solo RBF, mai dati in chiaro) sul topic RBF riservato del run, per poi
 * tornare in ascolto — può quindi gestire più run consecutivi senza essere
 * riavviato.
 */
public class DataOwnerService {

	/**
	 * Non piu' {@code final}: una {@link ConfigPush} accettata dalla SMU
	 * sostituisce l'istanza con una ricaricata da {@link DataOwnerConfigLoader}.
	 * {@code volatile} basta (nessuna sincronizzazione ulteriore necessaria):
	 * un solo scrittore (il thread di {@link #configExecutor}), letture con
	 * uno snapshot locale a inizio metodo in {@link #handleStartCommand} e
	 * {@link #handleConfigPush} cosi' un'esecuzione non vede la config
	 * cambiare a meta' strada.
	 */
	private volatile DataOwnerConfig config;
	/**
	 * Versione dell'ultima {@link ConfigPush} accettata, riportata alla SMU su
	 * richiesta ({@link #start()}, topic {@link MqttTopics#checkVersionTopic}).
	 * Seminata all'avvio dal campo {@code version} del file JSON locale
	 * ({@link DataOwnerConfig#getVersion()}, {@code "0"} se il file non e' mai
	 * stato toccato da una push) e riallineata ad ogni {@link #handleConfigPush}
	 * accettata, che scrive la nuova versione nello stesso file, nella stessa
	 * operazione atomica del resto della config — resta quindi sempre coerente
	 * col contenuto applicato, anche attraverso un riavvio del processo.
	 */
	private volatile String appliedConfigVersion;
	/** Mai riscritto da questo processo: solo letto all'avvio da {@link #main}. */
	private final Path localConfigPath;
	/** Riscritto atomicamente ad ogni {@link #handleConfigPush} accettata. */
	private final Path liveConfigPath;
	/**
	 * Non piu' {@code final}: un {@link ConfigPush} che cambia {@code mqttBrokerUrl}
	 * sostituisce l'istanza con una connessa al nuovo broker, dopo averla
	 * verificata (vedi {@link #handleConfigPush}) - stesso trattamento gia'
	 * riservato a {@link #config}.
	 */
	private volatile MqttClientWrapper client;
	/** Timeout di verifica di un nuovo {@code mqttBrokerUrl} prima di accettare lo switch (vedi {@link #handleConfigPush}). */
	private static final long BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS = 15L;
	private final Gson gson = new Gson();
	/** Solo per la scrittura dei file di config su disco (leggibili); i payload MQTT restano compatti su {@link #gson}. */
	private final Gson fileGson = new GsonBuilder().setPrettyPrinting().create();
	/**
	 * Esegue {@link #handleStartCommand} fuori dal thread di callback di Paho
	 * ({@code CommsCallback}), che è lo stesso thread su cui vengono anche
	 * notificati i completamenti (ack) delle {@code publish()} sincrone e la
	 * consegna di ogni messaggio successivo per questo client: elaborare la
	 * pipeline (potenzialmente lunga, minuti su CSV grandi) e poi pubblicare
	 * l'RBF direttamente dentro il listener di {@link #start()} blocca quel
	 * thread, quindi qualunque comando ripubblicato nel frattempo dalla
	 * Linkage Unit resta in coda e non viene mai gestito finché la chiamata
	 * corrente non ritorna — visto dall'esterno, il Data Owner sembra
	 * "in ascolto" (la connessione TCP resta viva) ma non risponde più a
	 * nulla. Un solo thread basta: i comandi per questo party vanno comunque
	 * gestiti in sequenza, non in parallelo.
	 */
	private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "data-owner-command-worker");
		thread.setDaemon(true);
		return thread;
	});
	/**
	 * Executor dedicato e separato da {@link #commandExecutor}: un reconfigure
	 * spinto dalla SMU non deve attendere in coda dietro un run in corso (ne'
	 * viceversa) — i due canali MQTT sono indipendenti — ma resta comunque
	 * serializzato rispetto ad altri reconfigure tramite questo thread singolo.
	 */
	private final ExecutorService configExecutor = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "data-owner-config-worker");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * @param config         configurazione del Data Owner (party, sorgente dati,
	 *                       endpoint broker - gia' fusa da locale+live)
	 * @param localConfigPath percorso del file JSON locale (mai riscritto da
	 *                       questo processo)
	 * @param liveConfigPath percorso del file JSON live: riusato per riscrivere/
	 *                       ricaricare il file a ogni {@link ConfigPush} accettata
	 *                       dalla SMU
	 * @throws MqttException se il client MQTT non può essere istanziato
	 */
	public DataOwnerService(DataOwnerConfig config, Path localConfigPath, Path liveConfigPath) throws MqttException {
		this.config = config;
		this.localConfigPath = localConfigPath;
		this.liveConfigPath = liveConfigPath;
		this.appliedConfigVersion = config.getVersion();
		this.client = new MqttClientWrapper(config.getMqttBrokerUrl(), config.getMqttClientId());
	}

	/**
	 * Si connette al broker e si sottoscrive al proprio topic di comando.
	 * Ritorna subito dopo la sottoscrizione: i comandi vengono gestiti in modo
	 * asincrono dal thread di rete di Paho, chiamare {@link #awaitForever()} per
	 * mantenere vivo il processo.
	 *
	 * @throws MqttException se connessione o sottoscrizione falliscono
	 */
	public void start() throws MqttException {
		client.connect();
		subscribeAll(client);
		System.out.println("[" + config.getParty() + "] in ascolto su " + MqttTopics.commandTopic(config.getParty())
				+ ", " + MqttTopics.configTopic(config.getParty()) + " e "
				+ MqttTopics.checkVersionTopic(config.getParty()));
	}

	/**
	 * Sottoscrive i 3 topic del Data Owner sul client dato — estratto da
	 * {@link #start()} per essere riusabile anche dopo un hot-reconnect verso un
	 * nuovo {@code mqttBrokerUrl} (vedi {@link #handleConfigPush}), dove il
	 * client passato non e' piu' necessariamente {@link #client} al momento
	 * della chiamata ma lo diventa subito dopo.
	 */
	private void subscribeAll(MqttClientWrapper target) throws MqttException {
		target.subscribe(MqttTopics.commandTopic(config.getParty()), (topic, message) -> {
			final StartCommand command = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					StartCommand.class);
			commandExecutor.submit(() -> handleStartCommand(command));
		});
		target.subscribe(MqttTopics.configTopic(config.getParty()), (topic, message) -> {
			final ConfigPush push = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					ConfigPush.class);
			configExecutor.submit(() -> handleConfigPush(push));
		});
		target.subscribe(MqttTopics.brokerCheckTopic(config.getParty()), (topic, message) -> {
			final BrokerCheckRequest req = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					BrokerCheckRequest.class);
			configExecutor.submit(() -> handleBrokerCheck(req));
		});
		target.subscribe(MqttTopics.checkVersionTopic(config.getParty()), (topic, message) -> {
			// Lettura volatile + publish, O(1): nessun accesso a disco/pipeline che
			// possa bloccare il thread di callback Paho, quindi risponde qui
			// direttamente, senza passare da un executor dedicato (a differenza di
			// handleStartCommand/handleConfigPush). Pubblica su this.client (non sul
			// parametro target): per il momento in cui questo listener scatta,
			// this.client e' sempre il client corretto, anche se nel frattempo e'
			// cambiato per un hot-reconnect.
			try {
				client.publish(MqttTopics.versionReportTopic(config.getParty()),
						gson.toJson(new ConfigVersionReport(config.getParty(), appliedConfigVersion)));
			} catch (MqttException e) {
				System.err.println("[" + config.getParty() + "] impossibile rispondere al checkVersion: "
						+ e.getMessage());
			}
		});
	}

	/**
	 * Esegue la pipeline locale e pubblica l'RBF risultante per il run
	 * richiesto (destinato alla Linkage Unit). Esito e eventuali errori
	 * vengono sempre comunicati alla SMU tramite {@link StatusMessage} sul
	 * topic di stato del run, senza interrompere l'ascolto per i run
	 * successivi.
	 *
	 * @param command comando di avvio ricevuto dalla SMU
	 */
	private void handleStartCommand(StartCommand command) {
		final DataOwnerConfig cfg = this.config;
		final String runId = command.getRunId();
		System.out.println("[" + cfg.getParty() + "] comando ricevuto per run " + runId);
		if (cfg.isPending()) {
			System.err.println("[" + cfg.getParty() + "] StartCommand ricevuto ma non ancora configurato dalla SMU "
					+ "(version=" + cfg.getVersion() + "): nessuna ConfigPush ancora accettata da questo processo");
			try {
				client.publish(MqttTopics.doRunStatusTopic(cfg.getParty()), gson.toJson(new StatusMessage(runId,
						cfg.getParty(), "FAILED", "Data Owner non ancora configurato dalla SMU")));
			} catch (MqttException publishFailure) {
				System.err.println("[" + cfg.getParty() + "] impossibile pubblicare lo stato di errore: "
						+ publishFailure.getMessage());
			}
			return;
		}
		try {
			final RecordSource source = newRecordSource(cfg);
			final List<Record> encodedRecords = new DataOwnerPipeline(source, cfg).run();

			final List<RbfPayload.RbfRecord> rbfRecords = encodedRecords.stream()
					.map(RbfCodec::toRbfRecord)
					.collect(Collectors.toList());

			final String configHash = cfg.computeConfigHash();
			final int chunkSize = cfg.getRbfChunkSize();
			final int totalChunks = Math.max(1, (int) Math.ceil(rbfRecords.size() / (double) chunkSize));
			for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
				final int from = chunkIndex * chunkSize;
				final int to = Math.min(from + chunkSize, rbfRecords.size());
				final RbfPayload chunk = new RbfPayload(runId, cfg.getParty(), rbfRecords.subList(from, to),
						configHash, chunkIndex, totalChunks);
				client.publish(MqttTopics.rbfTopic(runId, cfg.getParty()), gson.toJson(chunk));
				System.out.println("[" + cfg.getParty() + "] RBF chunk " + (chunkIndex + 1) + "/" + totalChunks
						+ " pubblicato (" + chunk.getRecords().size() + " record) su "
						+ MqttTopics.rbfTopic(runId, cfg.getParty()));
			}

			client.publish(MqttTopics.doRunStatusTopic(cfg.getParty()),
					gson.toJson(new StatusMessage(runId, cfg.getParty(), "DONE", rbfRecords.size() + " record")));
			System.out.println("[" + cfg.getParty() + "] RBF completato: " + rbfRecords.size() + " record in "
					+ totalChunks + " chunk su " + MqttTopics.rbfTopic(runId, cfg.getParty()));
		} catch (Exception e) {
			System.err.println("[" + cfg.getParty() + "] errore durante l'elaborazione del run " + runId + ":");
			e.printStackTrace();
			try {
				client.publish(MqttTopics.doRunStatusTopic(cfg.getParty()),
						gson.toJson(new StatusMessage(runId, cfg.getParty(), "FAILED", e.getMessage())));
			} catch (MqttException publishFailure) {
				System.err.println("[" + cfg.getParty() + "] impossibile pubblicare lo stato di errore: "
						+ publishFailure.getMessage());
			}
		}
	}

	/**
	 * Fonde {@link ConfigPush#getConfigJson()} (chiavi {@code columns}/
	 * {@code bloomFilter}/{@code missingValueHandling}/{@code hmacKey}/
	 * {@code mqttBrokerUrl}, gia' risolte dalla SMU) dentro il file JSON live,
	 * sovrascrivendo solo quelle chiavi — identita' di party/sorgente dati
	 * restano quelle del file locale, mai spedite dalla SMU. Scrive su un file
	 * temporaneo, valida con {@link DataOwnerConfigLoader#load} + uno
	 * smoke-test (costruzione dell'hardener e calcolo di
	 * {@code computeEffectiveRbfBitLength()}/{@code computeConfigHash()}).
	 * <p>
	 * Se {@code mqttBrokerUrl} cambia, applica "verifica-poi-switch": prima di
	 * accettare qualunque cosa, si connette al NUOVO broker con un timeout
	 * limitato ({@link #BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS}); se irraggiungibile
	 * l'intera push e' rifiutata (stesso trattamento di un qualunque altro
	 * errore di validazione, file/config correnti invariati). Se raggiungibile:
	 * file sostituito atomicamente, config in memoria aggiornata, ack di
	 * successo pubblicato **sul client vecchio** (quello su cui e' arrivata
	 * questa push — la SMU deve ricevere conferma prima che il Data Owner lasci
	 * quel broker), e solo a quel punto il client attivo passa al nuovo
	 * (disconnessione del vecchio + nuova sottoscrizione sul nuovo). Qualunque
	 * fallimento successivo alla verifica lascia comunque intatti file/config/
	 * client in uso.
	 */
	private void handleConfigPush(ConfigPush push) {
		final String party = config.getParty();
		System.out.println("[" + party + "] push di configurazione ricevuta, versione " + push.getVersion());
		// Catturato subito: l'ack di questa push esce sempre su questo client, mai
		// su this.client, che potrebbe cambiare prima della fine del metodo.
		final MqttClientWrapper ackClient = this.client;
		final Path tempPath = liveConfigPath.resolveSibling(liveConfigPath.getFileName() + ".tmp");
		MqttClientWrapper candidateClient = null;
		try {
			final JsonObject incoming = JsonParser.parseString(push.getConfigJson()).getAsJsonObject();
			final String currentJson = Files.readString(liveConfigPath, StandardCharsets.UTF_8);
			final JsonObject merged = JsonParser.parseString(currentJson).getAsJsonObject();
			for (final String key : new String[] { "columns", "bloomFilter", "missingValueHandling", "hmacKey",
					"mqttBrokerUrl" }) {
				if (incoming.has(key)) {
					merged.add(key, incoming.get(key));
				}
			}
			merged.addProperty("version", push.getVersion());
			Files.writeString(tempPath, fileGson.toJson(merged), StandardCharsets.UTF_8);

			final DataOwnerConfig newConfig = DataOwnerConfigLoader.load(localConfigPath, tempPath);
			// Smoke-test: esercita davvero la catena di hardening e il calcolo del
			// digest una volta, invece di fidarsi del solo parsing/validazione JSON.
			newConfig.computeEffectiveRbfBitLength();
			newConfig.computeConfigHash();

			final String oldBrokerUrl = this.config.getMqttBrokerUrl();
			final boolean brokerChanged = !newConfig.getMqttBrokerUrl().equals(oldBrokerUrl);
			if (brokerChanged) {
				candidateClient = new MqttClientWrapper(newConfig.getMqttBrokerUrl(), newConfig.getMqttClientId());
				candidateClient.connect(BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS);
			}

			Files.move(tempPath, liveConfigPath, StandardCopyOption.REPLACE_EXISTING);
			this.config = newConfig;
			this.appliedConfigVersion = push.getVersion();
			ackClient.publish(MqttTopics.configAckTopic(party),
					gson.toJson(new ConfigAck(party, push.getVersion(), "OK", "configurazione applicata")));
			System.out.println("[" + party + "] configurazione v" + push.getVersion() + " applicata con successo");
			System.out.println(newConfig.describe());

			if (brokerChanged) {
				this.client = candidateClient;
				subscribeAll(candidateClient);
				ackClient.disconnect();
				System.out.println("[" + party + "] passato dal broker " + oldBrokerUrl + " al nuovo broker "
						+ newConfig.getMqttBrokerUrl());
			}
		} catch (DataOwnerConfigException | IOException | MqttException | RuntimeException e) {
			deleteQuietly(tempPath);
			if (candidateClient != null) {
				try {
					candidateClient.disconnect();
				} catch (Exception ignored) {
					// best-effort: la push e' comunque rifiutata, this.client resta quello
					// corretto (ackClient), un client candidato mai installato non ha
					// alcun effetto osservabile se non viene disconnesso correttamente.
				}
			}
			System.err.println("[" + party + "] push di configurazione v" + push.getVersion() + " rifiutata: "
					+ e.getMessage());
			try {
				ackClient.publish(MqttTopics.configAckTopic(party),
						gson.toJson(new ConfigAck(party, push.getVersion(), "ERROR", e.getMessage())));
			} catch (MqttException publishFailure) {
				System.err.println("[" + party + "] impossibile pubblicare l'ack di errore: "
						+ publishFailure.getMessage());
			}
		}
	}

	/**
	 * Gestisce una {@link BrokerCheckRequest} (pre-flight, fase 0 di una
	 * migrazione broker): apre una connessione di prova usa-e-getta verso
	 * l'URL indicato (client id diverso da quello principale, cosi' non lo
	 * scalza dal broker), la richiude subito in ogni caso, e pubblica l'esito
	 * **sul client principale** (mai toccato da questo metodo). Nessuna
	 * scrittura su file, nessuna modifica a {@link #config}/{@link #client}:
	 * un pre-flight fallito non lascia alcun side-effect da ripulire.
	 */
	private void handleBrokerCheck(BrokerCheckRequest req) {
		final String party = config.getParty();
		final String url = req.getMqttBrokerUrl();
		System.out.println("[" + party + "] pre-flight richiesto per " + url + "...");
		String status = "OK";
		String detail = "raggiungibile";
		MqttClientWrapper probe = null;
		try {
			probe = new MqttClientWrapper(url, config.getMqttClientId() + "-probe");
			probe.connect(BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS);
		} catch (MqttException | RuntimeException e) {
			status = "ERROR";
			detail = e.getMessage();
		} finally {
			if (probe != null) {
				try {
					probe.disconnect();
				} catch (Exception ignored) {
					// best-effort: era solo una connessione di prova, mai installata.
				}
			}
		}
		try {
			client.publish(MqttTopics.brokerCheckAckTopic(party), gson.toJson(new StatusMessage(null, party, status, detail)));
		} catch (MqttException e) {
			System.err.println("[" + party + "] impossibile pubblicare l'esito del pre-flight: " + e.getMessage());
		}
	}

	private static void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// best-effort: un file temporaneo residuo non e' pericoloso, verra'
			// sovrascritto dal prossimo tentativo.
		}
	}

	private RecordSource newRecordSource(DataOwnerConfig cfg) {
		switch (cfg.getDataSourceType()) {
			case CSV:
				return new CsvRecordSource(cfg.getCsvFilePath(), cfg.isCsvHasHeader(), cfg.getCsvDelimiter());
			case DB: {
				final DbSourceConfig dbConfig = cfg.getDbConfig();
				return new JdbcRecordSource(dbConfig.getJdbcUrl(), dbConfig.getUsername(), dbConfig.getPassword(),
						dbConfig.getTableName());
			}
			default:
				throw new UnsupportedDataSourceException("Tipo di sorgente dati non gestito: "
						+ cfg.getDataSourceType());
		}
	}

	/**
	 * Blocca il thread chiamante all'infinito, cosi' il processo resta vivo e in
	 * ascolto (i comandi MQTT sono gestiti in modo asincrono dal thread di rete
	 * di Paho).
	 *
	 * @throws InterruptedException se il thread viene interrotto (es. shutdown)
	 */
	public void awaitForever() throws InterruptedException {
		final Object lock = new Object();
		synchronized (lock) {
			while (true) {
				lock.wait();
			}
		}
	}

	/**
	 * Avvia il Data Owner Service come processo standalone.
	 *
	 * @param args {@code localConfigJsonPath}, es.
	 *             {@code primat-data-owner-service/src/main/resources/config/examples/example_local.json}
	 *             — il file locale dichiara al proprio interno ({@code liveConfigPath}) dove si
	 *             trova (o dovra' essere creato) il file "live"; vedi
	 *             {@link DataOwnerConfigLoader#loadBootstrap(Path)}.
	 * @throws Exception se l'avvio del servizio fallisce
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Uso: DataOwnerService <localConfigJsonPath>");
			System.exit(1);
		}

		final Path localConfigPath = Paths.get(args[0]);
		final DataOwnerConfigLoader.BootstrapResult bootstrap;
		try {
			bootstrap = DataOwnerConfigLoader.loadBootstrap(localConfigPath);
		}
		catch (DataOwnerConfigException e) {
			// Errore di configurazione utente: messaggio leggibile, niente stack
			// trace grezzo.
			System.err.println("Errore di configurazione: " + e.getMessage());
			System.exit(1);
			return;
		}

		System.out.println(bootstrap.config.describe());

		final DataOwnerService service = new DataOwnerService(bootstrap.config, localConfigPath, bootstrap.livePath);
		service.start();
		service.awaitForever();
	}
}
