/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.io;

import java.io.IOException;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchemaConfiguration;

/**
 * Astrazione della sorgente dati locale di un Data Owner. Disaccoppia
 * {@link de.uni_leipzig.dbs.pprl.primat.dataowner.service.DataOwnerPipeline}
 * dal formato di storage concreto: oggi solo CSV ({@link CsvRecordSource}),
 * in futuro un database tramite una nuova implementazione (es. JDBC) senza
 * modificare il resto del servizio.
 */
public interface RecordSource {

	/**
	 * Legge tutti i record disponibili localmente, già mappati sullo schema
	 * fornito.
	 *
	 * @param schema schema di lettura (nomi/tipi delle colonne)
	 * @return i record letti, non ancora pre-processati né codificati
	 * @throws IOException se la sorgente dati non è raggiungibile o leggibile
	 */
	List<Record> readAll(RecordSchemaConfiguration schema) throws IOException;
}
