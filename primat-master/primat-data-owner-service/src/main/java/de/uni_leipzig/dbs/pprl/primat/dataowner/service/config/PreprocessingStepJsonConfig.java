/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import java.util.List;

/**
 * Un singolo step della catena {@code preprocessing} di una colonna QID nel
 * JSON del Data Owner. Oltre ai normalizzatori "puri" a singola colonna
 * ({@code {"type": "TRIM"}}, {@code {"type": "TRUNCATE", "from": 0, "to": 20}}
 * - {@code from}/{@code to} hanno senso solo per {@code type == "TRUNCATE"}),
 * puo' essere il primo step di una colonna QID "virtuale" (senza
 * {@code index}) e valere {@code "MERGE"} ({@code sources}/{@code merger}) o
 * {@code "SPLIT"} ({@code source}/{@code splitter}/{@code parts}/{@code part}):
 * calcola il valore iniziale della colonna da altre colonne {@code RAW}
 * invece di leggerlo fisicamente dalla sorgente dati. Vedi
 * {@link PreprocessingStepFactory} per validazione, esecuzione e descrizione
 * canonica per il digest.
 */
public class PreprocessingStepJsonConfig {

	private String type;
	private Integer from;
	private Integer to;
	private List<String> sources;
	private MergerJsonConfig merger;
	private String source;
	private SplitterJsonConfig splitter;
	private Integer parts;
	private Integer part;

	public String getType() {
		return type;
	}

	public Integer getFrom() {
		return from;
	}

	public Integer getTo() {
		return to;
	}

	/** @return nomi delle colonne {@code RAW} da unire (solo {@code type == "MERGE"}). */
	public List<String> getSources() {
		return sources;
	}

	/** @return il merger da applicare a {@link #getSources()} (solo {@code type == "MERGE"}). */
	public MergerJsonConfig getMerger() {
		return merger;
	}

	/** @return nome della colonna {@code RAW} da dividere (solo {@code type == "SPLIT"}). */
	public String getSource() {
		return source;
	}

	/** @return lo splitter da applicare a {@link #getSource()} (solo {@code type == "SPLIT"}). */
	public SplitterJsonConfig getSplitter() {
		return splitter;
	}

	/** @return numero totale di parti prodotte dallo split (solo {@code type == "SPLIT"}). */
	public Integer getParts() {
		return parts;
	}

	/** @return indice (0-based) della parte che questa colonna rappresenta (solo {@code type == "SPLIT"}). */
	public Integer getPart() {
		return part;
	}
}
