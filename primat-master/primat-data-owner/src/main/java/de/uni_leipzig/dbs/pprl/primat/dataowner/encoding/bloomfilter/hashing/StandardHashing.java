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
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.utils.ByteUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HashUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.MessageDigestAlgorithm;


/**
 * 
 * @author mfranke
 *
 */
public class StandardHashing extends HashingMethod {

	private List<MessageDigestAlgorithm> hashes;

	public StandardHashing(int bfSize, List<MessageDigestAlgorithm> hashes) {
		super(bfSize);
		this.hashes = hashes;
	}

	public StandardHashing(int bfSize, MessageDigestAlgorithm... hashes) {
		super(bfSize);
		this.hashes = List.of(hashes);
	}

	@Override
	public List<Integer> hashToInts(String element, int hashFunctions) {

		if (this.hashes.size() < hashFunctions) {
			throw new RuntimeException("The hashing method is defined with less hash functions than specified!");
		}

		final List<Integer> positions = new ArrayList<>(hashFunctions);

		for (int i = 0; i < hashFunctions; i++) {
			final MessageDigestAlgorithm hash = this.hashes.get(i);
			final int pos = Math.abs(ByteUtils.bytesToInt(HashUtils.getDigest(hash, element))) % this.bfSize;
			positions.add(pos);
		}

		return positions;
	}
}