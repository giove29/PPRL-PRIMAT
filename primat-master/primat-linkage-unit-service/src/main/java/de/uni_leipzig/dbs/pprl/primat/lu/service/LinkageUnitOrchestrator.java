/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.lsh.LshBlocker;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.JaccardLshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.LshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.lu.database.DbConnection;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ConsoleProgressBar;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.BlockingEvaluationResult;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.LshPassProbability;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.OracleThreshold;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimate;
import de.uni_leipzig.dbs.pprl.primat.lu.service.MultiSourceLinkage.LinkageOutcome;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.ClusteringMethod;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigException;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.SimilarityThresholdSpec;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttClientWrapper;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttTopics;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.BrokerCheckRequest;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.LuBrokerPush;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.LuConfigPush;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfCodec;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfPayload;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StartCommand;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StatusMessage;

/**
 * Servizio long-running lato Linkage Unit: si connette al broker MQTT esterno
 * (da avviare prima) e resta in ascolto della configurazione di run che la
 * SMU spinge su {@link MqttTopics#luConfigTopic()} (protocollo StartCommand,
 * regia della SMU — la Linkage Unit non comanda piu' da sola i Data Owner,
 * si limita a raccogliere gli RBF che la SMU ha gia' fatto partire). Per ogni
 * run: conferma/rifiuta la configurazione ricevuta (rbfSize atteso), raccoglie
 * gli RBF (uno per party, su topic separati), verifica il digest di ognuno
 * contro quello atteso dalla SMU, esegue il blocking JaccardLSH (MinHash) e
 * UNA sola strategia di clustering, scelta dalla config JSON caricata da
 * {@link LinkageUnitConfigLoader} — sola fonte di verità, niente più
 * auto-routing dirty/clean — e infine pubblica l'esito (ack o errore) alla
 * SMU su {@link MqttTopics#luRunStatusTopic()}. Puo' eseguire piu' run
 * consecutivi senza essere riavviata.
 */
public class LinkageUnitOrchestrator {

