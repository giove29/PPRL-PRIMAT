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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class RecordCluster {

	private Record leftRecord;
	private Map<Record, SimilarityVector> rightRecords;

	public RecordCluster(Record leftRecord) {
		this.leftRecord = leftRecord;
		this.rightRecords = new HashMap<Record, SimilarityVector>();
	}

	public Record getLeftRecord() {
		return this.leftRecord;
	}

	public Map<Record, SimilarityVector> getRightRecordsWithSimilarity() {
		return this.rightRecords;
	}

	public Set<Record> getRightRecords() {
		return this.rightRecords.keySet();
	}

	public boolean addRightRecord(Record rightRecord, SimilarityVector similarityVector) {

		if (this.rightRecords.containsKey(rightRecord)) {
			return false;
		}
		else {
			this.rightRecords.put(rightRecord, similarityVector);
			return true;
		}
	}

	public boolean isPresent(Record rightRecord) {
		return this.rightRecords.containsKey(rightRecord);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(leftRecord.getId());
		sb.append(": {");

		final StringJoiner sj = new StringJoiner("; ");
		this.rightRecords.forEach((k, v) -> {
			sj.add("(" + k.getId() + "," + v.toString() + ")");
		});
		sb.append(sj.toString());
		sb.append("}");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((leftRecord == null) ? 0 : leftRecord.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof RecordCluster)) {
			return false;
		}
		RecordCluster other = (RecordCluster) obj;

		if (leftRecord == null) {

			if (other.leftRecord != null) {
				return false;
			}
		}
		else if (!leftRecord.equals(other.leftRecord)) {
			return false;
		}
		return true;
	}
}