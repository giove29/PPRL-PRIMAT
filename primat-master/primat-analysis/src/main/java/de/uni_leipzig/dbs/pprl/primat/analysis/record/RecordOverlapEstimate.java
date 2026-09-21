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
package de.uni_leipzig.dbs.pprl.primat.analysis.record;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.phonetic.PhoneticCodeExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.CountingBloomFilter;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.ArrayUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.CombinatoricsUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HMacAlgorithm;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HashUtils;
import de.uni_leipzig.dbs.pprl.primat.common.utils.RecordUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Estimate the number of records that are part of multiple groups
 * 
 * @author frohde
 */
public class RecordOverlapEstimate extends RecordAnalyzer {
	public static final String SOURCE_PAIR = "source pair";
	public static final String METHOD = "method";
	public static final String LENGTH = "length";
	public static final String OVERLAP_MEAN = "overlapMean";
	public static final String OVERLAP_STD = "overlapStd";

	public static final RecordFingerprintBuilder DEFAULT_RECORD_FINGERPRINT_BUILDER = new PkeSouFNSouLNDOB();
	public static final int DEFAULT_VECTOR_LENGTH = 1000;
	public static final int DEFAULT_ITERATIONS = 1;

	private int vectorLength;
	private int iterations;
	private RecordFingerprintBuilder recordFingerprintBuilder;

	public RecordOverlapEstimate() {
		this.vectorLength = DEFAULT_VECTOR_LENGTH;
		this.iterations = DEFAULT_ITERATIONS;
		this.recordFingerprintBuilder = DEFAULT_RECORD_FINGERPRINT_BUILDER;
	}

	@Override
	public ResultSet analyze(List<Record> records) {
		ResultSet resultSet = getResultSet();
		Map<String, List<Record>> groupedRecords = RecordUtils.groupByParty(records);
		estimateOverlap(groupedRecords).forEach(resultSet::addResult);
		return resultSet;
	}

	private List<Result> estimateOverlap(Map<String, List<Record>> recordsBySources) {
		List<Result> results = new ArrayList<>();

		List<String> sourceNames = recordsBySources.keySet().stream().sorted().collect(Collectors.toList());

		Map<String, List<CountingBloomFilter>> cryptoSets = new HashMap<>();

		for (Map.Entry<String, List<Record>> recordsBySource : recordsBySources.entrySet()) {
			cryptoSets.put(recordsBySource.getKey(), getCryptoSets(recordsBySource.getValue()));
		}

		CombinatoricsUtils.combinations(sourceNames, 2).forEach(sourcePair -> {
			Result result = new Result();
			result.setParam(SOURCE_PAIR, String.join("-", sourcePair));
			result.setParam(LENGTH, String.valueOf(vectorLength));
			result.setParam(METHOD, recordFingerprintBuilder.getClass().getSimpleName());
			DescriptiveStatistics stats = new DescriptiveStatistics();
			IntStream.range(0, iterations).boxed().map(i -> estimateOverlap(cryptoSets.get(sourcePair.get(0)).get(i),
				cryptoSets.get(sourcePair.get(1)).get(i))).forEach(stats::addValue);

			for (int i = 0; i < stats.getN(); i++) {
				List<String> row = new ArrayList<>();
				row.add(String.valueOf(vectorLength));
				row.add(recordFingerprintBuilder.getClass().getSimpleName());
				row.add(String.valueOf(i));
				row.add(String.format(Locale.ENGLISH, "%.3f", stats.getElement(i)));
			}
			result.addMetric(OVERLAP_MEAN, BigDecimal.valueOf(stats.getMean()));
			result.addMetric(OVERLAP_STD, BigDecimal.valueOf(stats.getStandardDeviation()));
			results.add(result);
		});
		return results;
	}

	public static double estimateOverlap(CountingBloomFilter csA, CountingBloomFilter csB) {
		double pc = new PearsonsCorrelation().correlation(ArrayUtils.intToDouble(csA.getData()), ArrayUtils.intToDouble(csB.getData()));
		long sumA = csA.cardinality();
		long sumB = csB.cardinality();
		long maxAB = Math.max(sumA, sumB);
		long minAB = Math.min(sumA, sumB);
		double overlapRelative = pc * Math.sqrt((double) maxAB / (double) minAB);
		double overlap = overlapRelative * minAB;
		return overlap;
	}

