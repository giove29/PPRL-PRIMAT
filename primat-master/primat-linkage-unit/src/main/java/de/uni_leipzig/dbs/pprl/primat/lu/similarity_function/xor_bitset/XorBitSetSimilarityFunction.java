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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.xor_bitset;

import java.util.BitSet;

import de.uni_leipzig.dbs.pprl.primat.common.utils.Pair;
import de.uni_leipzig.dbs.pprl.primat.common.utils.StringUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.SimilarityFunction;


/**
 * 
 * @author mfranke
 *
 */
public abstract class XorBitSetSimilarityFunction implements SimilarityFunction<Pair<Integer, BitSet>> {

	public XorBitSetSimilarityFunction() {
	}

	@Override
	public double calculateSimilarity(Pair<Integer, BitSet> left, Pair<Integer, BitSet> right) {

		if (left == null || right == null) {
			return 0d;
		}

		if (left.getRight() == null || left.getRight().isEmpty() || right.getRight() == null
			|| right.getRight().isEmpty()) {
			return 0d;
		}

		return this.calculateSim(left, right);
	}

	protected abstract double calculateSim(Pair<Integer, BitSet> left, Pair<Integer, BitSet> right);

	@Override
	public String toString() {
		return StringUtils.upperCamelCaseToTitleCase(this.getClass().getSimpleName());
	}
}