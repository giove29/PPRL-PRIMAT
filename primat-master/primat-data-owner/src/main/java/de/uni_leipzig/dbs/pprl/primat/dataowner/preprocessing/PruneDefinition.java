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

import java.util.Set;
import java.util.TreeSet;

import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;

import java.util.Collections;
import java.util.HashSet;


/**
 * 
 * @author mfranke
 *
 */
public class PruneDefinition {

	private Set<Integer> columnsToPrune;

	public PruneDefinition(Set<Integer> columnsToPrune) {
		this.columnsToPrune = columnsToPrune;
	}

	public PruneDefinition() {
		this(new TreeSet<>(Collections.reverseOrder()));
	}
	
	public void add(String columnToPrune) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(columnToPrune);
		this.add(col);
	}

	public void add(int columnToPrune) {
		this.columnsToPrune.add(columnToPrune);
	}

	public boolean contains(String column) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(column);
		return this.contains(col);
	}
	
	public boolean contains(int column) {
		return this.columnsToPrune.contains(column);
	}

	public Set<Integer> getColumnsToPrune() {
		return this.columnsToPrune;
	}
	
	public void setColumnsToPruneByName(Set<String> columnsToPrune) {
		final Set<Integer> cols = new HashSet<>();
		for (final String column : columnsToPrune) {
			final Integer col = RecordSchema.INSTANCE.getAttributeColumn(column);
			cols.add(col);
		}
		this.setColumnsToPrune(cols);
	}
	
	public void setColumnsToPrune(Integer... columnsToPrune) {
		this.setColumnsToPrune(Set.of(columnsToPrune));
	}

	public void setColumnsToPrune(Set<Integer> columnsToPrune) {
		this.columnsToPrune = columnsToPrune;
	}
}