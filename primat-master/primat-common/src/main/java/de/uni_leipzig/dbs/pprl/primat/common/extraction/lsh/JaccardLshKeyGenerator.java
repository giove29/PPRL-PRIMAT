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
import java.util.Collections;
import java.util.List;
import java.util.Random;


/**
 * 
 * @author mfranke
 *
 */
public class JaccardLshKeyGenerator extends LshKeyGenerator {

	public JaccardLshKeyGenerator(Integer keySize, Integer keys, Integer valueRange, Long seed) {
		super(keySize, keys, valueRange, seed);
	}

	@Override
	public List<LshBlockingFunction> generate() {
		final List<Integer> positions = this.getListOfPositions();
		final List<LshBlockingFunction> res = new ArrayList<>(this.keys);

		for (int keys = 0; keys < this.keys; keys++) {
			final List<List<Integer>> permutationsForKey = new ArrayList<>(this.keySize);

			for (int keySize = 0; keySize < this.keySize; keySize++) {
				final long seed = this.seed + keySize + this.seed * keys;
				final Random rnd = new Random(seed);

				final List<Integer> permutation = new ArrayList<Integer>(positions);
				Collections.shuffle(permutation, rnd);
				permutationsForKey.add(permutation);
			}
			final JaccardLshBlockingFunction key = new JaccardLshBlockingFunction(permutationsForKey);
			res.add(key);
		}
		return res;
	}

	private List<Integer> getListOfPositions() {
		final List<Integer> positions = new ArrayList<Integer>(this.valueRange);

		for (int i = 0; i < this.valueRange; i++) {
			positions.add(i);
		}
		return positions;
	}

	@Override
	public String toString() {
		return "Jaccard";
	}

}
