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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity;

import java.util.ArrayList;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.SimilarityFunction;


/**
 * 
 * @author mfranke
 *
 */
public abstract class AttributeSimilarityCalculator<T extends QidAttribute<U>, U> {

	private List<SimilarityFunction<U>> similarityFunctions;

	public AttributeSimilarityCalculator(List<SimilarityFunction<U>> list) {
		this.similarityFunctions = list;
	}

	public List<Double> calculateSimilarities(T left, T right) {
		final List<Double> similarities = new ArrayList<>(this.similarityFunctions.size());

		for (final SimilarityFunction<U> simFunc : this.similarityFunctions) {
			final double similarity = simFunc.calculateSimilarity(left, right);
			similarities.add(similarity);
		}

		return similarities;
	}

	public abstract List<Double> visit(AttributeSimilarityCalculatorVisitor visitor);

}