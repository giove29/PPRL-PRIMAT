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
package de.uni_leipzig.dbs.pprl.primat.lu.distance_function;

import java.util.BitSet;

import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary.BinarySimilarity;


/**
 * 
 * @author mfranke
 *
 */
public enum BinaryDistanceFunction implements DistanceFunction<BitSet> {

	HAMMING_DISTANCE {
		@Override
		public double computeDistance(BitSet left, BitSet right) {
			final BitSet xor = BitSetUtils.xor(left, right);
			return xor.cardinality();
		}
	},

	NORMALIZED_HAMMING_DISTANCE {
		@Override
		public double computeDistance(BitSet left, BitSet right) {
			final double hammingDistance = HAMMING_DISTANCE.computeDistance(left, right);
			final double maxLength = Math.max(left.cardinality(), right.cardinality());
			return hammingDistance / maxLength;
		}
	},

	JACCARD_COEFFICIENT {
		@Override
		public double computeDistance(BitSet left, BitSet right) {
			final double jaccardSim = BinarySimilarity.JACCARD_SIMILARITY.calculateSimilarity(left, right);
			return 1 - jaccardSim;
		}
	};
}