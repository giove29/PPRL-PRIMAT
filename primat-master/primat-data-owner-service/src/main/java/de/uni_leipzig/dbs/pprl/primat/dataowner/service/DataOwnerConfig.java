/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.BloomFilterHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ColumnConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataSourceType;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DbSourceConfig;

/**
 * Configurazione locale di un Data Owner Service: mai trasmessa in rete,
 * caricata da un file JSON tramite {@link DataOwnerConfigLoader} e letta da
 * ciascun processo indipendentemente. Identifica il party, la sorgente dati
 * locale (colonne, ruoli, tipi), il tuning dell'RBF (lunghezza, hash
 * function/salt per colonna, hardening) e l'endpoint del broker MQTT verso
 * cui pubblicare gli RBF.
 */
public class DataOwnerConfig {

	private final String party;
	private final DataSourceType dataSourceType;
	private final String csvFilePath;
	private final DbSourceConfig dbConfig;
	private final String mqttBrokerUrl;
	private final String mqttClientId;
	private final List<ColumnConfig> columns;
	private final int bloomFilterLength;
	private final BloomFilterHardener hardener;
	private final boolean debug;

	/**
	 * Costruito esclusivamente da {@link DataOwnerConfigLoader} dopo la
	 * validazione del JSON: nessun controllo aggiuntivo viene fatto qui, questa
	 * classe e' un semplice contenitore immutabile.
	 *
	 * @param party             identificativo del party (es. "A")
	 * @param dataSourceType    tipo di sorgente dati: sia {@code CSV}
	 *                          ({@link de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.CsvRecordSource})
	 *                          sia {@code DB}
	 *                          ({@link de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.JdbcRecordSource})
	 *                          sono effettivamente leggibili; {@link UnsupportedDataSourceException}
	 *                          resta solo come guardia per valori enum futuri non gestiti
	 * @param csvFilePath       percorso del CSV, {@code null} se {@code dataSourceType != CSV}
	 * @param dbConfig          dettagli della sorgente DB, {@code null} se {@code dataSourceType != DB}
	 * @param mqttBrokerUrl     endpoint del broker MQTT (es. "tcp://localhost:1883")
	 * @param columns           schema completo delle colonne, gia' validato
	 * @param bloomFilterLength lunghezza in bit dell'RBF
	 * @param hardener          tecnica di hardening da applicare all'RBF dopo la codifica
	 * @param debug             se {@code true}, la pipeline stampa a schermo i primi 2 record ad ogni passo di preprocessing
	 */
	public DataOwnerConfig(String party, DataSourceType dataSourceType, String csvFilePath, DbSourceConfig dbConfig,
			String mqttBrokerUrl, List<ColumnConfig> columns, int bloomFilterLength, BloomFilterHardener hardener,
			boolean debug) {
		this.party = party;
		this.dataSourceType = dataSourceType;
		this.csvFilePath = csvFilePath;
		this.dbConfig = dbConfig;
		this.mqttBrokerUrl = mqttBrokerUrl;
		this.mqttClientId = "data-owner-" + party;
		this.columns = columns;
		this.bloomFilterLength = bloomFilterLength;
		this.hardener = hardener;
		this.debug = debug;
	}

	public String getParty() {
		return party;
	}

	public DataSourceType getDataSourceType() {
		return dataSourceType;
	}

	public String getCsvFilePath() {
		return csvFilePath;
	}

	/** @return dettagli della sorgente DB (tabella/connessione), {@code null} se non configurata. */
	public DbSourceConfig getDbConfig() {
		return dbConfig;
	}

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	public String getMqttClientId() {
		return mqttClientId;
	}

	/** @return lo schema completo delle colonne (ruolo, tipo, tuning RBF), gia' validato dal loader. */
	public List<ColumnConfig> getColumns() {
		return columns;
	}

	public int getBloomFilterLength() {
		return bloomFilterLength;
	}

	/** @return la tecnica di hardening da applicare all'RBF dopo la codifica ({@code NoHardener} se nessuna). */
	public BloomFilterHardener getHardener() {
		return hardener;
	}

	/** @return {@code true} se la pipeline deve stampare a schermo i primi 2 record ad ogni passo di preprocessing. */
	public boolean isDebug() {
		return debug;
	}
}
