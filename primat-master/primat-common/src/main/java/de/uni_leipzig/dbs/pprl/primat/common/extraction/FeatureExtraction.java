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

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Executes the feature extraction process by applying 
 * the rules specified on the {@link ExtractorDefinition}.
 * All extracted features are returned as a {@link List} of
 * strings.
 * 
 * @author mfranke
 *
 */
public class FeatureExtraction {

	protected final ExtractorDefinition extractorDef;

	public FeatureExtraction(ExtractorDefinition extractorDef) {
		this.extractorDef = extractorDef;
	}

	public List<String> getFeatures(Record record) {
		final List<FeatureExtractor> featExList = extractorDef.getExtractors();
		final List<Integer> columns = extractorDef.getColumns();

		final List<String> allFeatures = new ArrayList<>();

		for (final Integer col : columns) {

			for (final FeatureExtractor featEx : featExList) {
				final QidAttribute<?> qidAttr = record.getQidAttribute(col);
				final List<String> features = featEx.extract(qidAttr);
				allFeatures.addAll(features);
			}

		}

		return allFeatures;
	}
}