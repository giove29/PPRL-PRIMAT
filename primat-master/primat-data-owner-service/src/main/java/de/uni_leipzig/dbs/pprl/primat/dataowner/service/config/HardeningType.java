/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Tecnica di hardening del Bloom Filter RBF selezionabile da JSON. Mappa
 * direttamente su implementazioni di {@code BloomFilterHardener} gia'
 * esistenti in {@code primat-data-owner} (nessun nuovo algoritmo introdotto
 * da questa configurazione).
 */
public enum HardeningType {
	/** Nessun hardening: {@code NoHardener}, comportamento odierno. */
	NONE,
	/**
	 * XOR-folding [Schnell, Borgs 2016]: {@code XorFolder}, richiede il campo
	 * {@code foldCount} nella sezione {@code hardening} del JSON.
	 */
	XOR_FOLD,
	/**
	 * BLIP / randomized response [Alaggan et. al. 2012]: {@code RandomizedResponse},
	 * richiede il campo {@code probability} (e opzionalmente {@code seed}) nella
	 * sezione {@code hardening} del JSON.
	 */
	BLIP
}
