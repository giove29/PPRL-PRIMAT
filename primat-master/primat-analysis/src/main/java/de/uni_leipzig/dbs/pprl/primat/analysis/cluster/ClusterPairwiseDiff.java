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
import org.apache.commons.text.similarity.LevenshteinDistance;

import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeAvailability;
import de.uni_leipzig.dbs.pprl.primat.analysis.difference.CustomDiff;
import de.uni_leipzig.dbs.pprl.primat.analysis.difference.CustomDiffOperation;
import de.uni_leipzig.dbs.pprl.primat.analysis.difference.DiffOperation;
import de.uni_leipzig.dbs.pprl.primat.analysis.difference.Diff;
import de.uni_leipzig.dbs.pprl.primat.analysis.difference.DiffCalculator;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * Analyze the cluster of records belonging to the same real world entity by the
 * differences between pairs of these records
 * 
 * @author frohde
 */
public class ClusterPairwiseDiff extends ClusterAnalyzer {
	private static final String PREFIX_PAIRS = "Pairs_";
	private static final String PREFIX_MASKED = "Masked_";
	private static final String PREFIX_BYTYPE = "ByType_";
	private static final String FULL_RECORD = "full record";

	private static final double DEFAULT_MIN_EQUAL_CHARACTER_SHARE = 0.5;
	private static final int DEFAULT_MAX_LENGTH_OF_DIFF = 3;
	private static final boolean DEFAULT_INCLUDE_ATTRIBUTE_PAIRS = true;
	private static final boolean DEFAULT_INCLUDE_MASKED_DIFFS = true;
	private static final boolean DEFAULT_INCLUDE_DIFFS_BY_TYPE = true;

	/**
	 * Minimal share of equal characters per attribute value pair prohibits
	 * leaks of large parts of attributes values (e.g. ?[ei/üll]?? for "Meier"
	 * and "Müller")
	 */
	private double minEqualCharacterShare;

	/**
	 * Maximal length of a attribute segment to be included in the MASKED or
	 * BYTYPE output prohibits leaks of substrings from long attribute values
	 * (e.g. ????[ashi]?? is probably "kardashian")
	 */
	private int maxLengthOfDiff;

	/**
	 * Include differing attribute pairs in plain text in the result
	 */
	private boolean includeAttributePairs;

	/**
	 * Include masked differences between differing attribute values in the
	 * result
	 */
	private boolean includeMaskedDiffs;

	/**
	 * Include differing substrings with the type of the difference (e.g.
	 * replacement, swap) in the result
	 */
	private boolean includeDiffsByType;

	public ClusterPairwiseDiff() {
		this.minEqualCharacterShare = DEFAULT_MIN_EQUAL_CHARACTER_SHARE;
		this.maxLengthOfDiff = DEFAULT_MAX_LENGTH_OF_DIFF;
		this.includeAttributePairs = DEFAULT_INCLUDE_ATTRIBUTE_PAIRS;
		this.includeMaskedDiffs = DEFAULT_INCLUDE_MASKED_DIFFS;
		this.includeDiffsByType = DEFAULT_INCLUDE_DIFFS_BY_TYPE;
		System.out.println("Initialized: " + this.toString());
	}

