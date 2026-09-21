package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

public interface QualityEstimate {

    double estimateQuality(SimilarityGraph<Record> similarityGraph);

}
