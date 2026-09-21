/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/** Dettagli della sorgente CSV, sezione {@code dataSource.csv} del JSON. */
public class CsvSourceConfig {

	private String filePath;
	private boolean hasHeader;
	private String delimiter;

	/** @return separatore di campo del CSV, un solo carattere (default {@code ";"}). */
	public String getDelimiter() {
		return delimiter == null ? ";" : delimiter;
	}

	public String getFilePath() {
		return filePath;
	}

	/** @return {@code true} se la prima riga del CSV e' un'intestazione da saltare (default {@code false}). */
	public boolean isHasHeader() {
		return hasHeader;
	}
}
