/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Parametri del blocking JaccardLSH (MinHash), tutti opzionali: default in
 * {@link LinkageUnitConfigLoader} pari all'hardcoded odierno di
 * {@code LinkageUnitOrchestrator} ({@code keySize=4, keys=30, valueRange=1024, seed=42}).
 */
public class JaccardLshJsonConfig {

	private Integer keySize;
	private Integer keys;
	private Integer valueRange;
	private Long seed;

	public Integer getKeySize() {
		return keySize;
	}

	public Integer getKeys() {
		return keys;
	}

	public Integer getValueRange() {
		return valueRange;
	}

	public Long getSeed() {
		return seed;
	}
}
