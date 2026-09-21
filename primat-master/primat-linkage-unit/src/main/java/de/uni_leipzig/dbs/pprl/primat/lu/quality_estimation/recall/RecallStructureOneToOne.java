package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

public class RecallStructureOneToOne extends RecallEstimation {


    protected RecallStructureOneToOne(String name, long tps) {
        super(name, tps);
    }

    public RecallStructureOneToOne() {
        this("PR_ALT_MIN", 0);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int source = similarityGraph.sourceVertices() - similarityGraph.unmappedSourceVertices();
        int target = similarityGraph.targetVertices() - similarityGraph.unmappedTargetVertices();
        int sourceAll = similarityGraph.getSource().size();
        int targetAll = similarityGraph.getTarget().size();
        this.tps = source + target;
        this.realMatches = Math.min(sourceAll, targetAll);
        return Math.min(1d, QualityMetrics.getPseudoRecallAltMin(source, target, sourceAll, targetAll));
    }
}
