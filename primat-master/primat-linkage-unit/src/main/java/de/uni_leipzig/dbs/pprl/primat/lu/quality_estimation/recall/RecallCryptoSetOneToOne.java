package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class RecallCryptoSetOneToOne extends RecallCryptoSetBased {

    public RecallCryptoSetOneToOne(String keyDef, DescriptiveStatistics overlap) {
        super("1:1", keyDef, 0, overlap);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        int target = similarityGraph.targetVertices() - similarityGraph.unmappedTargetVertices();
        this.tps = Math.min(source, target);
        double overlap = overlapStatistics.getMean();
//        if(isDuplicateFree) {
//            overlap = Math.min(overlapStatistics.getMean(), Math.min(source, target));
//        }
        System.out.println(overlap);

        return Math.min(1d, tps/overlap);

    }
}
