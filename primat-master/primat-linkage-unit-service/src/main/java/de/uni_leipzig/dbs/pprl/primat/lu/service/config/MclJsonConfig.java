/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/** Sezione {@code mcl} del JSON, mirror dei campi tuning di {@code MclConfig}. */
public class MclJsonConfig {

	private Integer expansionPower;
	private Double inflationExponent;
	private Double pruningThreshold;
	private Integer maxIterations;
	private Double convergenceEpsilon;
	private Double selfLoopWeight;

	public Integer getExpansionPower() {
		return expansionPower;
	}

	public Double getInflationExponent() {
		return inflationExponent;
	}

	public Double getPruningThreshold() {
		return pruningThreshold;
	}

	public Integer getMaxIterations() {
		return maxIterations;
	}

	public Double getConvergenceEpsilon() {
		return convergenceEpsilon;
	}

	public Double getSelfLoopWeight() {
		return selfLoopWeight;
	}
}
