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
import de.uni_leipzig.dbs.pprl.primat.common.utils.StringUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.SimilarityFunction;


/**
 * 
 * @author mfranke
 *
 */
public class BitMaskSimilarityFunction implements SimilarityFunction<BitSet> {

	private BitSet bitMask;
	private BinarySimilarity similarity;

	public BitMaskSimilarityFunction(BinarySimilarity similarity, BitSet bitMask) {

		if (bitMask == null) {
			throw new IllegalArgumentException();
		}
		this.bitMask = bitMask;
	}

	private BitSet applyBitMask(BitSet bitVector) {
		return BitSetUtils.andNot(bitVector, this.bitMask);
	}

	@Override
	public double calculateSimilarity(BitSet left, BitSet right) {

		if (left == null || right == null) {
			return 0d;
		}

		if (left.isEmpty() || right.isEmpty()) {
			return 0d;
		}

		final BitSet leftMasked = this.applyBitMask(left);
		final BitSet rightMasked = this.applyBitMask(right);
		return similarity.calculateSimilarity(leftMasked, rightMasked);
	}

	@Override
	public String toString() {
		return StringUtils.upperCamelCaseToTitleCase(this.getClass().getSimpleName());
	}
}