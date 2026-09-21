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
 * Implements the balanced Bloom filter technique proposed by Schnell et. al. A
 * balanced Bloom filter is constructed by adding a negative copy to the Bloom
 * filter and then permute the bits. The aim of this process is to unify the
 * Hamming weights across a set of Bloom filters.
 * 
 * @author mfranke
 *
 */
public class Balancer implements BloomFilterHardener {

	private final long seed;

	/**
	 * Constructs a new Bloom filter balancer.
	 * 
	 * @param seed value for the permutation process.
	 */
	public Balancer(long seed) {
		this.seed = seed;
	}

	/**
	 * {@inheritDoc}}
	 */
	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		final BitSet bitset = bf.getBitVector();
		final BitSet complement = BitSetUtils.not(bitset);
		final BitSet concat = BitSetUtils.concatenateLongWords(bitset, complement);
		final BitSet permConcat = BitSetUtils.permuteBytes(concat, this.seed);

		return new BloomFilter(2 * bf.getSize(), permConcat);
	}

}