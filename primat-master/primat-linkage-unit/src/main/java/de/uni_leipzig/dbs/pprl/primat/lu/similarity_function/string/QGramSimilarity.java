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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.string;

import java.util.HashSet;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.utils.StringUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.set.JaccardSimilarity;

/**
 * 
 * @author mfranke
 *
 */
public class QGramSimilarity extends StringSimilarityFunction {

	public static final String DEFAULT_PADDING_CHAR = "#";

	private int q;
	private String paddingCharacter;

	public QGramSimilarity(int q) {
		this(q, false);
	}

	public QGramSimilarity(int q, boolean padding) {
		this.q = q;
		this.paddingCharacter = padding ? DEFAULT_PADDING_CHAR : "";
	}

	@Override
	protected double calculateSim(String left, String right) {
		final List<String> leftTokens = StringUtils.getQGrams(left, this.q, this.paddingCharacter);
		final List<String> rightTokens = StringUtils.getQGrams(right, this.q, this.paddingCharacter);

		final JaccardSimilarity<String> jaccard = new JaccardSimilarity<>();
		return jaccard.calculateSimilarity(new HashSet<>(leftTokens), new HashSet<>(rightTokens));
	}
}