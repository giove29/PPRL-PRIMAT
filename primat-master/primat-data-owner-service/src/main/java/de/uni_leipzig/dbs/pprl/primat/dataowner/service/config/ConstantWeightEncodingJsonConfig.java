/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Sezione opzionale {@code constantWeightEncoding} di una colonna QID nel
 * JSON del Data Owner: normalizza il numero di trigrammi unici estratti per
 * quell'attributo dentro la banda {@code [minTrigrams, maxTrigrams]}.
 */
public class ConstantWeightEncodingJsonConfig {

	private Boolean enabled;
	private Integer minTrigrams;
	private Integer maxTrigrams;

	public Boolean getEnabled() {
		return enabled;
	}

	public Integer getMinTrigrams() {
		return minTrigrams;
	}

	public Integer getMaxTrigrams() {
		return maxTrigrams;
	}
}
