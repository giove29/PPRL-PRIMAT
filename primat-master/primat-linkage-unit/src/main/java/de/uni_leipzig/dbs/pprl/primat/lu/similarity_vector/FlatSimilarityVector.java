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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * @author mfranke
 *
 */
public class FlatSimilarityVector {

	private Map<Integer, Double> similarityVector;

	public FlatSimilarityVector() {
		this.similarityVector = new TreeMap<>();
	}

	public void setSimilarity(int pos, Double similarities) {
		this.similarityVector.put(pos, similarities);
	}

	public Double getSimilarity(int pos) {
		return this.similarityVector.get(pos);
	}

	public List<Double> getSimilarities() {
		return new ArrayList<Double>(this.similarityVector.values());
	}

}