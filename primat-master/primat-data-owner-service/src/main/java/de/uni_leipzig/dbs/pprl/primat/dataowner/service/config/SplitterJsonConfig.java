/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Sezione {@code splitter} di uno step {@code SPLIT} nella catena
 * {@code preprocessing} di una colonna QID virtuale: {@code type}
 * (es. {@code "BLANK"}) piu' {@code position} (richiesto solo per
 * {@code type == "POSITION"}) e {@code pattern} (richiesto solo per
 * {@code type == "REGEX"}). Vedi {@link PreprocessingStepFactory}.
 */
public class SplitterJsonConfig {

	private String type;
	private Integer position;
	private String pattern;

	public String getType() {
		return type;
	}

	public Integer getPosition() {
		return position;
	}

	public String getPattern() {
		return pattern;
	}
}
