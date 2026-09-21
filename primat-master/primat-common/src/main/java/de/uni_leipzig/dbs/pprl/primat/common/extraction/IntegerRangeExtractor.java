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
import java.util.Optional;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NumericAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NumericAttributeRetriever;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * 
 * @author mfranke
 *
 */
public class IntegerRangeExtractor implements FeatureExtractor {

	private final int rangeStart;
	private final int rangeEnd;
	private final int range;

	/**
	 * 
	 * @param rangeStart
	 * @param rangeEnd
	 * @param range
	 */
	public IntegerRangeExtractor(int rangeStart, int rangeEnd, int range) {
		this.rangeStart = rangeStart;
		this.rangeEnd = rangeEnd;
		this.range = range;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attrValue) {
		final NumericAttributeRetriever numAttRetr = new NumericAttributeRetriever();
		attrValue.accept(numAttRetr);
		final Optional<NumericAttribute> optional = numAttRetr.get();

		if (optional.isPresent()) {
			final NumericAttribute numAttr = optional.get();
			final Integer number = numAttr.getValue().intValue();

			final List<String> res = new ArrayList<>();
			res.add(number.toString());

			for (int i = 1; i <= this.range; i++) {
				final Integer positive = number + i;

				if (positive > this.rangeEnd) {
					final Integer postive2 = this.rangeStart + i - 1;
					res.add(postive2.toString());
				}
				else {
					res.add(positive.toString());
				}

				final Integer negative = number - i;

				if (negative < this.rangeStart) {
					final Integer negative2 = this.rangeEnd - i + 1;
					res.add(negative2.toString());
				}
				else {
					res.add(negative.toString());
				}
			}
			return res;
		}
		else {
			return null;
		}
	}

}
