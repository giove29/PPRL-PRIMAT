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
 * Randomized Response is a technique that was initially proposed within the
 * RAPPOR framework by Google (Fanti et. al.: Building a rappor with the
 * unknown: Privacy-preserving learning of associations and data dictionaries,
 * 2015).
 * 
 * In [Alaggan et. al.: BLIP: Non-interactive differentially-private similarity
 * computation on bloom filters, 2012] the concept was applied on Bloom filters,
 * tagged as BLoom-and-flIP (BLIP).
 * 
 * The basic idea is to change a bit based on a probability f. The bit is
 * changed equally to one or zero.
 * 
 * In [Schnell, Borgs: Randomized Response and Balanced Bloom Filters for
 * Privacy Preserving Record Linkage, 2016] the concept is evaluated for Bloom
 * filters used in the context of PPRL.
 * 
 * @author mfranke
 *
 */
public class RandomizedResponse implements BloomFilterHardener {

	// TODO: Unify for all hardener
	private final double truth;
	private final double yes;
	private final Random rnd;

	/**
	 * 
	 * @param probability for changing a bit.
	 * @param seed        value for the random number generator.
	 */
	public RandomizedResponse(double probability, long seed) {
		this.truth = 1 - probability;
		this.yes = 0.5d * probability;
		this.rnd = new Random(seed);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		final BitSet bs = (BitSet) bf.getBitVector().clone();
		final int size = bf.getSize();

		for (int i = 0; i < size; i++) {

			final double r = rnd.nextDouble();
			if (r <= this.truth) {
				// Leave bit as it is.
			}
			else if (r <= this.truth + this.yes) {
				bs.set(i);
			}
			else {
				bs.clear(i);
			}
		}

		return new BloomFilter(size, bs);
	}
}