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
import java.util.TreeMap;

import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.Splitter;


/**
 * 
 * @author mfranke
 *
 */
public class SplitDefinition {

	private Map<Integer, Splitter> columnSplitMapping;

	public SplitDefinition() {
		this.columnSplitMapping = new TreeMap<>();
	}

	public Splitter setSplitter(String column, Splitter splitter) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(column);
		return this.setSplitter(col, splitter);
	}
	
	public Splitter setSplitter(int column, Splitter splitter) {
		return this.columnSplitMapping.put(column, splitter);
	}
	
	public Splitter getSplitter(String column) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(column);
		return this.getSplitter(col);
	}

	public Splitter getSplitter(int column) {
		return this.columnSplitMapping.get(column);
	}

	public boolean hasSplitter(String column) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(column);
		return this.hasSplitter(col);
	}
	
	public boolean hasSplitter(int column) {
		return this.columnSplitMapping.containsKey(column);
	}

	public Map<Integer, Splitter> getColumnToSplitterMapping() {
		return this.columnSplitMapping;
	}
}