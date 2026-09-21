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
public class UnnamedRecordSchemaConfiguration extends RecordSchemaConfiguration {
	
	protected final SortedMap<Integer, NonQidAttributeType> nonQidAttrMap;
	protected final SortedMap<Integer, QidAttributeType> qidAttrMap;
	
	protected UnnamedRecordSchemaConfiguration(Builder builder) {
		super(builder);
		this.nonQidAttrMap = builder.nonQidAttrMap;
		this.qidAttrMap = builder.qidAttrMap;
	}
	
	@Override
	public SortedMap<Integer, NonQidAttributeType> getNonQidAttributeMap(){
		return Collections.unmodifiableSortedMap(this.nonQidAttrMap);
	}
	
	@Override
	public SortedMap<Integer, QidAttributeType> getQidAttributeMap(){
		return Collections.unmodifiableSortedMap(this.qidAttrMap);
	}
	
	public static class Builder 
		extends RecordSchemaConfiguration.Builder<Builder> {
		
		protected SortedMap<Integer, NonQidAttributeType> nonQidAttrMap;
		protected SortedMap<Integer, QidAttributeType> qidAttrMap;
		
        public Builder() {
    		this.nonQidAttrMap = new TreeMap<>();
    		this.qidAttrMap = new TreeMap<>();
        }
        
        public static void checkColumn(int column) {
        	if (column < 0) {
        		throw new IllegalArgumentException("Column value must be positive!");
        	}
        }
        
        public Builder add(int column, NonQidAttributeType attrType) {
        	checkColumn(column);
        	
        	if (attrType == NonQidAttributeType.ID && this.nonQidAttrMap.containsValue(NonQidAttributeType.ID)) {
        		throw new RuntimeException("ID attribute already specified!");
        	}
        	if (attrType == NonQidAttributeType.GLOBAL_ID && this.nonQidAttrMap.containsValue(NonQidAttributeType.GLOBAL_ID)) {
        		throw new RuntimeException("Global ID attribute already specified!");
        	}
        	if (attrType == NonQidAttributeType.PARTY && this.nonQidAttrMap.containsValue(NonQidAttributeType.PARTY)) {
        		throw new RuntimeException("Party attribute already specified!");
        	}
        	
        	this.nonQidAttrMap.put(column, attrType);
        	
        	return this;
        }
        
        public Builder add(int column, QidAttributeType attrType) {
        	checkColumn(column);
        	this.qidAttrMap.put(column, attrType);    	
        	return this;
        }
        
        protected void preBuildCheck() {
        	if (!this.nonQidAttrMap.containsValue(NonQidAttributeType.ID)) {
        		throw new RuntimeException("No ID attribute specified!");
        	}
        	if (!this.nonQidAttrMap.containsValue(NonQidAttributeType.PARTY)) {
        		throw new RuntimeException("No party attribute specified!");
        	}
        	if (this.qidAttrMap.size() < 1) {
        		throw new RuntimeException("At least one qid attribute must be specified!");
        	}	
        }

        public UnnamedRecordSchemaConfiguration build() {
        	this.preBuildCheck();
            return new UnnamedRecordSchemaConfiguration(this);
        }
    }

	@Override
	public void accept(RecordSchemaConfigurationVisitor visitor) {
		visitor.visit(this);
	}
}