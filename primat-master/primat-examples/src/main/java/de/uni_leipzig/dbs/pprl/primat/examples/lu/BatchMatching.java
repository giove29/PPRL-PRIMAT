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

package de.uni_leipzig.dbs.pprl.primat.examples.lu;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.LshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.RandomHammingLshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.model.LinkageConstraint;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DoubleListAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.lsh.LshBlocker;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.ThresholdClassificator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.Matcher;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.batch.BatchMatcher;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.NoPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxBothPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxRightPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity.BitSetAttributeSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.BaseRecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.RecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.BatchSimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.RedundancyCheckStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.SimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary.BinarySimilarity;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorFlattener;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.FlatSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorFlattener;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.NoThresholdRefinement;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ThresholdClassificationRefinement;



/**
 * 
 * @author mfranke
 *
 */
public class BatchMatching {

	public static void main(String[] args) throws IOException {

		final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
				.add(0, NonQidAttributeType.ID)
				.add(1, NonQidAttributeType.GLOBAL_ID)
				.add(1, NonQidAttributeType.PARTY)
				.add(2, QidAttributeType.BITSET, "BS")
				.build();	
		
		final String inputPath = args[0];
		final double threshold = Double.parseDouble(args[1]);
		final int matches = Integer.parseInt(args[2]);
		
		final String namePartyA = "A";
		final String namePartyB = "B";
		
		final DatasetReader reader = new DatasetReader(inputPath, rsc);
		final List<Record> dataset = reader.read();
	
		
		final List<Record> datasetA = dataset.stream()
			.filter(r -> r.getPartyAttribute().getValue().getName().equals(namePartyA)).collect(Collectors.toList());
		
		final List<Record> datasetB = dataset.stream()
			.filter(r -> r.getPartyAttribute().getValue().getName().equals(namePartyB)).collect(Collectors.toList());

		final Map<Party, Collection<Record>> input = new HashMap<>();
		input.put(new Party(namePartyA), datasetA);
		input.put(new Party(namePartyB), datasetB);
		
		System.out.println("#Records source" + namePartyA + ": " + datasetA.size());
		System.out.println("#Records source B" + namePartyB + ": " + datasetB.size());

		final ComparisonStrategy compStrat = ComparisonStrategy.SOURCE_CONSISTENT;

		final LshKeyGenerator keyGen = new RandomHammingLshKeyGenerator(16, 30, 1024, 42L);
		final Blocker blocker = new LshBlocker(keyGen);

		final RecordSimilarityCalculator simCalc = new BaseRecordSimilarityCalculator(
			List.of(new BitSetAttributeSimilarityCalculator(List.of(BinarySimilarity.JACCARD_SIMILARITY))));

		final SimilarityVectorFlattener flattener = new BaseSimilarityVectorFlattener(
			List.of(DoubleListAggregator.FIRST));
		final FlatSimilarityVectorAggregator aggregator = new BaseSimilarityVectorAggregator(
			DoubleListAggregator.FIRST);
		final SimilarityVectorAggregator agg = new SimilarityVectorAggregator(flattener, aggregator);
		
		
		final Classificator classifier = new ThresholdClassificator(threshold, agg);
		
		final MatchStrategyFactory<Record> matchFactory = new SimilarityGraphMatchStrategyFactory<>();
		final NonMatchStrategyFactory<Record> nonMatchFactory = new IgnoreNonMatchesStrategyFactory<>();
		final LinkageResultPartitionFactory<Record> linkResFac = new LinkageResultPartitionFactory<>(matchFactory, nonMatchFactory);
		final SimilarityClassification simClass = new BatchSimilarityClassification(compStrat, simCalc, classifier,
			RedundancyCheckStrategy.MATCH_TWICE, linkResFac);
		final ThresholdClassificationRefinement threshRef = new NoThresholdRefinement();		
		
		final PostprocessingStrategy<Record> postprocessor = new PostprocessingStrategy<>();
		postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_ONE, new MaxBothPostprocessor<Record>());
		postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_ONE, new MaxRightPostprocessor<Record>());
		postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_MANY, new NoPostprocessor<Record>());
		postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_MANY, new NoPostprocessor<Record>());

		final Matcher<Record> matcher = new BatchMatcher(blocker, simClass, threshRef, postprocessor);

		final LinkageResult<Record> linkRes = matcher.match(input);

		final TrueMatchChecker trueMatchChecker = new IdEqualityTrueMatchChecker();
		final QualityEvaluator<Record> evaluator = new QualityEvaluator<>(trueMatchChecker);

		final PartyPair partyPairAB = new PartyPair(new Party(namePartyA), new Party(namePartyB));

		final LinkageResultPartition<Record> part = linkRes.getPartition(partyPairAB);
		evaluator.addMatches(part.getMatchStrategy().getMatches());
		
	
		final long truePos = evaluator.getTruePositives();
		final long falsePos = evaluator.getFalsePositives();

		final double recall = QualityMetrics.getRecall(truePos, matches);
		final double precision = QualityMetrics.getPrecision(truePos, truePos + falsePos);
		final double fmeasure = QualityMetrics.getFMeasure(recall, precision);

		System.out.println("Recall: " + recall);
		System.out.println("Precision: " + precision);
		System.out.println("F-Measuer: " + fmeasure);
	}
	
}