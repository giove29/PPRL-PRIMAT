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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.OrderedBidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableOrderedBidiMap;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;

/**System.out.println(RecordSchema.INSTANCE.nameColumnMap);
 * 
 * @author mfranke
 *
 */
public enum RecordSchema {
	
	INSTANCE;
	
	public Map<String, QidAttributeType> nameAttrTypeMap;
	public TreeBidiMap<String, Integer> nameColumnMap;
	
	private RecordSchema() {
		this.nameAttrTypeMap = new HashMap<>();
		this.nameColumnMap = new TreeBidiMap<>();
	}
	
	public Map<String, QidAttributeType> getNameAttributeTypeMapping(){
		return Collections.unmodifiableMap(this.nameAttrTypeMap);
	}
	
	public OrderedBidiMap<String, Integer> getNameColumnMapping(){
		return UnmodifiableOrderedBidiMap.unmodifiableOrderedBidiMap(this.nameColumnMap);
	}
	
	public void clear() {
		this.nameAttrTypeMap.clear();
		this.nameColumnMap.clear();
	}
	
	public void split(String name, int parts) {
		final Integer column = this.getAttributeColumn(name);
		split(column, parts);
	}
	
	public void mergeByNames(List<String> names) {
		final List<Integer> cols = names.stream()
				.map(name -> this.getAttributeColumn(name))
				.collect(Collectors.toList());
		this.merge(cols);
	}
	
	public void merge(List<Integer> columns) {
		Collections.sort(columns);
		
		final List<String> names = columns.stream()
				.map(col -> this.getAttributeName(col))
				.collect(Collectors.toList());
				
		for (final String name : names) {
			this.nameAttrTypeMap.remove(name);
			this.nameColumnMap.remove(name);
		}
		
		final Integer first = columns.get(0);
		
		final String newColName = String.join("_", names);
		this.nameAttrTypeMap.put(newColName, QidAttributeType.STRING);
		this.nameColumnMap.put(newColName, first);
		
		this.nameColumnMap.replaceAll((k, v) -> {
			if (v.compareTo(first) <= 0) {
				return v;
			}
			else {
				return v -  columns.size();
			}
		});
		
	}
	
	public void split(Integer column, int parts) {		
		final int offsetForTail = parts - 1;
		
		final TreeBidiMap<String, Integer> newNameColumnMap = new TreeBidiMap<>();
		
		for (final Entry<String, Integer> entry : this.nameColumnMap.entrySet()) {
			final String name = entry.getKey();
			final Integer col = entry.getValue();
			
			if (col.compareTo(column) > 0) {
				newNameColumnMap.put(name, col + offsetForTail);
			}
			else {
				newNameColumnMap.put(name,  col);
			}
		}
		
		this.nameColumnMap = newNameColumnMap;
		
		final String name = this.getAttributeName(column);		
		this.nameColumnMap.remove(name);
		final QidAttributeType attrType = this.nameAttrTypeMap.remove(name);
		
		for (int i = 0; i < parts; i++) {
			final String colName = name + "_" + i;
			this.nameColumnMap.put(colName, column + i);
			this.nameAttrTypeMap.put(colName, attrType);
		}

	}
	
	public void remove(String name) {
		this.remove(this.nameColumnMap.get(name));
	}
	
	public void remove(Integer column) {
		final String name = this.nameColumnMap.getKey(column);
		this.nameAttrTypeMap.remove(name);
		this.nameColumnMap.removeValue(column);
		
		this.nameColumnMap.replaceAll((k, v) -> {
			if (v.compareTo(column) < 0) {
				return v;
			}
			else {
				return v - 1;
			}
		});
	}
	
	public void put(String name, QidAttributeType type, Integer column) {
		this.nameAttrTypeMap.put(name, type);
		this.nameColumnMap.put(name, column);
	}
	
	public Integer getAttributeColumn(String name){
		return this.nameColumnMap.get(name);
	}
	
	public QidAttributeType getAttributeType(String name) {
		return this.nameAttrTypeMap.get(name);
	}
	
	public String getAttributeName(int column) {
		return this.nameColumnMap.getKey(column);
	}
	
	public QidAttributeType getAttributeType(int column) {
		return this.getAttributeType(this.getAttributeName(column));
	}
}