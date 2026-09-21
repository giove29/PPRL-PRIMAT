package de.uni_leipzig.dbs.pprl.primat.examples.lu.quality_estimation;


import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.LshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.RandomHammingLshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.model.LinkageConstraint;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DoubleListAggregator;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.DataSources;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.PrivateIdKeyDefinitions;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.lsh.LshBlocker;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.ThresholdClassificator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.Matcher;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.NoPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxBothPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxRightPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.QualityEstimator;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision.*;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall.*;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity.BitSetAttributeSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.BaseRecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.RecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.BatchSimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.RedundancyCheckStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.SimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary.BinarySimilarity;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.*;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.StandardThresholdClassificationRefinement;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ThresholdClassificationRefinement;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class ExperimentNCVR {

    public static Map<Party, Collection<Record>> partitionRecords(List<Record> dataset){
        final Map<Party, Collection<Record>> result = new HashMap<>();
        for (final Record rec : dataset) {
            final Party party = rec.getParty();
            if (!result.containsKey(party)) {
                result.put(party, new ArrayList<>());
            }
            result.get(party).add(rec);
        }
        return result;
    }

    private static int getMatches(List<Record> dataSet, PartyPair partyPair) {
        int matches = 0;
        Set<String> duplicates = new HashSet<>();
        for(Record r: dataSet){
            if(r.getParty().getName().equals(partyPair.getLeftParty().getName()) ||
                    r.getParty().getName().equals(partyPair.getRightParty().getName())){
                if(duplicates.contains(r.getGlobalIdentifier())){
                    matches++;
                }else {
                    duplicates.add(r.getGlobalIdentifier());
                }
            }
        }
        return matches;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        final String inputPath = args[0];
        OverlapEstimation overlapEstimation = new OverlapEstimation();
        List<Record> dataset = DataSources.getNCVR(inputPath);

        List<List<ExtractorDefinition>> privateKeyExtractions = Arrays.asList(
                PrivateIdKeyDefinitions.get2FirstLettersFunction(),
                PrivateIdKeyDefinitions.get3FirstLettersFunction(),
                PrivateIdKeyDefinitions.getSoundexFunction()
        );
        Random rnd = new Random(42);
        Map<String, DescriptiveStatistics> manualEstimations = overlapEstimation.estimateOverlap(dataset, privateKeyExtractions, rnd,
                10, "A", "B");



        dataset = DataSources.getEncodedNCVR(dataset);
        dataset.stream().forEach(i -> i.getIdAttribute().setValue(i.getId() + "-" + i.getParty().getName()));

        final List<Record> datasetA = dataset.stream()
                .filter(r -> r.getPartyAttribute().getValue().getName().equals("A")).collect(Collectors.toList());
        final List<Record> datasetB = dataset.stream()
                .filter(r -> r.getPartyAttribute().getValue().getName().equals("B")).collect(Collectors.toList());

        final Map<Party, Collection<Record>> input = new HashMap<>();
        input.put(new Party("A"), datasetA);
        input.put(new Party("B"), datasetB);

        System.out.println("Party A:" + datasetA.size());
        System.out.println("Party B:" + datasetB.size());

        final ComparisonStrategy compStrat = ComparisonStrategy.SOURCE_CONSISTENT;

        final LshKeyGenerator keyGen = new RandomHammingLshKeyGenerator(16, 30, 1024, 42L);
        final Blocker blocker = new LshBlocker(keyGen);

        // */

        final RecordSimilarityCalculator simCalc = new BaseRecordSimilarityCalculator(
                List.of(new BitSetAttributeSimilarityCalculator(
                        List.of(BinarySimilarity.JACCARD_SIMILARITY, BinarySimilarity.OVERLAP_SIMILARITY))));

        final SimilarityVectorFlattener flattener = new BaseSimilarityVectorFlattener(
                List.of(DoubleListAggregator.FIRST));
        final FlatSimilarityVectorAggregator aggregator = new BaseSimilarityVectorAggregator(
                DoubleListAggregator.FIRST);
        final SimilarityVectorAggregator agg = new SimilarityVectorAggregator(flattener, aggregator);
        final Classificator classifier = new ThresholdClassificator(0.5d, agg);
        final MatchStrategyFactory<Record> matchFactory = new SimilarityGraphMatchStrategyFactory<>();
        final NonMatchStrategyFactory<Record> nonMatchFactory = new IgnoreNonMatchesStrategyFactory<>();
        final LinkageResultPartitionFactory<Record> linkResFac = new LinkageResultPartitionFactory<>(matchFactory,
                nonMatchFactory);
        final SimilarityClassification simClass = new BatchSimilarityClassification(compStrat, simCalc, classifier,
                RedundancyCheckStrategy.MATCH_TWICE, linkResFac);
        final ThresholdClassificationRefinement threshRef = new StandardThresholdClassificationRefinement();


        final PostprocessingStrategy<Record> postprocessor = new PostprocessingStrategy<>();
        postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_ONE, new MaxBothPostprocessor<>());
        postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_ONE, new MaxRightPostprocessor<>());
        postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_MANY, new NoPostprocessor<>());
        postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_MANY, new NoPostprocessor<>());

        final Matcher<Record> matcher = new BatchMatcher(blocker, simClass, null);
        SimilarityGraph<Record> similarityGraph;
        LinkageResult<Record> linkRes;
        linkRes = matcher.match(input);
        System.out.println("match records..");

        final TrueMatchChecker trueMatchChecker = new IdEqualityTrueMatchChecker();
        final QualityEvaluator<Record> evaluator = new QualityEvaluator<>(trueMatchChecker);

        final PartyPair partyPairAB = new PartyPair(new Party("A"), new Party("B"));

        final LinkageResultPartition<Record> linkResPart = linkRes.getPartition(partyPairAB);
        final MatchStrategy<Record> matchStrat = linkResPart.getMatchStrategy();
        final SimilarityGraph<Record> simGraph = ((SimilarityGraphMatchStrategy<Record>) matchStrat)
                .getSimilarityGraph();

        final LinkageResultPartition<Record> part = linkRes.getPartition(partyPairAB);
        evaluator.addMatches(part.getMatchStrategy().getMatches());
        final int matches = getMatches(dataset, partyPairAB);
        System.out.println("gold matches: " + matches);

        final double[] thresholds = { 0.5d, 0.55d, 0.6d, 0.65d, 0.7d, 0.75d, 0.8d, 0.85d, 0.9d, 0.95d, 1d };
        final double[] recallRes = new double[thresholds.length];
        final double[] precisionRes = new double[thresholds.length];
        final double[] fmeasureRes = new double[thresholds.length];
        Map<String, List<Double>> resultPseudoMeasures = new LinkedHashMap<>();
        List<Double> probTps =new ArrayList<>();
        List<Double> goldTps = new ArrayList<>();
        for (int i = 0; i < thresholds.length; i++) {
            final double t = thresholds[i];
            final SimilarityGraph<Record> filteredGraph = simGraph.getSimilarityFilteredSubgraph(t);
            List<PrecisionEstimation> precisionEstimations = new ArrayList<>();
            PrecisionEstimation probEstimation = new PrecisionProbabilityDeduplicated(filteredGraph.edges());
            precisionEstimations.addAll(Arrays.asList(new PrecisionOneToOneBase(filteredGraph.edges()),
                    new PrecisionStructureOneToOne(filteredGraph.edges()),
                    new PrecisionStructureOneToN(filteredGraph.edges()),
                    probEstimation
                    )
            );

            QualityEstimator estimator = new QualityEstimator(manualEstimations);
            estimator.computePrecision(filteredGraph, precisionEstimations);
            List<RecallEstimation> recallEstimations = new ArrayList<>();
            recallEstimations.addAll(Arrays.asList(
                   new RecallStructureNToM(),
                   new RecallStructureOneToOne(),
                    new RecallStructureOneToN()
            ));
            probTps.add(probEstimation.getTps());

            for (Map.Entry<String, DescriptiveStatistics> e : manualEstimations.entrySet()) {
                String name = e.getKey();
                recallEstimations.add(new RecallCryptoSetOneToN(name, e.getValue()));
                recallEstimations.add(new RecallCryptoSetOneToOne(name, e.getValue()));
                recallEstimations.add(new RecallCryptoSetProb(name, probEstimation.getTps(), e.getValue()));
            }
            estimator.computeRecall(filteredGraph, recallEstimations);
            Map<String, Double> qualityEstimations = estimator.getEstimationResult();
            for(Map.Entry<String, Double> e: qualityEstimations.entrySet()){
                List<Double> list = resultPseudoMeasures.get(e.getKey());
                if(list == null){
                    list = new ArrayList<>();
                    resultPseudoMeasures.put(e.getKey(), list);
                }
                if(!Double.isNaN(e.getValue()))
                    list.add(e.getValue());
            }

            final long truePos = evaluator.getTruePositivesFor(t);
            goldTps.add((double) truePos);
            final long falsePos = evaluator.getFalsePositivesFor(t);

            final double recall = QualityMetrics.getRecall(truePos, matches);
            recallRes[i] = recall;
            final double precision = QualityMetrics.getPrecision(truePos, truePos + falsePos);
            precisionRes[i] = precision;
            final double fmeasure = QualityMetrics.getFMeasure(recall, precision);
            fmeasureRes[i] = fmeasure;
        }

        Table qualityResults =
                Table
                        .create("QualityResults")
                        .addColumns(
                                DoubleColumn.create("Threshold", thresholds),
                                DoubleColumn.create("Recall", recallRes),
                                DoubleColumn.create("Precision", precisionRes),
                                DoubleColumn.create("F-Measure", fmeasureRes)
                        );
        for(Map.Entry<String, List<Double>> e: resultPseudoMeasures.entrySet()) {
            qualityResults.addColumns(DoubleColumn.create(e.getKey(), e.getValue().toArray(new Double[]{})));
        }
        qualityResults.addColumns(DoubleColumn.create("prob_tps", probTps.toArray(new Double[]{})));
        qualityResults.addColumns(DoubleColumn.create("gold_tps", goldTps.toArray(new Double[]{})));
        System.out.println(qualityResults);
    }
}
