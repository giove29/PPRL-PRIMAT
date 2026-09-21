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

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;


/**
 * Class for writing (exporting) objects to csv records.
 * 
 * 
 * @author mfranke
 *
 */
public class CSVWriter {

	public final CSVFormat CSV_FORMAT = CSVFormat.RFC4180;

	private final CSVPrinter csvPrinter;

	public CSVWriter(String outputFile) throws IOException {
		final FileWriter fileWriter;

		if (!outputFile.endsWith(".csv")) {
			fileWriter = new FileWriter(outputFile + ".csv");
		}
		else {
			fileWriter = new FileWriter(outputFile);
		}

		this.csvPrinter = new CSVPrinter(fileWriter, CSV_FORMAT);
	}

	public void close() throws IOException {
		this.csvPrinter.flush();
		this.csvPrinter.close();
	}

	public void writeIterable(List<? extends Iterable<?>> records) throws IOException {

		for (final Iterable<?> rec : records) {
			this.csvPrinter.printRecord(rec);
		}

		this.close();
	}

	public void writeRecords(List<? extends CSVPrintable> records) throws IOException {

		for (final CSVPrintable entry : records) {
			this.csvPrinter.printRecord(entry.getValuesToPrint());
		}

		this.close();
	}

	public void writeRecords(List<? extends CSVPrintable> records, Iterable<?> header) throws IOException {
		this.csvPrinter.printRecord(header);
		this.writeRecords(records);
	}
}