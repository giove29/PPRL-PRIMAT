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
package de.uni_leipzig.dbs.pprl.primat.common.model;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;


/**
 * 
 * @author mfranke
 *
 */
public class NamedRecordSchemaConfiguration extends UnnamedRecordSchemaConfiguration{

	private final SortedMap<Integer, String> qidAttrNameMap;
	
	private NamedRecordSchemaConfiguration(Builder builder) {
		super(builder.builder);
		this.qidAttrNameMap = builder.qidAttrNameMap;
	}
	
	public SortedMap<Integer, String> getQidAttributeNameMap(){
		return Collections.unmodifiableSortedMap(this.qidAttrNameMap);
	}
	
	@Override
	public void accept(RecordSchemaConfigurationVisitor visitor) {
		visitor.visit(this);
	}

	public static class Builder extends RecordSchemaConfiguration.Builder<Builder> {
		private final UnnamedRecordSchemaConfiguration.Builder builder;
		private final SortedMap<Integer, String> qidAttrNameMap;

		public Builder() {
			this.builder = new UnnamedRecordSchemaConfiguration.Builder();
			this.qidAttrNameMap = new TreeMap<>();
		}

		public Builder add(int column, QidAttributeType attrType, String name) {
			this.builder.add(column, attrType);
			
			if (name == null || name.isEmpty() || name.isBlank()) {
				throw new IllegalArgumentException("Illegal value for column name!");
			}
			if (this.qidAttrNameMap.containsValue(name)) {
				throw new RuntimeException("Duplicate attribute name!");
			}

			this.qidAttrNameMap.put(column, name);

			return this;
		}

		public Builder add(int column, NonQidAttributeType attrType) {
			this.builder.add(column, attrType);     	
			return this;
		}


		public NamedRecordSchemaConfiguration build() {
			this.builder.preBuildCheck();
			return new NamedRecordSchemaConfiguration(this);
		}
	}

}