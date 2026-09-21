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

import java.util.ArrayList;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtraction;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.ListAggregator;


/**
 * Class for assigning blocking keys to records.
 * 
 * @author mfranke
 *
 */
public class Blocker {

	private List<BlockingKeyDefinition> bkDefs;

	public Blocker() {
		this(new ArrayList<>());
	}

	public Blocker(List<BlockingKeyDefinition> bkDefs) {
		this.bkDefs = bkDefs;
	}

	public void addBlockingKeyDefinition(BlockingKeyDefinition bkDef) {
		this.bkDefs.add(bkDef);
	}

	public void addBlockingKeys(Record record) {

		for (int i = 0; i < this.bkDefs.size(); i++) {
			final BlockingKeyDefinition bkDef = this.bkDefs.get(i);
			final List<ExtractorDefinition> exDefs = bkDef.getExtratorDefinitions();
			final ListAggregator<String> aggregator = bkDef.getAggregator();

			final List<String> allFeatures = new ArrayList<>();

			for (final ExtractorDefinition exDef : exDefs) {
				final FeatureExtraction featEx = new FeatureExtraction(exDef);
				final List<String> features = featEx.getFeatures(record);
				allFeatures.addAll(features);
			}

			final String bkv = aggregator.aggregate(allFeatures);
			record.addBlockingKey(new BlockingKeyAttribute(i, bkv));
		}
	}

	public void addBlockingKeys(List<Record> records) {

		for (final Record record : records) {
			this.addBlockingKeys(record);
		}
	}
}
