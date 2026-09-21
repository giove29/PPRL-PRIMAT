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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.database.DbConnection;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.Matcher;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;


/**
 * 
 * @author mfranke
 *
 */
public abstract class IncrementalMatcher<T extends Linkable> implements Matcher<Cluster> {

	protected final LinkageResultPartitionFactory<T> linkResFac;
	protected final Classificator classifier;
	protected final PostprocessingStrategy<T> postprocessor;
	protected final ClusterFactory clusterFactory;
	protected final DbConnection dbConnection;

	public IncrementalMatcher(LinkageResultPartitionFactory<T> linkResFac, Classificator classifier,
		PostprocessingStrategy<T> postprocessor, ClusterFactory clusterFactory, DbConnection dbConnection) {
		this.linkResFac = linkResFac;
		this.classifier = classifier;
		this.postprocessor = postprocessor;
		this.clusterFactory = clusterFactory;
		this.dbConnection = dbConnection;
	}

	@Override
	public LinkageResult<Cluster> match(Map<Party, Collection<Record>> input) {

		dbConnection.addParties(input.keySet());

		final List<Record> inputRecords = new ArrayList<>();

		for (final Collection<Record> dataset : input.values()) {
			inputRecords.addAll(dataset);
		}

		final Map<String, Record> newRecords = inputRecords.stream()
			.collect(Collectors.toMap(Record::getId, Function.identity()));

		dbConnection.addBlockingKeysStaging(newRecords.values());

		final List<LinkedPair<Cluster>> matches = this.getMatches(newRecords);

		/*
		 * Cluster Selection Strategies: (A1) Select cluster where no record of
		 * the same source is mapped (A2) Never mind if there is a record of the
		 * same source
		 * 
		 * (B1) Select cluster with highest similarity to cluster representant
		 * (B2) Select cluster with highest average similarity to cluster
		 * members (B3) Select cluster with largest number of links above
		 * threshold between cluster members
		 */

		final Map<String, Record> mappedRecords = new HashMap<String, Record>();

		for (final LinkedPair<Cluster> pair : matches) {
			final Record rec = pair.getLeftRecord();
			final String id = rec.getId();

			mappedRecords.put(id, rec);
		}

		/*
		 * Cluster Representant Selection Strategies: * (B) Build cluster
		 * representant by merging cluster members (B1) OR (B2) AND (B3)
		 * Counting Bloom Filter
		 */
		dbConnection.updateAffectedCluster(matches);

		final Map<String, Record> unmappedRecords = new HashMap<>(newRecords);
		unmappedRecords.keySet().removeAll(mappedRecords.keySet());

		dbConnection.insertSingletonsNewCluster(clusterFactory, unmappedRecords);

		return null;
	}

	protected abstract List<LinkedPair<Cluster>> getMatches(Map<String, Record> newRecords);

}