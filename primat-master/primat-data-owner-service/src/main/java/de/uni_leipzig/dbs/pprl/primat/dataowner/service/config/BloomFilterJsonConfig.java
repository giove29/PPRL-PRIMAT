/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import java.util.List;

/**
 * Sezione opzionale {@code bloomFilter} del JSON di configurazione del Data
 * Owner. Se l'intera sezione o singoli campi sono omessi, {@code
 * DataOwnerConfigLoader} applica i default (lunghezza 1024, nessun
 * hardening).
 */
public class BloomFilterJsonConfig {

	private Integer length;
	private HardeningJsonConfig hardening;
	private List<HardeningJsonConfig> hardeningChain;

	public Integer getLength() {
		return length;
	}

	/** @return l'hardener singolo configurato, mutuamente esclusivo con {@link #getHardeningChain()}. */
	public HardeningJsonConfig getHardening() {
		return hardening;
	}

	/**
	 * @return la catena di step di hardening da applicare in sequenza (es. BLIP
	 *         poi XOR_FOLD), mutuamente esclusiva con {@link #getHardening()}.
	 *         {@code null}/vuota se non configurata.
	 */
	public List<HardeningJsonConfig> getHardeningChain() {
		return hardeningChain;
	}
}
