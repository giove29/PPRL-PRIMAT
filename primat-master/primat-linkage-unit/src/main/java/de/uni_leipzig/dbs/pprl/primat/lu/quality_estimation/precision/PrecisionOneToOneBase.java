package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

public class PrecisionOneToOneBase extends PrecisionEstimation {


    protected PrecisionOneToOneBase(String name, int foundMatches) {
        super(name, foundMatches);
    }

    public PrecisionOneToOneBase(int foundMatches) {
        this("oneToOneBase", foundMatches);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        this.tps = source;
        return QualityMetrics.getPseudoPrecision(source, foundMatches);

    }
}
