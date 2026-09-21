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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing;

import java.util.Map;
import java.util.Map.Entry;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.Attribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.Normalizer;


/**
 * 
 * @author mfranke
 *
 */
public class FieldNormalizer implements Preprocessor {

	private final NormalizeDefinition normalization;

	public FieldNormalizer(NormalizeDefinition normalization) {
		this.normalization = normalization;
	}

	@Override
	public void preprocess(Record record) {
		final Map<Integer, Normalizer> normMap = normalization.getColumnToNormalizerMapping();

		for (final Entry<Integer, Normalizer> entry : normMap.entrySet()) {
			final int column = entry.getKey();
			final Normalizer normalizer = entry.getValue();

			final Attribute<?> attribute = record.getQidAttribute(column);
			final String attributeValue = attribute.getStringValue();
			final String normalizedValue = normalizer.normalize(attributeValue);
			attribute.setValueFromString(normalizedValue);
		}
	}

	@Override
	public void updateSchema() {}
}