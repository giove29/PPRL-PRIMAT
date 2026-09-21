/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.global_greedy.data_structures;

import java.io.Serializable;

/**
 * Configurazione per
 * {@link de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.global_greedy.GlobalGreedyClusteringPostprocessor},
 * sibling di {@code CenterClusteringConfig}/{@code MclConfig}.
 */
public class GlobalGreedyConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Soglia opzionale di similarità sotto la quale un arco viene ignorato.
	 * {@code null} (default) accetta ogni arco già presente nel grafo, dato
	 * che gli archi sono già filtrati a monte dalla soglia di classificazione
	 * (non duplichiamo quella soglia qui).
	 */
	private Double mergeThreshold;

	public GlobalGreedyConfig() {
		this.mergeThreshold = null;
	}

	/**
	 * @throws IllegalArgumentException se {@code mergeThreshold} è fuori da [0,1]
	 */
	public void checkConfigCorrectness() {
		if (mergeThreshold != null && (mergeThreshold < 0d || mergeThreshold > 1d)) {
			throw new IllegalArgumentException("mergeThreshold must be in [0,1]");
		}
	}

	public Double getMergeThreshold() {
		return mergeThreshold;
	}

	public void setMergeThreshold(Double mergeThreshold) {
		this.mergeThreshold = mergeThreshold;
	}
}
