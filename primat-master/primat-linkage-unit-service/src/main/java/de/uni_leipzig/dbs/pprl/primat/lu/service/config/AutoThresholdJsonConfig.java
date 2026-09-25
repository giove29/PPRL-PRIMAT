/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Sezione opzionale {@code autoThreshold} del JSON della Linkage Unit: tuning
 * della soglia automatica ({@code "similarityThreshold": "auto_precision"} /
 * {@code "auto_recall"}).
 */
public class AutoThresholdJsonConfig {

	private Double epsilon;

	/** @return spostamento assoluto (in similarita' Jaccard) del ginocchio; {@code null} = default 0.03. */
	public Double getEpsilon() {
		return epsilon;
	}
}
