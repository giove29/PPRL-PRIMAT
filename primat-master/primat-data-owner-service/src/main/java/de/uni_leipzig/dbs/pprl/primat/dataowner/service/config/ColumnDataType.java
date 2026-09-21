/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Tipo di contenuto di una colonna QID, cosi' come dichiarato nel JSON di
 * configurazione del Data Owner. Determina esclusivamente quale
 * {@code NormalizerChain} viene applicata alla colonna prima della codifica
 * RBF (vedi {@code DataOwnerPipeline}); non influisce sul tipo di attributo
 * usato nello schema di lettura, che resta sempre testuale per non alterare
 * l'estrazione trigram-based gia' validata.
 */
public enum ColumnDataType {
	/** Testo libero: trim, maiuscolo, rimozione accenti/caratteri speciali, troncamento a 20 caratteri. */
	TEXT,
	/** Valore numerico rappresentato come testo (es. anno di nascita): trim e rimozione dei caratteri non numerici. */
	NUMERIC
}