	private List<CountingBloomFilter> getCryptoSets(Collection<Record> records) {
		List<CountingBloomFilter> sets = new ArrayList<>();

		for (int i = 0; i < iterations; i++) {
			final int tmp = i;
			CountingBloomFilter cryptoSet = new CountingBloomFilter(vectorLength);
			records.stream().map(recordFingerprintBuilder).map(pk -> getPublicID(pk, tmp)).forEach(r -> cryptoSet.increment(r));
			sets.add(cryptoSet);
		}
		return sets;
	}

	private int getPublicID(String privateID, int seed) {
		return HashUtils.toPositiveIntHash(HashUtils.getHmac(HMacAlgorithm.HMAC_MD_5, privateID, String.valueOf(seed)), vectorLength);
	}

	public void setVectorLength(int vectorLength) {
		this.vectorLength = vectorLength;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public void setRecordFingerprintBuilder(RecordFingerprintBuilder recordFingerprintBuilder) {
		this.recordFingerprintBuilder = recordFingerprintBuilder;
	}

	public static class PkeUniqueId extends RecordFingerprintBuilder {
		@Override
		public String apply(Record record) {
			return record.getUniqueIdentifier();
		}
	}

	public static class Pke1FN1LNDOB extends RecordFingerprintBuilder {
		@Override
		public String apply(Record r) {
			String fn = r.getQidAttribute("FN").getStringValue();
			String ln = r.getQidAttribute("LN").getStringValue();
			String dob = r.getQidAttribute("DOB").getStringValue();
			String privKey = new StringBuilder().append(fn.isEmpty() ? "" : fn.charAt(0))
				.append(ln.isEmpty() ? "" : ln.charAt(0)).append(dob).toString();
			return privKey;
		}
	}

	public static class Pke3FN3LNDOB extends RecordFingerprintBuilder {
		@Override
		public String apply(Record r) {
			String fn = r.getQidAttribute("FN").getStringValue();
			String ln = r.getQidAttribute("LN").getStringValue();
			String dob = r.getQidAttribute("DOB").getStringValue();
			String privKey = StringUtils.substring(fn, 0, 3) + StringUtils.substring(ln, 0, 3) + dob;
			return privKey;
		}
	}

	public static class PkeSouFNSouLNDOB extends RecordFingerprintBuilder {
		@Override
		public String apply(Record r) {
			String fn = r.getQidAttribute("FN").getStringValue();
			String ln = r.getQidAttribute("LN").getStringValue();
			String dob = r.getQidAttribute("DOB").getStringValue();
			String soundexFN = PhoneticCodeExtractor.SOUNDEX.get(fn);
			String soundexLN = PhoneticCodeExtractor.SOUNDEX.get(ln);
			String privKey = soundexFN + soundexLN + dob;
			return privKey;
		}
	}

	public static class Pke1FN1LNYOB extends RecordFingerprintBuilder {
		@Override
		public String apply(Record r) {
			String fn = r.getQidAttribute("FN").getStringValue();
			String ln = r.getQidAttribute("LN").getStringValue();
			String dob = r.getQidAttribute("DOB").getStringValue();
			String privKey = new StringBuilder().append(fn.isEmpty() ? "" : fn.charAt(0))
				.append(ln.isEmpty() ? "" : ln.charAt(0)).append(fn.isEmpty() ? "" : dob.substring(dob.length() - 4))
				.toString();
			return privKey;
		}
	}

	public static class Pke3FN3LNYOB extends RecordFingerprintBuilder {
		@Override
		public String apply(Record r) {
			String fn = r.getQidAttribute("FN").getStringValue();
			String ln = r.getQidAttribute("LN").getStringValue();
			String dob = r.getQidAttribute("DOB").getStringValue();
			String privKey = new StringBuilder().append(StringUtils.substring(fn, 0, 3))
				.append(StringUtils.substring(ln, 0, 3)).append(fn.isEmpty() ? "" : dob.substring(dob.length() - 4))
				.toString();
			return privKey;
		}
	}

	public static class PkeSouFNSouLNYOB extends RecordFingerprintBuilder {
		@Override
		public String apply(Record r) {
			String fn = r.getQidAttribute("FN").getStringValue();
			String ln = r.getQidAttribute("LN").getStringValue();
			String dob = r.getQidAttribute("DOB").getStringValue();
			String soundexFN = PhoneticCodeExtractor.SOUNDEX.get(fn);
			String soundexLN = PhoneticCodeExtractor.SOUNDEX.get(ln);
			String privKey = soundexFN + soundexLN + dob.substring(dob.length() - 4);
			return privKey;
		}
	}

	public static abstract class RecordFingerprintBuilder implements Function<Record, String> {
		@Override
		public String apply(Record r) {
			return "";
		}
	}
}
