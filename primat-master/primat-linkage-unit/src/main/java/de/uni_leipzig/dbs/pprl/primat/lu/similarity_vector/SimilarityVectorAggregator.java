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

/**
 * Aggregates a {@link SimilarityVector} into a single double value.
 * 
 * @author mfranke
 *
 */
public class SimilarityVectorAggregator {

	private SimilarityVectorFlattener flattener;
	private FlatSimilarityVectorAggregator aggregator;

	public SimilarityVectorAggregator(SimilarityVectorFlattener flattener, FlatSimilarityVectorAggregator aggregator) {
		this.flattener = flattener;
		this.aggregator = aggregator;
	}

	public Double aggregate(SimilarityVector similarityVector) {
		final FlatSimilarityVector flatSimVec = this.flattener.flatten(similarityVector);
		final Double agg = this.aggregator.aggregate(flatSimVec);
		return agg;
	}

}