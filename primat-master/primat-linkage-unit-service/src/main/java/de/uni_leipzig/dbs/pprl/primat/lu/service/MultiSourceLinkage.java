/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.LinkageConstraint;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DoubleListAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.ThresholdClassificator;
import de.uni_leipzig.dbs.pprl.primat.lu.database.DbConnection;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.BlockingEvaluationResult;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.BlockingEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.PerformanceMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.Matcher;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.batch.BatchMatcher;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.MultipartiteClusteringStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.NoPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.AffinityPropagationPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.CenterClusteringPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures.CenterClusteringConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.ClipClusteringPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.data_structures.ClipConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.global_greedy.GlobalGreedyClusteringPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.global_greedy.data_structures.GlobalGreedyConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.MarkovClusteringPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity.BitSetAttributeSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.BaseRecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.RecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.BatchSimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.RedundancyCheckStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.SimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary.BinarySimilarity;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorFlattener;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.FlatSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorFlattener;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.NoThresholdRefinement;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ProgressListener;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ThresholdClassificationRefinement;

/**
 * Generalizza a N party il flusso a due party di {@code BatchMatching} di
 * {@code primat-examples}: blocking + classificazione a soglia su similarità
 * Jaccard del bitset RBF, seguita da UNA sola strategia di clustering che
 * opera direttamente su un grafo di similarità multi-partito unico
 * ({@link MultiPartiteSimilarityGraph}, costruito con
 * {@link MultiPartiteSimilarityGraph#from(LinkageResult)}), scelta dalla
 * config JSON della Linkage Unit ({@code ClusteringMethod}, vedi
 * {@code LinkageUnitConfigLoader}) — non più auto-derivata dalla composizione
 * delle sorgenti del run.
 */
public class MultiSourceLinkage {

	/**
	 * Nanosecondi impiegati dalla sola chiamata a
	 * {@code clusterer.cluster(graph)} durante l'ultima {@code runXxx(...)}
	 * invocata su questa istanza — vedi {@link #getLastClusteringElapsedNanos()}.
	 */
	private long lastClusteringElapsedNanos;

	private ProgressListener classificationProgress = ProgressListener.NOOP;

	public void setClassificationProgress(ProgressListener listener) {
		this.classificationProgress = listener;
	}

	private ProgressListener clusteringProgress = ProgressListener.NOOP;

	private ProgressListener persistenceProgress = ProgressListener.NOOP;

	private Runnable onClusteringFinished = () -> {
	};

	public void setClusteringProgress(ProgressListener listener) {
		this.clusteringProgress = listener;
	}

	public void setPersistenceProgress(ProgressListener listener) {
		this.persistenceProgress = listener;
	}

	/** Invocato subito dopo il clustering, prima di qualsiasi scrittura su DB. */
	public void setOnClusteringFinished(Runnable callback) {
		this.onClusteringFinished = callback;
	}

	/**
	 * Esito della valutazione del blocking (RR/PC/PQ) dell'ultima
	 * {@code runXxx(...)} invocata su questa istanza, calcolato sull'output
	 * grezzo del blocking, prima di similarity calculation/classificazione —
	 * vedi {@link #getLastBlockingEvaluation()}.
	 */
	private BlockingEvaluationResult lastBlockingEvaluation;

	/**
	 * Le strategie di clustering multi-sorgente supportate — solo etichetta
	 * di risultato, non più un parametro di dispatch (il dispatch vero è
	 * {@code ClusteringMethod} nella config JSON della Linkage Unit).
	 */
	public enum LinkStrategy {
		/** Multi-Source Clean-Dirty Affinity Propagation (richiede almeno una party clean). */
		MSCD_AP,
		/** Center Clustering. */
		CENTER_CLUSTERING,
		/** Markov Clustering. */
		MCL,
		/** Global Greedy source-consistent (tutte le sorgenti Clean). */
		GLOBAL_GREEDY,
		/** CLIP (tutte le sorgenti Clean). */
		CLIP
	}

