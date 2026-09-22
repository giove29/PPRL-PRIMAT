/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import java.util.List;

/**
 * Sezione top-level opzionale {@code missingValueHandling} del JSON del Data
 * Owner: se abilitata, un attributo QID vuoto viene codificato tramite
 * {@code MissingValueBucketing} invece dei trigrammi di padding standard.
 * {@code anchorPriority} e' l'ordine di priorita' (nomi di colonna QID) con
 * cui scegliere l'attributo "anchor" da cui derivare il bucket.
 */
public class MissingValueHandlingJsonConfig {

	private Boolean enabled;
	private List<String> anchorPriority;

	public Boolean getEnabled() {
		return enabled;
	}

	public List<String> getAnchorPriority() {
		return anchorPriority;
	}
}
