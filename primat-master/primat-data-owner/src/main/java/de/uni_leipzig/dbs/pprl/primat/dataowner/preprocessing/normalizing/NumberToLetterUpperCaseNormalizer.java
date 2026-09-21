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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Numbers to upper case letters.
 * 
 * @author mfranke
 *
 */
public final class NumberToLetterUpperCaseNormalizer extends CharacterReplacer {

	public NumberToLetterUpperCaseNormalizer() {
		super(OCR_NUMBER_TO_UPPER_CASE_LETTER_REPLACEMENT_MAP);
	}

	public static final Map<Character, String> OCR_NUMBER_TO_UPPER_CASE_LETTER_REPLACEMENT_MAP = createMap();

	private static Map<Character, String> createMap() {
		final Map<Character, String> result = new HashMap<Character, String>();
		result.put('0', "O");
		result.put('1', "L");
		result.put('2', "Z");
		// result.put('3', "3");
		result.put('4', "A");
		result.put('5', "S");
		result.put('6', "G");
		// result.put('7', "7");
		result.put('8', "B");
		// result.put('9', "9");
		return Collections.unmodifiableMap(result);
	}
}