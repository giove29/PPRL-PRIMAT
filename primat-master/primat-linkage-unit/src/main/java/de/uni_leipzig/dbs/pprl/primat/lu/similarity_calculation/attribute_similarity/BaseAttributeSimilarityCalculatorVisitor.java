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
import java.util.Optional;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttributeRetriever;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IntegerSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IntegerSetAttributeRetriever;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttributeRetriever;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.XorBitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.XorBitSetAttributeRetriever;


/**
 * 
 * @author mfranke
 *
 */
public class BaseAttributeSimilarityCalculatorVisitor implements AttributeSimilarityCalculatorVisitor {

	private QidAttribute<?> left;
	private QidAttribute<?> right;

	public BaseAttributeSimilarityCalculatorVisitor(QidAttribute<?> left, QidAttribute<?> right) {
		this.left = left;
		this.right = right;
	}

	public List<Double> visit(XorBitSetAttributeSimilarityCalculator attributeSimilarityCalculator) {
		final XorBitSetAttributeRetriever bsRet = new XorBitSetAttributeRetriever();
		this.left.accept(bsRet);
		final Optional<XorBitSetAttribute> bsLeft = bsRet.getXorBitSetAttribute();

		bsRet.clear();
		this.right.accept(bsRet);
		final Optional<XorBitSetAttribute> bsRight = bsRet.getXorBitSetAttribute();

		if (bsLeft.isPresent() && bsRight.isPresent()) {
			return attributeSimilarityCalculator.calculateSimilarities(bsLeft.get(), bsRight.get());
		}
		else {
			// TODO
			throw new IllegalArgumentException();
		}
	}

	public List<Double> visit(BitSetAttributeSimilarityCalculator attributeSimilarityCalculator) {
		final BitSetAttributeRetriever bsRet = new BitSetAttributeRetriever();
		this.left.accept(bsRet);
		final Optional<BitSetAttribute> bsLeft = bsRet.getBitSetAttribute();

		bsRet.clear();
		this.right.accept(bsRet);
		final Optional<BitSetAttribute> bsRight = bsRet.getBitSetAttribute();

		if (bsLeft.isPresent() && bsRight.isPresent()) {
			return attributeSimilarityCalculator.calculateSimilarities(bsLeft.get(), bsRight.get());
		}
		else {
			// TODO
			throw new IllegalArgumentException();
		}
	}

	public List<Double> visit(IntegerSetAttributeSimilarityCalculator attributeSimilarityCalculator) {
		final IntegerSetAttributeRetriever intSetRet = new IntegerSetAttributeRetriever();
		this.left.accept(intSetRet);
		final Optional<IntegerSetAttribute> intSetLeft = intSetRet.getIntegerSetAttribute();

		intSetRet.clear();
		this.right.accept(intSetRet);
		final Optional<IntegerSetAttribute> intSetRight = intSetRet.getIntegerSetAttribute();

		if (intSetLeft.isPresent() && intSetRight.isPresent()) {
			return attributeSimilarityCalculator.calculateSimilarities(intSetLeft.get(), intSetRight.get());
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public List<Double> visit(StringAttributeSimilarityCalculator attributeSimilarityCalculator) {
		final StringAttributeRetriever stringSetRet = new StringAttributeRetriever();
		this.left.accept(stringSetRet);
		final Optional<StringAttribute> stringLeft = stringSetRet.getStringAttribute();

		stringSetRet.clear();
		this.right.accept(stringSetRet);
		final Optional<StringAttribute> stringRight = stringSetRet.getStringAttribute();

		if (stringLeft.isPresent() && stringRight.isPresent()) {
			return attributeSimilarityCalculator.calculateSimilarities(stringLeft.get(), stringRight.get());
		}
		else {
			// TODO
			throw new IllegalArgumentException();
		}
	}

}