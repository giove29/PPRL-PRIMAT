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

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * Utility class for calculating hash values.
 * 
 * @author mfranke
 */
public class HashUtils {

	private HashUtils() {
		throw new RuntimeException();
	}

	public static int toPositiveIntHash(byte[] digest, int maxValue) {
		return Math.abs(ByteUtils.bytesToInt(digest)) % maxValue;
	}

	private static MessageDigest getMessageDigest(MessageDigestAlgorithm algorithm) {

		try {
			return MessageDigest.getInstance(algorithm.getName());
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static byte[] getDigest(MessageDigestAlgorithm algorithm, String data) {
		return getDigest(algorithm, data.getBytes());
	}

	public static byte[] getDigest(MessageDigestAlgorithm algorithm, byte[] data) {
		return getMessageDigest(algorithm).digest(data);
	}

	private static Mac getMac(HMacAlgorithm algorithm) {

		try {
			return Mac.getInstance(algorithm.getName());
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static byte[] getHmac(HMacAlgorithm algorithm, String data, String key) {
		final SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), algorithm.getName());
		final Mac mac = getMac(algorithm);

		try {
			mac.init(signingKey);
			return mac.doFinal(data.getBytes());
		}
		catch (InvalidKeyException e) {
			throw new IllegalArgumentException(e);
		}
	}

}