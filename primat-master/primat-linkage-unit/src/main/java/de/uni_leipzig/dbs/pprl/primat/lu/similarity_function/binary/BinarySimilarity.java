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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary;

import java.util.BitSet;

import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.SimilarityFunction;

/**
 * 
 * @author mfranke
 *
 */
public enum BinarySimilarity implements SimilarityFunction<BitSet> {
	
	BRAUN_BLANQUET_SIMILARITY {
		@Override
		public double calculateSimilarity(BitSet left, BitSet right) {
			final int card1 = left.cardinality();
			final int card2 = right.cardinality();
			final int and = BitSetUtils.and(left, right).cardinality();

			final double nom = (double) and;
			final double divisor = (double) (Math.max(card1, card2));

			final double result = nom / divisor;

			return result;
		}
	},

	COSINE_SIMILARITY {
		@Override
		// = |A AND B| / (SQRT(|A| * |B|) ?
		public double calculateSimilarity(BitSet left, BitSet right) {
			final BitSet and = BitSetUtils.and(left, right);
			final double dotProduct = and.cardinality();

			final double leftL2Norm = Math.sqrt(left.cardinality());
			final double rightL2Norm = Math.sqrt(right.cardinality());

			final double cosineSim = dotProduct / (leftL2Norm * rightL2Norm);
			return cosineSim;
		}
	},
	// Dice = 2*Jaccard / (1 + Jaccard)
	// Semimetric (does not fulfill the triangle inequality)
	DICE_SIMILARITY {
		@Override
		public double calculateSimilarity(BitSet left, BitSet right) {
			final int card1 = left.cardinality();
			final int card2 = right.cardinality();
			final int and = BitSetUtils.and(left, right).cardinality();

			final double nom = (double) (2 * and);
			final double divisor = (double) (card1 + card2);

			return nom / divisor;
		}
	},

	HAMMING_SIMILARITY {
		@Override
		public double calculateSimilarity(BitSet left, BitSet right) {
			final double xor = BitSetUtils.xor(left, right).cardinality();
			return 1 - (xor / Math.max(left.length(), right.length()));
		}
	},

	// Jaccard = Dice / (2 - Dice)
	// Metric
	JACCARD_SIMILARITY {
		@Override
		public double calculateSimilarity(BitSet left, BitSet right) {
			final int card1 = left.cardinality();
			final int card2 = right.cardinality();
			final int and = BitSetUtils.and(left, right).cardinality();

			final double nom = (double) and;
			final double divisor = (double) (card1 + card2 - and);

			return nom / divisor;
		}
	},
	// A.k.a. Simpson Similarity
	OVERLAP_SIMILARITY {
		@Override
		public double calculateSimilarity(BitSet left, BitSet right) {
			final int card1 = left.cardinality();
			final int card2 = right.cardinality();
			final int and = BitSetUtils.and(left, right).cardinality();

			final double nom = (double) and;
			final double divisor = (double) (Math.min(card1, card2));

			final double result = nom / divisor;

			return result;
		}
	},

	STRICT_OVERLAP_SIMILARITY {
		@Override
		public double calculateSimilarity(BitSet left, BitSet right) {
			final double overlapSim = OVERLAP_SIMILARITY.calculateSimilarity(left, right);
			return overlapSim < 1d ? 0d : 1d;
		}
	};
}
