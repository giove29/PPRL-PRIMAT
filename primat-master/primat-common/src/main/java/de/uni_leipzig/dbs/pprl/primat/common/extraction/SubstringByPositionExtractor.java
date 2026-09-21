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

package de.uni_leipzig.dbs.pprl.primat.common.extraction;

import java.util.ArrayList;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Feature extraction function that extracts a substring by its position.
 * 
 * @author mfranke
 *
 */
public class SubstringByPositionExtractor implements FeatureExtractor {

	private int beginIndex;
	private int endIndex;

	/**
	 * Constructs a new feature extraction function that extracts a substring at
	 * the specified position within the attribute value. An optimistic approach
	 * is used to extract the substring, i. e., returns an empty feature set or
	 * maximum available substring if the attribute value is to short.
	 * 
	 * 
	 * @param beginIndex the start position.
	 * @param endIndex   the end position.
	 */
	public SubstringByPositionExtractor(int beginIndex, int endIndex) {
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
	}

	private static boolean isEmptyString(String string) {
		return string == null || string.length() == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attr) {
		final String attrValue = attr.getStringValue();

		final List<String> result = new ArrayList<>();

		if (isEmptyString(attrValue)) {
			result.add("");
			return result;
		}

		final int valueLength = attrValue.length();

		if (beginIndex > valueLength) {
			return result;
		}
		else {
			final int customEndIndex = valueLength < endIndex ? valueLength : endIndex;
			final String substring = attrValue.substring(beginIndex, customEndIndex);
			result.add(substring);
			return result;
		}
	}
}
