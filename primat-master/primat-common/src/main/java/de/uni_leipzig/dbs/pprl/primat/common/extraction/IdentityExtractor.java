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
 * A feature extractor functions that applies the identity function, i. e., the
 * complete attribute value is returned as string token.
 * 
 * @author mfranke
 *
 */
public class IdentityExtractor implements FeatureExtractor {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attr) {
		final List<String> result = new ArrayList<>();
		final String value = attr.getStringValue();
		result.add(value);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}