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

import java.text.Normalizer;


/**
 * Removes diacritical marks..
 * 
 * @author mfranke
 *
 */
public class AccentRemover extends RegexReplacer {

	private static final String REGEX = "\\p{InCombiningDiacriticalMarks}+";

	public AccentRemover() {
		super(REGEX, "");
	}

	@Override
	public String normalize(String string) {
		final StringBuilder decomposed = new StringBuilder(Normalizer.normalize(string, Normalizer.Form.NFD));
		convertRemainingAccentCharacters(decomposed);
		return super.normalize(decomposed.toString());
	}

	private static void convertRemainingAccentCharacters(final StringBuilder decomposed) {

		for (int i = 0; i < decomposed.length(); i++) {

			if (decomposed.charAt(i) == '\u0141') {
				decomposed.deleteCharAt(i);
				decomposed.insert(i, 'L');
			}
			else if (decomposed.charAt(i) == '\u0142') {
				decomposed.deleteCharAt(i);
				decomposed.insert(i, 'l');
			}
		}
	}
}