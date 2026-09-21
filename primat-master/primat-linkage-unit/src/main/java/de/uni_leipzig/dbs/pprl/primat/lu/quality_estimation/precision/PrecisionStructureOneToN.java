package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

public class PrecisionStructureOneToN extends PrecisionEstimation {


    protected PrecisionStructureOneToN(String name, int foundMatches) {
        super(name, foundMatches);
    }

    public PrecisionStructureOneToN(int foundMatches) {
        this("oneToN", foundMatches);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        int target = similarityGraph.targetVertices() - similarityGraph.unmappedTargetVertices();
        this.tps = source + target;
        return QualityMetrics.getPseudoPrecisionSum(source, target, foundMatches);
    }

}
