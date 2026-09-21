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

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.Attribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.Splitter;


/**
 * 
 * @author mfranke
 *
 */
public class FieldSplitter implements Preprocessor {

	private final SplitDefinition splitDef;

	public FieldSplitter(SplitDefinition splitDef) {
		this.splitDef = splitDef;
	}

	@Override
	public void preprocess(Record record) {
		final List<QidAttribute<?>> recAttr = record.getAttributes();

		final ListIterator<QidAttribute<?>> it = recAttr.listIterator(recAttr.size());

		while (it.hasPrevious()) {
			final int index = it.previousIndex();
			final Attribute<?> attribute = it.previous();
			final String value = attribute.getStringValue();
			final Splitter splitter = splitDef.getSplitter(index);

			if (splitter != null) {
				it.remove();
				final String[] splitRes = splitter.split(value);

				for (final String split : splitRes) {
					it.add(new StringAttribute(split));
				}

				for (int i = splitRes.length; i < splitter.parts(); i++) {
					it.add(new StringAttribute(""));
				}

				for (int i = 0; i < splitter.parts(); i++) {
					it.previous();
				}
			}
		}
	}

	@Override
	public void updateSchema() {
		final Map<Integer, Splitter> mapping = this.splitDef.getColumnToSplitterMapping();
		
		for (final Entry<Integer, Splitter> entry : mapping.entrySet()) {
			final Integer column = entry.getKey();
			final Splitter splitter = entry.getValue();
			final int parts = splitter.parts();
			
			RecordSchema.INSTANCE.split(column, parts);
		}
	}
}