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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;

/**
 *
 */
class BlockingEvaluatorTest {

	private static Record record(String id, String globalId, Party party) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setGlobalIdAttribute(new GlobalIdAttribute(globalId));
		record.setParty(party);
		return record;
	}

	@Test
	void evaluatesImperfectBlocking() {
		final Party partyA = new Party("A");
		final Party partyB = new Party("B");

		// one true match (g1) correctly co-occurring in the block,
		// one true match (g3) missed by blocking (false negative),
		// one non-match (a2/b1, different global ids) co-occurring (false positive candidate).
		final Record a1 = record("a1", "g1", partyA);
		final Record a2 = record("a2", "g2", partyA);
		final Record b1 = record("b1", "g1", partyB);
		final Record b2 = record("b2", "g3", partyB);

		final Block block = new Block();
		block.add(partyA, a1);
		block.add(partyA, a2);
		block.add(partyB, b1);

		final BlockingEvaluator evaluator = new BlockingEvaluator(new IdEqualityTrueMatchChecker());

		// full cross product would be 2 (party A) * 2 (party B) = 4
		final long maxComparisons = 4;
		// gold standard has 2 true matches: (a1,b1)=g1 and (b2 vs. its A-side counterpart)=g3
		final long expectedMatches = 2;

		final BlockingEvaluationResult result = evaluator.evaluate(Arrays.asList(block), maxComparisons,
			expectedMatches);

		// candidate pairs: a1-b1, a2-b1 => 2
		assertEquals(2, result.getCandidatePairs());
		// true matches surviving blocking: only a1-b1 (g1); g3 (b2) never entered a block
		assertEquals(1, result.getTrueMatchesInBlocks());
		// RR = 1 - candidates/max = 1 - 2/4 = 0.5
		assertEquals(0.5, result.getReductionRatio(), 1e-9);
		// PC = trueMatchesInBlocks / expectedMatches = 1/2 = 0.5
		assertEquals(0.5, result.getPairsCompleteness(), 1e-9);
		// PQ = trueMatchesInBlocks / candidatePairs = 1/2 = 0.5
		assertEquals(0.5, result.getPairsQuality(), 1e-9);
	}

	@Test
	void deduplicatesPairsThatCoOccurInMultipleBlocks() {
		final Party partyA = new Party("A");
		final Party partyB = new Party("B");

		// a1/b1 share two blocking keys (e.g. two LSH bands), so they end up
		// co-occurring in two distinct blocks; the pair must still be counted once.
		final Record a1 = record("a1", "g1", partyA);
		final Record b1 = record("b1", "g1", partyB);

		final Block blockOne = new Block();
		blockOne.add(partyA, a1);
		blockOne.add(partyB, b1);

		final Block blockTwo = new Block();
		blockTwo.add(partyA, a1);
		blockTwo.add(partyB, b1);

		final BlockingEvaluator evaluator = new BlockingEvaluator(new IdEqualityTrueMatchChecker());

		final BlockingEvaluationResult result = evaluator.evaluate(Arrays.asList(blockOne, blockTwo), 1, 1);

		assertEquals(1, result.getCandidatePairs());
		assertEquals(1, result.getTrueMatchesInBlocks());
		assertEquals(1.0, result.getPairsCompleteness(), 1e-9);
		assertEquals(1.0, result.getPairsQuality(), 1e-9);
	}

	@Test
	void evaluatesPerfectBlocking() {
		final Party partyA = new Party("A");
		final Party partyB = new Party("B");

		final Record a1 = record("a1", "g1", partyA);
		final Record b1 = record("b1", "g1", partyB);

		final Block block = new Block();
		block.add(partyA, a1);
		block.add(partyB, b1);

		final BlockingEvaluator evaluator = new BlockingEvaluator(new IdEqualityTrueMatchChecker());

		final BlockingEvaluationResult result = evaluator.evaluate(Arrays.asList(block), 1, 1);

		assertEquals(1, result.getCandidatePairs());
		assertEquals(1, result.getTrueMatchesInBlocks());
		assertEquals(0.0, result.getReductionRatio(), 1e-9);
		assertEquals(1.0, result.getPairsCompleteness(), 1e-9);
		assertEquals(1.0, result.getPairsQuality(), 1e-9);
	}

	@Test
	void countsWithinPartyPairsOnlyForDirtyPartiesInAMixedBlock() {
		// party A clean (2 record, mai confrontata con se stessa), party B
		// dirty (2 record, si', within-party ammessa) nello stesso blocco.
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", false);

		final Record a1 = record("a1", "g1", partyA);
		final Record a2 = record("a2", "g2", partyA);
		final Record b1 = record("b1", "g1", partyB);
		final Record b2 = record("b2", "g1", partyB);

		final Block block = new Block();
		block.add(partyA, a1);
		block.add(partyA, a2);
		block.add(partyB, b1);
		block.add(partyB, b2);

		final BlockingEvaluator evaluator = new BlockingEvaluator(new IdEqualityTrueMatchChecker());

		// cross A-B: 2*2=4 (a1-b1, a1-b2, a2-b1, a2-b2), within B: 1 (b1-b2),
		// within A: 0 (mai contata, A e' clean) => 5 candidate pairs totali
		final long maxComparisons = 5;
		// g1 compare 3 volte (a1,b1,b2): 3 coppie vere (a1-b1, a1-b2, b1-b2)
		final long expectedMatches = 3;

		final BlockingEvaluationResult result = evaluator.evaluate(Arrays.asList(block), maxComparisons,
			expectedMatches);

		assertEquals(5, result.getCandidatePairs());
		// veri match tra i candidati: a1-b1 (g1), a1-b2 (g1), b1-b2 (g1, within party B dirty);
		// a2-b1/a2-b2 sono candidati ma non veri match (g2 != g1)
		assertEquals(3, result.getTrueMatchesInBlocks());
		assertEquals(0.0, result.getReductionRatio(), 1e-9);
		assertEquals(1.0, result.getPairsCompleteness(), 1e-9);
		assertEquals(0.6, result.getPairsQuality(), 1e-9);
	}
}
