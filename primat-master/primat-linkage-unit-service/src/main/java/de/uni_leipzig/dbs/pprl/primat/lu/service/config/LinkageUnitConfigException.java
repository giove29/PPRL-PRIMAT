/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Unico tipo di eccezione per ogni problema di caricamento o validazione del
 * JSON di configurazione della Linkage Unit: file non trovato/non leggibile,
 * JSON malformato, campi obbligatori mancanti o non validi, party duplicate,
 * database mancante quando richiesto, persistenza abilitata con MCL, MSCD-AP
 * senza alcuna party duplicate-free. Sempre checked, cosi' che
 * {@code LinkageUnitOrchestrator.main} sia costretto a gestirla con un
 * messaggio leggibile invece di uno stack trace grezzo. Mirror di
 * {@code DataOwnerConfigException}.
 */
public class LinkageUnitConfigException extends Exception {

	private static final long serialVersionUID = 1L;

	public LinkageUnitConfigException(String message) {
		super(message);
	}

	public LinkageUnitConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
