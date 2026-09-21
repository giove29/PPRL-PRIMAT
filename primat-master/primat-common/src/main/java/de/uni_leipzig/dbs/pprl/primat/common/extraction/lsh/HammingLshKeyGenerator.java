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
import java.util.List;


/**
 * 
 * @author mfranke
 *
 */
public abstract class HammingLshKeyGenerator extends LshKeyGenerator {

	public HammingLshKeyGenerator(Integer keySize, Integer keys, Integer valueRange, Long seed) {
		super(keySize, keys, valueRange, seed);
	}

	protected abstract Integer[] getPositions();

	@Override
	public List<LshBlockingFunction> generate() {
		final List<LshBlockingFunction> lshBkFuncs = new ArrayList<>(this.keys);
		final List<List<Integer>> keyPositions = this.generatePositions();

		for (final List<Integer> positions : keyPositions) {
			final LshBlockingFunction bkFunc = new HammingLshBlockingFunction(positions);
			lshBkFuncs.add(bkFunc);
		}

		return lshBkFuncs;
	}

	public List<List<Integer>> generatePositions() {
		final Integer[] randomPositions = this.getPositions();

		final List<List<Integer>> keyPositions = new ArrayList<>(this.keys);

		for (int keyIndex = 0; keyIndex < this.keys; keyIndex++) {
			final int offset = keyIndex * this.keySize;
			final List<Integer> positions = new ArrayList<>(this.keySize);

			for (int posIndex = 0; posIndex < this.keySize; posIndex++) {
				final int position = randomPositions[posIndex + offset];
				positions.add(position);
			}

			keyPositions.add(positions);
		}

		return keyPositions;
	}

	@Override
	public String toString() {
		return "Hamming";
	}

}