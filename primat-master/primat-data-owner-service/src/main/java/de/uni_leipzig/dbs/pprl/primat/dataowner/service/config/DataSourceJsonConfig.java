/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/** Sezione {@code dataSource} del JSON di configurazione del Data Owner. */
public class DataSourceJsonConfig {

	private DataSourceType type;
	private CsvSourceConfig csv;
	private DbSourceConfig db;

	public DataSourceType getType() {
		return type;
	}

	public CsvSourceConfig getCsv() {
		return csv;
	}

	public DbSourceConfig getDb() {
		return db;
	}
}
