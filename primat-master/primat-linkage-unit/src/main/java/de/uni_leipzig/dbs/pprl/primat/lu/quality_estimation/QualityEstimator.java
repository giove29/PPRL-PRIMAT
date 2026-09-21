package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.MapUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.LinkStrength;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision.PrecisionEstimation;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall.RecallEstimation;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;

public class QualityEstimator {

    private Map<String, DescriptiveStatistics> overlapStatistics;

    private Map<String, Double> estimationResult;

    public QualityEstimator(Map<String, DescriptiveStatistics> estimations) {
        estimationResult = new LinkedHashMap<>();
        this.overlapStatistics = estimations;

    }
    public QualityEstimator() {
        estimationResult = new LinkedHashMap<>();
        this.overlapStatistics = new LinkedHashMap<>();
        DescriptiveStatistics defStat = new DescriptiveStatistics();
        defStat.addValue(1);
    }

    private double estimateTPByProb(SimilarityGraph<Record> simGraph) {
        double estTP = 0.0;
        final SimilarityGraph<Record> filteredGraph = this.filterWeakLinks(simGraph);
        for (SimilarityVector sv : filteredGraph.getGraph().edgeSet()) {
            double aggSimA = 0;
            double aggSimB = 0;
            //outgouing edges from source node of sv

            for (SimilarityVector svOut : simGraph.getGraph().edgesOf(simGraph.getEdgeSource(sv))) {
                aggSimA += svOut.getAggregatedValue();
            }

            for (SimilarityVector svIn : simGraph.getGraph().edgesOf(simGraph.getEdgeTarget(sv))) {
                aggSimB += svIn.getAggregatedValue();
            }

            double pA = sv.getAggregatedValue() / aggSimA;
            double pB = sv.getAggregatedValue() / aggSimB;
            estTP += (pA * pB);
        }
        return estTP;
    }


    public void computeQuality(SimilarityGraph<Record> simGraph) {
        estimationResult = new LinkedHashMap<>();
        int links = simGraph.links();
        System.out.println("similarity graph with " + links + " vertices");
        double estTP = this.estimateTPByProb(simGraph);
        int source = simGraph.sourceVertices() - simGraph.unmappedSourceVertices();
        int target = simGraph.targetVertices() - simGraph.unmappedTargetVertices();

        int sourceAll = simGraph.getSource().size();
        int targetAll = simGraph.getTarget().size();
        List<Double> cryptoSetsRecall = new ArrayList<>();
        for(Map.Entry<String, DescriptiveStatistics> stat : this.overlapStatistics.entrySet()){
            cryptoSetsRecall.add(QualityMetrics.getRecall((long) estTP, (long) stat.getValue().getMean()));
        }



        System.out.println(estTP);
        System.out.println("BEFORE");
        for (int i = 0; i < cryptoSetsRecall.size(); i++){
            System.out.println("P-Recall crypto_prob_"+ i +": " + cryptoSetsRecall.get(i));
            estimationResult.put("P_Recall_crypto_prop_"+i, cryptoSetsRecall.get(i));
        }
        List<Double> cryptoSetsRecall2 = new ArrayList<>();
        for(Map.Entry<String, DescriptiveStatistics> stat : this.overlapStatistics.entrySet()){
            cryptoSetsRecall2.add(QualityMetrics.getRecall((long) Math.min(source, target), (long) stat.getValue().getMean()));
        }
        for (int i = 0; i < cryptoSetsRecall2.size(); i++){
            System.out.println("P-Recall crypto 1:1_"+ i +": " + cryptoSetsRecall2.get(i));
            estimationResult.put("P_Recall_crypto_1:1_"+i, cryptoSetsRecall2.get(i));
        }
        List<Double> cryptoSetsRecall3 = new ArrayList<>();
        for(Map.Entry<String, DescriptiveStatistics> stat : this.overlapStatistics.entrySet()){
            cryptoSetsRecall3.add(QualityMetrics.getRecall(source + target, (long) (2 * stat.getValue().getMean())));
        }
        for (int i = 0; i < cryptoSetsRecall3.size(); i++){
            System.out.println("P-Recall crypto 1:n_"+ i +": " + cryptoSetsRecall3.get(i));
            estimationResult.put("P_Recall_crypto_1:n_"+i, cryptoSetsRecall3.get(i));
        }


        estimationResult.put("P_Recall_1", QualityMetrics.getPseudoRecall(links, sourceAll, targetAll));
        estimationResult.put("P_Recall_2", QualityMetrics.getPseudoRecallAlt(source, target, sourceAll, targetAll));
        estimationResult.put("P_Recall_3", QualityMetrics.getPseudoRecallAltMin(source, target, sourceAll, targetAll));

        System.out.println("P-Recall1: " + QualityMetrics.getPseudoRecall(links, sourceAll, targetAll));
        System.out.println("P-Recall2: " + QualityMetrics.getPseudoRecallAlt(source, target, sourceAll, targetAll));
        System.out.println("P-Recall3: " + QualityMetrics.getPseudoRecallAltMin(source, target, sourceAll, targetAll));


        System.out.println("P-Precision1_base: " + QualityMetrics.getPseudoPrecision(source, links));
        System.out.println("P-Precision_1:1: " + QualityMetrics.getPseudoPrecisionMin(source, target, links));
        System.out.println("P-Precision_1:n: " + QualityMetrics.getPseudoPrecisionSum(source, target, links));
        System.out.println("P-PrecisionProb: " + QualityMetrics.getPseudoPrecision((long) estTP, links));

        estimationResult.put("P-Precision1_base", QualityMetrics.getPseudoPrecision(source, links));
        estimationResult.put("P-Precision_1:1", QualityMetrics.getPseudoPrecisionMin(source, target, links));
        estimationResult.put("P-Precision_1:n", QualityMetrics.getPseudoPrecisionSum(source, target, links));
        estimationResult.put("P-PrecisionProb", QualityMetrics.getPseudoPrecision((long) estTP, links));
    }

