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

package de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh;

import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttributeRetriever;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * 
 * @author mfranke
 *
 */
public abstract class LshBlockingFunction implements FeatureExtractor {

	public String apply(QidAttribute<?> attrValue) {
		final BitSetAttributeRetriever bsr = new BitSetAttributeRetriever();
		attrValue.accept(bsr);
		final Optional<BitSetAttribute> optional = bsr.getBitSetAttribute();

		if (optional.isPresent()) {
			final BitSetAttribute bsa = optional.get();
			final String lshKey = this.apply(bsa.getValue());
			return lshKey;
		}
		else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attrValue) {
		return List.of(this.apply(attrValue));
	}

	public abstract String apply(BitSet attribute);

}