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
package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVRecord;

import de.uni_leipzig.dbs.pprl.primat.common.csv.CSVReader;
import de.uni_leipzig.dbs.pprl.primat.common.csv.CSVRecordWrapper;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchemaConfiguration;

/**
 * 
 * @author mfranke
 *
 */
public final class DatasetReader {
	
	private String filePath;
	private boolean hasHeader;
	private char delimiter;
	private RecordSchemaConfiguration rsc;
	
	public DatasetReader(String filePath, RecordSchemaConfiguration rsc) {
		this(filePath, false, rsc);
	}

	public DatasetReader(String filePath, boolean hasHeader, RecordSchemaConfiguration rsc) {
		this(filePath, hasHeader, ';', rsc);
	}

	public DatasetReader(String filePath, boolean hasHeader, char delimiter, RecordSchemaConfiguration rsc) {
		this.filePath = filePath;
		this.hasHeader = hasHeader;
		this.delimiter = delimiter;
		this.rsc = rsc;
	}

	public List<Record> read() throws IOException {
		final CSVReader csvReader = new CSVReader(filePath, hasHeader, delimiter);
		final List<CSVRecord> stringRecords = csvReader.read();
		final List<String> headerNames = csvReader.getHeader();
		final CSVRecordWrapper wrapper = new CSVRecordWrapper(rsc, headerNames);
		return wrapper.from(stringRecords);
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public boolean hasHeader() {
		return hasHeader;
	}

	public void setHasHeader(boolean hasHeader) {
		this.hasHeader = hasHeader;
	}

	public RecordSchemaConfiguration getRecordSchemaConfiguration() {
		return rsc;
	}

	public void setRecordSchemaConfiguration(RecordSchemaConfiguration rsc) {
		this.rsc = rsc;
	}
}