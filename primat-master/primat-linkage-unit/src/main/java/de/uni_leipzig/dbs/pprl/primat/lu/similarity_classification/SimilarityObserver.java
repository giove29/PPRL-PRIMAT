/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;

/**
 * Osserva la similarita' aggregata di ogni confronto eseguito da
 * {@link BatchSimilarityClassification}, match o non-match, senza influire
 * sulla classificazione. Con {@code RedundancyCheckStrategy.MATCH_TWICE} la
 * stessa coppia puo' essere notificata piu' volte (una per blocco condiviso):
 * la deduplicazione spetta all'osservatore.
 */
@FunctionalInterface
public interface SimilarityObserver {

	SimilarityObserver NOOP = (left, right, similarity) -> {
	};

	void observe(Record left, Record right, double similarity);
}