    public void computePrecision(SimilarityGraph<Record> simGraph, List<PrecisionEstimation> precFunctions) {

        int links = simGraph.links();
        System.out.println("similarity graph with " + links + " vertices");

        for (PrecisionEstimation precEst: precFunctions) {
            double prec = precEst.estimateQuality(simGraph);
            estimationResult.put(precEst.getName(), prec);

        }
    }

    public void computeRecall(SimilarityGraph<Record> simGraph, List<RecallEstimation> recallEstimations) {
        for(RecallEstimation estimation: recallEstimations) {
            estimationResult.put(estimation.getName(), estimation.estimateQuality(simGraph));
        }
    }




    public void computeQuality(LinkageResult<Record> linkageResult) {
        estimationResult = new HashMap<>();
        final PartyPair partyPairAB = new PartyPair(new Party("A"), new Party("B"));
        final LinkageResultPartition<Record> linkResPart = linkageResult.getPartition(partyPairAB);
        final MatchStrategy<Record> matchStrat = linkResPart.getMatchStrategy();
        final SimilarityGraph<Record> simGraph = ((SimilarityGraphMatchStrategy<Record>) matchStrat)
                .getSimilarityGraph();
        this.computeQuality(simGraph);
        int overlappingRecs = 0;
        int overlappingNonIdenticalRecs = 0;
        int overlappingRecsButNoMatch = 0;

        for (final LinkedPair<Record> cleanMatch : matchStrat.getMatches()) {

            // System.out.println("SimVec: " + cleanMatch.getSimVec());
            if (cleanMatch.getSimVec().getSimilarities(0).get(1) == 1.0d) {
                overlappingRecs++;

                if (!cleanMatch.getLeftRecord().getId().equals(cleanMatch.getRightLinkable().getUniqueIdentifier())) {
                    overlappingRecsButNoMatch++;
                }

                if (cleanMatch.getSimVec().getSimilarities(0).get(0) < 1.0d) {
                    overlappingNonIdenticalRecs++;
                }
            }
        }
        System.out.println("#Overlapping Recs: " + overlappingRecs);
        System.out.println("#Overlapping Non Identical Recs: " + overlappingNonIdenticalRecs);
        System.out.println("Overlapping But No Match: " + overlappingRecsButNoMatch);
        // */
    }

    private SimilarityGraph<Record> filterWeakLinks(SimilarityGraph<Record> simGraph) {
        final Set<SimilarityVector> edges = simGraph.edgeSet();
        final Map<SimilarityVector, Double> edgesPrio = new HashedMap<>(edges.size());
        final Set<SimilarityVector> weakLinks = new HashSet<>();

        for (final SimilarityVector edge : edges) {
            final Record source = simGraph.getEdgeSource(edge);
            final List<SimilarityVector> sourceMaxLinks = simGraph.sortedEdgesOf(source);
            final double sourceMaxSim = sourceMaxLinks.get(0).getAggregatedValue();
            final Record target = simGraph.getEdgeTarget(edge);
            final List<SimilarityVector> targetMaxLinks = simGraph.sortedEdgesOf(target);
            final double targetMaxSim = targetMaxLinks.get(0).getAggregatedValue();
            boolean leftMax = false;
            for (SimilarityVector sourceMaxLink: sourceMaxLinks) {
                if (sourceMaxSim == edge.getAggregatedValue() && sourceMaxSim == sourceMaxLink.getAggregatedValue()) {
                    leftMax = edge.equals(sourceMaxLink);
                    if (leftMax) {
                        break;
                    }
                }
            }
            boolean rightMax = false;
            for (SimilarityVector targetMaxLink: targetMaxLinks) {
                if (targetMaxSim == edge.getAggregatedValue() && targetMaxSim == targetMaxLink.getAggregatedValue()) {
                    rightMax = edge.equals(targetMaxLink);
                    if (rightMax) {
                        break;
                    }
                }
            }
            final LinkStrength linkStrength = LinkStrength.getLinkStrength(leftMax, rightMax);

            if (linkStrength.equals(LinkStrength.WEAK)) {
                weakLinks.add(edge);
            }
        }
        SimilarityGraph<Record> filteredGraph  = simGraph.clone();
        filteredGraph.removeAllEdges(weakLinks);
        return filteredGraph;
    }

    public Map<String, Double> getEstimationResult() {
        return estimationResult;
    }
}
