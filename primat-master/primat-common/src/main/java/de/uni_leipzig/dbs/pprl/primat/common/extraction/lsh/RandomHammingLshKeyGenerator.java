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

import java.util.Random;


/**
 * 
 * @author mfranke
 *
 */
public class RandomHammingLshKeyGenerator extends HammingLshKeyGenerator {

	public RandomHammingLshKeyGenerator(Integer keySize, Integer keys, Integer valueRange, Long seed) {
		super(keySize, keys, valueRange, seed);
	}

	@Override
	protected Integer[] getPositions() {
		final Random rnd = new Random(this.seed);
		final int positions = this.keys * this.keySize;

		if (positions > this.valueRange) {// remove for overlapping keys
			throw new RuntimeException("HLSH key size is to large or to many keys!");
		}

		return rnd.ints(0, this.valueRange).distinct() // remove for overlapping
														// keys
			.limit(positions).boxed().toArray(Integer[]::new);
	}

	@Override
	public String toString() {
		return super.toString() + " (Random)";
	}
}