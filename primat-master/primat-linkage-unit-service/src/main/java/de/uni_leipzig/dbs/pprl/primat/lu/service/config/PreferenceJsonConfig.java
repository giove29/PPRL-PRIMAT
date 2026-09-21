/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/** Sezione {@code mscdAp.preference} del JSON, mirror di {@code PreferenceConfig}. */
public class PreferenceJsonConfig {

	private Boolean preferenceUseMinSimilarityDirtySrc;
	private Boolean preferenceUseMinSimilarityCleanSrc;
	private Double preferenceFixValueDirtySrc;
	private Double preferenceFixValueCleanSrc;
	private Integer preferencePercentileDirtySrc;
	private Integer preferencePercentileCleanSrc;
	private Double preferenceAdaptionStep;

	public Boolean getPreferenceUseMinSimilarityDirtySrc() {
		return preferenceUseMinSimilarityDirtySrc;
	}

	public Boolean getPreferenceUseMinSimilarityCleanSrc() {
		return preferenceUseMinSimilarityCleanSrc;
	}

	public Double getPreferenceFixValueDirtySrc() {
		return preferenceFixValueDirtySrc;
	}

	public Double getPreferenceFixValueCleanSrc() {
		return preferenceFixValueCleanSrc;
	}

	public Integer getPreferencePercentileDirtySrc() {
		return preferencePercentileDirtySrc;
	}

	public Integer getPreferencePercentileCleanSrc() {
		return preferencePercentileCleanSrc;
	}

	public Double getPreferenceAdaptionStep() {
		return preferenceAdaptionStep;
	}
}
