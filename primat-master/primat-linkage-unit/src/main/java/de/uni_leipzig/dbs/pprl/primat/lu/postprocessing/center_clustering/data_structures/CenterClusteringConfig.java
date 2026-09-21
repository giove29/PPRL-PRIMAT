/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures;

import java.io.Serializable;

/**
 * Configurazione per {@link de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.CenterClusteringEngine},
 * sibling di {@code MclConfig}/{@code ApConfig}.
 */
public class CenterClusteringConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Soglia opzionale di similarità per l'assegnazione centro-foglia.
	 * {@code null} (default) accetta ogni arco già presente nel grafo, dato
	 * che gli archi sono già filtrati a monte dalla soglia di classificazione
	 * (non duplichiamo quella soglia qui).
	 */
	private Double centerAssignmentThreshold;

	public CenterClusteringConfig() {
		this.centerAssignmentThreshold = null;
	}

	/**
	 * @throws IllegalArgumentException se {@code centerAssignmentThreshold} è fuori da [0,1]
	 */
	public void checkConfigCorrectness() {
		if (centerAssignmentThreshold != null && (centerAssignmentThreshold < 0d || centerAssignmentThreshold > 1d)) {
			throw new IllegalArgumentException("centerAssignmentThreshold must be in [0,1]");
		}
	}

	public Double getCenterAssignmentThreshold() {
		return centerAssignmentThreshold;
	}

	public void setCenterAssignmentThreshold(Double centerAssignmentThreshold) {
		this.centerAssignmentThreshold = centerAssignmentThreshold;
	}
}
