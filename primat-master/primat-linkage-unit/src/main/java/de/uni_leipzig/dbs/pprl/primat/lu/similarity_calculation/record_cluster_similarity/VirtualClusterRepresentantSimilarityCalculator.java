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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_cluster_similarity;

import java.util.BitSet;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.integer_array.IntegerArraySimilarityFunction;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class VirtualClusterRepresentantSimilarityCalculator implements RecordClusterSimilarityCalculator {

	private final IntegerArraySimilarityFunction simFunc;

	public VirtualClusterRepresentantSimilarityCalculator(IntegerArraySimilarityFunction simFunc) {
		this.simFunc = simFunc;
	}

	@Override
	public SimilarityVector calculateSimilarity(Record left, Cluster right) {
		final BitSet leftBitSet = left.getBitSetAttributes().get(0).getValue();
		// TODO: BitSet Count??
		// TODO: BitSet attribute first?
		final Integer[] leftArray = ArrayUtils.toObject(BitSetUtils.toIntArray(1024, leftBitSet));
		final Integer[] rightArray = ArrayUtils.toObject(right.getVirtualRepresentant().getData());

		final double similarity = this.simFunc.calculateSimilarity(leftArray, rightArray);
		final SimilarityVector simVec = new SimilarityVector();
		simVec.setSimilarities(0, List.of(similarity));
		simVec.setAggregatedValue(similarity);

		return simVec;
	}
}