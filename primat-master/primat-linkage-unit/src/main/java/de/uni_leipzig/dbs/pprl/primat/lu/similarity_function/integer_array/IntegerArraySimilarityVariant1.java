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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.integer_array;

/**
 * 
 * @author mfranke
 *
 */
public class IntegerArraySimilarityVariant1 extends IntegerArraySimilarityFunction {

	@Override
	public double calculateSimilarityConcrete(Integer[] left, Integer[] right) {
		int overlap = 0;
		int total = 0;

		for (int i = 0; i < left.length; i++) {
			total = total + left[i] + right[i];

			if (left[i] > 0 && right[i] > 0) {
				overlap = overlap + left[i] + right[i];
			}
		}

		return ((double) overlap) / total;
	}
}