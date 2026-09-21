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
 * Replace umlauts by equivalents.
 * 
 * @author mfranke
 *
 */
public final class UmlautNormalizer extends CharacterReplacer {

	public UmlautNormalizer() {
		super(UMLAUT_REPLACEMENT_MAP);
	}

	public static final Map<Character, String> UMLAUT_REPLACEMENT_MAP = createUmlautReplacementMap();

	private static Map<Character, String> createUmlautReplacementMap() {
		final Map<Character, String> result = new HashMap<Character, String>();
		result.put('\u00e4', "ae");
		result.put('\u00c4', "AE");
		result.put('\u00f6', "oe");
		result.put('\u00d6', "OE");
		result.put('\u00fc', "ue");
		result.put('\u00dc', "UE");
		result.put('\u00df', "ss");
		return Collections.unmodifiableMap(result);
	}
}