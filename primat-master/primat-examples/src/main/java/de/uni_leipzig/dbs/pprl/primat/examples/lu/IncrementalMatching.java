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
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.LshBlockingFunction;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.RandomHammingLshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterBlockingKeyStrategy;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterRepresentantStrategy;
import de.uni_leipzig.dbs.pprl.primat.common.model.LinkageConstraint;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DoubleListAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.ThresholdClassificator;
import de.uni_leipzig.dbs.pprl.primat.lu.database.DbConnection;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.incremental.IncrementalMatcher;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.incremental.RecordBasedIncrementalMatcher;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.NoPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxBothPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxRightPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.attribute_similarity.BitSetAttributeSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.BaseRecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.RecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary.BinarySimilarity;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.BaseSimilarityVectorFlattener;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.FlatSimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorAggregator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVectorFlattener;


/**
 * 
 * @author mfranke
 *
 */
public class IncrementalMatching {

	public static void addLshKeys(Collection<Record> records) throws SQLException {
		final RandomHammingLshKeyGenerator keyGen = new RandomHammingLshKeyGenerator(16, 30, 1024, 42L);
		final List<LshBlockingFunction> bkFuncs = keyGen.generate();

		for (final Record record : records) {
			final BitSetAttribute bsAttr = record.getBitSetAttributes().get(0);

			for (int i = 0; i < bkFuncs.size(); i++) {
				final LshBlockingFunction key = bkFuncs.get(i);
				final String bk = key.apply(bsAttr);
				final BlockingKeyAttribute bkAttr = new BlockingKeyAttribute(i, bk);
				record.addBlockingKey(bkAttr);
			}
		}
	}

	public static void main(String[] args) throws IOException, SQLException {

		final RecordSimilarityCalculator simCalcRecRec = new BaseRecordSimilarityCalculator(
			List.of(new BitSetAttributeSimilarityCalculator(List.of(BinarySimilarity.JACCARD_SIMILARITY))));
		final SimilarityVectorFlattener flattener = new BaseSimilarityVectorFlattener(
			List.of(DoubleListAggregator.FIRST));
		final FlatSimilarityVectorAggregator aggregator = new BaseSimilarityVectorAggregator(
			DoubleListAggregator.FIRST);
		final SimilarityVectorAggregator agg = new SimilarityVectorAggregator(flattener, aggregator);
		final Classificator classifier = new ThresholdClassificator(0.6d, agg);

		final ClusterFactory clusterFactory = new ClusterFactory(ClusterBlockingKeyStrategy.REPRESENTANT,
			ClusterRepresentantStrategy.RETAIN_FIRST);

		final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
				.add(0, NonQidAttributeType.ID)
				.add(1, NonQidAttributeType.GLOBAL_ID)
				.add(2, NonQidAttributeType.PARTY)
				.add(3, QidAttributeType.BITSET, "BS")
				.build();	
		
		final String inputPath = args[0];
		final DatasetReader reader = new DatasetReader(inputPath, rsc);
		final List<Record> dataset = reader.read();
		
		
		addLshKeys(dataset);

		final MatchStrategyFactory<Record> matchFactory1 = new SimilarityGraphMatchStrategyFactory<>();
		final NonMatchStrategyFactory<Record> nonMatchFactory1 = new IgnoreNonMatchesStrategyFactory<>();
		final LinkageResultPartitionFactory<Record> linkResFac1 = new LinkageResultPartitionFactory<>(matchFactory1,
			nonMatchFactory1);

		final PostprocessingStrategy<Record> postprocessor = new PostprocessingStrategy<>();
		postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_ONE, new MaxBothPostprocessor<Record>());
		postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_ONE, new MaxRightPostprocessor<Record>());
		postprocessor.setPostprocessor(LinkageConstraint.ONE_TO_MANY, new NoPostprocessor<Record>());
		postprocessor.setPostprocessor(LinkageConstraint.MANY_TO_MANY, new NoPostprocessor<Record>());

		// Esempio standalone: punta alla PU "PrimatMscdAp" (una delle 3 persistence-unit
		// dedicate alle strategie persistenti, vedi persistence.xml/DbConnection).
		final DbConnection dbConnection = new DbConnection("PrimatMscdAp",
			"jdbc:postgresql://localhost:5432/primat_mscd_ap", "primat", "primat");
		final IncrementalMatcher<Record> incMatcher = new RecordBasedIncrementalMatcher(simCalcRecRec, linkResFac1,
			classifier, postprocessor, clusterFactory, dbConnection);

		final Map<Party, Collection<Record>> data = new HashMap<>();

		final List<Record> datasetA = dataset.stream().filter(r -> r.getParty().getName().equals("A"))
				.collect(Collectors.toList());
		final List<Record> datasetB = dataset.stream().filter(r -> r.getParty().getName().equals("B"))
				.collect(Collectors.toList());

		final Party partyA = new Party("A");
		partyA.setDuplicateFree(false);
		datasetA.get(0).getParty().setDuplicateFree(false);
		data.put(partyA, datasetA);

		final Party partyB = new Party("B");
		data.put(partyB, datasetB);

		incMatcher.match(data);
	}
}