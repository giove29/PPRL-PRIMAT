package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class RecallCryptoSetProb extends RecallCryptoSetBased {

    public RecallCryptoSetProb(String keyDef, double tps, DescriptiveStatistics overlap) {
        super("prob", keyDef, tps, overlap);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        int target = similarityGraph.targetVertices() - similarityGraph.unmappedTargetVertices();
        double overlap = overlapStatistics.getMean();
//        if(isDuplicateFree) {
//            overlap = Math.min(overlapStatistics.getMean(), Math.min(source, target));
//        }

        return Math.min(1d,tps/overlap);

    }
}
