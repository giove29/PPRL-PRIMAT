/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Sezione opzionale {@code range} del JSON della Linkage Unit: intervallo di
 * soglie da testare con {@code "similarityThreshold": "range"} (solo
 * testing, mai persistente).
 */
public class RangeJsonConfig {

	private Double from;
	private Double to;
	private Double step;

	/** @return estremo inferiore dell'intervallo; {@code null} = default 0.5. */
	public Double getFrom() {
		return from;
	}

	/** @return estremo superiore dell'intervallo; {@code null} = default 0.9. */
	public Double getTo() {
		return to;
	}

	/** @return passo tra una soglia e la successiva; {@code null} = default 0.1. */
	public Double getStep() {
		return step;
	}
}
