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
package de.uni_leipzig.dbs.pprl.primat.analysis.attribute;

import java.util.List;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.analysis.Analyzer;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Analyzer of all attributes values grouped by the attribute name e.g. average
 * length of the attributes
 * 
 * @author frohde
 */
public abstract class AttributeAnalyzer extends Analyzer {

	protected int recordCount;

	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
	}

	public abstract ResultSet analyze(Map<Integer, List<QidAttribute<?>>> attributes);

}
