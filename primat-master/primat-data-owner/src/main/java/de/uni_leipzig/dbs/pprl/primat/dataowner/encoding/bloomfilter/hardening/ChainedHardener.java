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

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;

/**
 * Applies a sequence of {@link BloomFilterHardener} steps in order, on top of
 * the output of the previous one.
 *
 * The resulting {@link QidAttribute} type is decided by the <b>last</b> step
 * (via its own {@link BloomFilterHardener#harden(BloomFilter)}), not just its
 * bitset: e.g. if the last step is an {@link XorFolder}, the chain still
 * produces a {@code XorBitSetAttribute} carrying the cardinality of the
 * bitset at that point in the chain (i.e. after all previous steps), which is
 * what downstream similarity calculation expects.
 *
 * @author mfranke
 *
 */
public class ChainedHardener implements BloomFilterHardener {

	private final List<BloomFilterHardener> steps;

	/**
	 * @param steps the hardening steps to apply in order; must not be empty.
	 */
	public ChainedHardener(List<BloomFilterHardener> steps) {
		this.steps = steps;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BloomFilter hardenBloomFilter(BloomFilter bf) {
		BloomFilter res = bf;
		for (final BloomFilterHardener step : steps) {
			res = step.hardenBloomFilter(res);
		}
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public QidAttribute<?> harden(BloomFilter bf) {
		BloomFilter res = bf;
		for (int i = 0; i < steps.size() - 1; i++) {
			res = steps.get(i).hardenBloomFilter(res);
		}
		return steps.get(steps.size() - 1).harden(res);
	}
}
