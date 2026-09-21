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

import java.util.Collection;
import java.util.Collections;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.Attribute;


/**
 * Find the most frequent attribute values and store them as a sorted list
 * Additionally the {@link ResultSet} contains the share of attribute values
 * that belong to the xy% most frequent distinct attribute values e.g. the
 * output: +--------------+--------+---------+---------+--------+--------+ |
 * param | 0.01 | 0.05 | 0.1 | 0.2 | 0.5 |
 * +--------------+--------+---------+---------+--------+--------+ | vorname |
 * 0.1011 | 0.3703 | 0.5581 | 0.7586 | 0.9323 |
 * +--------------+--------+---------+---------+--------+--------+ means that
 * the 1% most frequent distinct attribute values make up about 10% of all
 * attribute values.
 * 
 * @author frohde
 */
public class AttributeMostFrequent extends AttributeFrequencyAnalyzer {

	public AttributeMostFrequent() {
		super();
		System.out.println("Initialized: " + this.toString());
	}

	@Override
	protected Collection<String> getValues(Attribute<?> attribute, Integer attributeName) {
		if (AttributeAvailability.isInvalidOrEmpty(attribute)) {
			return Collections.emptyList();
		}
		else {
			String value = attribute.getStringValue();

			if (toLowerCase) {
				value = value.toLowerCase();
			}
			return Collections.singletonList(value);
		}
	}

	@Override
	protected String buildDescription() {
		return "Share of attribute values that belong to the (x*100)% most-frequent distinct attribute values\n"
			+ "where \"x\" is the column head and the cell entries are the shares.\n"
			+ "The output directory contains files for each attribute type \n"
			+ "that list distinct attributes values sorted by their frequency.";
	}

	@Override
	public String toString() {
		return "AttributeMostFrequent{" + "maxNumber=" + maxNumber + ", minCount=" + minCount + ", cumShares="
			+ cumShares + ", toLowerCase=" + toLowerCase + '}';
	}
}
