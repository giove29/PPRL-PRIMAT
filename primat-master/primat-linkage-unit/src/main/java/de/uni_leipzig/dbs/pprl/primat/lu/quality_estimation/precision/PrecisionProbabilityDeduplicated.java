package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.LinkStrength;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrecisionProbabilityDeduplicated extends PrecisionEstimation{


    protected PrecisionProbabilityDeduplicated(String name, int foundMatches) {
        super(name, foundMatches);
    }

    public PrecisionProbabilityDeduplicated(int foundMatches) {
        this("probabilityDedup", foundMatches);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        double estTP = 0;
        final SimilarityGraph<Record> filteredGraph = this.filterWeakLinks(similarityGraph);
        for (SimilarityVector sv : filteredGraph.getGraph().edgeSet()) {
            double aggSimA = 0;
            double aggSimB = 0;
            //outgouing edges from source node of sv

            for (SimilarityVector svOut : similarityGraph.getGraph().edgesOf(similarityGraph.getEdgeSource(sv))) {
                aggSimA += svOut.getAggregatedValue();
            }

            for (SimilarityVector svIn : similarityGraph.getGraph().edgesOf(similarityGraph.getEdgeTarget(sv))) {
                aggSimB += svIn.getAggregatedValue();
            }

            double pA = sv.getAggregatedValue() / aggSimA;
            double pB = sv.getAggregatedValue() / aggSimB;
            estTP += (pA * pB);
        }
        this.tps = estTP;
        return tps/foundMatches;

    }

    private SimilarityGraph<Record> filterWeakLinks(SimilarityGraph<Record> simGraph) {
        final Set<SimilarityVector> edges = simGraph.edgeSet();
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
}
