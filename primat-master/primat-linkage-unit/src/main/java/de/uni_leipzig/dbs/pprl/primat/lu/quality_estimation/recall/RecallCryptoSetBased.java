package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

public abstract class RecallCryptoSetBased extends RecallEstimation{

    protected final DescriptiveStatistics overlapStatistics;
    protected  final String keyDefinition;

    public RecallCryptoSetBased(String name, String keyDefinition, double tps, DescriptiveStatistics overlapStatistics, boolean isDuplicateFree) {
        super("cs_" + keyDefinition + "_" + name, tps);
        this.keyDefinition = keyDefinition;
        this.overlapStatistics = overlapStatistics;
        this.isDuplicateFree = isDuplicateFree;
    }

    public RecallCryptoSetBased(String name, String keyDefinition, double tps, DescriptiveStatistics overlapStatistics) {
        super("cs_" + keyDefinition + "_" + name, tps);
        this.keyDefinition = keyDefinition;
        this.overlapStatistics = overlapStatistics;
        this.isDuplicateFree = true;
    }



}
