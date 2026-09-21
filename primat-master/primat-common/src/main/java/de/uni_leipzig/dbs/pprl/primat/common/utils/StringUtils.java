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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.codec.binary.Hex;


/**
 * 
 * 
 * @author mfranke
 *
 */
public final class StringUtils {

	private StringUtils() {
		throw new RuntimeException();
	}

	public static String upperCamelCaseToTitleCase(String s) {
		return s.replaceAll(
			String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"),
			" ");
	}

	// TODO: Add more case conversions, i.e., sentence case <-> title case <->
	// all lowercase <-> all uppercase <-> snake case

	public static String encodeString(byte[] data) {
		return new String(data);
	}

	public static String encodeHexString(byte[] data) {
		return Hex.encodeHexString(data);
	}

	public static String encodeBase64String(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}

	// TODO: Skip-grams and positional q-grams

	public static List<String> getQGrams(String value, int q, String paddingCharacter) {
		String paddedString = value;

		for (int i = 1; i < q; i++) {
			paddedString = paddingCharacter + paddedString + paddingCharacter;
		}

		return getQGrams(paddedString, q);
	}

	public static List<String> getQGrams(String value, int q) {

		// Does not fit the pure definition of q-grams
		if (value.length() < q) {
			return List.of(value);
		}

		final char[] chars = value.toCharArray();
		final List<String> tokens = new ArrayList<String>();

		for (int i = 0; i <= chars.length - q; i++) {
			final StringBuilder token = new StringBuilder();

			for (int j = i; j < i + q; j++) {
				token.append(chars[j]);
			}

			tokens.add(token.toString());
		}
		return tokens;
	}
}