	@Override
	public ResultSet analyze(Map<String, List<Record>> clusters) {
		ResultSet resultSet = getResultSet();
		resultSet.setDescription(buildDescription());

		Map<String, DescriptiveStatistics> stats = new HashMap<>();
		stats.put(FULL_RECORD, new DescriptiveStatistics());

		Map<String, List<AttributePair>> differingAttributePairs = new HashMap<>();

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

					if (attrDistance > 0) {

						if (!differingAttributePairs.containsKey(ap.attributeName)) {
							differingAttributePairs.put(ap.attributeName, new ArrayList<>());
						}
						differingAttributePairs.get(ap.attributeName).add(ap);
					}
					stats.get(ap.attributeName).addValue(attrDistance);
					recordDistance += attrDistance;
				}
				stats.get(FULL_RECORD).addValue(recordDistance);
			}
		}

		List<String> attributeNames = stats.keySet().stream().sorted().collect(Collectors.toList());

		for (String attributeName : attributeNames) {
			Result result = new Result();
			result.setParam(HEADER_ATTRIBUTE, attributeName);
			addDescriptiveStatisticMetrics(result, stats.get(attributeName),
				Arrays.asList("count", "median", "mean", "min", "max", "sd"));
			resultSet.addResult(result);
		}

		buildDifferingAttributePairsResults(differingAttributePairs).forEach(resultSet::addAdditionalResult);

		return resultSet;
	}

	private String buildDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("All attribute values of the same type are compared within each cluster on the basis "
			+ "of the Levenshtein distance.\nAdditionally the attribute distances are accumulated to the record "
			+ "distance.\n");

		if (includeAttributePairs || includeMaskedDiffs || includeDiffsByType) {
			sb.append("The output directory contains files for each attribute type with the following filename "
				+ "prefixes:\n");

			if (includeAttributePairs) {
				sb.append("\t " + PREFIX_PAIRS + " : contains all differing attribute value pairs in plain text\n");
			}

			if (includeMaskedDiffs) {
				sb.append("\t " + PREFIX_MASKED
					+ " : contains the masked differences between differing attribute value pairs\n");
			}

			if (includeDiffsByType) {
				sb.append("\t " + PREFIX_BYTYPE + " : contains the differing substrings and the type of difference "
					+ "(REPLACEMENT, SWAP, INSERTION)");
			}
		}
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

			if (!AttributeAvailability.isInvalidOrEmpty(attr0) || !AttributeAvailability.isInvalidOrEmpty(attr1)) {
				attributePairs.add(new AttributePair(attrName, attr0, attr1));
			}
		}
		return attributePairs;
	}

	private List<Table> buildDifferingAttributePairsResults(
		Map<String, List<AttributePair>> allDifferingAttributePairs) {
		List<Table> results = new ArrayList<>();

		for (Map.Entry<String, List<AttributePair>> attributePairs : allDifferingAttributePairs.entrySet()) {
			System.out.println("Number of differing attributes for type " + attributePairs.getKey() + ": "
				+ attributePairs.getValue().size());

			if (includeAttributePairs) {
				results.add(buildAttributePairs(attributePairs.getKey(), attributePairs.getValue()));
			}

			List<List<CustomDiff>> diffs = getFilteredDiffs(attributePairs.getValue());
			System.out.println("Number of differing attributes after privacy filtering: " + diffs.size());

			if (includeMaskedDiffs) {
				results.add(buildMaskedDiffs(attributePairs.getKey(), diffs));
			}

			if (includeDiffsByType) {
				results.add(buildDiffsByType(attributePairs.getKey(), diffs));
			}
		}
		return results;
	}

	private List<List<CustomDiff>> getFilteredDiffs(List<AttributePair> attributePairs) {
		return attributePairs.stream().filter(ap -> ap.getEqualCharactersShare() > minEqualCharacterShare)
			.map(AttributePair::getDiffs)
			.map(ds -> ds.stream()
				.filter(d -> d.getS0().length() <= maxLengthOfDiff || d.getS1().length() <= maxLengthOfDiff)
				.collect(Collectors.toList()))
			.collect(Collectors.toList());
	}

	private Table buildAttributePairs(String attributeName, List<AttributePair> attributePairs) {
		StringColumn colAttr0 = StringColumn.create("attr0");
		StringColumn colAttr1 = StringColumn.create("attr1");
		attributePairs.forEach(ap -> {
			colAttr0.append(ap.v0.getStringValue());
			colAttr1.append(ap.v1.getStringValue());
		});
		return Table.create(PREFIX_PAIRS + attributeName, colAttr0, colAttr1);
	}

	private Table buildMaskedDiffs(String attributeName, List<List<CustomDiff>> diffs) {
		StringColumn colDiff = StringColumn.create("attributeDiff");
		diffs.stream().map(ds -> ds.stream().map(CustomDiff::toString).collect(Collectors.joining()))
			.filter(s -> s.matches(".*[^?]+.*")) // Remove strings containing
													// only "?" chars
			.forEach(colDiff::append);
		return Table.create(PREFIX_MASKED + attributeName, colDiff);
	}

	private Table buildDiffsByType(String attributeName, List<List<CustomDiff>> diffs) {
		StringColumn colType = StringColumn.create("type");
		StringColumn colString0 = StringColumn.create("string0");
		StringColumn colString1 = StringColumn.create("string1");
		diffs.stream().flatMap(List::stream).filter(diff -> diff.getOperation() != CustomDiffOperation.EQUAL)
			.forEach(diff -> {
				colType.append(diff.getOperation().toString());
				colString0.append(diff.getS0());
				colString1.append(diff.getS1());
			});
		return Table.create(PREFIX_BYTYPE + attributeName, colType, colString0, colString1);
	}

	public double getMinEqualCharacterShare() {
		return minEqualCharacterShare;
	}

	public void setMinEqualCharacterShare(double minEqualCharacterShare) {
		this.minEqualCharacterShare = minEqualCharacterShare;
	}

	public int getMaxLengthOfDiff() {
		return maxLengthOfDiff;
	}

	public void setMaxLengthOfDiff(int maxLengthOfDiff) {
		this.maxLengthOfDiff = maxLengthOfDiff;
	}

	public boolean isIncludeAttributePairs() {
		return includeAttributePairs;
	}

	public void setIncludeAttributePairs(boolean includeAttributePairs) {
		this.includeAttributePairs = includeAttributePairs;
	}

	public boolean isIncludeMaskedDiffs() {
		return includeMaskedDiffs;
	}

	public void setIncludeMaskedDiffs(boolean includeMaskedDiffs) {
		this.includeMaskedDiffs = includeMaskedDiffs;
	}

	public boolean isIncludeDiffsByType() {
		return includeDiffsByType;
	}

	public void setIncludeDiffsByType(boolean includeDiffsByType) {
		this.includeDiffsByType = includeDiffsByType;
	}

	@Override
	public String toString() {
		return "ClusterPairwiseDiff{" + "minEqualCharacterShare=" + minEqualCharacterShare + ", maxLengthOfDiff="
			+ maxLengthOfDiff + ", includeAttributePairs=" + includeAttributePairs + ", includeMaskedDiffs="
			+ includeMaskedDiffs + ", includeDiffsByType=" + includeDiffsByType + '}';
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
		private List<CustomDiff> diffs;
		private double equalCharactersShare = 1.0;

		AttributePair(String attributeName, QidAttribute<?> attr0, QidAttribute<?> attr1) {
			super(attr0, attr1);
			this.attributeName = attributeName;
		}

		double getDistance() {
			final String s0 = v0.getStringValue();
			final String s1 = v1.getStringValue();

			final LevenshteinDistance ld = new LevenshteinDistance();
			return ld.apply(s0, s1);
		}

		List<CustomDiff> getDiffs() {
			String s0 = v0.getStringValue();
			String s1 = v1.getStringValue();

			int equalChars = 0;

			final DiffCalculator diffCalc = new DiffCalculator();
			LinkedList<Diff> tmpDiffs = diffCalc.diff_main(s0, s1);

			for (int i = 0; i < tmpDiffs.size(); i++) {
				Diff curDiff = tmpDiffs.get(i);

				switch (curDiff.getOperation()) {
					case EQUAL:
						equalChars += curDiff.getText().length();
						diffs.add(new CustomDiff(CustomDiffOperation.EQUAL, curDiff.getText(), ""));
						break;
					case DELETE:
						if (tmpDiffs.size() > i + 1) {
							Diff nextDiff = tmpDiffs.get(i + 1);

							if (nextDiff.getOperation() == DiffOperation.INSERT) {
								diffs.add(new CustomDiff(CustomDiffOperation.REPLACEMENT, curDiff.getText(),
									nextDiff.getText()));
								i += 1;
								break;
							}

							if (tmpDiffs.size() > i + 2) {
								Diff afterNextDiff = tmpDiffs.get(i + 2);

								if (nextDiff.getOperation() == DiffOperation.EQUAL
									&& afterNextDiff.getOperation() == DiffOperation.INSERT
									&& afterNextDiff.getText().equals(curDiff.getText())) {
									diffs.add(new CustomDiff(CustomDiffOperation.SWAP, curDiff.getText(),
										nextDiff.getText()));
									i += 2;
									break;
								}
							}
						}
					case INSERT:
						diffs.add(new CustomDiff(CustomDiffOperation.INSERT, curDiff.getText(), ""));
				}
			}
			equalCharactersShare = (double) equalChars / Math.min(s0.length(), s1.length());
			return diffs;
		}

		double getEqualCharactersShare() {

			if (needsDiffEvaluation()) {
				getDiffs();
			}
			return equalCharactersShare;
		}

		private boolean needsDiffEvaluation() {
			return (diffs == null);
		}
	}
}