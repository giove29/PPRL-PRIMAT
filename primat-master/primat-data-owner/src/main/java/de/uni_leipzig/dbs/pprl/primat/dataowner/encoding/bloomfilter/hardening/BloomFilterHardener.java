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

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;


/**
 * Interface for Bloom filter hardening techniques that are applied <b>after</b>
 * the actual creation of the Bloom filter, i.e., after the elements of the set
 * to represent are mapped into the Bloom filter.
 * 
 * @author mfranke
 *
 */
public interface BloomFilterHardener {

	/**
	 * Apply a hardening function and return the hardened Bloom filter.
	 * 
	 * @param  bf the input Bloom filter to harden.
	 * @return    the hardened output Bloom filter.
	 */
	public default QidAttribute<?> harden(BloomFilter bf) {
		final BloomFilter hardBf = this.hardenBloomFilter(bf);
		return new BitSetAttribute(hardBf.getBitVector());
	}

	/**
	 * Apply a hardening function and return the hardened Bloom filter.
	 *
	 * @param  bf the input Bloom filter to harden.
	 * @return    the hardened output Bloom filter.
	 */
	public BloomFilter hardenBloomFilter(BloomFilter bf);

	/**
	 * Declares, without needing an actual {@link BloomFilter} instance, how many
	 * bits a filter of {@code inputLength} bits will have after this hardener is
	 * applied. Default is the identity (length unchanged), correct for any
	 * hardener that only flips/perturbs bits rather than resizing the filter
	 * (e.g. {@link NoHardener}, {@link RandomizedResponse}/BLIP); a hardener that
	 * changes the length (e.g. {@link XorFolder}) must override this.
	 *
	 * @param  inputLength length in bits of the filter before this hardener.
	 * @return             length in bits of the filter after this hardener.
	 */
	public default int resultingLength(int inputLength) {
		return inputLength;
	}

}
