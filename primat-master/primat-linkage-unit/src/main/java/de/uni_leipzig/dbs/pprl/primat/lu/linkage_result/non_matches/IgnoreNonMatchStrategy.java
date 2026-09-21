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
package de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches;

import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author     mfranke
 *
 * @param  <T>
 */
public class IgnoreNonMatchStrategy<T extends Linkable> implements NonMatchStrategy<T> {

	public IgnoreNonMatchStrategy() {
	}

	@Override
	public boolean isContained(Record left, T right) {
		return false;
	}

	@Override
	public boolean add(Record left, T right, SimilarityVector v, double weight) {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Set<LinkedPair<T>> getNonMatches() {
		return Set.of();
	}
}