package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

public class PrecisionStructureOneToOne extends PrecisionEstimation {


    protected PrecisionStructureOneToOne(String name, int foundMatches) {
        super(name, foundMatches);
    }

    public PrecisionStructureOneToOne(int foundMatches) {
        this("oneToOne", foundMatches);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        int target = similarityGraph.targetVertices() - similarityGraph.unmappedTargetVertices();
        this.tps = Math.min(source, target);
        return QualityMetrics.getPseudoPrecisionMin(source, target, this.foundMatches);
    }


}
