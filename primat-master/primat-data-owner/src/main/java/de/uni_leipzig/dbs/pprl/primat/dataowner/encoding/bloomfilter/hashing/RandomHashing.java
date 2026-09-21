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
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.uni_leipzig.dbs.pprl.primat.common.utils.HMacAlgorithm;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HashUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.RandomFactory;


/**
 * Random hashing method.
 * 
 * @author mfranke
 *
 */
public class RandomHashing extends HashingMethod {

	public static final String DEFAULT_KEY = "SECRET";

	private final RandomFactory rndFac;
	private final String key;

	public RandomHashing(int bfSize, RandomFactory rndFac) {
		this(bfSize, DEFAULT_KEY, rndFac);
	}

	public RandomHashing(int bfSize, String key, RandomFactory rndFac) {
		super(bfSize);
		this.rndFac = rndFac;

		if (key != null && !key.equals("")) {
			this.key = key;
		}
		else {
			this.key = DEFAULT_KEY;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Integer> hashToInts(String element, int hashFunctions) {
		final String saltedInput = element + this.salt;
		final Random rnd = this.getRandom(saltedInput, this.key);
		
		final List<Integer> positions = new ArrayList<>(hashFunctions);

		for (int i = 0; i < hashFunctions; i++) {
			final int pos = rnd.nextInt(this.bfSize);
			positions.add(pos);
		}

		return positions;
	}

	private Random getRandom(String token, String key) {
		final byte[] byteHash = HashUtils.getHmac(HMacAlgorithm.HMAC_SHA_384, token, key);
		final long hash = ByteBuffer.wrap(byteHash).getLong();
		
		final Random rnd = this.rndFac.getRandom();
		rnd.setSeed(hash);
		return rnd;
	}

}