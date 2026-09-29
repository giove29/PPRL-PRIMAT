/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Parametri del blocking JaccardLSH (MinHash), tutti opzionali: default in
 * {@link LinkageUnitConfigLoader} pari all'hardcoded odierno di
 * {@code LinkageUnitOrchestrator} ({@code keySize=4, keys=30, seed=42}). Il
 * range dei valori hash del MinHash non e' piu' configurabile qui: coincide
 * sempre con {@code rbfSize} (vedi {@code LinkageUnitConfig#getRbfSize()}).
 */
public class JaccardLshJsonConfig {

	private Integer keySize;
	private Integer keys;
	private Long seed;

	public Integer getKeySize() {
		return keySize;
	}

	public Integer getKeys() {
		return keys;
	}

	public Long getSeed() {
		return seed;
	}
}
