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

import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;


/**
 * Ranbaduge and Schnell: Securing Bloom Filters for Privacy-preserving Record
 * Linkage (CIKM 2020)
 * 
 * @author mfranke
 *
 */
public class WindowXor implements BloomFilterHardener {

	private final int windowSize;

	public WindowXor(int windowSize) {

		if (windowSize < 1) {
			throw new IllegalArgumentException("Window size must be 1 or larger");
		}
		this.windowSize = windowSize;
	}

	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		final BloomFilter hardBf = new BloomFilter(bf);

		final BitSet bs = hardBf.getBitVector();

		for (int w1Start = 0; w1Start < hardBf.getSize() - windowSize; w1Start++) {
			final int w1End = (w1Start + windowSize);

			final BitSet w1 = bs.get(w1Start, w1End);

			final BitSet w2 = new BitSet();

			for (int j = 0; j < windowSize; j++) {
				final int w2Idx = (w1Start + j + 1) % hardBf.getSize();
				final boolean value = bs.get(w2Idx);
				w2.set(j, value);
			}

			final BitSet xor = BitSetUtils.xor(w1, w2);

			for (int j = 0; j < windowSize; j++) {
				final boolean value = xor.get(j);
				bs.set(w1Start + j, value);
			}
		}

		return hardBf;
	}
}