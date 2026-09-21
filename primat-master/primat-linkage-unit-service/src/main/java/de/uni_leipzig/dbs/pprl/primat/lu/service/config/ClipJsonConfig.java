/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/** Sezione {@code clip} del JSON, mirror di {@code ClipConfig}. */
public class ClipJsonConfig {

	private Double weightSimilarity;
	private Double weightLinkDegree;
	private Double weightLinkStrength;
	private Double valueStrong;
	private Double valueNormal;
	private Double valueWeak;
	private Boolean ignoreWeakLinks;
	private Double mergeThreshold;

	public Double getWeightSimilarity() {
		return weightSimilarity;
	}

	public Double getWeightLinkDegree() {
		return weightLinkDegree;
	}

	public Double getWeightLinkStrength() {
		return weightLinkStrength;
	}

	public Double getValueStrong() {
		return valueStrong;
	}

	public Double getValueNormal() {
		return valueNormal;
	}

	public Double getValueWeak() {
		return valueWeak;
	}

	public Boolean getIgnoreWeakLinks() {
		return ignoreWeakLinks;
	}

	public Double getMergeThreshold() {
		return mergeThreshold;
	}
}
