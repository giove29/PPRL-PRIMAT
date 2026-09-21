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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.string;

import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.SimilarityFunction;


/**
 * 
 * @author mfranke
 *
 */
public abstract class StringSimilarityFunction implements SimilarityFunction<String> {

	@Override
	public double calculateSimilarity(String left, String right) {

		if (left == null || right == null) {
			return 0d;
		}

		if (left.length() == 0 || right.length() == 0) {
			return 0d;
		}

		if (left.equals(right)) {
			return 1d;
		}

		return this.calculateSim(left, right);
	}

	protected abstract double calculateSim(String left, String right);
}