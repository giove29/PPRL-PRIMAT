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

import javax.crypto.Mac;


/**
 * Hash-based (key-based) Message Authentication Codes (HMAC). A MAC provides a
 * way to check the integrity of a message, i.e., to authenticate the message.
 * MACs based on cryptographic hash functions are called HMACs.
 * 
 * See {@link Mac}
 * 
 * @author mfranke
 *
 */
public enum HMacAlgorithm {

	/**
	 * The HMAC-MD5 keyed-hashing algorithm as defined in RFC 2104: "HMAC:
	 * Keyed-Hashing for Message Authentication" (February 1997).
	 */
	HMAC_MD_5("HmacMD5"),

	HMAC_SHA1("HmacSHA1"),

	HMAC_SHA_224("HmacSHA224"),

	HMAC_SHA_256("HmacSHA256"),

	HMAC_SHA_384("HmacSHA384"),

	HMAC_SHA_512("HmacSHA512"), HMAC_SHA_512_224("HmacSHA512/224"), HMAC_SHA_512_256("HmacSHA512/256"),

	HMAC_SHA3_224("HmacSHA3-224"), HMAC_SHA3_256("HmacSHA3-256"), HMAC_SHA3_384("HmacSHA3-384"),
	HMAC_SHA3_512("HmacSHA3-512");

	private final String name;

	private HMacAlgorithm(String name) {
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