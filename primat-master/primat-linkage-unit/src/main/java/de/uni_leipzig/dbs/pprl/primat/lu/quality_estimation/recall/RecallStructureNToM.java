package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

public class RecallStructureNToM extends RecallEstimation {


    protected RecallStructureNToM(String name, long tps) {
        super(name, tps);
    }

    public RecallStructureNToM() {
        this("PR", 0);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        int links = similarityGraph.edges();
        int sourceAll = similarityGraph.getSource().size();
        int targetAll = similarityGraph.getTarget().size();
        this.tps = links;
        this.realMatches = Math.min(sourceAll, targetAll);
        return Math.min(1d, QualityMetrics.getPseudoRecall(links, sourceAll, targetAll));
    }
}
