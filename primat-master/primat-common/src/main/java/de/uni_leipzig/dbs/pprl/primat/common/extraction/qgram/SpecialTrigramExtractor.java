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

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Special Trigram extractor function that takes only the first
 * extracted trigram. The intention is to optimize the treatment
 * of abbreviated attribute values to single characters, e. g.,
 * for middle names. The benefit is best illustrated with an example:
 * 
 * Anna Lena Meier
 * vs.
 * Anna L. Meier
 * 
 * The standard trigram extractor with padding will produce
 * ##L, #Le, Len, ena, na#, a## 
 * vs.
 * ##L, #L#, L##.
 * 
 * Instead, the special trigram extractor with padding will produce
 * ##L.
 * 
 * 
 * @author mfranke
 *
 */
public class SpecialTrigramExtractor extends QGramExtractor {

	/**
	 * Constructs a new trigram extractor function.
	 */
	public SpecialTrigramExtractor() {
		super(3);
	}

	public SpecialTrigramExtractor(boolean padding) {
		super(3, padding);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attr) {
		final List<String> trigrams = super.extract(attr);

		if (padding && trigrams.size() == 3) {
			return List.of(trigrams.get(0));
		}
		else if (!padding && trigrams.size() == 1) {
			return List.of();
		}
		else {
			return trigrams;
		}
	}
}