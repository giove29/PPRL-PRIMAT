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
	private final Path configPath;
	private final MqttClientWrapper client;
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
	 * @param config     configurazione locale del Data Owner (party, sorgente dati,
	 *                   endpoint broker)
	 * @param configPath percorso del file JSON da cui {@code config} e' stata
	 *                   caricata: riusato per riscrivere/ricaricare il file a
	 *                   ogni {@link ConfigPush} accettata dalla SMU
	 * @throws MqttException se il client MQTT non può essere istanziato
	 */
	public DataOwnerService(DataOwnerConfig config, Path configPath) throws MqttException {
		this.config = config;
		this.configPath = configPath;
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
		client.subscribe(MqttTopics.commandTopic(config.getParty()), (topic, message) -> {
			final StartCommand command = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					StartCommand.class);
			commandExecutor.submit(() -> handleStartCommand(command));
		});
		client.subscribe(MqttTopics.configTopic(config.getParty()), (topic, message) -> {
			final ConfigPush push = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					ConfigPush.class);
			configExecutor.submit(() -> handleConfigPush(push));
		});
		client.subscribe(MqttTopics.checkVersionTopic(config.getParty()), (topic, message) -> {
			// Lettura volatile + publish, O(1): nessun accesso a disco/pipeline che
			// possa bloccare il thread di callback Paho, quindi risponde qui
			// direttamente, senza passare da un executor dedicato (a differenza di
			// handleStartCommand/handleConfigPush).
			try {
				client.publish(MqttTopics.versionReportTopic(config.getParty()),
						gson.toJson(new ConfigVersionReport(config.getParty(), appliedConfigVersion)));
			} catch (MqttException e) {
				System.err.println("[" + config.getParty() + "] impossibile rispondere al checkVersion: "
						+ e.getMessage());
			}
		});
		System.out.println("[" + config.getParty() + "] in ascolto su " + MqttTopics.commandTopic(config.getParty())
				+ ", " + MqttTopics.configTopic(config.getParty()) + " e "
				+ MqttTopics.checkVersionTopic(config.getParty()));
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
		try {
			final RecordSource source = newRecordSource(cfg);
			final List<Record> encodedRecords = new DataOwnerPipeline(source, cfg).run();

			final List<RbfPayload.RbfRecord> rbfRecords = encodedRecords.stream()
					.map(RbfCodec::toRbfRecord)
					.collect(Collectors.toList());

			final RbfPayload payload = new RbfPayload(runId, cfg.getParty(), rbfRecords, cfg.computeConfigHash());
			client.publish(MqttTopics.rbfTopic(runId, cfg.getParty()), gson.toJson(payload));

			client.publish(MqttTopics.doRunStatusTopic(cfg.getParty()),
					gson.toJson(new StatusMessage(runId, cfg.getParty(), "DONE", rbfRecords.size() + " record")));
			System.out.println("[" + cfg.getParty() + "] RBF pubblicato (" + rbfRecords.size() + " record) su "
					+ MqttTopics.rbfTopic(runId, cfg.getParty()));
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
	 * {@code bloomFilter}/{@code missingValueHandling}/{@code hmacKey}, gia'
	 * risolte dalla SMU) dentro il file JSON locale, sovrascrivendo solo quelle
	 * chiavi — identita' di party/broker/sorgente dati restano quelle locali,
	 * mai spedite dalla SMU. Scrive su un file temporaneo, valida con
	 * {@link DataOwnerConfigLoader#load} + uno smoke-test (costruzione
	 * dell'hardener e calcolo di {@code computeEffectiveRbfBitLength()}/
	 * {@code computeConfigHash()}), e solo se tutto va a buon fine sostituisce
	 * il file reale e la config live; altrimenti pubblica un {@link ConfigAck}
	 * di errore senza toccare ne' il file ne' la config in uso.
	 */
	private void handleConfigPush(ConfigPush push) {
		final String party = config.getParty();
		System.out.println("[" + party + "] push di configurazione ricevuta, versione " + push.getVersion());
		final Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
		try {
			final JsonObject incoming = JsonParser.parseString(push.getConfigJson()).getAsJsonObject();
			final String currentJson = Files.readString(configPath, StandardCharsets.UTF_8);
			final JsonObject merged = JsonParser.parseString(currentJson).getAsJsonObject();
			for (final String key : new String[] { "columns", "bloomFilter", "missingValueHandling", "hmacKey" }) {
				if (incoming.has(key)) {
					merged.add(key, incoming.get(key));
				}
			}
			merged.addProperty("version", push.getVersion());
			Files.writeString(tempPath, fileGson.toJson(merged), StandardCharsets.UTF_8);

			final DataOwnerConfig newConfig = DataOwnerConfigLoader.load(tempPath);
			// Smoke-test: esercita davvero la catena di hardening e il calcolo del
			// digest una volta, invece di fidarsi del solo parsing/validazione JSON.
			newConfig.computeEffectiveRbfBitLength();
			newConfig.computeConfigHash();

			Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			this.config = newConfig;
			this.appliedConfigVersion = push.getVersion();
			client.publish(MqttTopics.configAckTopic(party),
					gson.toJson(new ConfigAck(party, push.getVersion(), "OK", "configurazione applicata")));
			System.out.println("[" + party + "] configurazione v" + push.getVersion() + " applicata con successo");
			System.out.println(newConfig.describe());
		} catch (DataOwnerConfigException | IOException | MqttException | RuntimeException e) {
			deleteQuietly(tempPath);
			System.err.println("[" + party + "] push di configurazione v" + push.getVersion() + " rifiutata: "
					+ e.getMessage());
			try {
				client.publish(MqttTopics.configAckTopic(party),
						gson.toJson(new ConfigAck(party, push.getVersion(), "ERROR", e.getMessage())));
			} catch (MqttException publishFailure) {
				System.err.println("[" + party + "] impossibile pubblicare l'ack di errore: "
						+ publishFailure.getMessage());
			}
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
	 * @param args {@code configJsonPath}, es.
	 *             {@code primat-data-owner-service/src/main/resources/config/party_A.json}
	 * @throws Exception se l'avvio del servizio fallisce
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Uso: DataOwnerService <configJsonPath>");
			System.exit(1);
		}

		final Path configPath = Paths.get(args[0]);
		final DataOwnerConfig config;
		try {
			config = DataOwnerConfigLoader.load(configPath);
		}
		catch (DataOwnerConfigException e) {
			// Errore di configurazione utente: messaggio leggibile, niente stack
			// trace grezzo.
			System.err.println("Errore di configurazione: " + e.getMessage());
			System.exit(1);
			return;
		}

		System.out.println(config.describe());

		final DataOwnerService service = new DataOwnerService(config, configPath);
		service.start();
		service.awaitForever();
	}
}
