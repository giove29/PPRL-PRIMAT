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


/**
 * 
 * @author mfranke
 *
 */
public class TightXorFoldingSimilarity extends XorBitSetSimilarityFunction {

	@Override
	public double calculateSim(Pair<Integer, BitSet> left, Pair<Integer, BitSet> right) {
		final int orgCardLeft = left.getLeft();
		final int orgCardRight = right.getLeft();

		final int cardLeft = left.getRight().cardinality();
		final int cardRight = right.getRight().cardinality();

		final double nom = (double) orgCardLeft + orgCardRight - Math.abs(cardLeft - cardRight);
		final double devisor = (double) orgCardLeft + orgCardRight + Math.abs(cardLeft - cardRight);

		return nom / devisor;
	}
}