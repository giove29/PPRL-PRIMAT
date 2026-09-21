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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification;

import java.util.Collection;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.SubBlock;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.Classificator;
import de.uni_leipzig.dbs.pprl.primat.lu.classification.MatchStatus;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.RecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ProgressListener;

/**
 * 
 * @author mfranke
 *
 */
public class BatchSimilarityClassification implements SimilarityClassification {

	private ComparisonStrategy comparisonStrategy;
	private RecordSimilarityCalculator simCalc;
	private Classificator classifier;
	private RedundancyCheckStrategy redundantMatchingStrategy;
	private LinkageResultPartitionFactory<Record> linkResPartFactory;

	private ProgressListener progress = ProgressListener.NOOP;

	private Set<PartyPair> partyPairs;
	private LinkageResult<Record> linkageResult;

	public BatchSimilarityClassification(ComparisonStrategy comparisonStrategy, RecordSimilarityCalculator simCalc,
		Classificator classifier, RedundancyCheckStrategy redundantMatchingStrategy,
		LinkageResultPartitionFactory<Record> linkResPartFactory) {
		this.comparisonStrategy = comparisonStrategy;
		this.simCalc = simCalc;
		this.classifier = classifier;
		this.redundantMatchingStrategy = redundantMatchingStrategy;
		this.linkResPartFactory = linkResPartFactory;
		this.partyPairs = null;
		this.linkageResult = null;
	}

	private void initializeVariables(Set<Party> parties) {
		this.partyPairs = this.comparisonStrategy.getPartyPairs(parties);
		this.linkageResult = new LinkageResult<Record>(partyPairs, this.linkResPartFactory);
	}

	@Override
	public LinkageResult<Record> classifyRecords(Set<Party> parties, Collection<Block> blocks) {
		this.initializeVariables(parties);

		long done = 0;
		final long total = blocks.size();
		progress.update(0, total);
		for (final Block block : blocks) {
			this.getMatchesForBlock(block);
			progress.update(++done, total);
		}

		return this.linkageResult;
	}

	public void setProgressListener(ProgressListener progress) {
		this.progress = progress;
	}

	private void getMatchesForBlock(Block block) {

		for (final PartyPair partyPair : this.partyPairs) {
			final Party partyA = partyPair.getLeftParty();
			final Party partyB = partyPair.getRightParty();

			final SubBlock blockA = block.getPartySubBlock(partyA);
			final SubBlock blockB = block.getPartySubBlock(partyB);

			if (this.isEmpty(blockA, blockB)) {
				continue;
			}
			else {
				this.addMatchesForSubBlock(partyPair, blockA, blockB);
			}
		}
	}

	private boolean isEmpty(SubBlock left, SubBlock right) {
		return this.isEmpty(left) || this.isEmpty(right);
	}

	private boolean isEmpty(SubBlock subBlock) {
		return subBlock == null || subBlock.isEmpty();
	}

	private void addMatchesForSubBlock(PartyPair partyPair, SubBlock left, SubBlock right) {
		// TODO: if party left = party right --> all correct?
		final LinkageResultPartition<Record> linkResultPart = linkageResult.getPartition(partyPair);

		for (final Record recLeft : left.getRecords()) {

			for (final Record recRight : right.getRecords()) {

				if (partyPair.isIdentityPair() && recLeft.equals(recRight)) {
					continue;
				}
				else if (this.redundantMatchingStrategy.isChecked(linkResultPart, recLeft, recRight)) {
					continue;
				}
				else {
					final SimilarityVector similarityVector = simCalc.calculateSimilarity(recLeft, recRight);
					
					// TODO: get single similarity value that was used for
					// threshold classifier
					final MatchStatus matchStatus = classifier.classify(similarityVector);

					// TODO: weight handling
					final LinkedPair<Record> pair = new LinkedPair<Record>(recLeft, recRight, similarityVector,
						similarityVector.getAggregatedValue());
					matchStatus.handle(linkResultPart, pair);
				}
			}
		}
	}
}