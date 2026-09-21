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

package de.uni_leipzig.dbs.pprl.primat.common.blocking;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.utils.ListAggregator;
import de.uni_leipzig.dbs.pprl.primat.common.utils.StringListAggregator;


/**
 * 
 * Class for the definition of a blocking key.
 * A blocking key is constructed by extracting features
 * from a record and then combining those features into
 * a single value.
 * 
 * @author mfranke
 *
 */
public final class BlockingKeyDefinition {

	private final List<ExtractorDefinition> exDefs;
	private final ListAggregator<String> aggregator;

	public BlockingKeyDefinition(List<ExtractorDefinition> exDefs, StringListAggregator aggregator) {
		this.exDefs = exDefs;
		this.aggregator = aggregator;
	}

	public List<ExtractorDefinition> getExtratorDefinitions() {
		return this.exDefs;
	}

	public ListAggregator<String> getAggregator() {
		return this.aggregator;
	}

}