	/** Esito di un run di linkage: Link Table e metriche di qualità rispetto al ground truth (GLOBAL_ID). */
	public static class LinkageOutcome {

		private final Set<Cluster> linkTable;
		private final List<LinkedPair<Record>> matches;
		private final long truePositives;
		private final long falsePositives;
		private final long totalTrueMatches;
		private final double recall;
		private final double precision;
		private final double fMeasure;

		LinkageOutcome(Set<Cluster> linkTable, List<LinkedPair<Record>> matches, long truePositives,
				long falsePositives, long totalTrueMatches, double recall, double precision, double fMeasure) {
			this.linkTable = linkTable;
			this.matches = matches;
			this.truePositives = truePositives;
			this.falsePositives = falsePositives;
			this.totalTrueMatches = totalTrueMatches;
			this.recall = recall;
			this.precision = precision;
			this.fMeasure = fMeasure;
		}

		public Set<Cluster> getLinkTable() {
			return linkTable;
		}

		public List<LinkedPair<Record>> getMatches() {
			return matches;
		}

		public long getTruePositives() {
			return truePositives;
		}

		public long getFalsePositives() {
			return falsePositives;
		}

		public long getTotalTrueMatches() {
			return totalTrueMatches;
		}

		public double getRecall() {
			return recall;
		}

		public double getPrecision() {
			return precision;
		}

		public double getFMeasure() {
			return fMeasure;
		}
	}

	/**
	 * Esegue blocking, classificazione a soglia e clustering MSCD-AP sul
	 * grafo N-ario unico, poi persiste la Link Table risultante su database
	 * tramite {@link PersistentLinkTableBuilder}: gli id dei {@link Cluster}
	 * restano stabili tra run successivi e {@code input} può includere,
	 * oltre al batch fresco, lo storico già persistito (record con
	 * {@code Record#getCluster()} valorizzato).
	 *
	 * @param input          record di tutti i party (2 o più, almeno una clean),
	 *                       già codificati come RBF
	 * @param blocker        strategia di blocking da usare
	 * @param apConfig       configurazione per {@link AffinityPropagationPostprocessor}
	 * @param threshold      soglia di similarità Jaccard per la classificazione
	 *                       match / non-match
	 * @param clusterFactory factory usata per gli eventuali nuovi cluster
	 * @param dbConnection   connessione al DB dedicato a questa strategia
	 *                       (MSCD-AP, vedi {@code LinkageUnitConfig}), o
	 *                       {@code null} se {@code persistence.enabled=false}
	 *                       in config: la Link Table resta allora in memoria
	 *                       (nessun DB toccato) e va esportata su CSV lato
	 *                       chiamante (vedi {@link ClusterCsvWriter})
	 * @return la Link Table risultante e le metriche di qualità rispetto al
	 *         ground truth
	 */
	public LinkageOutcome runMscdAp(Map<Party, Collection<Record>> input, Blocker blocker, ApConfig apConfig,
			double threshold, ClusterFactory clusterFactory, DbConnection dbConnection) {
		final MultipartiteClusteringStrategy clusterer = new AffinityPropagationPostprocessor<>(-0.1, -0.5,
				apConfig.getDampingFactor(), apConfig);
		final List<LinkedPair<Record>> matches = classifyAndCluster(input, blocker, clusterer, threshold);

		final Collection<Record> allRecords = input.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toList());
		final Set<Cluster> linkTable = dbConnection != null
				? PersistentLinkTableBuilder.build(matches, allRecords, clusterFactory, dbConnection, persistenceProgress)
				: LinkTableBuilder.build(matches, allRecords);

