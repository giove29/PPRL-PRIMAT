/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Sezione {@code mscdAp} del JSON, mirror dei campi tuning di {@code ApConfig}
 * (non include {@code cleanSources}: viene popolato a runtime
 * dall'orchestrator in base a {@code parties}, non dalla config).
 */
public class ApJsonConfig {

	private Integer maxApIteration;
	private Integer maxAdaptionIteration;
	private Integer convergenceIter;
	private Double dampingFactor;
	private Double dampingAdaptionStep;
	private Double allSameSimClusteringThreshold;
	private Integer noiseDecimalPlace;
	private Boolean singletonsForUnconvergedComponents;
	private PreferenceJsonConfig preference;

	public Integer getMaxApIteration() {
		return maxApIteration;
	}

	public Integer getMaxAdaptionIteration() {
		return maxAdaptionIteration;
	}

	public Integer getConvergenceIter() {
		return convergenceIter;
	}

	public Double getDampingFactor() {
		return dampingFactor;
	}

	public Double getDampingAdaptionStep() {
		return dampingAdaptionStep;
	}

	public Double getAllSameSimClusteringThreshold() {
		return allSameSimClusteringThreshold;
	}

	public Integer getNoiseDecimalPlace() {
		return noiseDecimalPlace;
	}

	public Boolean getSingletonsForUnconvergedComponents() {
		return singletonsForUnconvergedComponents;
	}

	public PreferenceJsonConfig getPreference() {
		return preference;
	}
}
