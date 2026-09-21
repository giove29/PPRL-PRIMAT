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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.set;

import java.util.HashSet;
import java.util.Set;


/**
 * 
 * @author mfranke
 *
 * @param  <T>
 */
public class JaccardSimilarity<T> extends SetSimilarityFunction<T> {

	@Override
	public double calculateSim(Set<T> left, Set<T> right) {
		final Set<T> unionSet = new HashSet<>(left);
		unionSet.addAll(right);
		final int intersectionSize = left.size() + right.size() - unionSet.size();
		return 1.0d * intersectionSize / unionSet.size();

	}
}