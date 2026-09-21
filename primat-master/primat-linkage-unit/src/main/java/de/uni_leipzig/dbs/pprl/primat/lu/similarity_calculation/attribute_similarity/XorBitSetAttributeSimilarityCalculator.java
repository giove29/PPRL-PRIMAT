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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.XorBitSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.XorBitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.SimilarityFunction;


/**
 * 
 * @author mfranke
 *
 */
public class XorBitSetAttributeSimilarityCalculator
	extends AttributeSimilarityCalculator<XorBitSetAttribute, XorBitSet> {

	public XorBitSetAttributeSimilarityCalculator(List<SimilarityFunction<XorBitSet>> list) {
		super(list);
	}

	@Override
	public List<Double> visit(AttributeSimilarityCalculatorVisitor visitor) {
		return visitor.visit(this);
	}
}