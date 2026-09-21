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

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;


/**
 * 
 * @author frohde
 */
public class SecretDerivation {

	public static final int ITERATION_COUNT = 10000;
	public static final int KEY_LENGTH = 256;
	public static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

	private final SecretKeyFactory skf;
	private static final Base64.Encoder b64encoder = Base64.getEncoder();

	public SecretDerivation() {
		try {
			skf = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Missing secret key algorithm: " + SECRET_KEY_ALGORITHM);
		}
	}

	public SecretKey deriveSecret(Key password, Key salt) {
		char[] passwordChars = b64encoder.encodeToString(password.getEncoded()).toCharArray();
		byte[] saltBytes = salt.getEncoded();
		return deriveSecret(passwordChars, saltBytes);
	}

	public SecretKey deriveSecret(char[] password, byte[] salt) {
		KeySpec keySpec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
		SecretKey secretKey = null;

		try {
			secretKey = skf.generateSecret(keySpec);
		}
		catch (InvalidKeySpecException e) {
			throw new RuntimeException(e.fillInStackTrace());
		}
		return secretKey;
	}
}
