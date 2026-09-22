/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.BloomFilterHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ColumnConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ColumnRole;
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
	private final boolean csvHasHeader;
	private final char csvDelimiter;
	private final DbSourceConfig dbConfig;
	private final String mqttBrokerUrl;
	private final String mqttClientId;
	private final List<ColumnConfig> columns;
	private final int bloomFilterLength;
	private final BloomFilterHardener hardener;
	private final boolean debug;
	private final boolean missingValueHandlingEnabled;
	private final List<String> missingValueAnchorPriority;
	private final List<String> hardeningDescriptions;

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
	 * @param missingValueHandlingEnabled se {@code true}, un attributo QID vuoto viene codificato tramite
	 *                                     {@code MissingValueBucketing} invece dei trigrammi di padding standard
	 * @param missingValueAnchorPriority  nomi di colonna QID in ordine di priorita' per la scelta dell'anchor,
	 *                                     vuota se {@code missingValueHandlingEnabled} e' {@code false}
	 * @param hardeningDescriptions       descrizione leggibile di ciascuno step della catena di hardening
	 *                                     (solo per {@link #describe()}), vuota se nessun hardening e' configurato
	 */
	public DataOwnerConfig(String party, DataSourceType dataSourceType, String csvFilePath, boolean csvHasHeader,
			char csvDelimiter, DbSourceConfig dbConfig, String mqttBrokerUrl, List<ColumnConfig> columns, int bloomFilterLength,
			BloomFilterHardener hardener, boolean debug, boolean missingValueHandlingEnabled,
			List<String> missingValueAnchorPriority, List<String> hardeningDescriptions) {
		this.party = party;
		this.dataSourceType = dataSourceType;
		this.csvFilePath = csvFilePath;
		this.csvHasHeader = csvHasHeader;
		this.csvDelimiter = csvDelimiter;
		this.dbConfig = dbConfig;
		this.mqttBrokerUrl = mqttBrokerUrl;
		this.mqttClientId = "data-owner-" + party;
		this.columns = columns;
		this.bloomFilterLength = bloomFilterLength;
		this.hardener = hardener;
		this.debug = debug;
		this.missingValueHandlingEnabled = missingValueHandlingEnabled;
		this.missingValueAnchorPriority = missingValueAnchorPriority;
		this.hardeningDescriptions = hardeningDescriptions;
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

	/** @return {@code true} se il CSV ha una riga di intestazione da saltare. */
	public boolean isCsvHasHeader() {
		return csvHasHeader;
	}

	/** @return separatore di campo del CSV. */
	public char getCsvDelimiter() {
		return csvDelimiter;
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

	/** @return {@code true} se un attributo QID vuoto va codificato tramite {@code MissingValueBucketing}. */
	public boolean isMissingValueHandlingEnabled() {
		return missingValueHandlingEnabled;
	}

	/** @return nomi di colonna QID in ordine di priorita' per la scelta dell'anchor (lista vuota se la tecnica e' disabilitata). */
	public List<String> getMissingValueAnchorPriority() {
		return missingValueAnchorPriority;
	}

	/**
	 * @return descrizione leggibile, riga per riga, dell'intera configurazione
	 *         ereditata dal JSON: ogni tecnica opzionale (hardening,
	 *         missing-value handling, CWE per colonna) e' etichettata "On"/"Off"
	 *         con i relativi parametri quando attiva. Pensata per essere
	 *         stampata a schermo all'avvio del Data Owner (nessun dato in
	 *         chiaro delle colonne, solo tuning/struttura).
	 */
	public String describe() {
		final StringBuilder sb = new StringBuilder();
		sb.append("==================== Data Owner [").append(party).append("] ====================\n");
		sb.append("Broker MQTT:            ").append(mqttBrokerUrl).append('\n');
		sb.append("Sorgente dati:          ").append(dataSourceType);
		if (dataSourceType == DataSourceType.CSV) {
			sb.append(" (file=").append(csvFilePath).append(", delimiter='").append(csvDelimiter)
					.append("', hasHeader=").append(csvHasHeader).append(')');
		}
		else if (dbConfig != null) {
			sb.append(" (table=").append(dbConfig.getTableName()).append(')');
		}
		sb.append('\n');
		sb.append("Debug:                  ").append(debug ? "On" : "Off").append('\n');
		sb.append("RBF length:             ").append(bloomFilterLength).append(" bit\n");
		sb.append("Hardening:              ");
		if (hardeningDescriptions.isEmpty()) {
			sb.append("Off\n");
		}
		else {
			sb.append("On -> ").append(String.join(" -> ", hardeningDescriptions)).append('\n');
		}
		sb.append("Missing-value handling: ").append(missingValueHandlingEnabled ? "On" : "Off");
		if (missingValueHandlingEnabled) {
			sb.append(" (anchorPriority=").append(missingValueAnchorPriority).append(')');
		}
		sb.append('\n');
		sb.append("Colonne QID:\n");
		for (final ColumnConfig column : columns) {
			if (column.getRole() != ColumnRole.QID) {
				continue;
			}
			sb.append("  - ").append(column.getName())
					.append(" [dataType=").append(column.getDataType())
					.append(", hashFunctions=").append(column.getHashFunctionsOrDefault())
					.append(", salt=\"").append(column.getSaltOrDefault()).append('"')
					.append(", CWE=");
			if (column.isConstantWeightEncodingEnabled()) {
				sb.append("On (minTrigrams=").append(column.getConstantWeightEncoding().getMinTrigrams())
						.append(", maxTrigrams=").append(column.getConstantWeightEncoding().getMaxTrigrams())
						.append(')');
			}
			else {
				sb.append("Off");
			}
			sb.append(", missingValueTokenCount=").append(column.getMissingValueTokenCountOrDefault())
					.append("]\n");
		}
		sb.append("=======================================================");
		return sb.toString();
	}
}
