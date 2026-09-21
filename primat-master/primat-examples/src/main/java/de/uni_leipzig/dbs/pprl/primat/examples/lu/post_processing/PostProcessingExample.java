package de.uni_leipzig.dbs.pprl.primat.examples.lu.post_processing;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.DataSources;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.io.CSVGraphReader;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.AffinityPropagationPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

import java.io.IOException;
import java.util.*;

public class PostProcessingExample {

    private static int getMatchesWithinTheSame(List<Record> dataSet) {
        Map<String, Integer> duplicates = new HashMap<>();
        for(Record r: dataSet){
            Integer c = duplicates.putIfAbsent(r.getGlobalIdentifier(), 1);
            if (c != null) {
                duplicates.put(r.getGlobalId(), 1 + c);
            }
        }
        int matches = duplicates.values().stream().filter(c -> c>1).mapToInt(i ->(i*(i-1))/2).sum();
        return matches;
    }

    public static void main(String[] args) throws IOException {
        String inputPath = args[0];
        String edgePath = args[1];
        String nodePath = args[2];
        List<Record> records = DataSources.getAlmser(inputPath, nodePath);
        Map<Party, List<Record>> recordMap= new HashMap<>();
        Map<Party, Set<String>> globalIdentifierMap = new HashMap<>();
        for(Record r : records){
            if(recordMap.get(r.getParty())==null){
                recordMap.put(r.getParty(), new ArrayList<>());
                globalIdentifierMap.put(r.getParty(), new HashSet<>());
            }
            List<Record> recordList = recordMap.get(r.getParty());
            Set<String> globalIdentifiers = globalIdentifierMap.get(r.getParty());
            recordList.add(r);
            globalIdentifiers.add(r.getGlobalId());
        }
        List<String> cleanSrc = new ArrayList<>();
        ApConfig config = new ApConfig();
        for(Map.Entry<Party, List<Record>> e : recordMap.entrySet()) {
            Set<String> globalIdentifiers = globalIdentifierMap.get(e.getKey());
            if (globalIdentifiers.size() == e.getValue().size()) {
                cleanSrc.add(e.getKey().getName());
                config.addCleanSource(e.getKey().getName());
            }
        }
        CSVGraphReader graphReader = new CSVGraphReader();
        SimilarityGraph<Record> similarityGraph = graphReader.readGraph(edgePath, records);
        final MatchStrategyFactory<Record> matchFactory = new SimilarityGraphMatchStrategyFactory<>();
        final NonMatchStrategyFactory<Record> nonMatchFactory = new IgnoreNonMatchesStrategyFactory<>();
        final LinkageResultPartitionFactory<Record> linkResFac = new LinkageResultPartitionFactory<>(matchFactory,
                nonMatchFactory);
        LinkageResultPartition<Record> resultPartition = linkResFac.createLinkageResultPartition(
                new PartyPair(new Party("all"), new Party("all")));
        for (SimilarityVector e : similarityGraph.edgeSet())
            resultPartition.addMatch(similarityGraph.getEdgeSource(e), similarityGraph.getEdgeTarget(e), e, e.getAggregatedValue());
        double[] cleanSrcPrefs = new double[]{0.1};
        double[] cleanDirtyPrefs = new double[]{0.6};
        int numberOfMatches = getMatchesWithinTheSame(records);
        for (double prefClnSource :cleanSrcPrefs) {
            for(double prefDirtySource: cleanDirtyPrefs) {
                AffinityPropagationPostprocessor<Record> postprocessor = new AffinityPropagationPostprocessor<>(
                        prefClnSource, prefDirtySource, 0.5, config);
                postprocessor.clean(resultPartition);
                final TrueMatchChecker trueMatchChecker = new IdEqualityTrueMatchChecker();
                final QualityEvaluator<Record> evaluator = new QualityEvaluator<>(trueMatchChecker);
                evaluator.addMatches(resultPartition.getMatchStrategy());
                final long truePos = evaluator.getTruePositives();
                final long falsePos = evaluator.getFalsePositives();
                final long falseNeg = evaluator.getFalseNegatives();
                final double recall = QualityMetrics.getRecall(truePos, numberOfMatches);
                final double precision = QualityMetrics.getPrecision(truePos, truePos + falsePos);
                System.out.println(precision);
                System.out.println(recall);
                System.out.println(resultPartition.matches());
            }
        }

    }
}
