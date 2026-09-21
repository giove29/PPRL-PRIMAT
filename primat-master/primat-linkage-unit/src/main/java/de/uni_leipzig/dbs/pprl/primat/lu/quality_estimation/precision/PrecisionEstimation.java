package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.QualityEstimate;

public abstract class PrecisionEstimation implements QualityEstimate {

    protected int foundMatches;

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

    protected PrecisionEstimation (String name, int foundMatches) {
        this.foundMatches = foundMatches;
        this.name = "p_" + name;
    }

    @Override
    public abstract double estimateQuality(SimilarityGraph<Record> similarityGraph);


}
