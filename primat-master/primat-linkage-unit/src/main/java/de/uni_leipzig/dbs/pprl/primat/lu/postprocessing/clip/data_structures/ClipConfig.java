/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.data_structures;

import java.io.Serializable;

/**
 * Configurazione per
 * {@link de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.ClipClusteringPostprocessor},
 * sibling di {@code CenterClusteringConfig}/{@code GlobalGreedyConfig}.
 * Stessi campi/default della classe legacy (e mai completata)
 * {@code postprocessing.CLIP}, qui riusati per calcolare la priorità di un
 * arco secondo l'algoritmo CLIP di letteratura (FAMER/Leipzig DBS): una
 * combinazione pesata di similarità, "link degree" e "link strength"
 * (mutual-best-match, criterio Symmetric Best Match).
 */
public class ClipConfig implements Serializable {

	private static final long serialVersionUID = 1L;

	private double weightSimilarity;
	private double weightLinkDegree;
	private double weightLinkStrength;
	private double valueStrong;
	private double valueNormal;
	private double valueWeak;
	private boolean ignoreWeakLinks;
	private Double mergeThreshold;

	public ClipConfig() {
		this.weightSimilarity = 0.5d;
		this.weightLinkDegree = 0.1d;
		this.weightLinkStrength = 0.4d;
		this.valueStrong = 1d;
		this.valueNormal = 0.5d;
		this.valueWeak = 0d;
		this.ignoreWeakLinks = true;
		this.mergeThreshold = null;
	}

	/**
	 * @throws IllegalArgumentException se un peso è negativo o
	 *                                   {@code mergeThreshold} è fuori da [0,1]
	 */
	public void checkConfigCorrectness() {
		if (weightSimilarity < 0d || weightLinkDegree < 0d || weightLinkStrength < 0d) {
			throw new IllegalArgumentException("i pesi (weightSimilarity/weightLinkDegree/weightLinkStrength) devono essere non negativi");
		}
		if (mergeThreshold != null && (mergeThreshold < 0d || mergeThreshold > 1d)) {
			throw new IllegalArgumentException("mergeThreshold must be in [0,1]");
		}
	}

	public double getWeightSimilarity() {
		return weightSimilarity;
	}

	public void setWeightSimilarity(double weightSimilarity) {
		this.weightSimilarity = weightSimilarity;
	}

	public double getWeightLinkDegree() {
		return weightLinkDegree;
	}

	public void setWeightLinkDegree(double weightLinkDegree) {
		this.weightLinkDegree = weightLinkDegree;
	}

	public double getWeightLinkStrength() {
		return weightLinkStrength;
	}

	public void setWeightLinkStrength(double weightLinkStrength) {
		this.weightLinkStrength = weightLinkStrength;
	}

	public double getValueStrong() {
		return valueStrong;
	}

	public void setValueStrong(double valueStrong) {
		this.valueStrong = valueStrong;
	}

	public double getValueNormal() {
		return valueNormal;
	}

	public void setValueNormal(double valueNormal) {
		this.valueNormal = valueNormal;
	}

	public double getValueWeak() {
		return valueWeak;
	}

	public void setValueWeak(double valueWeak) {
		this.valueWeak = valueWeak;
	}

	public boolean isIgnoreWeakLinks() {
		return ignoreWeakLinks;
	}

	public void setIgnoreWeakLinks(boolean ignoreWeakLinks) {
		this.ignoreWeakLinks = ignoreWeakLinks;
	}

	public Double getMergeThreshold() {
		return mergeThreshold;
	}

	public void setMergeThreshold(Double mergeThreshold) {
		this.mergeThreshold = mergeThreshold;
	}
}
