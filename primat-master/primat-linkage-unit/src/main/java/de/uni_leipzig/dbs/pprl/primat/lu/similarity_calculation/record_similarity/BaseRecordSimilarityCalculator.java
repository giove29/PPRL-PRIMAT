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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity.AttributeSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity.BaseAttributeSimilarityCalculatorVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class BaseRecordSimilarityCalculator implements RecordSimilarityCalculator {

	// TODO: Add support for exchange groups, i.e., do not only compare records
	// field-by-field (1-1, 2-2, ... ) but instead
	// specify additional pairs of fields (1-2, 1-3)

	private List<AttributeSimilarityCalculator<?, ?>> attributeSimilarityCalculators;

	public BaseRecordSimilarityCalculator(List<AttributeSimilarityCalculator<?, ?>> attributeSimilarityCalculators) {
		this.attributeSimilarityCalculators = attributeSimilarityCalculators;
	}

	@Override
	public SimilarityVector calculateSimilarity(Record left, Record right) {
		final SimilarityVector similarityVector = new SimilarityVector();

		for (int i = 0; i < this.attributeSimilarityCalculators.size(); i++) {
			final AttributeSimilarityCalculator<?, ?> attrSimCalc = this.attributeSimilarityCalculators.get(i);
			final QidAttribute<?> leftAttribute = left.getQidAttribute(i);
			final QidAttribute<?> rightAttribute = right.getQidAttribute(i);
			final BaseAttributeSimilarityCalculatorVisitor vis = new BaseAttributeSimilarityCalculatorVisitor(
				leftAttribute, rightAttribute);
			final List<Double> similarities = attrSimCalc.visit(vis);
			similarityVector.setSimilarities(i, similarities);
		}
		return similarityVector;
	}
}