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

import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;


/**
 * Defines how features are extracted from record attributes.
 * 
 * @author mfranke
 *
 */
public class ExtractorDefinition {

	private String name;
	private List<Integer> columns;
	private List<FeatureExtractor> extractors;

	public ExtractorDefinition() {
		this("");
	}

	public ExtractorDefinition(String name) {
		this.name = name;
		this.columns = new ArrayList<>();
		this.extractors = new ArrayList<>();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Integer> getColumns() {
		return this.columns;
	}

	public void setExtractors(FeatureExtractor... extractors) {
		this.setExtractors(List.of(extractors));
	}

	public void setExtractors(List<FeatureExtractor> extractors) {
		this.extractors = extractors;
	}

	public List<FeatureExtractor> getExtractors() {
		return this.extractors;
	}

	public void setColumnsByName(String... colNames) {
		this.setColumnsByName(List.of(colNames));
	}
	
	public void setColumnsByName(List<String> colNames) {
		final List<Integer> cols = new ArrayList<>(colNames.size());
		
		for (final String colName : colNames) {
			final Integer col = RecordSchema.INSTANCE.getAttributeColumn(colName);
			if (col != null) {
				cols.add(col);
			}
		}
		
		this.columns = cols;
	}
	
	public void setColumns(Integer... cols) {
		this.setColumns(List.of(cols));
	}

	public void setColumns(List<Integer> cols) {
		this.columns = cols;
	}


	@Override
	public String toString() {
		return this.getName() + " " + this.getColumns();
	}
}