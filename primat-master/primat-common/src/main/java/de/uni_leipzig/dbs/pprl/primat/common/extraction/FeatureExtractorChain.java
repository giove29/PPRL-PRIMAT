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
import java.util.Arrays;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Class for creating a chain of feature extractors, i. e., several
 * {@link FeatureExtractor} objects are grouped together to simplify complex
 * feature extraction processes.
 * 
 * 
 * @author mfranke
 *
 */
public class FeatureExtractorChain implements FeatureExtractor {

	private List<FeatureExtractor> featureExtractors;

	/**
	 * Creates a new (empty) feature extraction chain.
	 */
	public FeatureExtractorChain() {
		this.featureExtractors = new ArrayList<>();
	}

	/**
	 * Creates a new feature extraction chain consisting of the specified
	 * feature extraction functions.
	 * 
	 * @param featureExtractionFunctions a list of {@link FeatureExtractor}
	 *                                   functions.
	 */
	@SafeVarargs
	public FeatureExtractorChain(FeatureExtractor... featureExtractionFunctions) {
		this.featureExtractors = Arrays.asList(featureExtractionFunctions);
	}

	/**
	 * Adds a {@link FeatureExtractor} to the chain.
	 * 
	 * @param func
	 */
	public void add(FeatureExtractor func) {
		this.featureExtractors.add(func);
	}

	/**
	 * @return a List of all {@link FeatureExtractor} functions in the chain.
	 */
	public List<FeatureExtractor> getFeatureExtractors() {
		return this.featureExtractors;
	}

	/**
	 * {@inheritDoc}}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attr) {
		final List<String> result = new ArrayList<>();

		for (final FeatureExtractor mapFunction : this.featureExtractors) {
			final List<String> funcResult = mapFunction.extract(attr);
			result.addAll(funcResult);
		}

		return result;
	}
}