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

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.utils.ListAggregator;


/**
 * 
 * @author mfranke
 *
 */
public class BaseSimilarityVectorFlattener implements SimilarityVectorFlattener {

	private List<ListAggregator<Double>> aggregators;

	public BaseSimilarityVectorFlattener(List<ListAggregator<Double>> aggregators) {
		this.aggregators = aggregators;
	}

	public FlatSimilarityVector flatten(SimilarityVector similarityVector) {

		if (this.aggregators.size() != similarityVector.getDimension()) {
			throw new IllegalArgumentException();
		}

		final FlatSimilarityVector flatVector = new FlatSimilarityVector();

		for (int i = 0; i < this.aggregators.size(); i++) {
			final List<Double> similarities = similarityVector.getSimilarities(i);
			final ListAggregator<Double> aggregationMode = this.aggregators.get(i);
			final Double sim = aggregationMode.aggregate(similarities);
			flatVector.setSimilarity(i, sim);
		}

		return flatVector;
	}
}