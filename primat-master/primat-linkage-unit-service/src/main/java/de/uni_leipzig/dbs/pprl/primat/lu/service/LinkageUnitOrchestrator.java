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
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.BlockingEvaluationResult;
import de.uni_leipzig.dbs.pprl.primat.lu.service.MultiSourceLinkage.LinkageOutcome;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.ClusteringMethod;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigException;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.mqtt.EmbeddedBrokerLauncher;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttClientWrapper;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttTopics;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfCodec;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfPayload;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StartCommand;

/**
 * Servizio long-running lato Linkage Unit: avvia il broker MQTT embedded,
 * invia il comando di avvio a tutti i Data Owner registrati, raccoglie i loro
 * RBF (uno per party, su topic separati) ed esegue il blocking JaccardLSH
 * (MinHash) seguito da UNA sola strategia di clustering sul risultato, scelta
 * dalla config JSON caricata da {@link LinkageUnitConfigLoader} — sola fonte
 * di verità, niente più auto-routing dirty/clean. Può eseguire più run
 * consecutivi (uno per invocazione di {@link #runOnce}) senza che i Data
 * Owner debbano essere riavviati.
 */
public class LinkageUnitOrchestrator {

	/**
	 * Porta del broker MQTT embedded. Indipendente dal {@code mqtt.brokerUrl}
	 * della config (quello e' l'endpoint a cui il client si connette): il
	 * default di entrambi coincide ("tcp://localhost:1883"), ma se
	 * {@code mqtt.brokerUrl} viene cambiato in config resta responsabilità di
	 * chi configura assicurarsi che punti a un broker realmente in ascolto su
	 * questa porta (o a uno esterno già avviato).
	 */
	private static final int BROKER_PORT = 1883;

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
	 * Avvia il broker embedded e connette il client orchestratore.
	 *
	 * @throws Exception se l'avvio del broker o la connessione falliscono
	 */
	public void start() throws Exception {
		final EmbeddedBrokerLauncher broker = new EmbeddedBrokerLauncher(BROKER_PORT);
		broker.start();
		client.connect();
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

		System.out.println("=== Run " + runId + " - record ricevuti per party ===");
		for (final Party party : parties) {
			System.out.println("  " + party.getName() + ": " + input.get(party).size());
		}

		final LshKeyGenerator keyGenerator = new JaccardLshKeyGenerator(config.getLshKeySize(), config.getLshKeys(),
				config.getLshValueRange(), config.getLshSeed());
		final Blocker blocker = new LshBlocker(keyGenerator);
		System.out.println("=== Blocking (JaccardLSH) - blocchi: " + blocker.getBlocks(input).size() + " ===");

		final ClusteringMethod method = config.getClusteringMethod();
		System.out.println("=== Strategia scelta: " + method + " ===");

		final long runStartNanos = System.nanoTime();
		final MultiSourceLinkage linkage = new MultiSourceLinkage();
		final boolean persistenceEnabled = config.isPersistenceEnabled();
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
		final long runElapsedMillis = (System.nanoTime() - runStartNanos) / 1_000_000;
		final long clusteringElapsedMillis = linkage.getLastClusteringElapsedNanos() / 1_000_000;
		final long persistenceElapsedMillis = runElapsedMillis - clusteringElapsedMillis;
		System.out.println("=== Fase di clustering completata in " + clusteringElapsedMillis + " ms ===");
		System.out.println("=== Fase di persistenza/salvataggio completata in " + persistenceElapsedMillis + " ms ===");

		if (!persistenceEnabled) {
			ClusterCsvWriter.write(outcome, config.getCsvOutputPath());
		}

		printOutcome(method.name(), outcome, linkage.getLastBlockingEvaluation());
		printLinkTable(outcome.getLinkTable());
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
		final DbConnection dbConnection = config.getDbConnection();
		dbConnection.addParties(new HashSet<>(config.getParties()));

		final List<Record> freshRecords = freshInput.values().stream()
				.flatMap(Collection::stream).collect(Collectors.toList());
		// le chiavi LSH sui record freschi sono già state calcolate dalla
		// chiamata a blocker.getBlocks(input) qualche riga sopra in runOnce()

		final Set<Cluster> candidateClusters = dbConnection.getCandidateClusters(freshRecords);
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
		// così il primo comando pubblicato subito dopo l'avvio del broker embedded.
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

	private static void printOutcome(String label, LinkageOutcome outcome, BlockingEvaluationResult blockingEval) {
		System.out.printf("  %s - TP: %d, FP: %d, ground-truth totali: %d, recall: %.3f, precision: %.3f, F-measure: %.3f%n",
				label, outcome.getTruePositives(), outcome.getFalsePositives(), outcome.getTotalTrueMatches(),
				outcome.getRecall(), outcome.getPrecision(), outcome.getFMeasure());
		System.out.printf("  %s - coppie generate: %d, RR: %.0f%%, PC: %.0f%%, PQ: %.0f%%%n",
				label, blockingEval.getCandidatePairs(), blockingEval.getReductionRatio() * 100,
				blockingEval.getPairsCompleteness() * 100, blockingEval.getPairsQuality() * 100);
	}

	private static void printLinkTable(java.util.Set<Cluster> linkTable) {
		for (final Cluster cluster : linkTable) {
			final StringBuilder members = new StringBuilder();
			for (final Record record : cluster.getRecords()) {
				if (members.length() > 0) {
					members.append(", ");
				}
				members.append(record.getParty().getName()).append(':').append(record.getId());
			}
			System.out.println("    cluster " + cluster.getId() + ": [" + members + "]");
		}
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

		final LinkageUnitOrchestrator orchestrator = new LinkageUnitOrchestrator(config);
		orchestrator.start();
		orchestrator.runOnce();
	}
}
