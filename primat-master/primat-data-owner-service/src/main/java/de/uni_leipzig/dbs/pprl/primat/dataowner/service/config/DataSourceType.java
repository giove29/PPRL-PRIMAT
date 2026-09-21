/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/** Sorgente dati supportata dal Data Owner Service. */
public enum DataSourceType {
	/** File CSV locale, letto tramite {@code CsvRecordSource}. */
	CSV,
	/**
	 * Database relazionale: il JSON puo' gia' dichiarare tabella/connessione
	 * (sezione {@code db}), ma la lettura non e' ancora implementata.
	 * {@code DataOwnerService.newRecordSource()} lancia
	 * {@code UnsupportedDataSourceException} se questo tipo viene usato per
	 * avviare un run — predisposto per una futura {@code JdbcRecordSource}
	 * senza altre modifiche al resto del servizio.
	 */
	DB
}
