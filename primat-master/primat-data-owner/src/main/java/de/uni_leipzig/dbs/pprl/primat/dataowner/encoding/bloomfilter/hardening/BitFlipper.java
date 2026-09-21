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
 * 
 * @author mfranke
 *
 */
public class BitFlipper implements BloomFilterHardener {

	private final double probability;
	private final Random rnd;

	public BitFlipper(double probability, long seed) {
		this.probability = probability;
		this.rnd = new Random(seed);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		final BloomFilter hardBf = new BloomFilter(bf);
		final BitSet bs = hardBf.getBitVector();

		for (int i = 0; i < hardBf.getSize(); i++) {

			if (this.rnd.nextDouble() <= this.probability) {
				bs.flip(i);
			}
			else {
				// nothing to do here
			}
		}

		return hardBf;
	}
}
