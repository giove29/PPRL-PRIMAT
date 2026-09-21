/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;

/**
 * Strategia di clustering che opera direttamente su un grafo di similarità
 * multi-partito unico (N sorgenti simultanee), a differenza di
 * {@link Postprocessor}, che opera per singola {@code PartyPair}.
 */
public interface MultipartiteClusteringStrategy {

	List<LinkedPair<Record>> cluster(MultiPartiteSimilarityGraph graph);
}
