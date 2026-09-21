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

import java.security.MessageDigest;


/**
 * Secure hash algorithms for computing a condensed representation of electronic
 * data (message). When a message is input to a hash algorithm, the result is an
 * output called a message digest. A message digest ranges in length from
 * 160-512 bits, depending on the algorithm.
 * 
 * See {@link MessageDigest}
 * 
 * @author mfranke
 *
 */
public enum MessageDigestAlgorithm {
	/**
	 * The MD2 message digest algorithm as defined in RFC 1319.
	 */
	MD_2("MD2"),
	/**
	 * The MD5 message digest algorithm as defined in RFC 1321.
	 */
	MD_5("MD5"),
	/**
	 * Hash algorithms defined in FIPS PUB 180-4.
	 */
	SHA_1("SHA-1"), SHA_224("SHA-224"), SHA_256("SHA-256"), SHA_384("SHA-384"), SHA_512("SHA-512"),

	/**
	 * Permutation-based hash and extendable-output functions as defined in FIPS
	 * PUB 202. An input message length can vary; the length of the output
	 * digest is fixed.
	 */
	SHA3_224("SHA3-224"), SHA3_256("SHA3-256"), SHA3_384("SHA3-384"), SHA3_512("SHA3-512");

	private final String name;

	private MessageDigestAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}