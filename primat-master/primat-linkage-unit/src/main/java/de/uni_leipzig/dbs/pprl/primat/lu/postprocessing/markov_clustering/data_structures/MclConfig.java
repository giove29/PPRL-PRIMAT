/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures;

import java.io.Serializable;

/**
 * Configurazione per {@link de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.MarkovClusteringEngine},
 * sibling di {@code ApConfig}.
 */
public class MclConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	private int expansionPower;
	private double inflationExponent;
	private double pruningThreshold;
	private int maxIterations;
	private double convergenceEpsilon;
	private double selfLoopWeight;

	public MclConfig() {
		this.expansionPower = 2;
		this.inflationExponent = 2.0;
		this.pruningThreshold = 1e-4;
		this.maxIterations = 100;
		this.convergenceEpsilon = 1e-6;
		this.selfLoopWeight = 1.0;
	}

	/**
	 * @throws IllegalArgumentException se un parametro è fuori dal range consentito
	 */
	public void checkConfigCorrectness() {
		if (expansionPower < 2) {
			throw new IllegalArgumentException("expansionPower must be >= 2");
		}
		if (inflationExponent <= 1) {
			throw new IllegalArgumentException("inflationExponent must be > 1");
		}
		if (pruningThreshold < 0) {
			throw new IllegalArgumentException("pruningThreshold must be >= 0");
		}
		if (maxIterations <= 0) {
			throw new IllegalArgumentException("maxIterations must be > 0");
		}
		if (convergenceEpsilon < 0) {
			throw new IllegalArgumentException("convergenceEpsilon must be >= 0");
		}
		if (selfLoopWeight < 0) {
			throw new IllegalArgumentException("selfLoopWeight must be >= 0");
		}
	}

	public int getExpansionPower() {
		return expansionPower;
	}

	public void setExpansionPower(int expansionPower) {
		this.expansionPower = expansionPower;
	}

	public double getInflationExponent() {
		return inflationExponent;
	}

	public void setInflationExponent(double inflationExponent) {
		this.inflationExponent = inflationExponent;
	}

	public double getPruningThreshold() {
		return pruningThreshold;
	}

	public void setPruningThreshold(double pruningThreshold) {
		this.pruningThreshold = pruningThreshold;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public double getConvergenceEpsilon() {
		return convergenceEpsilon;
	}

	public void setConvergenceEpsilon(double convergenceEpsilon) {
		this.convergenceEpsilon = convergenceEpsilon;
	}

	public double getSelfLoopWeight() {
		return selfLoopWeight;
	}

	public void setSelfLoopWeight(double selfLoopWeight) {
		this.selfLoopWeight = selfLoopWeight;
	}
}
