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

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram.QGramExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HashFunctionGenerator;


/**
 * 
 * @author mfranke
 *
 */
public class MinHashExtractor implements FeatureExtractor {

	private QGramExtractor qgramExtractor;
	private HashFunctionGenerator hashFuncGen;

	public MinHashExtractor(long seed, QGramExtractor qgramExtractor) {
		this.qgramExtractor = qgramExtractor;
		this.hashFuncGen = new HashFunctionGenerator(1, seed);
	}

	@Override
	public List<String> extract(QidAttribute<?> attrValue) {
		final List<String> qgrams = this.qgramExtractor.extract(attrValue);

		int minHash = Integer.MAX_VALUE;

		for (final String qgram : qgrams) {
			final int hash = this.hashFuncGen.firstHash(qgram);

			if (hash < minHash) {
				minHash = hash;
			}
		}

		return List.of(String.valueOf(minHash));
	}

}
