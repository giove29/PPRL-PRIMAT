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

package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author mfranke
 *
 */
public class WeightingFunction {

	private List<Double> weights;

	public WeightingFunction(List<Double> weights) {
		this.weights = weights;
	}

	public List<Double> applyWeights(final List<Double> values) {

		if (values.size() != this.weights.size()) {
			throw new IllegalArgumentException();
		}

		final List<Double> result = new ArrayList<>(values.size());

		for (int i = 0; i < result.size(); i++) {
			final Double weightedValue = values.get(i) * this.weights.get(i);
			result.add(weightedValue);
		}
		return result;
	}
}