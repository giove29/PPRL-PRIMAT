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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.utils.ByteUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HashUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.MessageDigestAlgorithm;


/**
 * 
 * @author mfranke
 *
 */
public class DoubleHashing extends HashingMethod {

	private MessageDigestAlgorithm hash1;
	private MessageDigestAlgorithm hash2;

	/**
	 * 
	 * @param bfSize
	 */
	public DoubleHashing(int bfSize) {
		this(bfSize, "", MessageDigestAlgorithm.SHA_1, MessageDigestAlgorithm.MD_5);
	}

	/**
	 * 
	 * @param bfSize
	 * @param salt
	 * @param hash1
	 * @param hash2
	 */
	public DoubleHashing(int bfSize, String salt, MessageDigestAlgorithm hash1, MessageDigestAlgorithm hash2) {
		super(bfSize);
		this.salt = salt;
		this.hash1 = hash1;
		this.hash2 = hash2;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Integer> hash(String element, int hashFunctions) {
		final String input = element + this.salt;

		final int hashValue1 = ByteUtils.bytesToInt(HashUtils.getDigest(hash1, input));
		final int hashValue2 = ByteUtils.bytesToInt(HashUtils.getDigest(hash2, input));

		final Set<Integer> positions = new HashSet<>();

		for (int hashNumber = 0; hashNumber < hashFunctions; hashNumber++) {
			final int position = Math.abs(hashValue1 + hashNumber * hashValue2) % this.bfSize;
			positions.add(position);
		}

		return positions;
	}

	@Override
	public List<Integer> hashToInts(String element, int hashFunctions) {
		final List<Integer> result = new ArrayList<>(hashFunctions);
		final String input = element + this.salt;

		final int hashValue1 = ByteUtils.bytesToInt(HashUtils.getDigest(hash1, input));
		final int hashValue2 = ByteUtils.bytesToInt(HashUtils.getDigest(hash2, input));

		for (int hashNumber = 0; hashNumber < hashFunctions; hashNumber++) {
			final int position = Math.abs(hashValue1 + hashNumber * hashValue2) % this.bfSize;
			result.add(position);
		}

		return result;
	}
}