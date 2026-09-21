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


import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging.Merger;


/**
 * 
 * @author mfranke
 *
 */
public class MergeDefinition {

	private Map<LinkedHashSet<Integer>, Merger> columnMergeMapping;

	public MergeDefinition() {
		this.columnMergeMapping = new HashMap<>();
	}

	public Merger setMerger(Merger merger, String... columns) {
		final LinkedHashSet<Integer> cols = new LinkedHashSet<>(columns.length);
		for (final String column : columns) {
			final Integer col = RecordSchema.INSTANCE.getAttributeColumn(column);
			cols.add(col);
		}
		return this.setMerger(merger, cols);
	}
	
	public Merger setMerger(Merger merger, Integer... columns) {
		return this.columnMergeMapping.put(new LinkedHashSet<>(Set.of(columns)), merger);
	}	
	
	public Merger setMerger(Merger merger, LinkedHashSet<Integer> columns) {
		return this.columnMergeMapping.put(columns, merger);
	}

	public boolean hasMerger(String column) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(column);
		return this.hasMerger(col);
	}
	
	public boolean hasMerger(int column) {
		for (final LinkedHashSet<Integer> entry : this.columnMergeMapping.keySet()) {

			if (entry.contains(column)) {
				return true;
			}
			continue;
		}
		return false;
	}

	public Map<LinkedHashSet<Integer>, Merger> getColumnToMergerMapping() {
		return this.columnMergeMapping;
	}
}