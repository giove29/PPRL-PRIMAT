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

package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.Random;


/**
 * 
 * @author mfranke
 *
 */
public final class HashFunctionGenerator {

	private final int k;
	private final int[] internalSeeds;
	private final MessageDigestAlgorithm hash1;
	private final MessageDigestAlgorithm hash2;

	public HashFunctionGenerator(int k, long seed) {
		this(k, seed, MessageDigestAlgorithm.MD_5, MessageDigestAlgorithm.SHA_1);
	}

	public HashFunctionGenerator(int k, long seed, MessageDigestAlgorithm hash1, MessageDigestAlgorithm hash2) {

		if (k < 1) {
			throw new IllegalArgumentException();
		}
		this.hash1 = hash1;
		this.hash2 = hash2;
		this.k = k;
		this.internalSeeds = new int[k];
		final Random rnd = new Random(seed * k);

		for (int i = 0; i < k; i++) {
			this.internalSeeds[i] = rnd.nextInt();
		}
	}

	public int firstHash(String data) {
		return this.hash(0, data);
	}

	public int[] hash(String data) {
		final int[] res = new int[this.k];

		for (int i = 0; i < this.k; i++) {
			res[i] = this.hash(i, data);
		}
		return res;
	}

	public int hash(int n, String data) {

		if (n < 0 || n >= k) {
			throw new IllegalArgumentException();
		}

		final byte[] digest1 = HashUtils.getDigest(hash1, data);
		final byte[] digest2 = HashUtils.getDigest(hash2, data);

		final int hashValue1 = ByteUtils.bytesToInt(digest1);
		final int hashValue2 = ByteUtils.bytesToInt(digest2);

		return hashValue1 + this.internalSeeds[n] * hashValue2;
	}

	public int getHashes() {
		return this.k;
	}

	public MessageDigestAlgorithm getHash1() {
		return this.getHash1();
	}

	public MessageDigestAlgorithm getHash2() {
		return this.getHash2();
	}

}