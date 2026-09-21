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
package de.uni_leipzig.dbs.pprl.primat.lu.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * 
 * @author mfranke
 *
 */
public class RecordClusterSpace {

	private Map<Record, RecordCluster> recordClusters;

	public RecordClusterSpace() {
		this.recordClusters = new HashMap<Record, RecordCluster>();
	}

	public boolean isPresent(Record record) {
		return this.recordClusters.containsKey(record);
	}

	public RecordCluster getCluster(Record record) {
		return this.recordClusters.get(record);
	}

	public void addRecordCluster(Record record) {
		this.recordClusters.put(record, new RecordCluster(record));
	}

	public Collection<RecordCluster> getRecordClusters() {
		return this.recordClusters.values();
	}

	@Override
	public String toString() {
		final StringJoiner sj = new StringJoiner("\n");
		this.recordClusters.forEach((k, v) -> {
			sj.add(v.toString());
		});
		return sj.toString();
	}
}