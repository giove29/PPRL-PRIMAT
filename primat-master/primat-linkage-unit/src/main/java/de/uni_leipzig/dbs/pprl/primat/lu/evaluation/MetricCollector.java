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

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy;


/**
 * Collector for metrics.
 * 
 * @author mfranke
 *
 */
public enum MetricCollector {

	INSTANCE;

	private Map<String, Number> metrics;

	private MetricCollector() {
		this.metrics = new HashMap<>();
	}

	public Map<String, Number> getMetrics() {
		return this.metrics;
	}

	public void registerInput(Map<Party, Collection<Record>> input, ComparisonStrategy compStrat) {
		this.metrics.put("parties", PerformanceMetrics.getNumberOfParties(input));
		this.metrics.put("records", PerformanceMetrics.getTotalNumberOfRecords(input));
		this.metrics.put("maxComparisons", PerformanceMetrics.getMaxComparisons(input, compStrat));
	}

	public void registerBlocks(Collection<Block> blocks) {
		this.metrics.put("blocks", blocks.size());
		this.metrics.put("avgBlockSize", PerformanceMetrics.getAverageBlockSize(blocks));
		this.metrics.put("maxBlockSize", PerformanceMetrics.getMaxBlockSize(blocks));
	}

	public void registerBlockingTime(long executionTime) {
		this.metrics.put("timeBlocking", executionTime);
	}

	public void registerTimeClassification(long executionTime) {
		this.metrics.put("timeClassification", executionTime);
	}

	public void registerTimePostprocessing(long executionTime) {
		this.metrics.put("timePostprocessing", executionTime);
	}

	public void registerLinkageResult(LinkageResult<?> linkRes) {
		final Set<PartyPair> partitions = linkRes.partitions();

		final DoubleSummaryStatistics matchStats = new DoubleSummaryStatistics();
		final DoubleSummaryStatistics nonMatchStats = new DoubleSummaryStatistics();

		for (final PartyPair partyPair : partitions) {
			final LinkageResultPartition<?> part = linkRes.getPartition(partyPair);

			for (final LinkedPair<?> match : part.getMatchStrategy().getMatches()) {
				matchStats.accept(match.getAggregatedSim());
			}

			for (final LinkedPair<?> nonMatch : part.getNonMatchStrategy().getNonMatches()) {
				nonMatchStats.accept(nonMatch.getAggregatedSim());
			}
		}

		final long matches = matchStats.getCount();
		this.metrics.put("classifiedMatches", matches);
		this.metrics.put("avgSimClassifiedMatches", matchStats.getAverage());

		final long nonMatches = nonMatchStats.getCount();
		this.metrics.put("classifiedNonMatches", nonMatches);
		this.metrics.put("avgSimClassifiedNonMatches", nonMatchStats.getAverage());

		final long candidates = matches + nonMatches;
		this.metrics.put("candidates", candidates);

		final Number maxComparisons = this.metrics.getOrDefault("maxComparisons", 1);
		final double rr = PerformanceMetrics.getReductionRatio(candidates, maxComparisons.longValue());
		this.metrics.put("reductionRatio", rr);
	}

	public void registerExpectedMatches(int expectedMatches) {
		this.metrics.put("expectedMatches", expectedMatches);
	}

	public <T extends Linkable> void registerLinkageResultQuality(LinkageResult<T> linkRes,
		TrueMatchChecker trueMatchChecker) {
		// TODO: evaluation for all partitions or on partition-level??
		// Pseudo-measures over multiple partitions, i.e., A-B, A-C, B-C ???
		final QualityEvaluator<T> qualityEvaluator = new QualityEvaluator<T>(trueMatchChecker);
		qualityEvaluator.add(linkRes);

		final int matches = linkRes.matches();
		final int nonMatches = linkRes.nonMatches();
		final int falsePositives = qualityEvaluator.getCount(ResultType.FALSE_POSITIVE);
		final int truePositives = qualityEvaluator.getCount(ResultType.TRUE_POSITIVE);
		final int falseNegatives = qualityEvaluator.getCount(ResultType.FALSE_NEGATIVE);

		this.metrics.put("falsePositives", falsePositives);
		this.metrics.put("matches", matches);
		this.metrics.put("truePositives", truePositives);
		this.metrics.put("falseNegatives", falseNegatives);

		final int expectedMatches = this.metrics.getOrDefault("expectedMatches", -1).intValue();

		if (expectedMatches != -1) {
			final double pairsCompleteness = QualityMetrics.getRecall(truePositives + falseNegatives, expectedMatches);
			final double pairsQuality = QualityMetrics.getPrecision(truePositives, matches + nonMatches);

			this.metrics.put("pairsCompleteness", pairsCompleteness);
			this.metrics.put("pairsQuality", pairsQuality);

			final double recall = QualityMetrics.getRecall(truePositives, expectedMatches);
			final double precision = QualityMetrics.getPrecision(truePositives, matches);
			final double fmeasure = QualityMetrics.getFMeasure(recall, precision);

			this.metrics.put("recall", recall);
			this.metrics.put("precision", precision);
			this.metrics.put("fmeasure", fmeasure);
		}

		final Set<PartyPair> parts = linkRes.partitions();

		for (final PartyPair part : parts) {
			final MatchStrategy<T> matchStrat = linkRes.getPartition(part).getMatchStrategy();
			final int sourceNodes = matchStrat.sourceSize();
			final int targetNodes = matchStrat.targetSize();
			final int links = matchStrat.size();

			this.metrics.put("sourcNodes_" + part.toString(), sourceNodes);
			this.metrics.put("targetNodes_" + part.toString(), targetNodes);
			this.metrics.put("links_" + part.toString(), links);

			// TODO: only use similarity graphs or pass features to matchStrat
			final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<>();
			matchStrat.accept(vis);
			final SimilarityGraph<T> g = vis.getSimilarityGraph();

			final int singleLinks = g.singleLinks();
			final int multiLinks = g.multiLinks();

			this.metrics.put("singleLinks_" + part.toString(), singleLinks);
			this.metrics.put("multiLinks_" + part.toString(), multiLinks);
		}
	}

	/*
	 * public static final String LEFT_1_TO_1_MAPPINGS = "left1To1Mappings";
	 * public static final String LEFT_1_TO_N_MAPPINGS = "left1ToNMappings";
	 * public static final String RIGHT_1_TO_1_MAPPINGS = "right1To1Mappings";
	 * public static final String RIGHT_1_TO_N_MAPPINGS = "right1ToNMappings";
	 * public static final String MULTI_MATCH_RATIO = "multiMatchRatio"; public
	 * static final String ONE_TO_ONE_LINK_RATIO = "oneToOneLinkRatio"; public
	 * static final String MULTI_LINK_RATIO = "multiLinkRatio"; public static
	 * final String LEFT_ONE_TO_ONE_LINKS = "leftOneToOneLinks"; public static
	 * final String LEFT_MULTI_LINKS = "leftMultiLinks"; public static final
	 * String RIGHT_ONE_TO_ONE_LINKS = "rightOneToOneLinks"; public static final
	 * String RIGHT_MULTI_LINKS = "leftMultiLinks";
	 */
}