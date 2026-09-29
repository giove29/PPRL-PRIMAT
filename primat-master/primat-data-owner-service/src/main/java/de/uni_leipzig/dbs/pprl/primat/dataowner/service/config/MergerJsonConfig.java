/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Sezione {@code merger} di uno step {@code MERGE} nella catena
 * {@code preprocessing} di una colonna QID virtuale: {@code type}
 * (es. {@code "BLANK"}) piu' {@code separator}, richiesto solo per
 * {@code type == "SIMPLE"}. Vedi {@link PreprocessingStepFactory}.
 */
public class MergerJsonConfig {

	private String type;
	private String separator;

	public String getType() {
		return type;
	}

	public String getSeparator() {
		return separator;
	}
}
