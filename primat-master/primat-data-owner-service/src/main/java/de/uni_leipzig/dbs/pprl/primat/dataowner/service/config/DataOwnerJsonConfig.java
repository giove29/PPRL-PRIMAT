/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import java.util.List;

/**
 * Rappresentazione grezza (1:1, nessuna validazione) del file JSON di
 * configurazione del Data Owner, cosi' come deserializzato da Gson in
 * {@code DataOwnerConfigLoader}. Vedi il JSON di esempio in
 * {@code src/main/resources/config/party_A.json}.
 */
public class DataOwnerJsonConfig {

	private String party;
	private String mqttBrokerUrl;
	private DataSourceJsonConfig dataSource;
	private BloomFilterJsonConfig bloomFilter;
	private List<ColumnConfig> columns;
	private Boolean debug;
	private MissingValueHandlingJsonConfig missingValueHandling;

	public String getParty() {
		return party;
	}

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	public DataSourceJsonConfig getDataSource() {
		return dataSource;
	}

	public BloomFilterJsonConfig getBloomFilter() {
		return bloomFilter;
	}

	public List<ColumnConfig> getColumns() {
		return columns;
	}

	/** @return {@code true} se il JSON richiede le stampe di debug della pipeline, {@code false} (default) altrimenti. */
	public boolean isDebug() {
		return Boolean.TRUE.equals(debug);
	}

	/** @return la sezione top-level {@code missingValueHandling}, o {@code null} se assente. */
	public MissingValueHandlingJsonConfig getMissingValueHandling() {
		return missingValueHandling;
	}
}
