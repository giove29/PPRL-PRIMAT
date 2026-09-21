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

/**
 * Unigram extractor function.
 * 
 * This functions is equal to the {@link QGramExtractor} where q = 1.
 * 
 * @author mfranke
 *
 */
public class UnigramExtractor extends QGramExtractor {

	/**
	 * Constructs a new unigram extractor function.
	 */
	public UnigramExtractor() {
		super(1);
	}

	public UnigramExtractor(boolean padding) {
		super(1, padding);
	}

}