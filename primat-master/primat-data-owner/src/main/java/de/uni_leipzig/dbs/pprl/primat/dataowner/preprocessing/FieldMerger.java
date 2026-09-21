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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging.Merger;


/**
 * 
 * @author mfranke
 *
 */
public class FieldMerger implements Preprocessor {

	private final MergeDefinition mergeDef;

	public FieldMerger(MergeDefinition mergeDef) {
		this.mergeDef = mergeDef;
	}

	@Override
	public void preprocess(Record record) {
		final Map<LinkedHashSet<Integer>, Merger> fieldToMergerMapping = mergeDef.getColumnToMergerMapping();

		for (final Entry<LinkedHashSet<Integer>, Merger> mapping : fieldToMergerMapping.entrySet()) {
			final List<Integer> indices = new ArrayList<>(mapping.getKey());
			
			final Merger merger = mapping.getValue();

			final String[] attributeValues = this.getAttributeValues(record, indices);
			final String mergedAttributeValue = merger.merge(attributeValues);

			Collections.sort(indices, Comparator.reverseOrder());
			
			for (final Integer i : indices) {
				record.removeQidAttribute(i);
			}
			
			final QidAttribute<?> mergedAttribute = new StringAttribute(mergedAttributeValue);
			record.addQidAttribute(indices.get(indices.size()-1), mergedAttribute);
		}
	}

	private String[] getAttributeValues(Record record, List<Integer> indices) {
		final String[] attributeValues = new String[indices.size()];

		for (int i = 0; i < indices.size(); i++) {
			final int index = indices.get(i);
			attributeValues[i] = record.getQidAttribute(index).getStringValue();
		}

		return attributeValues;
	}

	@Override
	public void updateSchema() {
		final Map<LinkedHashSet<Integer>, Merger> fieldToMergerMapping = mergeDef.getColumnToMergerMapping();

		for (final Entry<LinkedHashSet<Integer>, Merger> mapping : fieldToMergerMapping.entrySet()) {
			RecordSchema.INSTANCE.merge(new ArrayList<>(mapping.getKey()));
		}
	}
}