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

import java.util.Arrays;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Feature extractor function where substring are obtained by applying a regular
 * expression. For that the method <code>String.split()</code> is used
 * internally.
 * 
 * Usually, this is used to retrieve tokens from a string, e.g., by splitting
 * the string on blanks, hyphens or dots.
 * 
 * @author mfranke
 *
 */
public class SubstringByRegexExtractor implements FeatureExtractor {

	private String regex;

	/**
	 * Constructs a new feature extraction functions that extracts features
	 * based on the specified regular expression.
	 * 
	 * @param regex the regular expression used to extract the features.
	 */
	public SubstringByRegexExtractor(String regex) {
		this.regex = regex;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attr) {
		final String attrValue = attr.getStringValue();
		final String[] parts = attrValue.split(this.regex);
		final List<String> result = Arrays.asList(parts);
		return result;
	}
}