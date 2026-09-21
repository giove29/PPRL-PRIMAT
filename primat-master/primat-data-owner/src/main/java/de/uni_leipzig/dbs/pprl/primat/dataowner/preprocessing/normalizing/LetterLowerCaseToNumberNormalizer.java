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
 * Lower case letters to numbers.
 * 
 * @author mfranke
 *
 */
public final class LetterLowerCaseToNumberNormalizer extends CharacterReplacer {

	public LetterLowerCaseToNumberNormalizer() {
		super(OCR_LOWER_CASE_LETTER_TO_NUMBER_REPLACEMENT_MAP);
	}

	public static final Map<Character, String> OCR_LOWER_CASE_LETTER_TO_NUMBER_REPLACEMENT_MAP = createMap();

	private static Map<Character, String> createMap() {
		final Map<Character, String> result = new HashMap<Character, String>();
		result.put('o', "0");
		result.put('l', "1");
		result.put('z', "2");
		// result.put('3', "3");
		result.put('q', "4");
		result.put('s', "5");
		// result.put('6', "6");
		// result.put('7', "7");
		// result.put('8', "8");
		result.put('g', "9");
		return Collections.unmodifiableMap(result);
	}
}