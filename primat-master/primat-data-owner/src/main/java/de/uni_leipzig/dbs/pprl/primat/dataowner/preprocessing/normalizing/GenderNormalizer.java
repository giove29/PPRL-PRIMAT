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
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Normalizes different gender representations.
 * 
 * @author mfranke
 *
 */
public class GenderNormalizer extends StringMapper {

	public GenderNormalizer() {
		super(GENDER_REPLACEMENT_MAP);
	}

	public static final Map<String, String> GENDER_REPLACEMENT_MAP = createGenderReplacementMap();

	private static String MALE = "mal";
	private static String FEMALE = "fem";
	private static String OTHER = "oth";

	private static Map<String, String> createGenderReplacementMap() {
		final Map<String, String> result = new LinkedHashMap<String, String>();
		result.put("male", MALE);
		result.put("female", FEMALE);
		result.put("m", MALE);
		result.put("f", FEMALE);
		result.put("w", FEMALE);
		result.put("0", MALE);
		result.put("1", FEMALE);
		result.put("other", OTHER);
		result.put("diverse", OTHER);
		result.put("unknown", OTHER);
		result.put("unk", OTHER);
		return Collections.unmodifiableMap(result);
	}

}
