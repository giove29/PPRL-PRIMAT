package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

public class RecallStructureOneToN extends RecallEstimation {

    protected RecallStructureOneToN(String name, long tps) {
        super(name, tps);
    }

    public RecallStructureOneToN() {
        this("PR_ALT", 0);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        int target = similarityGraph.targetVertices() - similarityGraph.unmappedTargetVertices();
        int sourceAll = similarityGraph.getSource().size();
        int targetAll = similarityGraph.getTarget().size();
        this.tps = source +target;
        this.realMatches = sourceAll + targetAll;
        return Math.min(1d, QualityMetrics.getPseudoRecallAlt(source, target, sourceAll, targetAll));
    }
}
