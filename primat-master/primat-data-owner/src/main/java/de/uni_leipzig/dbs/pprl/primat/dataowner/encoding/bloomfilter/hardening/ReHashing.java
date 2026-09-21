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
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening;

import java.util.BitSet;
import java.util.Random;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;


/**
 * Re-hashing is a technique that applies a sliding window on a Bloom filter.
 * The bit sequence in the current window is interpreted as {@link Integer}
 * value. This value is used as input for a random number generator that is used
 * to generate a specified number of rehashed values. This procedure should
 * increase the privacy properties of Bloom filters. It is clear that this will
 * be at the expanse of quality.
 * 
 * @author mfranke
 *
 */
public class ReHashing implements BloomFilterHardener {

	private final int windowSize;
	private final int stepSize;
	private final int rehashedValues;
	private final int rehashedBfSize;
	private final long seed;

	/**
	 * Creates a new re-hashing instance.
	 * 
	 * @param windowSize     size (number of bits) of the sliding window.
	 * @param stepSize       number of positions the sliding window is shifted
	 *                       to the right in each step.
	 * @param rehashedValues number of positions that are re-hashed in each
	 *                       step.
	 * @param rehashedBfSize size of the hardened Bloom filter.
	 * @param seed           value for the random number generator.
	 */
	public ReHashing(int windowSize, int stepSize, int rehashedValues, int rehashedBfSize, long seed) {
		this.windowSize = windowSize;
		this.stepSize = stepSize;
		this.rehashedValues = rehashedValues;
		this.rehashedBfSize = rehashedBfSize;
		this.seed = seed;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		final BitSet bitSet = bf.getBitVector();
		final int bitSetSize = bf.getSize();

		final BloomFilter hardenedBf = new BloomFilter(rehashedBfSize);

		int windowStart = 0;
		int windowEnd = windowStart + windowSize;
		int windowIndex = 0;

		while (windowEnd < bitSetSize) {
			final BitSet currentWindow = bitSet.get(windowStart, windowEnd);
			final long[] words = currentWindow.toLongArray();

			final long windowValue = words.length == 0 ? 0 : words[0];
			final Random rnd = new Random(this.seed + windowIndex); // +
																	// windowValue);
																	// // +
																	// this.stepSize

			final int windowSeed = rnd.nextInt();
			final Random windowRnd = new Random(windowSeed + windowValue);

			for (int i = 0; i < this.rehashedValues; i++) {
				final int position = windowRnd.nextInt(rehashedBfSize);
				hardenedBf.setPosition(position);
			}

			windowStart = windowStart + this.stepSize;
			windowEnd = windowEnd + this.stepSize;
			windowIndex++;
		}

		return hardenedBf;
	}
}