/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

/**
 * Lanciata quando un {@link DataOwnerConfig} valido richiede a runtime una
 * {@link de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataSourceType}
 * riconosciuta dal JSON ma non ancora implementata (oggi: {@code DB}). A
 * differenza di {@code DataOwnerConfigException} non e' un errore di
 * configurazione — il JSON e' valido, e' la lettura vera e propria a non
 * esistere ancora — quindi resta unchecked, coerente con la scelta di
 * lanciarla in profondita' nella catena di avvio del servizio.
 */
public class UnsupportedDataSourceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnsupportedDataSourceException(String message) {
		super(message);
	}
}
