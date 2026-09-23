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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.SubBlock;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;

/**
 * Evaluates the output of a blocking step (a {@link Collection} of
 * {@link Block}s) against a gold standard, independent of any downstream
 * similarity calculation or classification. Computes the three standard
 * blocking evaluation metrics (Christen, "Data Matching", 2012):
 *
 * <ul>
 * <li>Reduction Ratio (RR): fraction of comparisons avoided by blocking,
 * relative to the full cross product.</li>
 * <li>Pairs Completeness (PC): fraction of the true matches that are still
 * present as candidate pairs after blocking (i.e. recall of blocking).</li>
 * <li>Pairs Quality (PQ): fraction of the candidate pairs that are true
 * matches (i.e. precision of blocking).</li>
 * </ul>
 *
 * A pair of records can co-occur in several blocks (e.g. a record has one
 * blocking key per LSH band, so two similar records may share several
 * bands/blocks). Such a pair is counted only once across the whole
 * {@link #evaluate} call, mirroring the deduplication that the downstream
 * classification step ({@code RedundancyCheckStrategy.DONT_MATCH_TWICE})
 * already performs before computing similarity for a candidate pair.
 *
 */
public final class BlockingEvaluator {

	private final TrueMatchChecker trueMatchChecker;

	public BlockingEvaluator(TrueMatchChecker trueMatchChecker) {
		this.trueMatchChecker = Objects.requireNonNull(trueMatchChecker);
	}

	/**
	 * @param  blocks          the blocks produced by the blocking step to
	 *                         evaluate.
	 * @param  maxComparisons  the size of the full cross product without any
	 *                         blocking, e.g. as computed by
	 *                         {@link PerformanceMetrics#getMaxComparisons}.
	 * @param  expectedMatches the total number of true matches in the gold
	 *                         standard.
	 * @return                 the computed {@link BlockingEvaluationResult}.
	 */
	public BlockingEvaluationResult evaluate(Collection<Block> blocks, long maxComparisons, long expectedMatches) {
		return evaluate(blocks, maxComparisons, expectedMatches, false);
	}

	/**
	 * @param  blocks            the blocks produced by the blocking step to
	 *                           evaluate.
	 * @param  maxComparisons    the size of the full cross product without any
	 *                           blocking, e.g. as computed by
	 *                           {@link PerformanceMetrics#getMaxComparisons}.
	 * @param  expectedMatches   the total number of true matches in the gold
	 *                           standard.
	 * @param  selfPairsAllowed  {@code true} for a single-source deduplication
	 *                           run (exactly one party overall): within-party
	 *                           pairs are then counted too, same rule as
	 *                           {@code MultiSourceLinkage#selfPairsAllowed}
	 *                           (a block can then only ever contain that one
	 *                           party, so this never mixes with cross-party
	 *                           counting). {@code false} preserves the
	 *                           original cross-party-only behaviour.
	 * @return                   the computed {@link BlockingEvaluationResult}.
	 */
	public BlockingEvaluationResult evaluate(Collection<Block> blocks, long maxComparisons, long expectedMatches,
			boolean selfPairsAllowed) {
		long candidatePairs = 0;
		long trueMatchesInBlocks = 0;
		final Set<Map.Entry<Record, Record>> seenPairs = new HashSet<>();

		for (final Block block : blocks) {
			final List<Party> parties = new ArrayList<>(block.getParties());
			Collections.sort(parties);

			for (int i = 0; i < parties.size(); i++) {
				for (int j = i + 1; j < parties.size(); j++) {
					final SubBlock left = block.getPartySubBlock(parties.get(i));
					final SubBlock right = block.getPartySubBlock(parties.get(j));

					for (final Record leftRecord : left.getRecords()) {
						for (final Record rightRecord : right.getRecords()) {

							if (!seenPairs.add(new SimpleImmutableEntry<>(leftRecord, rightRecord))) {
								continue;
							}

							candidatePairs++;

							if (this.trueMatchChecker.isTrueMatch(leftRecord, rightRecord)) {
								trueMatchesInBlocks++;
							}
						}
					}
				}

				if (selfPairsAllowed) {
					final List<Record> records = new ArrayList<>(block.getPartySubBlock(parties.get(i)).getRecords());
					for (int a = 0; a < records.size(); a++) {
						for (int b = a + 1; b < records.size(); b++) {
							final Record leftRecord = records.get(a);
							final Record rightRecord = records.get(b);

							if (!seenPairs.add(new SimpleImmutableEntry<>(leftRecord, rightRecord))) {
								continue;
							}

							candidatePairs++;

							if (this.trueMatchChecker.isTrueMatch(leftRecord, rightRecord)) {
								trueMatchesInBlocks++;
							}
						}
					}
				}
			}
		}

		final double reductionRatio = 1.0d
			- PerformanceMetrics.getReductionRatio(candidatePairs, maxComparisons);
		final double pairsCompleteness = QualityMetrics.getRecall(trueMatchesInBlocks, expectedMatches);
		final double pairsQuality = QualityMetrics.getPrecision(trueMatchesInBlocks, candidatePairs);

		return new BlockingEvaluationResult(candidatePairs, trueMatchesInBlocks, reductionRatio, pairsCompleteness,
			pairsQuality);
	}
}
