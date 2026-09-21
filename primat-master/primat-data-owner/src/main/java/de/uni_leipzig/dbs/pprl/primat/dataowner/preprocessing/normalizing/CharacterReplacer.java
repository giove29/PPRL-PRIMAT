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

import java.util.Map;


/**
 * 
 * @author mfranke
 *
 */
public abstract class CharacterReplacer implements Normalizer {

	private final Map<Character, String> characterReplacementMap;

	public CharacterReplacer(Map<Character, String> characterReplacementMap) {
		this.characterReplacementMap = characterReplacementMap;
	}

	@Override
	public String normalize(String string) {
		final StringBuilder result = new StringBuilder();

		for (int i = 0; i < string.length(); i++) {
			final char charAtPos = string.charAt(i);

			if (characterReplacementMap.containsKey(charAtPos)) {
				final String charReplacement = characterReplacementMap.get(charAtPos);
				result.append(charReplacement);
			}
			else {
				result.append(charAtPos);
			}
		}

		return result.toString();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}