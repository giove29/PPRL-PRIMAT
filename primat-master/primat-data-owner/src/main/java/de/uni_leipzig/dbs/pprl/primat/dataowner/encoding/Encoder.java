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
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding;

import java.util.ArrayList;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * Interface for all current and future encoding techniques. At the moment the
 * most promising technique is based on Bloom filters
 * ({@link BloomFilterEncoder}).
 * 
 * @author mfranke
 *
 */
public interface Encoder {

	/**
	 * Encodes (encrypts) a record.
	 * 
	 * @param  record the record to encode.
	 * @return        the encoded record.
	 */
	public Record encode(Record record);

	/**
	 * Encodes (encrypts) a list of records.
	 * 
	 * @param  records the list of records to encode.
	 * @return         the encoded records.
	 */
	public default List<Record> encode(List<Record> records) {
		final List<Record> encodedRecords = new ArrayList<>(records.size());

		for (final Record record : records) {
			final Record encodedRecord = this.encode(record);
			encodedRecords.add(encodedRecord);
		}

		return encodedRecords;
	}

	public List<String> getSchema();
}