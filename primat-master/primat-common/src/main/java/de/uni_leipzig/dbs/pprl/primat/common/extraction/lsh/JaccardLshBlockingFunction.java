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

package de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.utils.ListUtils;


/**
 * 
 * @author mfranke
 *
 */
public class JaccardLshBlockingFunction extends LshBlockingFunction {

	private List<List<Integer>> permutations;

	public JaccardLshBlockingFunction(List<List<Integer>> permutations) {
		this.permutations = permutations;
	}

	private Integer getNextSetBit(BitSet bitset, List<Integer> perm) {

		for (final Integer pos : perm) {
			final boolean bitValue = bitset.get(pos);

			if (bitValue) {
				return pos;
			}
		}
		return null;
	}

	@Override
	public String apply(BitSet attribute) {
		final List<Integer> bkv = new ArrayList<>(this.permutations.size());

		for (final List<Integer> perm : this.permutations) {
			final Integer pos = this.getNextSetBit(attribute, perm);
			bkv.add(pos);
		}

		return ListUtils.toShortString(bkv);
	}

}