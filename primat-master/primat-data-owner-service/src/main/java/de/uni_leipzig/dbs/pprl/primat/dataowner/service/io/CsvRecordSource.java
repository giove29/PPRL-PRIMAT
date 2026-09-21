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
 * ";", header opzionale): nessuna nuova logica di parsing, solo adattamento
 * all'interfaccia {@link RecordSource}.
 */
public class CsvRecordSource implements RecordSource {

	private final String filePath;
	private final boolean hasHeader;
	private final char delimiter;

	/**
	 * @param filePath  percorso locale del CSV del party (mai condiviso con altri
	 *                  Data Owner)
	 * @param hasHeader {@code true} se la prima riga e' un'intestazione da saltare
	 */
	public CsvRecordSource(String filePath, boolean hasHeader) {
		this(filePath, hasHeader, ';');
	}

	/** @param delimiter separatore di campo del CSV */
	public CsvRecordSource(String filePath, boolean hasHeader, char delimiter) {
		this.filePath = filePath;
		this.hasHeader = hasHeader;
		this.delimiter = delimiter;
	}

	@Override
	public List<Record> readAll(RecordSchemaConfiguration schema) throws IOException {
		final DatasetReader reader = new DatasetReader(filePath, hasHeader, delimiter, schema);
		return reader.read();
	}
}
