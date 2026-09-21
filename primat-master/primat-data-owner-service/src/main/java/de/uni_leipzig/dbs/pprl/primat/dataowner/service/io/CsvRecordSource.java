/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.io;

import java.io.IOException;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;

/**
 * {@link RecordSource} basato su file CSV locali, wrapper sottile di
 * {@link DatasetReader} già esistente in {@code primat-common} (delimitatore
 * ";", nessun header): nessuna nuova logica di parsing, solo adattamento
 * all'interfaccia {@link RecordSource}.
 */
public class CsvRecordSource implements RecordSource {

	private final String filePath;

	/**
	 * @param filePath percorso locale del CSV del party (mai condiviso con altri
	 *                 Data Owner)
	 */
	public CsvRecordSource(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public List<Record> readAll(RecordSchemaConfiguration schema) throws IOException {
		final DatasetReader reader = new DatasetReader(filePath, schema);
		return reader.read();
	}
}
