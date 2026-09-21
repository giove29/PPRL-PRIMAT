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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.Attribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Measure the share of attribute values that are valid, invalid (e.g. null,
 * NaN) or empty for each attribute type
 * 
 * @author frohde
 */
public class AttributeAvailability extends AttributeAnalyzer {
	public static final String[] INVALID_STRINGS = {
		"null", "NULL", "Null", "NaN"
	};

	public static final String VALID = "valid";
	public static final String INVALID = "invalid";
	public static final String EMPTY = "empty";
	public static final String MISSING = "missing";

	@Override
	public ResultSet analyze(Map<Integer, List<QidAttribute<?>>> attributes) {
		ResultSet resultSet = getResultSet();
		resultSet
			.setDescription("Share of attribute values that are valid, invalid (e.g. null, NaN), empty or missing");

		for (Map.Entry<Integer, List<QidAttribute<?>>> attribute : attributes.entrySet()) {
			int empty = 0;
			int valid = 0;
			int invalid = 0;
			for (Attribute<?> attr : attribute.getValue()) {
				if (isEmpty(attr)) {
					empty++;
				}
				else if (isInvalid(attr)) {
					invalid++;
				}
				else {
					valid++;
				}
			}
			int missing = recordCount - empty - valid - invalid;

			Result result = new Result();
			result.setParam(HEADER_ATTRIBUTE, RecordSchema.INSTANCE.getAttributeName(attribute.getKey()));
			List<BigDecimal> values = Stream.of(valid, invalid, empty, missing).map(i -> (double) i / recordCount)
				.map(BigDecimal::valueOf).collect(Collectors.toList());
			result.addMetric(VALID, values.get(0));
			result.addMetric(INVALID, values.get(1));
			result.addMetric(EMPTY, values.get(2));
			result.addMetric(MISSING, values.get(3));
			resultSet.addResult(result);
		}
		return resultSet;
	}

	// TODO Make this class abstract and move the method isValid() to the
	// subclasses
	public static boolean isInvalid(Attribute<?> attribute) {
		return Arrays.asList(INVALID_STRINGS).contains(attribute.getStringValue());
	}

	public static boolean isEmpty(Attribute<?> attribute) {
		return attribute.getStringValue().isEmpty();
	}

	public static boolean isInvalidOrEmpty(Attribute<?> attribute) {
		return isEmpty(attribute) || isInvalid(attribute);
	}
}