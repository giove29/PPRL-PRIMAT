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
package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.BitSet;
import java.util.List;
import java.util.Random;


/**
 * 
 * @author mfranke
 *
 */
public class WeightedRandomSamplingBitSetAggregator implements BitSetListAggregator {

	private final long seed;
	private final int finalBitSetLength;
	private final List<Double> weights;
	private final List<Integer> bsSizes;
	
	public WeightedRandomSamplingBitSetAggregator(List<Double> weights, List<Integer> sizes, int finalBitSetLength, long seed) {
		final double sum = weights.stream().mapToDouble(Double::doubleValue).sum();
		
		if (sum == 1.0d) {
			throw new IllegalArgumentException("Weights must sum to 1!");
		}
		
		if (sizes.size() != weights.size()) {
			throw new IllegalArgumentException("Different length of input specifications!");
		}
			
		this.weights = weights;
		this.bsSizes = sizes;
		this.finalBitSetLength = finalBitSetLength;
		this.seed = seed;
	}
	
	public WeightedRandomSamplingBitSetAggregator(List<Double> weights, List<Integer> sizes, int finalBitSetLength) {
		this(weights, sizes, finalBitSetLength, 42L);
	}
	
	@Override
	public BitSet aggregate(List<BitSet> values) {
		if (weights.size() != values.size()) {
			throw new IllegalArgumentException("List must have the same length as the weight coefficients!");
		}
		
		final Random rnd = new Random(this.seed);
		
		final StringBuilder bitsetString = new StringBuilder(finalBitSetLength);
		
		for (int i = 0; i < values.size(); i++) {
			 final BitSet bitset = values.get(i);
			 final Double weight = this.weights.get(i);
			 final Integer bsSize = this.bsSizes.get(i);
			 
			 final int positionsToSample = (int) Math.floor(weight.doubleValue() * finalBitSetLength);
			 final int[] sample = rnd.ints(positionsToSample, 0, bsSize).toArray();
			 for (final int rndBit : sample) {
				 final String rndBitValue = bitset.get(rndBit) ? "1" : "0";
				 bitsetString.append(rndBitValue);
			 }
		}
		
		return BitSetUtils.fromBinaryBigEndian(bitsetString.toString());
	}	
}