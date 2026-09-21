package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class RecallCryptoSetOneToN extends RecallCryptoSetBased {

    public RecallCryptoSetOneToN(String keyDef, DescriptiveStatistics statistics) {
        super("1:n", keyDef, 0, statistics);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        int target = similarityGraph.targetVertices() - similarityGraph.unmappedTargetVertices();
        double overlap = overlapStatistics.getMean();

//        if(isDuplicateFree) {
//            overlap = Math.min(overlapStatistics.getMean(), Math.min(source, target));
//        }

        return Math.min(1, (source + target)/(2*overlap));

    }
}
