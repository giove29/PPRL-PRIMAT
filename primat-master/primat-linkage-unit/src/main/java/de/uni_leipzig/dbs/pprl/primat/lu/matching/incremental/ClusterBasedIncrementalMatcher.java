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
package de.uni_leipzig.dbs.pprl.primat.lu.matching.incremental;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.Pair;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.MatchStatus;
import de.uni_leipzig.dbs.pprl.primat.lu.database.DbConnection;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.IncrementalLinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_cluster_similarity.RecordClusterSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class ClusterBasedIncrementalMatcher extends IncrementalMatcher<Cluster> {

	private final RecordClusterSimilarityCalculator simCalc;

	public ClusterBasedIncrementalMatcher(RecordClusterSimilarityCalculator simCalc,
		LinkageResultPartitionFactory<Cluster> linkResFac, Classificator classifier,
		PostprocessingStrategy<Cluster> postprocessor, ClusterFactory clusterFactory, DbConnection dbConnection) {
		super(linkResFac, classifier, postprocessor, clusterFactory, dbConnection);
		this.simCalc = simCalc;
	}

	@Override
	protected List<LinkedPair<Cluster>> getMatches(Map<String, Record> newRecords) {
		final List<Pair<Record, Cluster>> candidates = dbConnection.getCandidates(newRecords);

		for (final Pair<Record, Cluster> cand : candidates) {
			System.out.println("Candidate: " + cand);
		}

		final Set<Party> parties = new HashSet<>(dbConnection.getParties());

		final IncrementalLinkageResult<Cluster> linkRes = new IncrementalLinkageResult<>(parties, this.linkResFac);

		for (final Pair<Record, Cluster> candPair : candidates) {
			final Record left = candPair.getLeft();
			final Cluster cluster = candPair.getRight();

			final SimilarityVector simVec = this.simCalc.calculateSimilarity(left, cluster);
			final MatchStatus matchStatus = this.classifier.classify(simVec);

			final LinkedPair<Cluster> pair = new LinkedPair<>(left, cluster, simVec, simVec.getAggregatedValue());
			final LinkageResultPartition<Cluster> partition = linkRes.getPartition(left.getParty());
			matchStatus.handle(partition, pair);
		}

		for (final LinkageResultPartition<Cluster> partition : linkRes.getPartitionMap().values()) {
			postprocessor.clean(partition);
		}

		return linkRes.getMatches();
	}

}
