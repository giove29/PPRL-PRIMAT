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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import de.uni_leipzig.dbs.pprl.primat.lu.service.MultiSourceLinkage.LinkageOutcome;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.ClusteringMethod;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigException;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttClientWrapper;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttTopics;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfCodec;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfPayload;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StartCommand;

/**
 * Servizio long-running lato Linkage Unit: si connette al broker MQTT esterno
 * (da avviare prima), invia il comando di avvio a tutti i Data Owner registrati, raccoglie i loro
 * RBF (uno per party, su topic separati) ed esegue il blocking JaccardLSH
 * (MinHash) seguito da UNA sola strategia di clustering sul risultato, scelta
 * dalla config JSON caricata da {@link LinkageUnitConfigLoader} — sola fonte
 * di verità, niente più auto-routing dirty/clean. Può eseguire più run
 * consecutivi (uno per invocazione di {@link #runOnce}) senza che i Data
 * Owner debbano essere riavviati.
 */
public class LinkageUnitOrchestrator {

	private final LinkageUnitConfig config;
	private final MqttClientWrapper client;
	private final Gson gson = new Gson();

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
	 * Connette il client orchestratore al broker esterno (da avviare prima).
	 *
	 * @throws Exception se il broker non e' raggiungibile entro
	 *                    {@code mqtt.brokerConnectTimeoutSeconds}
	 */
	public void start() throws Exception {
		client.connect(config.getBrokerConnectTimeoutSeconds());
	}