		return buildOutcome(input, matches, linkTable);
	}

	/**
	 * Esegue blocking, classificazione a soglia e clustering Center Clustering
	 * sul grafo N-ario unico, poi persiste la Link Table su database (stesso
	 * builder di {@link #runMscdAp}, DB dedicato diverso).
	 *
	 * @param input                  record di tutti i party, già codificati come RBF
	 * @param blocker                strategia di blocking da usare
	 * @param centerClusteringConfig configurazione per {@link CenterClusteringPostprocessor}
	 * @param threshold              soglia di similarità Jaccard per la classificazione
	 *                               match / non-match
	 * @param clusterFactory         factory usata per gli eventuali nuovi cluster
	 * @param dbConnection           connessione al DB dedicato a Center Clustering,
	 *                               o {@code null} se {@code persistence.enabled=false}
	 *                               in config (vedi {@link #runMscdAp} per il dettaglio)
	 * @return la Link Table risultante e le metriche di qualità rispetto al
	 *         ground truth
	 */
	public LinkageOutcome runCenterClustering(Map<Party, Collection<Record>> input, Blocker blocker,
			CenterClusteringConfig centerClusteringConfig, double threshold, ClusterFactory clusterFactory,
			DbConnection dbConnection) {
		final MultipartiteClusteringStrategy clusterer = new CenterClusteringPostprocessor(centerClusteringConfig);
		final List<LinkedPair<Record>> matches = classifyAndCluster(input, blocker, clusterer, threshold);

		final Collection<Record> allRecords = input.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toList());
		final Set<Cluster> linkTable = dbConnection != null
				? PersistentLinkTableBuilder.build(matches, allRecords, clusterFactory, dbConnection, persistenceProgress)
				: LinkTableBuilder.build(matches, allRecords);

		return buildOutcome(input, matches, linkTable);
	}

	/**
	 * Esegue blocking, classificazione a soglia e clustering Global Greedy
	 * source-consistent sul grafo N-ario unico, poi persiste la Link Table su
	 * database (stesso builder di {@link #runMscdAp}, DB dedicato diverso).
	 * Dedicato allo scenario in cui tutte le party sono duplicate-free (Clean).
	 *
	 * @param input              record di tutti i party, tutti duplicate-free,
	 *                           già codificati come RBF
	 * @param blocker            strategia di blocking da usare
	 * @param globalGreedyConfig configurazione per {@link GlobalGreedyClusteringPostprocessor}
	 * @param threshold          soglia di similarità Jaccard per la classificazione
	 *                           match / non-match
	 * @param clusterFactory     factory usata per gli eventuali nuovi cluster
	 * @param dbConnection       connessione al DB dedicato a Global Greedy, o
	 *                           {@code null} se {@code persistence.enabled=false}
	 *                           in config (vedi {@link #runMscdAp} per il dettaglio)
	 * @return la Link Table risultante e le metriche di qualità rispetto al
	 *         ground truth
	 */
	public LinkageOutcome runGlobalGreedy(Map<Party, Collection<Record>> input, Blocker blocker,
			GlobalGreedyConfig globalGreedyConfig, double threshold, ClusterFactory clusterFactory,
			DbConnection dbConnection) {
		final MultipartiteClusteringStrategy clusterer = new GlobalGreedyClusteringPostprocessor(globalGreedyConfig);
		final List<LinkedPair<Record>> matches = classifyAndCluster(input, blocker, clusterer, threshold);

		final Collection<Record> allRecords = input.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toList());
		final Set<Cluster> linkTable = dbConnection != null
				? PersistentLinkTableBuilder.build(matches, allRecords, clusterFactory, dbConnection, persistenceProgress)
				: LinkTableBuilder.build(matches, allRecords);

		return buildOutcome(input, matches, linkTable);
	}

	/**
	 * Esegue blocking, classificazione a soglia e clustering CLIP sul grafo
	 * N-ario unico, poi persiste la Link Table su database (stesso builder di
	 * {@link #runMscdAp}, DB dedicato diverso). Dedicato allo scenario in cui
	 * tutte le party sono duplicate-free (Clean).
	 *
	 * @param input          record di tutti i party, tutti duplicate-free,
	 *                       già codificati come RBF
	 * @param blocker        strategia di blocking da usare
	 * @param clipConfig     configurazione per {@link ClipClusteringPostprocessor}
	 * @param threshold      soglia di similarità Jaccard per la classificazione
	 *                       match / non-match
	 * @param clusterFactory factory usata per gli eventuali nuovi cluster
	 * @param dbConnection   connessione al DB dedicato a CLIP, o {@code null} se
	 *                       {@code persistence.enabled=false} in config (vedi
	 *                       {@link #runMscdAp} per il dettaglio)
	 * @return la Link Table risultante e le metriche di qualità rispetto al
	 *         ground truth
	 */
	public LinkageOutcome runClip(Map<Party, Collection<Record>> input, Blocker blocker, ClipConfig clipConfig,
			double threshold, ClusterFactory clusterFactory, DbConnection dbConnection) {
		final MultipartiteClusteringStrategy clusterer = new ClipClusteringPostprocessor(clipConfig);
		final List<LinkedPair<Record>> matches = classifyAndCluster(input, blocker, clusterer, threshold);

		final Collection<Record> allRecords = input.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toList());
		final Set<Cluster> linkTable = dbConnection != null
				? PersistentLinkTableBuilder.build(matches, allRecords, clusterFactory, dbConnection, persistenceProgress)
				: LinkTableBuilder.build(matches, allRecords);

		return buildOutcome(input, matches, linkTable);
	}

	/**
	 * Esegue blocking, classificazione a soglia e clustering Markov (MCL)
	 * sul grafo N-ario unico. MCL non è persistente (come lo erano SBM/SM in
	 * precedenza): nessuna chiamata a {@code DbConnection}, ogni run
	 * ricalcola da zero e nessun {@code Cluster.id} è stabile tra run.
	 *
	 * @param input     record di tutti i party (2 o più, tutte dirty), già
	 *                  codificati come RBF
	 * @param blocker   strategia di blocking da usare
	 * @param mclConfig configurazione per {@link MarkovClusteringPostprocessor}
	 * @param threshold soglia di similarità Jaccard per la classificazione match /
	 *                  non-match
	 * @return la Link Table risultante e le metriche di qualità rispetto al
	 *         ground truth
	 */
	public LinkageOutcome runMcl(Map<Party, Collection<Record>> input, Blocker blocker, MclConfig mclConfig,
			double threshold) {
		final MultipartiteClusteringStrategy clusterer = new MarkovClusteringPostprocessor(mclConfig);
		final List<LinkedPair<Record>> matches = classifyAndCluster(input, blocker, clusterer, threshold);

		final Collection<Record> allRecords = input.values().stream().flatMap(Collection::stream)
				.collect(Collectors.toList());
		final Set<Cluster> linkTable = LinkTableBuilder.build(matches, allRecords);

		return buildOutcome(input, matches, linkTable);
	}

	/**
	 * Blocking + classificazione + clustering sul grafo N-ario unico,
	 * condiviso da {@link #runMscdAp} e {@link #runMcl}: l'unica differenza
	 * tra le due varianti è quale {@link MultipartiteClusteringStrategy} si
	 * applica e quale builder chiude infine sulla Link Table (persistente vs
	 * in memoria).
	 */
	private List<LinkedPair<Record>> classifyAndCluster(Map<Party, Collection<Record>> input, Blocker blocker,
			MultipartiteClusteringStrategy clusterer, double threshold) {
		long phaseStart = System.nanoTime();
		System.out.println("=== Valutazione del blocking in corso ===");
		final Collection<Block> blocks = blocker.getBlocks(input);
		final long maxComparisons = PerformanceMetrics.getMaxComparisons(input, ComparisonStrategy.SOURCE_CONSISTENT);
		final long expectedMatches = countGroundTruthMatches(input);
		lastBlockingEvaluation = new BlockingEvaluator(new IdEqualityTrueMatchChecker())
				.evaluate(blocks, maxComparisons, expectedMatches);
		System.out.println("=== Valutazione del blocking completata in " + elapsedMillis(phaseStart) + " ms ===");

		phaseStart = System.nanoTime();
		System.out.println("=== Classificazione (calcolo similarità) in corso ===");
		final LinkageResult<Record> linkageResult = classify(input, blocker, threshold);
		System.out.println("=== Classificazione completata in " + elapsedMillis(phaseStart) + " ms ===");

		phaseStart = System.nanoTime();
		System.out.println("=== Costruzione del grafo di similarità in corso ===");
		final MultiPartiteSimilarityGraph graph = MultiPartiteSimilarityGraph.from(linkageResult);
		System.out.println("=== Grafo di similarità costruito in " + elapsedMillis(phaseStart) + " ms ===");
		System.out.println("=== Clustering in corso (include il calcolo delle componenti connesse) ===");
		final long clusteringStartNanos = System.nanoTime();
		clusterer.setProgressListener(clusteringProgress);
		final List<LinkedPair<Record>> matches = clusterer.cluster(graph);
		lastClusteringElapsedNanos = System.nanoTime() - clusteringStartNanos;
		onClusteringFinished.run();
		return matches;
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}

	/**
	 * Tempo, in nanosecondi, impiegato dalla sola chiamata
	 * {@link MultipartiteClusteringStrategy#cluster(MultiPartiteSimilarityGraph)}
	 * dell'ultima invocazione di {@code runXxx(...)} su questa istanza —
	 * esclude blocking, classificazione e persistenza su DB.
	 */
	public long getLastClusteringElapsedNanos() {
		return lastClusteringElapsedNanos;
	}

	/**
	 * Valutazione del blocking (RR/PC/PQ, vedi {@link BlockingEvaluator})
	 * dell'ultima invocazione di {@code runXxx(...)} su questa istanza,
	 * calcolata sull'output grezzo del blocking (prima di similarity
	 * calculation e classificazione).
	 */
	public BlockingEvaluationResult getLastBlockingEvaluation() {
		return lastBlockingEvaluation;
	}

	private LinkageOutcome buildOutcome(Map<Party, Collection<Record>> input,
			List<LinkedPair<Record>> aggregatedMatches, Set<Cluster> linkTable) {
		final TrueMatchChecker trueMatchChecker = new IdEqualityTrueMatchChecker();
		final QualityEvaluator<Record> evaluator = new QualityEvaluator<>(trueMatchChecker);
		// Solo le coppie cross-party contano per TP/FP: countGroundTruthMatches
		// (il denominatore della recall) conta solo coppie di ground truth tra
		// party distinti, quindi il numeratore deve restare coerente — altrimenti
		// un duplicato within-party ritrovato correttamente gonfia la recall oltre 1.
		final List<LinkedPair<Record>> crossPartyMatches = aggregatedMatches.stream()
				.filter(p -> !p.getLeftRecord().getParty().getName().equals(p.getRight().getParty().getName()))
				.collect(Collectors.toList());
		evaluator.addMatches(crossPartyMatches);

		final long truePositives = evaluator.getTruePositives();
		final long falsePositives = evaluator.getFalsePositives();
		final long totalTrueMatches = countGroundTruthMatches(input);

		final double recall = QualityMetrics.getRecall(truePositives, totalTrueMatches);
		final double precision = QualityMetrics.getPrecision(truePositives, truePositives + falsePositives);
		final double fMeasure = QualityMetrics.getFMeasure(recall, precision);

		return new LinkageOutcome(linkTable, aggregatedMatches, truePositives, falsePositives, totalTrueMatches,
				recall, precision, fMeasure);
	}

	/**
	 * Esegue solo blocking + classificazione a soglia (nessun postprocessing);
	 * usato internamente da {@link #classifyAndCluster} cosi' la stessa
	 * classificazione grezza puo' alimentare {@link MultiPartiteSimilarityGraph}
	 * indipendentemente dalla strategia di clustering scelta.
	 *
	 * @param input     record di tutti i party
	 * @param blocker   strategia di blocking da usare
	 * @param threshold soglia di similarità Jaccard
	 * @return il {@code LinkageResult} grezzo, non ancora sottoposto a
	 *         postprocessing 1:1
	 */
	private LinkageResult<Record> classify(Map<Party, Collection<Record>> input, Blocker blocker, double threshold) {
		final ComparisonStrategy comparisonStrategy = ComparisonStrategy.SOURCE_CONSISTENT;

		final RecordSimilarityCalculator similarityCalculator = new BaseRecordSimilarityCalculator(
				List.of(new BitSetAttributeSimilarityCalculator(List.of(BinarySimilarity.JACCARD_SIMILARITY))));

		final SimilarityVectorFlattener flattener = new BaseSimilarityVectorFlattener(
				List.of(DoubleListAggregator.FIRST));
		final FlatSimilarityVectorAggregator flatAggregator = new BaseSimilarityVectorAggregator(
				DoubleListAggregator.FIRST);
		final SimilarityVectorAggregator aggregator = new SimilarityVectorAggregator(flattener, flatAggregator);

		final Classificator classificator = new ThresholdClassificator(threshold, aggregator);

		final MatchStrategyFactory<Record> matchFactory = new SimilarityGraphMatchStrategyFactory<>();
		final NonMatchStrategyFactory<Record> nonMatchFactory = new IgnoreNonMatchesStrategyFactory<>();
		final LinkageResultPartitionFactory<Record> partitionFactory = new LinkageResultPartitionFactory<>(
				matchFactory, nonMatchFactory);
		final BatchSimilarityClassification similarityClassification = new BatchSimilarityClassification(
				comparisonStrategy, similarityCalculator, classificator, RedundancyCheckStrategy.MATCH_TWICE,
				partitionFactory);
		similarityClassification.setProgressListener(classificationProgress);
		final ThresholdClassificationRefinement thresholdRefinement = new NoThresholdRefinement();

		// Nessun postprocessing qui: il clustering vero e proprio viene applicato
		// sul grafo N-ario in classifyAndCluster(), cosi' la stessa classificazione
		// grezza puo' essere clusterizzata con strategie diverse senza ripetere
		// blocking e classificazione.
		final PostprocessingStrategy<Record> noPostprocessing = new PostprocessingStrategy<>();
		noPostprocessing.setPostprocessor(LinkageConstraint.ONE_TO_ONE, new NoPostprocessor<>());
		noPostprocessing.setPostprocessor(LinkageConstraint.MANY_TO_ONE, new NoPostprocessor<>());
		noPostprocessing.setPostprocessor(LinkageConstraint.ONE_TO_MANY, new NoPostprocessor<>());
		noPostprocessing.setPostprocessor(LinkageConstraint.MANY_TO_MANY, new NoPostprocessor<>());

		final Matcher<Record> matcher = new BatchMatcher(blocker, similarityClassification, thresholdRefinement,
				noPostprocessing);
		return matcher.match(input);
	}

	/**
	 * Conta le coppie di ground truth (stesso GLOBAL_ID) tra ogni combinazione
	 * di party distinti, indipendentemente da quali coppie il matcher abbia
	 * effettivamente trovato: serve come denominatore della recall.
	 */
	private static long countGroundTruthMatches(Map<Party, Collection<Record>> input) {
		final List<Party> parties = new ArrayList<>(input.keySet());
		long total = 0;
		for (int i = 0; i < parties.size(); i++) {
			for (int j = i + 1; j < parties.size(); j++) {
				for (final Record left : input.get(parties.get(i))) {
					for (final Record right : input.get(parties.get(j))) {
						if (left.getGlobalId().equals(right.getGlobalId())) {
							total++;
						}
					}
				}
			}
		}
		return total;
	}
}
