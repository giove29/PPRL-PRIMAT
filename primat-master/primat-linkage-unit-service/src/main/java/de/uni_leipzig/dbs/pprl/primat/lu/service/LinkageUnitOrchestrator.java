/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.Gson;

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
	private final MqttClientWrapper client;
	private final Gson gson = new Gson();
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
	 * @param config configurazione completa del run (party, strategia, DB
	 *               dedicato, tuning), caricata da {@link LinkageUnitConfigLoader}
	 * @throws Exception se il client MQTT non può essere istanziato
	 */
	public LinkageUnitOrchestrator(LinkageUnitConfig config) throws Exception {
		this.config = config;
		this.client = new MqttClientWrapper(config.getMqttBrokerUrl(), "linkage-unit-orchestrator");
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
	 * Connette il client orchestratore al broker esterno (da avviare prima) e,
	 * fuori dalla modalita' di test {@code similarityThreshold: "range"} (vedi
	 * {@link #runRangeBenchmark()}, che non passa da qui), si sottoscrive al
	 * topic di configurazione run della SMU: ogni {@link LuConfigPush} viene
	 * gestita su {@link #runExecutor}, il thread chiamante ritorna subito.
	 *
	 * @throws Exception se il broker non e' raggiungibile entro
	 *                    {@code mqtt.brokerConnectTimeoutSeconds}
	 */
	public void start() throws Exception {
		client.connect(config.getBrokerConnectTimeoutSeconds());
		client.subscribe(MqttTopics.luConfigTopic(), (topic, message) -> {
			final LuConfigPush push = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					LuConfigPush.class);
			runExecutor.submit(() -> handleLuConfigPush(push));
		});
		System.out.println("Linkage Unit in ascolto su " + MqttTopics.luConfigTopic());
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
	 * StartCommand, fase 2): se {@code rbfSize} atteso dalla SMU differisce da
	 * quello attualmente configurato (es. la SMU ha appena rilevato un
	 * hardening XOR-fold lato Data Owner non ancora riflesso nel JSON locale
	 * della Linkage Unit), si riconfigura a caldo ({@link
	 * LinkageUnitConfig#setRbfSize}) e ristampa la propria configurazione
	 * aggiornata (mirror di {@code DataOwnerService.handleConfigPush}), poi
	 * pubblica l'ack su {@link MqttTopics#luConfigAckTopic()}, esegue il run
	 * ({@link #executeRun}) e pubblica l'esito finale (successo o errore — es.
	 * digest non corrispondente, matching fallito) su {@link
	 * MqttTopics#luRunStatusTopic()}. In ogni caso (successo o errore) il
	 * thread torna libero per il run successivo, il client resta connesso e
	 * in ascolto.
	 */
	private void handleLuConfigPush(LuConfigPush push) {
		final String runId = push.getRunId();
		System.out.println("run " + runId + ": configurazione ricevuta dalla SMU (rbfSize atteso "
				+ push.getRbfSize() + ")");
		if (push.getRbfSize() != config.getRbfSize()) {
			System.out.println("run " + runId + ": rbfSize atteso dalla SMU (" + push.getRbfSize()
					+ ") diverso da quello attualmente configurato (" + config.getRbfSize()
					+ "), riconfigurazione in corso...");
			config.setRbfSize(push.getRbfSize());
			System.out.println(config.describe());
		}
		try {
			publishLuStatus(MqttTopics.luConfigAckTopic(), runId, "OK", "configurazione run accettata");
		} catch (MqttException e) {
			System.err.println("run " + runId + ": impossibile pubblicare l'ack di configurazione: "
					+ e.getMessage());
			return;
		}
		try {
			final LinkageOutcome outcome = executeRun(runId, push.getExpectedDigest());
			publishLuStatusQuietly(MqttTopics.luRunStatusTopic(), runId, "OK",
					outcome.getLinkTable().size() + " cluster prodotti");
		} catch (Exception e) {
			System.err.println("run " + runId + ": errore ->");
			e.printStackTrace();
			publishLuStatusQuietly(MqttTopics.luRunStatusTopic(), runId, "ERROR", e.getMessage());
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
	 * {@link #runRangeBenchmark()} (una soglia per iterazione). {@code
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
	 * Esito di una singola soglia testata da {@link #runRangeBenchmark()}.
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
	 *
	 * @return un {@link RangeIterationResult} per ogni soglia testata, nello
	 *         stesso ordine di {@code rangeValues()}
	 * @throws Exception se la raccolta degli RBF va in timeout o la
	 *                    pubblicazione/sottoscrizione MQTT fallisce
	 */
	public List<RangeIterationResult> runRangeBenchmark() throws Exception {
		final SimilarityThresholdSpec rangeSpec = config.getThresholdSpec();
		if (!rangeSpec.isRange()) {
			throw new IllegalStateException("runRangeBenchmark() richiede 'similarityThreshold: \"range\"', questa "
					+ "config ha " + rangeSpec);
		}
		final List<Double> thresholds = rangeSpec.rangeValues();
		final String runId = UUID.randomUUID().toString();
		// Tool di test da riga di comando, fuori dal protocollo di produzione
		// guidato dalla SMU: nessuno pubblica un vero StartCommand qui, quindi
		// questo metodo deve ancora farlo da solo (un solo publish, senza loop di
		// repubblica) per restare utilizzabile in autonomia.
		publishStartCommandOnce(runId);
		final Map<Party, Collection<Record>> input = waitForRbf(runId, null);

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
	 * Pubblica un {@link StartCommand} una sola volta a tutti i Data Owner
	 * registrati, senza alcuna repubblica. Usato solo da
	 * {@link #runRangeBenchmark()} (tool di test da riga di comando, fuori dal
	 * protocollo di produzione): nella pipeline guidata dalla SMU e' la SMU
	 * stessa a pubblicare il vero {@code StartCommand} dopo aver ricevuto
	 * l'ack di {@link #handleLuConfigPush}, la Linkage Unit non comanda mai i
	 * Data Owner in quel percorso.
	 */
	private void publishStartCommandOnce(String runId) throws MqttException {
		final StartCommand command = new StartCommand(runId, System.currentTimeMillis());
		for (final Party party : config.getParties()) {
			client.publish(MqttTopics.commandTopic(party.getName()), gson.toJson(command));
			System.out.println("  comando pubblicato su " + MqttTopics.commandTopic(party.getName()));
		}
	}

	/**
	 * Attende la raccolta di tutti gli RBF di un run (con timeout, log di
	 * progresso periodico), senza pubblicare o ripubblicare alcun comando: nel
	 * protocollo guidato dalla SMU e' la SMU a pubblicare il vero
	 * {@link StartCommand} ai Data Owner (dopo l'ack di {@link
	 * #handleLuConfigPush}), la Linkage Unit si limita ad ascoltare.
	 *
	 * @param expectedDigest digest di configurazione atteso (comunicato dalla
	 *                        SMU insieme al {@code runId}): se non {@code
	 *                        null}, ogni RBF ricevuto con un digest diverso fa
	 *                        fallire il run. Se {@code null} (solo il path di
	 *                        test {@link #runRangeBenchmark()}, senza SMU),
	 *                        resta il solo controllo di consistenza cross-party
	 *                        preesistente (tutte le sorgenti devono condividere
	 *                        lo stesso digest tra loro, qualunque esso sia).
	 */
	private Map<Party, Collection<Record>> waitForRbf(String runId, String expectedDigest) throws Exception {
		final List<Party> parties = config.getParties();
		final Map<String, RbfPayload> receivedByParty = new ConcurrentHashMap<>();
		final CountDownLatch latch = new CountDownLatch(parties.size());

		client.subscribe(MqttTopics.rbfTopicWildcard(runId), (topic, message) -> {
			final RbfPayload payload = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					RbfPayload.class);
			if (receivedByParty.putIfAbsent(payload.getParty(), payload) == null) {
				latch.countDown();
			}
		});

		boolean allReceived = false;
		final long deadline = System.currentTimeMillis() + config.getRbfCollectionTimeoutSeconds() * 1000L;
		while (!allReceived && System.currentTimeMillis() < deadline) {
			final long remainingSeconds = Math.max(1L, (deadline - System.currentTimeMillis()) / 1000L);
			allReceived = latch.await(Math.min(config.getRbfRepublishIntervalSeconds(), remainingSeconds),
					TimeUnit.SECONDS);
			if (!allReceived) {
				final List<String> missing = parties.stream().map(Party::getName)
						.filter(name -> !receivedByParty.containsKey(name)).collect(Collectors.toList());
				System.out.println("  run " + runId + ": ancora in attesa di " + missing);
			}
		}
		if (!allReceived) {
			throw new IllegalStateException("Timeout in attesa degli RBF per il run " + runId + ": ricevuti da "
					+ receivedByParty.keySet());
		}

		if (expectedDigest != null) {
			// Verifica diretta contro il digest comunicato dalla SMU (protocollo
			// StartCommand): copre sia la coerenza cross-party sia la
			// corrispondenza con la configurazione gia' confermata in fase di
			// checkVersion, in un solo controllo.
			final List<String> mismatched = parties.stream().map(Party::getName)
					.filter(name -> !expectedDigest.equals(receivedByParty.get(name).getConfigHash()))
					.collect(Collectors.toList());
			if (!mismatched.isEmpty()) {
				throw new IllegalStateException("Digest di configurazione non corrispondente a quello atteso dalla "
						+ "SMU per i party: " + mismatched);
			}
			System.out.println("Digest di configurazione verificato: tutte le " + parties.size()
					+ " sorgenti corrispondono al digest atteso dalla SMU.");
		}
		else {
			// Nessun digest atteso da confrontare (solo il path di test
			// runRangeBenchmark, senza SMU): resta il controllo di consistenza
			// cross-party — tutte le sorgenti di questo run devono condividere
			// esattamente la stessa configurazione di encoding tra loro (stesso
			// configHash), requisito indispensabile perche' le posizioni di bit
			// dell'RBF siano comparabili tra party. Blocca l'intero run (mai un
			// warning).
			final Map<String, List<String>> partiesByConfigHash = new HashMap<>();
			for (final Party party : parties) {
				final String hash = receivedByParty.get(party.getName()).getConfigHash();
				partiesByConfigHash.computeIfAbsent(hash, h -> new ArrayList<>()).add(party.getName());
			}
			if (partiesByConfigHash.size() > 1) {
				final StringBuilder detail = new StringBuilder();
				for (final Map.Entry<String, List<String>> entry : partiesByConfigHash.entrySet()) {
					detail.append("\n  hash ").append(entry.getKey()).append(" -> party ").append(entry.getValue());
				}
				throw new IllegalStateException("Le sorgenti di questo run non condividono la stessa configurazione di "
						+ "encoding (salt/hashFunctions/hardening/CWE devono essere identici su tutte le party perche' "
						+ "le posizioni di bit dell'RBF siano comparabili):" + detail
						+ "\nAllinea le configurazioni JSON dei Data Owner coinvolti prima di continuare.");
			}
		}

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
				final RbfPayload payload = receivedByParty.get(party.getName());
				snapshotsByParty.put(party.getName(),
						new DbConnection.EncodingSnapshot(config.getRbfSize(), payload.getConfigHash()));
			}
			config.getDbConnection().checkEncodingState(snapshotsByParty);
		}

		final Map<Party, Collection<Record>> input = new HashMap<>();
		for (final Party party : parties) {
			final RbfPayload payload = receivedByParty.get(party.getName());
			final List<Record> records = new ArrayList<>();
			for (final RbfPayload.RbfRecord rbfRecord : payload.getRecords()) {
				records.add(RbfCodec.toRecord(rbfRecord, party));
			}
			input.put(party, records);
		}
		return input;
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
	 * al JSON di configurazione). Fuori da {@code similarityThreshold: "range"}
	 * (solo testing, un unico run e poi il processo termina), resta in ascolto
	 * all'infinito della configurazione di run spinta dalla SMU, potendo
	 * eseguire piu' run consecutivi senza essere riavviata. Multi-run con
	 * party diverse (es. rendere dirty una sorgente clean tra un run e
	 * l'altro) richiede comunque due file JSON e due processi distinti, non
	 * uno swap in-process.
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

		final LinkageUnitConfig config;
		try {
			config = LinkageUnitConfigLoader.load(Paths.get(args[0]));
		}
		catch (LinkageUnitConfigException e) {
			System.err.println("Errore di configurazione: " + e.getMessage());
			System.exit(1);
			return;
		}

		System.out.println(config.describe());

		final LinkageUnitOrchestrator orchestrator = new LinkageUnitOrchestrator(config);
		orchestrator.start();
		if (config.getThresholdSpec().isRange()) {
			orchestrator.runRangeBenchmark();
		}
		else {
			orchestrator.awaitForever();
		}
	}
}
