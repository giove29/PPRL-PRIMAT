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
package de.uni_leipzig.dbs.pprl.primat.lu.blocking;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * Subset of a block containing only the records of a specific party.
 * 
 * @author mfranke
 *
 */
public class SubBlock {

	private Set<Record> records;

	public SubBlock() {
		this.records = new HashSet<>();
	}

	public SubBlock(Set<Record> records) {
		this.records = records;
	}

	public boolean add(Record record) {
		return this.records.add(record);
	}

	public boolean addAll(Collection<? extends Record> records) {
		return this.records.addAll(records);
	}

	public Set<Record> getRecords() {
		return this.records;
	}

	public int getSize() {
		return this.records.size();
	}

	public boolean isEmpty() {
		return this.records.isEmpty();
	}
}