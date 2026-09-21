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
 * Ranbaduge and Schnell: Securing Bloom Filters for Privacy-preserving Record
 * Linkage (CIKM 2020)
 * 
 * @author mfranke
 *
 */
public class ReSamplingXor implements BloomFilterHardener {

	private final long seed;

	public ReSamplingXor(long seed) {
		this.seed = seed;
	}

	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		final Random rnd = new Random(this.seed);
		final BloomFilter hardBf = new BloomFilter(bf.getSize());
		final BitSet bs = bf.getBitVector();

		for (int i = 0; i < bf.getSize(); i++) {
			final int pos1 = rnd.nextInt(bf.getSize());
			final int pos2 = rnd.nextInt(bf.getSize());

			final boolean value = bs.get(pos1) ^ bs.get(pos2);
			hardBf.setPosition(i, value);
		}

		return hardBf;
	}

}
