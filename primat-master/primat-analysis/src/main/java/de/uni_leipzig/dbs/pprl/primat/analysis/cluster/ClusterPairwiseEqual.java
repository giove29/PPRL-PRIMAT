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
package de.uni_leipzig.dbs.pprl.primat.analysis.cluster;


import org.apache.commons.collections4.OrderedBidiMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeAvailability;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * Analyze the cluster of records belonging to the same real world entity by
 * counting the number of attributes these records differ
 * 
 * @author frohde
 */
public class ClusterPairwiseEqual extends ClusterAnalyzer {
	private static final String FULL_RECORD = "full record";
	private static final String FULL_RECORD_NUM_ERRORS = "#errors=";

	@Override
	public ResultSet analyze(Map<String, List<Record>> clusters) {
		ResultSet resultSet = getResultSet();
		resultSet.setDescription(buildDescription());

		Map<String, DescriptiveStatistics> stats = new HashMap<>();
		stats.put(FULL_RECORD, new DescriptiveStatistics());

		for (List<Record> cluster : clusters.values()) {
			List<Pair<Record>> recordPairs = buildRecordPairs(cluster);

			// TODO Normalize on number of pairs to prevent disproportional
			// influence of large clusters?
			for (Pair<Record> recordPair : recordPairs) {
				double recordDistance = 0;

				for (AttributePair ap : buildAttributePairs(recordPair)) {

					if (!stats.containsKey(ap.attributeName)) {
						stats.put(ap.attributeName, new DescriptiveStatistics());
					}
					double attrDistance = ap.getDistance();
					stats.get(ap.attributeName).addValue(attrDistance);
					recordDistance += attrDistance;
				}
				stats.get(FULL_RECORD).addValue(recordDistance);
				// if (recordDistance >= 2) System.out.println(recordDistance +
				// " " + recordPair.v0.getId() + " " +
				// recordPair.v1.getId());
				String numErrorKey = FULL_RECORD_NUM_ERRORS + String.format("%d", (int) recordDistance);

				if (!stats.containsKey(numErrorKey)) {
					stats.put(numErrorKey, new DescriptiveStatistics());
				}
				stats.get(numErrorKey).addValue(1);
			}
		}

		List<String> attributeNames = stats.keySet().stream().sorted().collect(Collectors.toList());

		long total = stats.get(FULL_RECORD).getN();

		for (String attributeName : attributeNames) {
			Result result = new Result();
			result.setParam(HEADER_ATTRIBUTE, attributeName);
			fillStats(stats.get(attributeName), total);
			addDescriptiveStatisticMetrics(result, stats.get(attributeName),
				Arrays.asList("count", "mean", "min", "max"));
			resultSet.addResult(result);
		}

		return resultSet;
	}

	private void fillStats(DescriptiveStatistics stat, long total) {
		long count = stat.getN();

		while (count < total) {
			stat.addValue(0);
			count++;
		}
	}

	private String buildDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("All attribute values of the same type are compared on equality within each cluster\n");
		return sb.toString();
	}

	private List<Pair<Record>> buildRecordPairs(List<Record> records) {
		List<Pair<Record>> recordPairs = new ArrayList<>();

		for (int i = 0; i < records.size(); i++) {

			for (int j = i + 1; j < records.size(); j++) {
				recordPairs.add(new Pair<>(records.get(i), records.get(j)));
			}
		}
		return recordPairs;
	}

	private List<AttributePair> buildAttributePairs(Pair<Record> recordPair) {
		List<AttributePair> attributePairs = new ArrayList<>();

		Record r0 = recordPair.v0;
		Record r1 = recordPair.v1;

		final OrderedBidiMap<String, Integer> attrNames = RecordSchema.INSTANCE.getNameColumnMapping();
		for (final Entry<String, Integer> attr : attrNames.entrySet()) {
			final String attrName = attr.getKey();
			
			QidAttribute<?> attr0 = r0.getQidAttribute(attrName);
			QidAttribute<?> attr1 = r1.getQidAttribute(attrName);

			if (!AttributeAvailability.isInvalidOrEmpty(attr0)
				|| !AttributeAvailability.isInvalidOrEmpty(attr1)) {
				attributePairs.add(new AttributePair(attrName, attr0, attr1));
			}
		}
		return attributePairs;
	}

	@Override
	public String toString() {
		return "ClusterPairwiseEqual";
	}

	private class Pair<T> {
		protected T v0;
		protected T v1;

		Pair(T v0, T v1) {
			this.v0 = v0;
			this.v1 = v1;
		}
	}

	private class AttributePair extends Pair<QidAttribute<?>> {
		private String attributeName;

		AttributePair(String attributeName, QidAttribute<?> attr0, QidAttribute<?> attr1) {
			super(attr0, attr1);
			this.attributeName = attributeName;
		}

		double getDistance() {
			String s0 = v0.getStringValue();
			String s1 = v1.getStringValue();
			return s0.equals(s1) ? 0 : 1;
		}

		@Override
		public String toString() {
			return "AttributePair{" + "v0=" + v0 + ", v1=" + v1 + ", attributeName='" + attributeName + '\'' + '}';
		}
	}
}
