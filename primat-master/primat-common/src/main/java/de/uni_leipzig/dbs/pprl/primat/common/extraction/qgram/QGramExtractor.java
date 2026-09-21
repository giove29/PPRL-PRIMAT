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

package de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.StringUtils;


/**
 * Feature extraction functions that extracts the q-gram set of an attribute
 * value.
 * 
 * The q-gram set of an attribute value is the set of all substrings of length q
 * that can be build from the attribute value by applying a sliding window
 * approach from left to right.
 * 
 * Since the beginning and end of a string are underrepresented, padding
 * characters can be introduced to the beginning and end.
 * 
 * @author mfranke
 *
 */
public class QGramExtractor implements FeatureExtractor {

	private static final String DEFAULT_PADDING_CHAR = "#";

	protected int q;
	protected boolean padding;
	protected String paddingCharacter;

	/**
	 * Constructs a new q-gram extractor function without using the character
	 * padding approach.
	 * 
	 * @param q the length of the substrings to build.
	 */
	public QGramExtractor(int q) {
		this(q, false);
	}

	/**
	 * Constructs a new q-gram extractor function.
	 * 
	 * @param q                the length of the substrings to build.
	 * @param characterPadding boolean value to enable/disable character
	 *                         padding.
	 */
	public QGramExtractor(int q, boolean padding) {
		this(q, padding, DEFAULT_PADDING_CHAR);
	}

	/**
	 * Constructs a new q-gram extractor function with a configurable padding
	 * character.
	 *
	 * @param q                the length of the substrings to build.
	 * @param padding          boolean value to enable/disable character
	 *                         padding.
	 * @param paddingCharacter the character used for padding, if enabled.
	 */
	public QGramExtractor(int q, boolean padding, String paddingCharacter) {

		if (q < 1 || q > 4) {
			throw new IllegalArgumentException();
		}
		this.q = q;
		this.padding = padding;
		this.paddingCharacter = padding ? paddingCharacter : "";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attr) {
		// TODO: necessary???
		final String value = new String(attr.getStringValue());
		return StringUtils.getQGrams(value, q, this.paddingCharacter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}