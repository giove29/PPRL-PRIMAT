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
package de.uni_leipzig.dbs.pprl.primat.lu.classification;

import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorAggregator;


/**
 * 
 * @author mfranke
 *
 */
public class ThresholdClassificator implements Classificator {

	private double upperThreshold;
	private double lowerThreshold;
	private SimilarityVectorAggregator similarityAggregator;

	public ThresholdClassificator(double threshold, SimilarityVectorAggregator similarityAggregator) {
		this(threshold, threshold, similarityAggregator);
	}

	public ThresholdClassificator(double upperThreshold, double lowerThreshold,
		SimilarityVectorAggregator similarityAggregator) {
		this.upperThreshold = upperThreshold;
		this.lowerThreshold = lowerThreshold;
		this.similarityAggregator = similarityAggregator;
	}

	public static ThresholdClassificator getSingleThresholdClassificator(double threshold,
		SimilarityVectorAggregator similarityAggregator) {
		return new ThresholdClassificator(Double.MAX_VALUE, threshold, similarityAggregator);
	}

	@Override
	public MatchStatus classify(SimilarityVector similarityVector) {
		final Double similarity = this.similarityAggregator.aggregate(similarityVector);
		similarityVector.setAggregatedValue(similarity);
		
		if (similarity != null) {

			if (similarity.compareTo(this.upperThreshold) >= 0) {
				return MatchStatus.DEFINITE_MATCH;
			}
			else if (similarity.compareTo(this.lowerThreshold) >= 0) {
				return MatchStatus.POSSIBLE_MATCH;
			}
			else {
				return MatchStatus.NON_MATCH;
			}
		}
		else {
			return MatchStatus.UNKNOWN;
		}
	}

}