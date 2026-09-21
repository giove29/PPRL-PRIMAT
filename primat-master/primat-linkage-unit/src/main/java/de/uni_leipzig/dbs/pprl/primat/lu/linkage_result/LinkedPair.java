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
package de.uni_leipzig.dbs.pprl.primat.lu.linkage_result;

import java.util.Objects;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.Pair;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author     mfranke
 *
 * @param  <T>
 */
public class LinkedPair<T extends Linkable> {

	private Pair<Record, T> pair;
	private SimilarityVector simVec;
	private double aggregatedSim;

	public LinkedPair(Record left, T right) {
		this(new Pair<>(left, right), null, 0d);
	}

	public LinkedPair(Record left, T right, SimilarityVector simVec, double aggregatedSim) {
		this(new Pair<>(left, right), simVec, aggregatedSim);
	}

	public LinkedPair(Pair<Record, T> pair, SimilarityVector simVec, double aggregatedSim) {
		this.pair = pair;
		this.simVec = simVec;
		this.aggregatedSim = aggregatedSim;
	}

	public Record getLeftRecord() {
		return this.pair.getLeft();
	}

	public T getRight() {
		return this.pair.getRight();
	}

	public Linkable getRightLinkable() {
		return this.pair.getRight();
	}

	public Pair<Record, T> getPair() {
		return this.pair;
	}

	public void setRecordPair(Pair<Record, T> recordPair) {
		this.pair = recordPair;
	}

	public SimilarityVector getSimVec() {
		return simVec;
	}

	public void setSimVec(SimilarityVector simVec) {
		this.simVec = simVec;
	}

	public double getAggregatedSim() {
		return aggregatedSim;
	}

	public void setAggregatedSim(double aggregatedSim) {
		this.aggregatedSim = aggregatedSim;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.pair);
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		LinkedPair<T> other = (LinkedPair<T>) obj;
		return Objects.equals(pair, other.pair);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(pair);
		builder.append(", ");
		builder.append(aggregatedSim);
		builder.append("]");
		return builder.toString();
	}
}
