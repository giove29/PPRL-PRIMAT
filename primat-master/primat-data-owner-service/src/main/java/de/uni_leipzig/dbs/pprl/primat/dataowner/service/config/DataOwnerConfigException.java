/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Unico tipo di eccezione per ogni problema di caricamento o validazione del
 * JSON di configurazione del Data Owner: file non trovato/non leggibile,
 * JSON malformato, campi obbligatori mancanti o non validi, colonne
 * inconsistenti (indici/nomi duplicati, ruoli mancanti, tipo QID assente),
 * parametri di tuning del Bloom Filter fuori range. Sempre checked, cosi'
 * che {@code DataOwnerService.main} sia costretto a gestirla con un
 * messaggio leggibile invece di uno stack trace grezzo.
 */
public class DataOwnerConfigException extends Exception {

	private static final long serialVersionUID = 1L;

	public DataOwnerConfigException(String message) {
		super(message);
	}

	public DataOwnerConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
