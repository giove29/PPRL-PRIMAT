/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Ruolo di una colonna dello schema all'interno del record, cosi' come
 * dichiarato nel JSON di configurazione del Data Owner. Guida la costruzione
 * dello schema di lettura ({@code NamedRecordSchemaConfiguration}): i primi
 * tre valori mappano su {@code NonQidAttributeType}, {@code QID} rappresenta
 * un attributo quasi-identificatore da normalizzare e codificare nell'RBF.
 */
public enum ColumnRole {
	PARTY,
	GLOBAL_ID,
	ID,
	QID
}