	/**
	 * Esegue un run completo: pubblica il comando ai Data Owner, attende la
	 * raccolta di tutti gli RBF (con timeout), poi esegue il blocking
	 * JaccardLSH e la strategia di clustering dichiarata in
	 * {@code config.getClusteringMethod()}.
	 *
	 * @return l'esito del run (Link Table + metriche), stampato su stdout
	 * @throws Exception se la raccolta degli RBF va in timeout o la
	 *                    pubblicazione/sottoscrizione MQTT fallisce
	 */
	public LinkageOutcome runOnce() throws Exception {
		final String runId = UUID.randomUUID().toString();
		final List<Party> parties = config.getParties();
		final Map<Party, Collection<Record>> input = collectRbf(runId);

		final StringBuilder counts = new StringBuilder();
		for (final Party party : parties) {
			counts.append(counts.length() > 0 ? ", " : "").append(party.getName()).append('=')
					.append(input.get(party).size());
		}

		final LshKeyGenerator keyGenerator = new JaccardLshKeyGenerator(config.getLshKeySize(), config.getLshKeys(),
				config.getLshValueRange(), config.getLshSeed());
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
		linkage.setClassificationProgress(new ConsoleProgressBar("Classificazione"));
		linkage.setClusteringProgress(new ConsoleProgressBar("Clustering"));
		linkage.setPersistenceProgress(new ConsoleProgressBar("Scrittura DB"));
		linkage.setOnClusteringFinished(() -> {
			MultiSourceLinkage.phaseLine("Clustering", linkage.getLastClusteringElapsedNanos() / 1_000_000);
			persistenceStartNanos[0] = System.nanoTime();
		});
		final DbConnection dbConnection = config.getDbConnection(); // null se persistenceEnabled == false
		final Map<Party, Collection<Record>> effectiveInput = persistenceEnabled ? buildPersistentInput(input) : input;
		final LinkageOutcome outcome;
		switch (method) {
			case CENTER_CLUSTERING: {
				outcome = linkage.runCenterClustering(effectiveInput, blocker,
						config.getCenterClusteringConfig(), config.getSimilarityThreshold(), config.getClusterFactory(),
						dbConnection);
				break;
			}
			case MSCD_AP: {
				final ApConfig apConfig = config.getApConfig();
				parties.stream().filter(Party::isDuplicateFree).map(Party::getName).forEach(apConfig::addCleanSource);
				outcome = linkage.runMscdAp(effectiveInput, blocker, apConfig,
						config.getSimilarityThreshold(), config.getClusterFactory(), dbConnection);
				break;
			}
			case GLOBAL_GREEDY: {
				outcome = linkage.runGlobalGreedy(effectiveInput, blocker,
						config.getGlobalGreedyConfig(), config.getSimilarityThreshold(), config.getClusterFactory(),
						dbConnection);
				break;
			}
			case CLIP: {
				outcome = linkage.runClip(effectiveInput, blocker, config.getClipConfig(),
						config.getSimilarityThreshold(), config.getClusterFactory(), dbConnection);
				break;
			}
			case MCL:
			default: {
				// persistenceEnabled è sempre false per MCL (validatePersistence lo
				// impedisce altrimenti in fase di caricamento config), quindi
				// effectiveInput == input: nessuna chiamata a DbConnection, ogni run
				// ricalcola da zero.
				outcome = linkage.runMcl(effectiveInput, blocker, config.getMclConfig(),
						config.getSimilarityThreshold());
				break;
			}
		}
		if (!persistenceEnabled) {
			ClusterCsvWriter.write(outcome, config.getCsvOutputPath(), new ConsoleProgressBar("Scrittura CSV"));
		}
		final long persistenceElapsedMillis = (System.nanoTime() - persistenceStartNanos[0]) / 1_000_000;
		MultiSourceLinkage.phaseLine(persistenceEnabled ? "Persistenza DB" : "Scrittura CSV", persistenceElapsedMillis);

		printOutcome(outcome, linkage.getLastBlockingEvaluation());
		return outcome;
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
		// chiamata a blocker.getBlocks(input) qualche riga sopra in runOnce()

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

	private Map<Party, Collection<Record>> collectRbf(String runId) throws Exception {
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

		// Il comando viene ripubblicato periodicamente finché non arrivano tutti gli
		// RBF attesi: un Data Owner avviato di recente potrebbe ancora essere nel suo
		// ciclo di retry di connessione (vedi MqttClientWrapper#connect) e perdere
		// così il primo comando pubblicato subito dopo la connessione al broker.
		// I comandi ripubblicati sono innocui: un Data Owner che ha già risposto
		// pubblica di nuovo lo stesso RBF, ignorato da putIfAbsent qui sotto.
		final StartCommand command = new StartCommand(runId, System.currentTimeMillis());
		boolean allReceived = false;
		final long deadline = System.currentTimeMillis() + config.getRbfCollectionTimeoutSeconds() * 1000L;
		while (!allReceived && System.currentTimeMillis() < deadline) {
			for (final Party party : parties) {
				if (!receivedByParty.containsKey(party.getName())) {
					client.publish(MqttTopics.commandTopic(party.getName()), gson.toJson(command));
					System.out.println("  comando pubblicato su " + MqttTopics.commandTopic(party.getName()));
				}
			}
			allReceived = latch.await(config.getRbfRepublishIntervalSeconds(), TimeUnit.SECONDS);
		}
		if (!allReceived) {
			throw new IllegalStateException("Timeout in attesa degli RBF per il run " + runId + ": ricevuti da "
					+ receivedByParty.keySet());
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

	private static void printOutcome(LinkageOutcome outcome, BlockingEvaluationResult blockingEval) {
		System.out.println();
		System.out.println("--- Risultati ---");
		System.out.printf("  Cluster:   %d%n", outcome.getLinkTable().size());
		System.out.printf("  Blocking:  coppie candidate %d | RR %.0f%% | PC %.0f%% | PQ %.0f%%%n",
				blockingEval.getCandidatePairs(), blockingEval.getReductionRatio() * 100,
				blockingEval.getPairsCompleteness() * 100, blockingEval.getPairsQuality() * 100);
		System.out.printf("  Linkage:   TP %d | FP %d | GT %d | recall %.3f | precision %.3f | F1 %.3f%n",
				outcome.getTruePositives(), outcome.getFalsePositives(), outcome.getTotalTrueMatches(),
				outcome.getRecall(), outcome.getPrecision(), outcome.getFMeasure());
		System.out.println("=====");
	}

	/**
	 * Avvia l'orchestratore come processo standalone: un solo argomento (path
	 * al JSON di configurazione), un solo run. Multi-run con party diverse
	 * (es. rendere dirty una sorgente clean tra un run e l'altro) richiede
	 * ora due file JSON e due processi distinti, non più uno swap in-process.
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
		orchestrator.runOnce();
	}
}
