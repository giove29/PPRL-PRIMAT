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
public abstract class StringReplacer implements Normalizer {

	private final Map<String, String> stringReplacementMap;

	public StringReplacer(Map<String, String> stringReplacementMap) {
		this.stringReplacementMap = stringReplacementMap;
	}

	@Override
	public String normalize(String string) {
		String result = string;

		for (final String key : stringReplacementMap.keySet()) {
			final String replacement = stringReplacementMap.get(key);
			result = result.replace(key, replacement);
		}
		return result;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}