/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/** Sezione {@code bloomFilter.hardening} del JSON di configurazione del Data Owner. */
public class HardeningJsonConfig {

	private HardeningType type;
	private Integer foldCount;
	private Double probability;
	private Long seed;

	public HardeningType getType() {
		return type;
	}

	/** @return il numero di ripiegamenti XOR configurato, obbligatorio solo se {@code type == XOR_FOLD}. */
	public Integer getFoldCount() {
		return foldCount;
	}

	/** @return la probabilita' di flip per bit (BLIP), obbligatoria solo se {@code type == BLIP}. */
	public Double getProbability() {
		return probability;
	}

	/** @return il seed del PRNG per BLIP, opzionale (se assente si usa un default fisso). */
	public Long getSeed() {
		return seed;
	}
}
