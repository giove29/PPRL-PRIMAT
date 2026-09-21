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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * 
 * @author mfranke
 *
 */
public class SimpleNormalizeDefinition {

	private String name;

	private Set<String> columnNames;

	private Set<Integer> columns;

	private List<Normalizer> normalizer;

	public SimpleNormalizeDefinition() {
		this("");
	}

	public SimpleNormalizeDefinition(String name) {
		this.name = name;
		this.columns = new HashSet<>();
		this.normalizer = new ArrayList<>();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Integer> getColumns() {
		return this.columns;
	}

	public List<Normalizer> getNormalizer() {
		return this.normalizer;
	}

	public void setColumns(Set<Integer> cols) {
		this.columns = cols;
	}

	public void setColumNames(Set<String> colNames) {
		this.columnNames = colNames;
	}

	public Set<String> getColumnNames() {
		return this.columnNames;
	}

	public void setNormalizer(List<Normalizer> norm) {
		this.normalizer = norm;
	}

	@Override
	public String toString() {
		return this.getName() + " " + this.getColumns();
	}
}
