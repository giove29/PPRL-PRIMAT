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
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategy;


/**
 * 
 * @author mfranke
 *
 * @param  <T>
 */
public class QualityEvaluator<T extends Linkable> {

	private TrueMatchChecker trueMatchChecker;
	private Map<ResultType, DescriptiveStatistics> data;

	public QualityEvaluator(TrueMatchChecker trueMatchChecker) {
		this.trueMatchChecker = Objects.requireNonNull(trueMatchChecker);
		this.data = new HashMap<ResultType, DescriptiveStatistics>();
		this.data.put(ResultType.TRUE_POSITIVE, new DescriptiveStatistics());
		this.data.put(ResultType.FALSE_POSITIVE, new DescriptiveStatistics());
		this.data.put(ResultType.TRUE_NEGATIVE, new DescriptiveStatistics());
		this.data.put(ResultType.FALSE_NEGATIVE, new DescriptiveStatistics());
	}

	private void addTruePositive(LinkedPair<T> pair) {
		this.data.get(ResultType.TRUE_POSITIVE).addValue(pair.getAggregatedSim());
	}

	private void addFalsePositive(LinkedPair<T> pair) {
		// System.out.println(pair);
		this.data.get(ResultType.FALSE_POSITIVE).addValue(pair.getAggregatedSim());
	}

	private void addTrueNegative(LinkedPair<T> pair) {
		this.data.get(ResultType.TRUE_NEGATIVE).addValue(pair.getAggregatedSim());
	}

	private void addFalseNegative(LinkedPair<T> pair) {
		this.data.get(ResultType.FALSE_NEGATIVE).addValue(pair.getAggregatedSim());
	}

	private long getResultTypeCountFor(ResultType resType, double threshold) {
		final double[] values = this.data.get(resType).getValues();
		return Arrays.stream(values).filter(i -> i >= threshold).count();
	}
	
	public long getTruePositives() {
		return this.get(ResultType.TRUE_POSITIVE);
	}
	
	public long getTruePositivesFor(double threshold) {
		return this.getResultTypeCountFor(ResultType.TRUE_POSITIVE, threshold);
	}

	public long getFalsePositives() {
		return this.get(ResultType.FALSE_POSITIVE);
	}
	
	public long getFalsePositivesFor(double threshold) {
		return this.getResultTypeCountFor(ResultType.FALSE_POSITIVE, threshold);
	}

	public long getTrueNegatives() {
		return this.get(ResultType.TRUE_NEGATIVE);
	}
	
	public long getTrueNegativesFor(double threshold) {
		return this.getResultTypeCountFor(ResultType.TRUE_NEGATIVE, threshold);
	}

	public long getFalseNegatives() {
		return this.get(ResultType.FALSE_NEGATIVE);
	}
	
	public long getFalseNegativesFor(double threshold) {
		return this.getResultTypeCountFor(ResultType.FALSE_NEGATIVE, threshold);
	}

	public long get(ResultType type) {
		return this.data.get(type).getN();
	}

	public int getCount(ResultType type) {
		return Math.toIntExact(this.data.get(type).getN());
	}

	public double getAverage(ResultType type) {
		return this.data.get(type).getMean();
	}

	public double getMin(ResultType type) {
		return this.data.get(type).getMin();
	}

	public double getMax(ResultType type) {
		return this.data.get(type).getMax();
	}

	public void addMatch(LinkedPair<T> pair) {

		if (this.trueMatchChecker.isTrueMatch(pair.getLeftRecord(), pair.getRightLinkable())) {
			this.addTruePositive(pair);
		}
		else {
			this.addFalsePositive(pair);
//			 System.out.println(pair);

		}
	}

	public void addNonMatch(LinkedPair<T> pair) {

		if (this.trueMatchChecker.isTrueMatch(pair.getLeftRecord(), pair.getRightLinkable())) {
			this.addFalseNegative(pair);
		}
		else {
			this.addTrueNegative(pair);
		}
	}

	public void addMatches(MatchStrategy<T> matchStrategy) {
		final Set<LinkedPair<T>> matches = matchStrategy.getMatches();
		this.addMatches(matches);
	}

	public void addMatches(Collection<LinkedPair<T>> matches) {

		for (final LinkedPair<T> pair : matches) {
			this.addMatch(pair);
		}
	}

	public void addNonMatches(NonMatchStrategy<T> nonMatchStrategy) {
		final Set<LinkedPair<T>> nonMatches = nonMatchStrategy.getNonMatches();
		this.addNonMatches(nonMatches);
	}

	public void addNonMatches(Collection<LinkedPair<T>> nonMatches) {

		for (final LinkedPair<T> pair : nonMatches) {
			this.addNonMatch(pair);
		}
	}

	public void add(LinkageResult<T> linkRes) {
		final List<LinkedPair<T>> matches = linkRes.getMatches();
		this.addMatches(matches);

		final List<LinkedPair<T>> nonMatches = linkRes.getNonMatches();
		this.addNonMatches(nonMatches);
	}
}
