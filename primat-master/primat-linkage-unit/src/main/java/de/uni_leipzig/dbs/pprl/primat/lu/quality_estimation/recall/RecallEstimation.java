package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.QualityEstimate;

public abstract class RecallEstimation implements QualityEstimate {

    protected int realMatches;

    public double getTps() {
        return tps;
    }

    public void setTps(double tps) {
        this.tps = tps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected double tps;

    protected String name;

    protected boolean isDuplicateFree;

    protected RecallEstimation (String name, double tps) {
        this.name = "r_" + name;
        this.tps = tps;
        this.isDuplicateFree = true;
    }

    protected RecallEstimation (String name, double tps, boolean isDuplicateFree) {
        this.name = "r_" + name;
        this.tps = tps;
        this.isDuplicateFree = isDuplicateFree;
    }

    @Override
    public abstract double estimateQuality(SimilarityGraph<Record> similarityGraph);

}
