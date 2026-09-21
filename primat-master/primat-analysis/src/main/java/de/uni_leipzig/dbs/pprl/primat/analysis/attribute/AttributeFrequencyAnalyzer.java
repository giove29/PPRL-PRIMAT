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

import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.Attribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;

/**
 * 
 * @author frohde
 *
 */
public abstract class AttributeFrequencyAnalyzer extends AttributeAnalyzer {
	private static final int DEFAULT_PROP_MAX_NUMBER = -1;
	private static final int DEFAULT_PROP_MIN_COUNT = 2;
	private static final List<Double> DEFAULT_PROP_SHARES = Arrays.asList(0.01, 0.05, 0.1, 0.2, 0.5);
	private static final boolean DEFAULT_TO_LOWER_CASE = true;

	/**
	 * Absolute limit of the number of values being stored with their share, set
	 * to -1 to deactivate
	 */
	protected int maxNumber;

	/**
	 * Attributes with a lower absolute frequency than this setting are not
	 * stored
	 */
	protected int minCount;

	/**
	 * Shares determining the output to the result summary
	 */
	protected List<Double> cumShares;

	/**
	 * Convert values to lower case
	 */
	protected boolean toLowerCase;

	public AttributeFrequencyAnalyzer() {
		this.maxNumber = DEFAULT_PROP_MAX_NUMBER;
		this.minCount = DEFAULT_PROP_MIN_COUNT;
		this.cumShares = DEFAULT_PROP_SHARES;
		this.toLowerCase = DEFAULT_TO_LOWER_CASE;
	}

	@Override
	public ResultSet analyze(Map<Integer, List<QidAttribute<?>>> attributes) {
		ResultSet resultSet = getResultSet();
		resultSet.setDescription(buildDescription());

		List<Integer> attributeNames = attributes.keySet().stream().sorted().collect(Collectors.toList());

		for (Integer attributeName : attributeNames) {
			List<QidAttribute<?>> curAttributes = attributes.get(attributeName);
			List<ValueFrequency> valueFrequencies = getSortedValueFrequencies(curAttributes, attributeName);

			if (valueFrequencies.isEmpty()) {
				continue;
			}
			Result result = new Result();
			result.setParam(HEADER_ATTRIBUTE, RecordSchema.INSTANCE.getAttributeName(attributeName));
			List<BigDecimal> shares = getShares(valueFrequencies, curAttributes.size());

			for (int i = 0; i < cumShares.size(); i++) {
				result.addMetric(cumShares.get(i).toString(), shares.get(i));
			}
			resultSet.addResult(result);

			if (!valueFrequencies.isEmpty()) {
				Table subResult = buildFrequencyResult(attributeName, valueFrequencies, curAttributes.size());
				resultSet.addAdditionalResult(subResult);
			}
		}
		return resultSet;
	}

	protected abstract Collection<String> getValues(Attribute<?> attribute, Integer attributeName);

	protected abstract String buildDescription();

	protected List<ValueFrequency> getSortedValueFrequencies(List<QidAttribute<?>> attributes, Integer attributeName) {
		Map<String, Long> valueCounts = attributes.stream()
			.flatMap(a -> getValues(a, attributeName).stream())
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		return valueCounts.entrySet().stream().filter(e -> e.getValue() > 0)
			.sorted(Map.Entry.comparingByValue((l1, l2) -> Long.compare(l2, l1))) // reverse
																					// order
			.map(e -> new ValueFrequency(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	private List<BigDecimal> getShares(List<ValueFrequency> valueFrequencies, long numberOfValues) {
		List<Double> relativeFrequencies = getRelativeFrequencies(valueFrequencies, numberOfValues);
		List<BigDecimal> shares = new ArrayList<>();

		for (Double cumShare : cumShares) {
			List<Double> subList = relativeFrequencies.subList(0,
				(int) Math.floor(cumShare * relativeFrequencies.size()));
			double share = subList.stream().mapToDouble(d -> d).sum();
			shares.add(BigDecimal.valueOf(share));
		}
		return shares;
	}

	private List<Double> getRelativeFrequencies(List<ValueFrequency> valueFrequencies, long numberOfValues) {
		return valueFrequencies.stream().map(ValueFrequency::getAbsoluteFrequency).map(l -> (double) l / numberOfValues)
			.collect(Collectors.toList());
	}

	protected Table buildFrequencyResult(Integer attributeName, List<ValueFrequency> valueFrequencies,
		long numberOfValues) {
		boolean checkNumberOfAttributes = (maxNumber > 0);
		StringColumn colValue = StringColumn.create(HEADER_ATTRIBUTE);
		LongColumn colAbsFrequency = LongColumn.create(HEADER_ABSOLUTE_FREQUENCY);
		DoubleColumn colRelFrequency = DoubleColumn.create(HEADER_RELATIVE_FREQUENCY);
		List<List<String>> rows = new ArrayList<>();

		for (ValueFrequency valueFrequency : valueFrequencies) {

			if (checkNumberOfAttributes && rows.size() >= maxNumber) {
				break;
			}

			if (valueFrequency.getAbsoluteFrequency() < minCount) {
				break;
			}
			double relativeFrequency = (double) valueFrequency.getAbsoluteFrequency() / numberOfValues;
			colValue.append(valueFrequency.getValue());
			colAbsFrequency.append(valueFrequency.getAbsoluteFrequency());
			colRelFrequency.append(relativeFrequency);
		}
		return Table.create(attributeName.toString(), colValue, colAbsFrequency, colRelFrequency);
	}

	public int getMaxNumber() {
		return maxNumber;
	}

	public void setMaxNumber(int maxNumber) {
		this.maxNumber = maxNumber;
	}

	public int getMinCount() {
		return minCount;
	}

	public void setMinCount(int minCount) {
		this.minCount = minCount;
	}

	public List<Double> getCumShares() {
		return cumShares;
	}

	public void setCumShares(List<Double> cumShares) {
		this.cumShares = cumShares;
	}

	public boolean isToLowerCase() {
		return toLowerCase;
	}

	public void setToLowerCase(boolean toLowerCase) {
		this.toLowerCase = toLowerCase;
	}

	protected class ValueFrequency {
		private String value;
		private long absoluteFrequency;

		public ValueFrequency(String value, long absoluteFrequency) {
			this.value = value;
			this.absoluteFrequency = absoluteFrequency;
		}

		public String getValue() {
			return value;
		}

		public long getAbsoluteFrequency() {
			return absoluteFrequency;
		}
	}
}
