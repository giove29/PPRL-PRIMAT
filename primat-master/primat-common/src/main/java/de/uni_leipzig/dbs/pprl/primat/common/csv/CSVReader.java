/*******************************************************************************
 *  Copyright © 2017 - 2022 Leipzig University (Database Research Group)
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under 
 * the License.
 *******************************************************************************/

package de.uni_leipzig.dbs.pprl.primat.common.csv;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


/**
 * Class for reading and parsing csv files.
 * 
 * @author mfranke
 *
 */
public class CSVReader {

	public static final CSVFormat CSV_DEFAULT_FORMAT = CSVFormat.RFC4180;

	private final CSVParser csvParser;
	private final LinkedList<String> header;

	public CSVReader(String filePath, boolean hasHeader) throws IOException {
		this(filePath, hasHeader, CSV_DEFAULT_FORMAT);
	}

	public CSVReader(String filePath, boolean hasHeader, char delimiter) throws IOException {
		this(filePath, hasHeader, CSV_DEFAULT_FORMAT.withDelimiter(delimiter));
	}

	public CSVReader(String filePath, boolean hasHeader, CSVFormat csvFormat) throws IOException {
		final Reader in = new FileReader(filePath);

		if (hasHeader) {
			this.csvParser = csvFormat.withFirstRecordAsHeader().parse(in);
			this.header = new LinkedList<String>(csvParser.getHeaderNames());
		}
		else {
			this.csvParser = csvFormat.parse(in);
			this.header = null;
		}
	}

	public static int numberOfColumns(List<CSVRecord> records) {

		if (records == null || records.size() == 0) {
			return -1;
		}
		else {
			return records.get(0).size();
		}
	}

	public static LinkedList<String> getDefaultHeader(List<CSVRecord> records) {

		if (records == null || records.size() == 0) {
			return null;
		}
		else {
			final CSVRecord record = records.get(0);
			final LinkedList<String> defaultHeader = new LinkedList<>();

			final int columns = record.size();

			for (int i = 0; i < columns; i++) {
				defaultHeader.add("Column_" + i);
			}

			return defaultHeader;
		}
	}

	public LinkedList<String> getHeader() {
		return this.header;
	}

	public List<CSVRecord> read() throws IOException {
		return this.csvParser.getRecords();
	}

	public void close() throws IOException {
		this.csvParser.close();
	}
}