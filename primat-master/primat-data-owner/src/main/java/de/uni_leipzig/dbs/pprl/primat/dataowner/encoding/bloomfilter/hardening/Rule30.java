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

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;


/**
 * 
 * @author mfranke
 *
 */
public class Rule30 implements BloomFilterHardener {

	public Rule30() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {

		final BitSet bitset = bf.getBitVector();
		final int size = bf.getSize();

		final BitSet result = new BitSet(size);

		for (int i = 0; i < size; i++) {
			final boolean previous = i == 0 ? bitset.get(size - 1) : bitset.get(i - 1);
			final boolean current = bitset.get(i);
			final boolean next = i == size - 1 ? bitset.get(0) : bitset.get(i + 1);
			final boolean newValue = previous ^ (current || next);
			result.set(i, newValue);
		}

		return new BloomFilter(size, result);
	}

	@Override
	public QidAttribute<?> harden(BloomFilter bf) {
		final BloomFilter hardBf = this.hardenBloomFilter(bf);
		return new BitSetAttribute(hardBf.getBitVector());
	}
}