	private final LinkageUnitConfig config;
	private final Path configPath;
	/** Non piu' final: sostituito da {@link #handleLuBrokerPush} dopo un hot-reconnect verso un nuovo {@code mqttBrokerUrl} (mirror {@code DataOwnerService.client}). */
	private volatile MqttClientWrapper client;
	private final Gson gson = new Gson();
	/** Solo per la scrittura del file di config su disco (leggibile); i payload MQTT restano compatti su {@link #gson}. */
	private final Gson fileGson = new GsonBuilder().setPrettyPrinting().create();
	/** Client id MQTT fisso della Linkage Unit, riusato anche per il client candidato in {@link #handleLuBrokerPush}. */
	private static final String CLIENT_ID = "linkage-unit-orchestrator";
	/** Timeout di verifica di un nuovo broker prima di accettarne la migrazione, mirror {@code DataOwnerService.BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS}. */
	private static final long BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS = 15L;
	/**
	 * Esegue {@link #handleLuConfigPush} fuori dal thread di callback di Paho,
	 * stesso motivo/pattern di {@code DataOwnerService.commandExecutor}: un
	 * run puo' durare a lungo (classificazione+clustering+persistenza), tenere
	 * occupato il thread di callback lascerebbe la Linkage Unit "in ascolto"
	 * ma incapace di consegnare messaggi successivi.
	 */
	private final ExecutorService runExecutor = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "lu-run-worker");
		thread.setDaemon(true);
		return thread;
	});
	/**
	 * Esegue {@link #handleLuBrokerPush} su un executor dedicato, separato da
	 * {@link #runExecutor}: una migrazione di broker non deve restare in coda
	 * dietro un run in corso, mirror della separazione
	 * {@code commandExecutor}/{@code configExecutor} lato {@code DataOwnerService}.
	 */
	private final ExecutorService brokerExecutor = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "lu-broker-worker");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * @param config     configurazione completa del run (party, strategia, DB
	 *                   dedicato, tuning), caricata da {@link LinkageUnitConfigLoader}
	 * @param configPath percorso del file JSON da cui {@code config} e' stata
	 *                   caricata: serve a {@link #handleLuConfigPush} per
	 *                   persistere su disco un {@code rbfSize} riconfigurato a
	 *                   caldo dalla SMU, mirror di {@code
	 *                   DataOwnerService#configPath}
	 * @throws Exception se il client MQTT non può essere istanziato
	 */
	public LinkageUnitOrchestrator(LinkageUnitConfig config, Path configPath) throws Exception {
		this.config = config;
		this.configPath = configPath;
		this.client = new MqttClientWrapper(config.getMqttBrokerUrl(), CLIENT_ID);
	}

	/**
	 * @return {@code true} se almeno una delle {@code parties} è duplicate-free
	 *         (clean). Usato solo come regola di validazione da
	 *         {@link LinkageUnitConfigLoader} (MSCD_AP richiede almeno una
	 *         party clean, altrimenti errore di config) — non più per
	 *         instradare il run, che oggi dipende esclusivamente da
	 *         {@code clusteringMethod} nella config.
	 */
	public static boolean anyPartyDuplicateFree(List<Party> parties) {
		return parties.stream().anyMatch(Party::isDuplicateFree);
	}

	/**
	 * @return {@code true} se TUTTE le {@code parties} sono duplicate-free.
	 *         Usato solo come regola di validazione da
	 *         {@link de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigLoader}
	 *         per {@code GLOBAL_GREEDY}/{@code CLIP}, il cui vincolo di
	 *         source-consistency e' corretto solo in assenza di duplicati
	 *         interni a qualunque sorgente.
	 */
	public static boolean allPartiesDuplicateFree(List<Party> parties) {
		return parties.stream().allMatch(Party::isDuplicateFree);
	}

	/**
	 * Connette il client orchestratore al broker esterno (da avviare prima) e
	 * si sottoscrive al topic di configurazione run della SMU, qualunque sia
	 * la modalita' di soglia configurata localmente: ogni {@link
	 * LuConfigPush} viene gestita su {@link #runExecutor}, il thread
	 * chiamante ritorna subito.
	 *
	 * @throws Exception se il broker non e' raggiungibile entro
	 *                    {@code mqtt.brokerConnectTimeoutSeconds}
	 */
	public void start() throws Exception {
		client.connect(config.getBrokerConnectTimeoutSeconds());
		subscribeAll(client);
		System.out.println("Linkage Unit in ascolto su " + MqttTopics.luConfigTopic() + " e "
				+ MqttTopics.luBrokerTopic());
	}

	/**
	 * Sottoscrive i 2 topic della Linkage Unit sul client dato — estratto da
	 * {@link #start()} per essere riusabile anche dopo un hot-reconnect verso
	 * un nuovo {@code mqttBrokerUrl} (vedi {@link #handleLuBrokerPush}), mirror
	 * di {@code DataOwnerService.subscribeAll}.
	 */
	private void subscribeAll(MqttClientWrapper target) throws MqttException {
		target.subscribe(MqttTopics.luConfigTopic(), (topic, message) -> {
			final LuConfigPush push = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					LuConfigPush.class);
			runExecutor.submit(() -> handleLuConfigPush(push));
		});
		target.subscribe(MqttTopics.luBrokerTopic(), (topic, message) -> {
			final LuBrokerPush push = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					LuBrokerPush.class);
			brokerExecutor.submit(() -> handleLuBrokerPush(push));
		});
		target.subscribe(MqttTopics.luBrokerCheckTopic(), (topic, message) -> {
			final BrokerCheckRequest req = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					BrokerCheckRequest.class);
			brokerExecutor.submit(() -> handleLuBrokerCheck(req));
		});
	}

	/**
	 * Blocca il thread chiamante all'infinito, cosi' il processo resta vivo e
	 * in ascolto (i run sono gestiti in modo asincrono da {@link #runExecutor}).
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
	 * Gestisce una {@link LuConfigPush} ricevuta dalla SMU (protocollo
	 * StartCommand, fase 2): roster di party e {@code rbfSize} non sono mai
	 * letti da un JSON locale (dal 2026-10-02 non esistono piu' come campi
	 * JSON, vedi {@link LinkageUnitConfig}) — arrivano **solo** da qui, puro
	 * stato in memoria valido per la sola durata di questo run. Valida subito
	 * il roster contro il {@code clusteringMethod} **locale** (mai spinto
	 * dalla SMU: la scelta dell'algoritmo resta solo da JSON, vedi {@link
	 * LinkageUnitConfigLoader#resolvePartyRoster}) e il {@code rbfSize} (vedi
	 * {@link LinkageUnitConfigLoader#validateRbfSize}): se uno dei due e'
	 * invalido (es. una party clean per una config MCL), pubblica un ack
	 * {@code "ERROR"} su {@link MqttTopics#luConfigAckTopic()} e il run non
	 * parte — nessuno stato applicato, nessun file toccato (non ce n'e' uno
	 * da toccare). Se validi: li applica in memoria ({@link
	 * LinkageUnitConfig#setParties}/{@link LinkageUnitConfig#setRbfSize}), li
	 * **stampa esplicitamente a schermo**, pubblica l'ack {@code "OK"} ed
	 * esegue il run: un singolo dispatch ({@link #executeRun}) per soglia
	 * fissa/{@code auto}/{@code auto_precision}/{@code auto_recall}, oppure
	 * lo sweep multi-soglia ({@link #executeRangeRun}) se {@code
	 * similarityThreshold: "range"} — questo e' l'UNICO punto di ingresso per
	 * qualunque run della Linkage Unit, qualunque sia la modalita' di soglia
	 * configurata localmente: la Linkage Unit non avvia mai un run di propria
	 * iniziativa. Pubblica l'esito finale (successo o errore — es. digest non
	 * corrispondente, matching fallito) su {@link
	 * MqttTopics#luRunStatusTopic()}. **In ogni caso, successo o errore**, il
	 * {@code finally} dimentica il roster e il rbfSize appena usati
	 * (`setParties(List.of())`/`setRbfSize(0)`) prima di tornare libera per
	 * il run successivo, che dovra' ripartire da un nuovo push della SMU —
	 * nessuno stato di configurazione sopravvive a un run.
	 */
	private void handleLuConfigPush(LuConfigPush push) {
		final String runId = push.getRunId();
		final List<Party> parties;
		try {
			parties = LinkageUnitConfigLoader.resolvePartyRoster(push.getParties(), config.getClusteringMethod());
			LinkageUnitConfigLoader.validateRbfSize(push.getRbfSize());
		} catch (LinkageUnitConfigException e) {
			publishLuStatusQuietly(MqttTopics.luConfigAckTopic(), runId, "ERROR",
					"configurazione rifiutata: " + e.getMessage());
			return;
		}
		config.setParties(parties);
		config.setRbfSize(push.getRbfSize());
		System.out.println("run " + runId + ": configurazione accettata dalla SMU - rbfSize=" + push.getRbfSize()
				+ " bit, party:");
		for (final Party party : parties) {
			System.out.println("  - " + party.getName() + " [duplicateFree=" + party.isDuplicateFree() + "]");
		}
		try {
			publishLuStatus(MqttTopics.luConfigAckTopic(), runId, "OK", "configurazione run accettata");
		} catch (MqttException e) {
			System.err.println("run " + runId + ": impossibile pubblicare l'ack di configurazione: "
					+ e.getMessage());
			return;
		}
		try {
			if (config.getThresholdSpec().isRange()) {
				final List<RangeIterationResult> results = executeRangeRun(runId, push.getExpectedDigest());
				double bestF1 = -1d;
				double bestThreshold = Double.NaN;
				for (final RangeIterationResult result : results) {
					if (result.getOutcome().getFMeasure() > bestF1) {
						bestF1 = result.getOutcome().getFMeasure();
						bestThreshold = result.getThreshold();
					}
				}
				publishLuStatusQuietly(MqttTopics.luRunStatusTopic(), runId, "OK",
						results.size() + " soglie testate, migliore F1 " + String.format(Locale.ROOT, "%.3f", bestF1)
								+ " a soglia " + String.format(Locale.ROOT, "%.2f", bestThreshold));
			}
			else {
				final LinkageOutcome outcome = executeRun(runId, push.getExpectedDigest());
				publishLuStatusQuietly(MqttTopics.luRunStatusTopic(), runId, "OK",
						outcome.getLinkTable().size() + " cluster prodotti");
			}
		} catch (Exception e) {
			System.err.println("run " + runId + ": errore ->");
			e.printStackTrace();
			publishLuStatusQuietly(MqttTopics.luRunStatusTopic(), runId, "ERROR", e.getMessage());
		} finally {
			config.setParties(List.of());
			config.setRbfSize(0);
		}
	}

	/**
	 * Gestisce una {@link LuBrokerPush} ricevuta dalla SMU su {@link
	 * MqttTopics#luBrokerTopic()} (canale dedicato, separato dal protocollo di
	 * run su {@link MqttTopics#luConfigTopic()}): mirror esatto di {@code
	 * DataOwnerService.handleConfigPush} per la parte broker. Scrive il nuovo
	 * {@code mqttBrokerUrl} su un file temporaneo sibling di {@link
	 * #configPath}, valida l'intero file ricaricandolo con {@link
	 * LinkageUnitConfigLoader#load(Path)}; se il broker e' davvero cambiato, si
	 * connette al nuovo con un timeout limitato ({@link
	 * #BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS}) PRIMA di accettare qualunque
	 * cosa (irraggiungibile = push rifiutata, file/config/client invariati).
	 * Se raggiungibile: file sostituito atomicamente, config in memoria
	 * aggiornata, ack di successo pubblicato sul client VECCHIO, e solo a quel
	 * punto il client attivo passa al nuovo (disconnessione del vecchio +
	 * nuova sottoscrizione). Qualunque fallimento successivo alla verifica
	 * lascia comunque intatti file/config/client in uso.
	 */
	private void handleLuBrokerPush(LuBrokerPush push) {
		final String newUrl = push.getMqttBrokerUrl();
		System.out.println("push di broker MQTT ricevuta dalla SMU: " + newUrl);
		// Catturato subito: l'ack di questa push esce sempre su questo client, mai
		// su this.client, che potrebbe cambiare prima della fine del metodo.
		final MqttClientWrapper ackClient = this.client;
		final Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
		MqttClientWrapper candidateClient = null;
		try {
			if (newUrl == null || newUrl.isBlank()) {
				throw new LinkageUnitConfigException("mqttBrokerUrl mancante o vuoto nella push");
			}
			final JsonObject current = JsonParser
					.parseString(Files.readString(configPath, StandardCharsets.UTF_8)).getAsJsonObject();
			current.addProperty("mqttBrokerUrl", newUrl);
			Files.writeString(tempPath, fileGson.toJson(current), StandardCharsets.UTF_8);

			// Valida l'intero file (non solo il campo appena cambiato), stesso
			// principio di handleLuConfigPush per rbfSize.
			final LinkageUnitConfig reloaded = LinkageUnitConfigLoader.load(tempPath);

			final String oldUrl = config.getMqttBrokerUrl();
			final boolean brokerChanged = !reloaded.getMqttBrokerUrl().equals(oldUrl);
			if (brokerChanged) {
				candidateClient = new MqttClientWrapper(reloaded.getMqttBrokerUrl(), CLIENT_ID);
				candidateClient.connect(BROKER_SWITCH_VERIFY_TIMEOUT_SECONDS);
			}

			Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			config.setMqttBrokerUrl(reloaded.getMqttBrokerUrl());
			ackClient.publish(MqttTopics.luBrokerAckTopic(),
					gson.toJson(new StatusMessage(null, "LU", "OK", "mqttBrokerUrl aggiornato")));
			System.out.println("configurazione broker applicata con successo");
			System.out.println(config.describe());

			if (brokerChanged) {
				this.client = candidateClient;
				subscribeAll(candidateClient);
				ackClient.disconnect();
				System.out.println("passata dal broker " + oldUrl + " al nuovo broker "
						+ reloaded.getMqttBrokerUrl());
			}
		} catch (IOException | LinkageUnitConfigException | MqttException | RuntimeException e) {
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
			System.err.println("push di broker MQTT rifiutata: " + e.getMessage());
			try {
				ackClient.publish(MqttTopics.luBrokerAckTopic(),
						gson.toJson(new StatusMessage(null, "LU", "ERROR", e.getMessage())));
			} catch (MqttException publishFailure) {
				System.err.println("impossibile pubblicare l'ack di errore: " + publishFailure.getMessage());
			}
		}
	}

	/**
	 * Gestisce una {@link BrokerCheckRequest} (pre-flight, fase 0 di una
	 * migrazione broker): apre una connessione di prova usa-e-getta verso
	 * l'URL indicato (client id diverso da {@link #CLIENT_ID}, cosi' non lo
	 * scalza dal broker), la richiude subito in ogni caso, e pubblica l'esito
	 * **sul client principale** (mai toccato da questo metodo). Nessuna
	 * scrittura su file, nessuna modifica a {@link #config}/{@link #client}:
	 * un pre-flight fallito non lascia alcun side-effect da ripulire. Mirror
	 * di {@code DataOwnerService.handleBrokerCheck}.
	 */
	private void handleLuBrokerCheck(BrokerCheckRequest req) {
		final String url = req.getMqttBrokerUrl();
		System.out.println("pre-flight richiesto per " + url + "...");
		String status = "OK";
		String detail = "raggiungibile";
		MqttClientWrapper probe = null;
		try {
			probe = new MqttClientWrapper(url, CLIENT_ID + "-probe");
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
			client.publish(MqttTopics.luBrokerCheckAckTopic(), gson.toJson(new StatusMessage(null, "LU", status, detail)));
		} catch (MqttException e) {
			System.err.println("impossibile pubblicare l'esito del pre-flight: " + e.getMessage());
		}
	}

	private void publishLuStatus(String topic, String runId, String status, String detail) throws MqttException {
		client.publish(topic, gson.toJson(new StatusMessage(runId, "LU", status, detail)));
	}

	private void publishLuStatusQuietly(String topic, String runId, String status, String detail) {
		try {
			publishLuStatus(topic, runId, status, detail);
		} catch (MqttException e) {
			System.err.println("run " + runId + ": impossibile pubblicare l'esito su " + topic + ": "
					+ e.getMessage());
		}
	}

	private static void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// best-effort: un file temporaneo residuo non e' pericoloso, verra'
			// sovrascritto dal prossimo tentativo (mirror di DataOwnerService).
		}
	}

	/**
	 * Esegue un run completo per un {@code runId} gia' concordato con la SMU:
	 * attende la raccolta di tutti gli RBF (con timeout, verificandone il
	 * digest contro {@code expectedDigest}), poi esegue il blocking JaccardLSH
	 * e la strategia di clustering dichiarata in
	 * {@code config.getClusteringMethod()}.
	 *
	 * @param runId          identificativo del run (generato dalla SMU)
	 * @param expectedDigest digest di configurazione atteso, comunicato dalla
	 *                       SMU insieme al {@code runId}: ogni RBF ricevuto
	 *                       con un digest diverso fa fallire il run
	 * @return l'esito del run (Link Table + metriche), stampato su stdout
	 * @throws Exception se la raccolta degli RBF va in timeout, un digest non
	 *                    corrisponde, o la pubblicazione/sottoscrizione MQTT
	 *                    fallisce
	 */
	private LinkageOutcome executeRun(String runId, String expectedDigest) throws Exception {
		final List<Party> parties = config.getParties();
		final Map<Party, Collection<Record>> input = waitForRbf(runId, expectedDigest);

		final StringBuilder counts = new StringBuilder();
		for (final Party party : parties) {
			counts.append(counts.length() > 0 ? ", " : "").append(party.getName()).append('=')
					.append(input.get(party).size());
		}

		final LshKeyGenerator keyGenerator = new JaccardLshKeyGenerator(config.getLshKeySize(), config.getLshKeys(),
				config.getRbfSize(), config.getLshSeed());
		final Blocker blocker = new LshBlocker(keyGenerator);
		final ClusteringMethod method = config.getClusteringMethod();
		System.out.println();
		System.out.println("=== PRIMAT Linkage Unit | strategia: " + method + " | persistenza: "
				+ (config.isPersistenceEnabled() ? "DB" : "CSV") + " ===");
		System.out.println("  run:     " + runId);
		System.out.println("  record:  " + counts);
		System.out.println("  blocchi: " + blocker.getBlocks(input).size() + " (JaccardLSH)");
		System.out.println();
		System.out.println("--- Fasi ---");

		final MultiSourceLinkage linkage = new MultiSourceLinkage();
		final boolean persistenceEnabled = config.isPersistenceEnabled();
		final long[] persistenceStartNanos = new long[1];
		linkage.setSimilarityHistogramEnabled(config.isDebug());
		linkage.setLshPassProbability(new LshPassProbability(config.getLshKeySize(), config.getLshKeys()));
		linkage.setProfileProgress(new ConsoleProgressBar("Profilo similarita'"));
		linkage.setClassificationProgress(new ConsoleProgressBar("Classificazione"));
		linkage.setClusteringProgress(new ConsoleProgressBar("Clustering"));
		linkage.setPersistenceProgress(new ConsoleProgressBar("Scrittura DB"));
		linkage.setOnClusteringFinished(() -> {
			MultiSourceLinkage.phaseLine("Clustering", linkage.getLastClusteringElapsedNanos() / 1_000_000);
			persistenceStartNanos[0] = System.nanoTime();
		});
		final DbConnection dbConnection = config.getDbConnection(); // null se persistenceEnabled == false
		final Map<Party, Collection<Record>> effectiveInput = persistenceEnabled ? buildPersistentInput(input) : input;
		final LinkageOutcome outcome = dispatchStrategy(method, effectiveInput, blocker, config.getThresholdSpec(),
				linkage, dbConnection);
		if (!persistenceEnabled) {
			ClusterCsvWriter.write(outcome, config.getCsvOutputPath(), new ConsoleProgressBar("Scrittura CSV"));
		}
		final long persistenceElapsedMillis = (System.nanoTime() - persistenceStartNanos[0]) / 1_000_000;
		MultiSourceLinkage.phaseLine(persistenceEnabled ? "Persistenza DB" : "Scrittura CSV", persistenceElapsedMillis);

		// In modalita' automatica l'istogramma esiste sempre (e' il profilo da cui si stima la soglia),
		// ma il CSV e le righe diagnostiche si scrivono solo con debug: true.
		final SimilarityHistogramCollector histogram = config.isDebug() ? linkage.getLastSimilarityHistogram() : null;
		final ThresholdEstimate estimate = linkage.getLastThresholdEstimate();
		if (histogram != null) {
			SimilarityHistogramCsvWriter.write(histogram, SimilarityHistogramCsvWriter.DEFAULT_OUTPUT_PATH);
			if (estimate != null) {
				SimilarityThresholdCsvWriter.write(estimate, SimilarityThresholdCsvWriter.DEFAULT_OUTPUT_PATH);
			}
		}

		final double appliedThreshold = estimate != null ? estimate.getThreshold() : config.getSimilarityThreshold();
		printOutcome(outcome, linkage.getLastBlockingEvaluation(), histogram, appliedThreshold, estimate,
				config.isPersistenceEnabled());
		return outcome;
	}

	/**
	 * Smista sulla strategia di clustering dichiarata in {@code config}, corpo
	 * condiviso da {@link #executeRun} (una sola soglia) e
	 * {@link #executeRangeRun} (una soglia per iterazione). {@code
	 * apConfig.addCleanSource(...)} e' idempotente ({@link
	 * de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig#addCleanSource}
	 * usa un {@code Set}), quindi e' sicuro richiamare questo metodo piu' volte
	 * sulla stessa {@code config} con lo stesso {@code apConfig} condiviso.
	 */
	private LinkageOutcome dispatchStrategy(ClusteringMethod method, Map<Party, Collection<Record>> input,
			Blocker blocker, SimilarityThresholdSpec thresholdSpec, MultiSourceLinkage linkage,
			DbConnection dbConnection) {
		switch (method) {
			case CENTER_CLUSTERING:
				return linkage.runCenterClustering(input, blocker, config.getCenterClusteringConfig(), thresholdSpec,
						config.getClusterFactory(), dbConnection);
			case MSCD_AP: {
				final ApConfig apConfig = config.getApConfig();
				config.getParties().stream().filter(Party::isDuplicateFree).map(Party::getName)
						.forEach(apConfig::addCleanSource);
				return linkage.runMscdAp(input, blocker, apConfig, thresholdSpec, config.getClusterFactory(),
						dbConnection);
			}
			case GLOBAL_GREEDY:
				return linkage.runGlobalGreedy(input, blocker, config.getGlobalGreedyConfig(), thresholdSpec,
						config.getClusterFactory(), dbConnection);
			case CLIP:
				return linkage.runClip(input, blocker, config.getClipConfig(), thresholdSpec,
						config.getClusterFactory(), dbConnection);
			case MCL:
			default:
				// dbConnection e' sempre null per MCL: nessuna chiamata a DbConnection,
				// ogni iterazione ricalcola da zero.
				return linkage.runMcl(input, blocker, config.getMclConfig(), thresholdSpec);
		}
	}

	/**
	 * Esito di una singola soglia testata da {@link #executeRangeRun}.
	 */
	public static final class RangeIterationResult {
		private final double threshold;
		private final LinkageOutcome outcome;

		RangeIterationResult(double threshold, LinkageOutcome outcome) {
			this.threshold = threshold;
			this.outcome = outcome;
		}

		public double getThreshold() {
			return threshold;
		}

		public LinkageOutcome getOutcome() {
			return outcome;
		}
	}

	/**
	 * Esegue un run in modalita' {@code similarityThreshold: "range"}: raccoglie
	 * gli RBF UNA sola volta, poi ripete classificazione+clustering una volta
	 * per ogni soglia di {@code config.getThresholdSpec().rangeValues()},
	 * stampando le performance di ogni iterazione. Modalita' di solo testing:
	 * {@code dbConnection} e' sempre {@code null} (mai persistenza/incremento,
	 * indipendentemente da {@code persistence.enabled} nel JSON, gia'
	 * cortocircuitato a {@code false} da {@code LinkageUnitConfigLoader}) e non
	 * viene mai scritto alcun file (ne' CSV della Link Table, ne' CSV/istogramma
	 * di debug): solo output a schermo, nulla di recuperabile dopo il run.
	 * Chiamato esclusivamente da {@link #handleLuConfigPush} quando la SMU
	 * avvia un run su una configurazione a soglia multipla: {@code runId} ed
	 * {@code expectedDigest} sono quelli comunicati dalla SMU (nessun
	 * publish di {@code StartCommand} qui, la SMU lo ha gia' fatto per i Data
	 * Owner prima di questo passo — la Linkage Unit resta un componente
	 * puramente passivo anche in questa modalita').
	 *
	 * @return un {@link RangeIterationResult} per ogni soglia testata, nello
	 *         stesso ordine di {@code rangeValues()}
	 * @throws Exception se la raccolta degli RBF va in timeout o la
	 *                    sottoscrizione MQTT fallisce
	 */
	private List<RangeIterationResult> executeRangeRun(String runId, String expectedDigest) throws Exception {
		final SimilarityThresholdSpec rangeSpec = config.getThresholdSpec();
		if (!rangeSpec.isRange()) {
			throw new IllegalStateException("executeRangeRun() richiede 'similarityThreshold: \"range\"', questa "
					+ "config ha " + rangeSpec);
		}
		final List<Double> thresholds = rangeSpec.rangeValues();
		final Map<Party, Collection<Record>> input = waitForRbf(runId, expectedDigest);

		final LshKeyGenerator keyGenerator = new JaccardLshKeyGenerator(config.getLshKeySize(), config.getLshKeys(),
				config.getRbfSize(), config.getLshSeed());
		final Blocker blocker = new LshBlocker(keyGenerator);
		final ClusteringMethod method = config.getClusteringMethod();

		System.out.println();
		System.out.println("=== PRIMAT Linkage Unit | TEST range soglie " + rangeSpec + " | strategia: " + method
				+ " | persistenza/incremento: DISABILITATI (solo testing) ===");
		System.out.println("  run:      " + runId);
		System.out.println("  soglie:   " + thresholds);
		System.out.println("  ATTENZIONE: nessuno schema di clustering viene salvato su file/DB in questa modalita'.");
		System.out.println();

		final MultiSourceLinkage linkage = new MultiSourceLinkage();
		linkage.setOnClusteringFinished(
				() -> MultiSourceLinkage.phaseLine("Clustering", linkage.getLastClusteringElapsedNanos() / 1_000_000));
		final BlockingEvaluationResult blockingEval = linkage.evaluateBlocking(input, blocker);
		System.out.printf(Locale.ROOT,
				"  Blocking (costante per tutte le soglie): coppie candidate %d | RR %.0f%% | PC %.0f%% | PQ %.0f%%%n",
				blockingEval.getCandidatePairs(), blockingEval.getReductionRatio() * 100,
				blockingEval.getPairsCompleteness() * 100, blockingEval.getPairsQuality() * 100);
		final List<RangeIterationResult> results = new ArrayList<>();
		double bestF1 = -1d;
		double bestThreshold = Double.NaN;
		for (final double t : thresholds) {
			System.out.printf(Locale.ROOT, "%n  SOGLIA: %.2f%n", t);
			final LinkageOutcome outcome = dispatchStrategy(method, input, blocker, SimilarityThresholdSpec.fixed(t),
					linkage, null);
			results.add(new RangeIterationResult(t, outcome));
			System.out.printf(Locale.ROOT, "  %-7s %8s %8s %8s %8s %8s %8s %8s %8s %10s%n", "soglia", "cluster", "TP",
					"FP", "FN", "GT", "recall", "precis.", "F1", "tempo(ms)");
			System.out.printf(Locale.ROOT, "  %-7.2f %8d %8d %8d %8d %8d %8.3f %8.3f %8.3f %10d%n", t,
					outcome.getLinkTable().size(), outcome.getTruePositives(), outcome.getFalsePositives(),
					outcome.getFalseNegatives(), outcome.getTotalTrueMatches(), outcome.getRecall(),
					outcome.getPrecision(), outcome.getFMeasure(), linkage.getLastClusteringElapsedNanos() / 1_000_000);
			if (outcome.getFMeasure() > bestF1) {
				bestF1 = outcome.getFMeasure();
				bestThreshold = t;
			}
		}
		System.out.printf(Locale.ROOT, "%n  Migliore F1: soglia %.2f (F1 %.3f)%n", bestThreshold, bestF1);
		System.out.println("=====");
		return results;
	}

	/**
	 * Registra le party del run e interroga Postgres per i soli {@link Cluster}
	 * storici candidati (che condividono almeno una blocking key con un
	 * record fresco, tramite {@code clusterBlock}), invece di ricaricare
	 * l'intero storico: il costo dipende dal numero di candidati, non dalla
	 * dimensione totale del DB. Un record fresco il cui {@link Record#getId()}
	 * è già tra i candidati viene scartato a favore dell'oggetto storico (che
	 * porta con sé {@link Record#getCluster()}), per evitare che lo stesso
	 * record fisico appaia come due oggetti distinti nell'union-find di
	 * {@link PersistentLinkTableBuilder}.
	 */
	private Map<Party, Collection<Record>> buildPersistentInput(Map<Party, Collection<Record>> freshInput) {
		final long phaseStart = System.nanoTime();
		final DbConnection dbConnection = config.getDbConnection();
		final long connectMs = (System.nanoTime() - phaseStart) / 1_000_000;
		long stepStart = System.nanoTime();
		dbConnection.addParties(new HashSet<>(config.getParties()));
		final long partiesMs = (System.nanoTime() - stepStart) / 1_000_000;
		stepStart = System.nanoTime();

		final List<Record> freshRecords = freshInput.values().stream()
				.flatMap(Collection::stream).collect(Collectors.toList());
		// le chiavi LSH sui record freschi sono già state calcolate dalla
		// chiamata a blocker.getBlocks(input) qualche riga sopra in executeRun()

		final Set<Cluster> candidateClusters = dbConnection.getCandidateClusters(freshRecords);
		final long candidatesMs = (System.nanoTime() - stepStart) / 1_000_000;
		MultiSourceLinkage.phaseLine("DB + cluster candidati", (System.nanoTime() - phaseStart) / 1_000_000);
		System.out.println("      connessione " + connectMs + " ms, party " + partiesMs + " ms, candidati "
				+ candidateClusters.size() + " in " + candidatesMs + " ms");
		final List<Record> history = candidateClusters.stream()
				.flatMap(c -> c.getRecords().stream()).collect(Collectors.toList());
		final Map<String, Record> historyById = history.stream()
				.collect(Collectors.toMap(Record::getId, r -> r, (a, b) -> a));

		final Map<Party, Collection<Record>> persistentInput = new HashMap<>();
		for (final Record historic : history) {
			persistentInput.computeIfAbsent(historic.getParty(), p -> new ArrayList<>()).add(historic);
		}
		for (final Record fresh : freshRecords) {
			if (!historyById.containsKey(fresh.getId())) {
				persistentInput.computeIfAbsent(fresh.getParty(), p -> new ArrayList<>()).add(fresh);
			}
		}
		return persistentInput;
	}

	/**
	 * Attende la raccolta di tutti gli RBF di un run, senza pubblicare o
	 * ripubblicare alcun comando: nel protocollo guidato dalla SMU e' la SMU a
	 * pubblicare il vero {@link StartCommand} ai Data Owner (dopo l'ack di
	 * {@link #handleLuConfigPush}), la Linkage Unit si limita ad ascoltare.
	 * <p>
	 * Dal 2026-10-02 un Data Owner con molti record pubblica i propri RBF in
	 * più "chunk" invece di un unico messaggio gigante (vedi
	 * {@link RbfPayload#getChunkIndex()}), quindi il timeout
	 * {@link de.uni_leipzig.dbs.pprl.primat.lu.service.LinkageUnitConfig#getRbfCollectionTimeoutSeconds()}
	 * non è più una deadline assoluta ma una finestra di **silenzio massimo**:
	 * si resetta ad ogni chunk ricevuto da un qualunque party, cosi' un Data
	 * Owner lento ma vivo (es. 64k record) non fa più abortire il run, mentre
	 * un Data Owner realmente bloccato o morto (nessun chunk per
	 * {@code rbfCollectionTimeoutSeconds} secondi) viene comunque rilevato.
	 *
	 * @param expectedDigest digest di configurazione atteso, comunicato dalla
	 *                        SMU insieme al {@code runId} (mai {@code null}:
	 *                        qualunque run della Linkage Unit, qualunque sia
	 *                        la modalita' di soglia, e' sempre innescato da un
	 *                        {@link LuConfigPush} della SMU che lo fornisce).
	 *                        Ogni chunk ricevuto con un digest diverso fa
	 *                        fallire il run (controllato gia' al primo chunk
	 *                        di ciascun party, non solo a raccolta completata).
	 */
	private Map<Party, Collection<Record>> waitForRbf(String runId, String expectedDigest) throws Exception {
		final List<Party> parties = config.getParties();
		final Map<String, PartyRbfAccumulator> accumulators = new ConcurrentHashMap<>();
		final CountDownLatch latch = new CountDownLatch(parties.size());
		final AtomicLong lastProgressAtMillis = new AtomicLong(System.currentTimeMillis());
		final AtomicReference<String> digestMismatch = new AtomicReference<>();

		client.subscribe(MqttTopics.rbfTopicWildcard(runId), (topic, message) -> {
			final RbfPayload chunk = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					RbfPayload.class);
			lastProgressAtMillis.set(System.currentTimeMillis());
			if (!expectedDigest.equals(chunk.getConfigHash())) {
				digestMismatch.compareAndSet(null, "Party '" + chunk.getParty() + "': digest di configurazione ("
						+ chunk.getConfigHash() + ") non corrisponde a quello atteso dalla SMU (" + expectedDigest
						+ ")");
				return;
			}
			final PartyRbfAccumulator accumulator = accumulators.computeIfAbsent(chunk.getParty(),
					p -> new PartyRbfAccumulator(chunk.getTotalChunks()));
			final boolean justCompleted = accumulator.addChunk(chunk);
			System.out.println("  run " + runId + ": [" + chunk.getParty() + "] chunk " + (chunk.getChunkIndex() + 1)
					+ "/" + chunk.getTotalChunks() + " ricevuto (" + accumulator.receivedCount() + "/"
					+ chunk.getTotalChunks() + ")");
			if (justCompleted) {
				latch.countDown();
			}
		});

		boolean allReceived = false;
		final long idleTimeoutMillis = config.getRbfCollectionTimeoutSeconds() * 1000L;
		while (!allReceived) {
			if (digestMismatch.get() != null) {
				throw new IllegalStateException(digestMismatch.get());
			}
			final long idleMillis = System.currentTimeMillis() - lastProgressAtMillis.get();
			if (idleMillis >= idleTimeoutMillis) {
				break;
			}
			final long pollSeconds = Math.max(1L,
					Math.min(config.getRbfRepublishIntervalSeconds(), (idleTimeoutMillis - idleMillis) / 1000L));
			allReceived = latch.await(pollSeconds, TimeUnit.SECONDS);
			if (!allReceived) {
				final List<String> missing = parties.stream().map(Party::getName)
						.filter(name -> accumulators.get(name) == null || !accumulators.get(name).isComplete())
						.collect(Collectors.toList());
				System.out.println("  run " + runId + ": ancora in attesa di " + missing);
			}
		}
		if (digestMismatch.get() != null) {
			throw new IllegalStateException(digestMismatch.get());
		}

		// Validazione esplicita "chunk mancanti" al momento della ricostruzione
		// finale: per costruzione il latch si decrementa solo quando un party
		// risulta completo, quindi qui sotto e' normalmente gia' vero - ma la
		// verifichiamo comunque in modo esplicito (indici mancanti elencati per
		// nome) cosi' il run viene sempre abortito con un errore descrittivo
		// (mai assemblato parzialmente) se anche un solo party non ha tutti i
		// suoi chunk, qualunque sia la causa (timeout idle scattato, o uno
		// stato altrimenti inconsistente).
		if (!allReceived || !isCollectionComplete(parties, accumulators)) {
			throw new IllegalStateException(buildMissingChunksMessage(runId, parties, accumulators));
		}
		System.out.println("Digest di configurazione verificato: tutte le " + parties.size()
				+ " sorgenti corrispondono al digest atteso dalla SMU.");

		// La lunghezza dell'RBF non e' piu' riportata dal Data Owner (che non la
		// invia piu' nel payload): config.getRbfSize() e' l'unica fonte di verita',
		// spinta dalla SMU e usata qui sotto per popolare lo snapshot di
		// persistenza con lo stesso valore per tutti i party (non piu' un
		// confronto per-party, dato che a monte non c'e' piu' nulla da confrontare).

		// Guardia sulla persistenza, prima di decodificare qualunque RBF in Record
		// e prima di qualunque persistNewClusters/getCandidateClusters: una
		// configurazione di encoding cambiata tra un run e l'altro (es. hardening
		// XOR-fold, ma anche solo un salt/hashFunctions/CWE) altera le blocking
		// key derivate dall'RBF, rompendo silenziosamente il riconoscimento
		// "stesso record fisico gia' visto" su cui si basa la persistenza
		// incrementale. Confronto su configHash (digest non reversibile, mai la
		// configurazione in chiaro): copre anche i cambi che non toccano la sola
		// dimensione dell'RBF. Nessun effetto per i run non persistenti (CSV).
		if (config.isPersistenceEnabled()) {
			final Map<String, DbConnection.EncodingSnapshot> snapshotsByParty = new HashMap<>();
			for (final Party party : parties) {
				snapshotsByParty.put(party.getName(),
						new DbConnection.EncodingSnapshot(config.getRbfSize(), expectedDigest));
			}
			config.getDbConnection().checkEncodingState(snapshotsByParty);
		}

		final Map<Party, Collection<Record>> input = new HashMap<>();
		for (final Party party : parties) {
			final PartyRbfAccumulator accumulator = accumulators.get(party.getName());
			final List<Record> records = new ArrayList<>();
			for (final RbfPayload.RbfRecord rbfRecord : accumulator.assembleOrdered()) {
				records.add(RbfCodec.toRecord(rbfRecord, party));
			}
			input.put(party, records);
		}
		return input;
	}

	static boolean isCollectionComplete(List<Party> parties, Map<String, PartyRbfAccumulator> accumulators) {
		return parties.stream().allMatch(party -> {
			final PartyRbfAccumulator accumulator = accumulators.get(party.getName());
			return accumulator != null && accumulator.isComplete();
		});
	}

	/** Messaggio d'errore descrittivo, con gli indici di chunk mancanti elencati per party, usato da {@link #waitForRbf}. */
	static String buildMissingChunksMessage(String runId, List<Party> parties,
			Map<String, PartyRbfAccumulator> accumulators) {
		final List<String> details = new ArrayList<>();
		for (final Party party : parties) {
			final PartyRbfAccumulator accumulator = accumulators.get(party.getName());
			if (accumulator == null) {
				details.add(party.getName() + " (nessun chunk ricevuto)");
			}
			else if (!accumulator.isComplete()) {
				details.add(party.getName() + " (ricevuti " + accumulator.receivedCount() + "/"
						+ accumulator.getTotalChunks() + ", mancanti indici " + accumulator.missingIndexes() + ")");
			}
		}
		return "Timeout in attesa degli RBF per il run " + runId + ": chunk mancanti -> "
				+ String.join("; ", details);
	}

	/**
	 * Accumula i chunk RBF di un singolo party per un singolo run (vedi
	 * {@link #waitForRbf}), deduplicando per {@link RbfPayload#getChunkIndex()}
	 * - un chunk ripubblicato o duplicato non altera lo stato. L'ordine di
	 * arrivo MQTT non e' garantito (Paho ammette piu' messaggi in-flight),
	 * quindi {@link #assembleOrdered()} riordina sempre per indice prima di
	 * restituire i record.
	 */
	static final class PartyRbfAccumulator {

		private final int totalChunks;
		private final Map<Integer, List<RbfPayload.RbfRecord>> chunksByIndex = new ConcurrentHashMap<>();

		PartyRbfAccumulator(int totalChunks) {
			this.totalChunks = totalChunks;
		}

		/** @return {@code true} se questo chunk ha appena reso il party completo (per decrementare il latch una sola volta). */
		boolean addChunk(RbfPayload chunk) {
			final boolean wasComplete = isComplete();
			chunksByIndex.putIfAbsent(chunk.getChunkIndex(), chunk.getRecords());
			return !wasComplete && isComplete();
		}

		boolean isComplete() {
			return chunksByIndex.size() == totalChunks;
		}

		int receivedCount() {
			return chunksByIndex.size();
		}

		int getTotalChunks() {
			return totalChunks;
		}

		List<Integer> missingIndexes() {
			final List<Integer> missing = new ArrayList<>();
			for (int i = 0; i < totalChunks; i++) {
				if (!chunksByIndex.containsKey(i)) {
					missing.add(i);
				}
			}
			return missing;
		}

		List<RbfPayload.RbfRecord> assembleOrdered() {
			final List<RbfPayload.RbfRecord> ordered = new ArrayList<>();
			for (int i = 0; i < totalChunks; i++) {
				ordered.addAll(chunksByIndex.get(i));
			}
			return ordered;
		}
	}

	private static void printHistogram(SimilarityHistogramCollector collector, double configuredThreshold) {
		final SimilarityHistogram matches = collector.getMatches();
		final SimilarityHistogram nonMatches = collector.getNonMatches();
		final SimilarityHistogram histogram = collector.getAll();
		if (histogram.getTotal() == 0) {
			return;
		}
		final double valley = histogram.valleyThreshold();
		final StringBuilder line = new StringBuilder(String.format(Locale.ROOT,
				"  Similarita': %d coppie confrontate | Otsu %s | valle %s | bimodale %s | soglia applicata %.2f",
				histogram.getTotal(), formatThreshold(histogram.otsuThreshold()), formatThreshold(valley),
				histogram.isBimodal() ? "si" : "no", configuredThreshold));
		if (histogram.isBimodal()) {
			final SimilarityHistogram.Stats low = histogram.statsBelow(valley);
			final SimilarityHistogram.Stats high = histogram.statsAtOrAbove(valley);
			line.append(String.format(Locale.ROOT, " | modo basso mu=%.3f sd=%.3f | modo alto mu=%.3f sd=%.3f",
					low.getMean(), low.getStdDev(), high.getMean(), high.getStdDev()));
		}
		System.out.println(line);
		final long matchesAbove = matches.countAtOrAbove(configuredThreshold);
		System.out.printf(Locale.ROOT,
				"    ground truth: match veri %d (sotto soglia applicata %d) | non-match sopra soglia applicata %d%n",
				matches.getTotal(), matches.getTotal() - matchesAbove, nonMatches.countAtOrAbove(configuredThreshold));
	}

	/**
	 * Riga della soglia automatica (ginocchio, IC, regime, affidabilita', stime
	 * di qualita' senza ground truth) con gli eventuali avvisi; con {@code
	 * debug: true} anche la soglia oracolo calcolata sulla ground truth, solo
	 * per validare la stima.
	 */
	private static void printThreshold(ThresholdEstimate estimate, SimilarityHistogramCollector histogram,
			long totalTrueMatches, boolean persistenceEnabled) {
		System.out.println("  Soglia:    " + estimate.describe());
		System.out.println("    stimatori: " + estimate.describeEstimators());
		if (!Double.isNaN(estimate.getEstimatedMatches())) {
			System.out.printf(Locale.ROOT, "    match attesi tra i candidati ~%.0f | persi dal blocking ~%.0f%n",
					estimate.getEstimatedMatches(), estimate.getEstimatedLostMatches());
		}
		for (final String warning : estimate.getWarnings()) {
			System.out.println("    WARN: " + warning);
		}
		if (persistenceEnabled) {
			System.out.println("    WARN: persistenza attiva: la soglia e' stimata sulle sole coppie di questo run "
					+ "(record freschi + cluster storici candidati) e puo' variare tra un run e l'altro");
		}
		if (histogram != null) {
			final OracleThreshold.Result oracle = OracleThreshold.bestF1(histogram.getMatches(),
					histogram.getNonMatches(), totalTrueMatches);
			System.out.printf(Locale.ROOT,
					"    oracolo (ground truth): soglia %.3f F1 %.3f | scarto ginocchio - oracolo %+.3f%n",
					oracle.getThreshold(), oracle.getF1(), estimate.getKnee() - oracle.getThreshold());
		}
	}

	private static String formatThreshold(double threshold) {
		return Double.isNaN(threshold) ? "n/d" : String.format(Locale.ROOT, "%.2f", threshold);
	}

	private static void printOutcome(LinkageOutcome outcome, BlockingEvaluationResult blockingEval,
			SimilarityHistogramCollector histogram, double configuredThreshold, ThresholdEstimate estimate,
			boolean persistenceEnabled) {
		System.out.println();
		System.out.println("--- Risultati ---");
		System.out.printf("  Cluster:   %d%n", outcome.getLinkTable().size());
		System.out.printf("  Blocking:  coppie candidate %d | RR %.0f%% | PC %.0f%% | PQ %.0f%%%n",
				blockingEval.getCandidatePairs(), blockingEval.getReductionRatio() * 100,
				blockingEval.getPairsCompleteness() * 100, blockingEval.getPairsQuality() * 100);
		System.out.printf("  Linkage:   TP %d | FP %d | TN %d | FN %d | GT %d | recall %.3f | precision %.3f | F1 %.3f%n",
				outcome.getTruePositives(), outcome.getFalsePositives(), outcome.getTrueNegatives(),
				outcome.getFalseNegatives(), outcome.getTotalTrueMatches(), outcome.getRecall(),
				outcome.getPrecision(), outcome.getFMeasure());
		if (estimate != null) {
			printThreshold(estimate, histogram, outcome.getTotalTrueMatches(), persistenceEnabled);
		}
		if (histogram != null) {
			printHistogram(histogram, configuredThreshold);
			System.out.println("  Istogramma scritto in " + SimilarityHistogramCsvWriter.DEFAULT_OUTPUT_PATH);
		}
		System.out.println("=====");
	}

	/**
	 * Avvia l'orchestratore come processo standalone: un solo argomento (path
	 * al JSON di configurazione). Resta sempre in ascolto all'infinito della
	 * configurazione di run spinta dalla SMU su {@link
	 * MqttTopics#luConfigTopic()}, qualunque sia la modalita' di soglia
	 * configurata localmente (fissa, {@code auto}/{@code auto_precision}/
	 * {@code auto_recall}, o {@code range}): la Linkage Unit non avvia mai un
	 * run di propria iniziativa (si veda {@link #handleLuConfigPush} per lo
	 * smistamento fisso/auto vs range), e puo' eseguire piu' run consecutivi
	 * senza essere riavviata. Multi-run con party diverse (es. rendere dirty
	 * una sorgente clean tra un run e l'altro) richiede comunque due file
	 * JSON e due processi distinti, non uno swap in-process.
	 *
	 * @param args {@code configJsonPath}, es.
	 *             {@code primat-linkage-unit-service/src/main/resources/config/mscd_ap.json}
	 * @throws Exception se l'avvio o l'esecuzione del run falliscono
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Uso: LinkageUnitOrchestrator <configJsonPath>");
			System.exit(1);
		}

		final Path configPath = Paths.get(args[0]);
		final LinkageUnitConfig config;
		try {
			config = LinkageUnitConfigLoader.load(configPath);
		}
		catch (LinkageUnitConfigException e) {
			System.err.println("Errore di configurazione: " + e.getMessage());
			System.exit(1);
			return;
		}

		System.out.println(config.describe());

		final LinkageUnitOrchestrator orchestrator = new LinkageUnitOrchestrator(config, configPath);
		orchestrator.start();
		// Qualunque sia la modalita' di soglia (fissa, auto*, o range), la
		// Linkage Unit resta sempre un componente passivo: il run parte solo
		// quando la SMU pubblica un LuConfigPush (vedi handleLuConfigPush).
		orchestrator.awaitForever();
	}
}
