/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.io;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import de.uni_leipzig.dbs.pprl.primat.common.csv.CSVRecordWrapper;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchemaConfiguration;

/**
 * {@link RecordSource} basato su una tabella JDBC (sezione {@code
 * dataSource.db} del JSON): legge l'intera tabella con {@code SELECT *
 * FROM tableName}. Stesso contratto posizionale gia' implicito per {@link
 * CsvRecordSource}: l'ordine delle colonne restituito dalla query deve
 * corrispondere a {@code columns[].index} nel JSON (nessuna assunzione
 * nuova, solo esplicitata qui perche' per il CSV l'ordine e' quello del
 * file, per il DB e' quello di definizione della tabella).
 *
 * <p>Ogni riga del {@link ResultSet} viene convertita in una riga CSV
 * sintetica (stesso delimitatore {@code ;} usato da {@link
 * de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader}) e poi
 * riparsata in un vero {@link CSVRecord}, cosi' da riusare {@link
 * CSVRecordWrapper} (che si aspetta un {@code CSVRecord}) senza duplicare
 * la logica di attribute-parsing di {@code AttributeHandler}/{@code
 * ColumnNameHandler}.
 */
public class JdbcRecordSource implements RecordSource {

	private static final Pattern SAFE_TABLE_NAME = Pattern.compile("[A-Za-z0-9_]+");
	private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.withDelimiter(';');

	private final String jdbcUrl;
	private final String username;
	private final String password;
	private final String tableName;

	/**
	 * @param jdbcUrl   url JDBC del DB del party (es. {@code jdbc:postgresql://host:5432/db})
	 * @param username  utente DB
	 * @param password  password DB
	 * @param tableName nome della tabella da leggere per intero; deve contenere solo
	 *                  lettere/cifre/underscore (whitelist, difesa in profondità anche
	 *                  se il valore proviene da un file di config locale fidato)
	 */
	public JdbcRecordSource(String jdbcUrl, String username, String password, String tableName) {
		if (tableName == null || !SAFE_TABLE_NAME.matcher(tableName).matches()) {
			throw new IllegalArgumentException(
					"Nome tabella non valido (solo lettere/cifre/underscore ammessi): " + tableName);
		}
		this.jdbcUrl = jdbcUrl;
		this.username = username;
		this.password = password;
		this.tableName = tableName;
	}

	@Override
	public List<Record> readAll(RecordSchemaConfiguration schema) throws IOException {
		final CSVRecordWrapper wrapper = new CSVRecordWrapper(schema);
		final List<Record> records = new ArrayList<>();
		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName)) {
			final int columnCount = resultSet.getMetaData().getColumnCount();
			while (resultSet.next()) {
				records.add(wrapper.from(toCsvRecord(resultSet, columnCount)));
			}
		}
		catch (SQLException e) {
			throw new IOException(
					"Errore durante la lettura dalla tabella '" + tableName + "': " + e.getMessage(), e);
		}
		return records;
	}

	private static CSVRecord toCsvRecord(ResultSet resultSet, int columnCount) throws SQLException, IOException {
		final StringWriter csvLine = new StringWriter();
		try (CSVPrinter printer = new CSVPrinter(csvLine, CSV_FORMAT)) {
			for (int i = 1; i <= columnCount; i++) {
				final Object value = resultSet.getObject(i);
				printer.print(value != null ? value.toString() : "");
			}
			printer.println();
		}
		try (CSVParser parser = CSVParser.parse(csvLine.toString(), CSV_FORMAT)) {
			return parser.getRecords().get(0);
		}
	}
}
