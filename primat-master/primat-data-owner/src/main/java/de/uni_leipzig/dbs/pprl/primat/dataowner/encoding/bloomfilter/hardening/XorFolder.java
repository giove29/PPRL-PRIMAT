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

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.XorBitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;


/**
 * 
 * Initially XOR-folding was proposed in the chemo-informatics domain, to speed
 * up searching chemical data bases.
 * 
 * XOR-folding for Bloom filters used in PPRL context was proposed in [Schnell,
 * Borgs: XOR-Folding for hardening Bloom Filter-based Encryptions for
 * Privacy-preserving Record Linkage, 2016].
 * 
 * The main idea is to fold a bit array n-times by using the XOR function, i.e.,
 * the bit array is split in two halves and both halves are combined by XORing
 * them.
 * 
 * 
 * @author mfranke
 *
 */
public class XorFolder implements BloomFilterHardener {

	private final int n;

	/**
	 * Initialize a new XOR-Folder that folds a Bloom filter <b>n</b>-times.
	 * 
	 * @param n the number of times the Bloom filter is folded.
	 */
	public XorFolder(int n) {
		this.n = n;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		BloomFilter res = bf;

		for (int i = 0; i < n; i++) {
			res = foldXor(res);
		}

		return res;
	}

	@Override
	public QidAttribute<?> harden(BloomFilter bf) {
		final BloomFilter hardBf = this.hardenBloomFilter(bf);
		return new XorBitSetAttribute(bf.cardinality(), hardBf.getBitVector());
	}

	private BloomFilter foldXor(BloomFilter bf) {
		final BitSet bitset = bf.getBitVector();
		final int size = bf.getSize();

		final BitSet firstHalf = bitset.get(0, size / 2);
		final BitSet secondHalf = bitset.get(size / 2, size);

		final BitSet foldedBitset = BitSetUtils.xor(firstHalf, secondHalf);
		return new BloomFilter(size / 2, foldedBitset);
	}

}