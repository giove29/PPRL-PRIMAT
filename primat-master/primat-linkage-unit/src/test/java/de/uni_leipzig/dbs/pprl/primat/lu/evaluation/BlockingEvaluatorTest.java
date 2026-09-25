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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

	private static List<String> iterationOrder(Block block, Party party) {
		return new ArrayList<>(block.getPartySubBlock(party).getRecords()).stream().map(Record::getId)
			.collect(Collectors.toList());
	}

	/** Blocco con la party dirty {@code party} che contiene x, y e {@code fillers} record distinti. */
	private static Block blockWith(Party party, Record x, Record y, List<Record> fillers) {
		final Block block = new Block();
		block.add(party, x);
		block.add(party, y);
		fillers.forEach(f -> block.add(party, f));
		return block;
	}

	/**
	 * {@code SubBlock} usa un {@code HashSet}: con una capacita' diversa (blocco
	 * piccolo vs. blocco che ha causato un resize) lo stesso paio di record puo'
	 * essere iterato in ordine opposto. Cerca un paio (x, y) per cui succede,
	 * cosi' il test riproduce davvero il caso "(a,b) in un blocco, (b,a) in un altro".
	 */
	private static Record[] findPairWithFlippingOrder(Party party, List<Record> fillers) {
		for (int i = 0; i < 300; i++) {
			for (int j = i + 1; j < 300; j++) {
				final Record x = record("x" + i, "gx" + i, party);
				final Record y = record("x" + j, "gx" + i, party);
				final Block small = blockWith(party, x, y, List.of());
				final Block big = blockWith(party, x, y, fillers);
				final List<String> smallOrder = iterationOrder(small, party);
				final List<String> bigOrder = iterationOrder(big, party);
				if (smallOrder.indexOf(x.getId()) < smallOrder.indexOf(y.getId()) != (bigOrder.indexOf(x.getId()) < bigOrder
					.indexOf(y.getId()))) {
					return new Record[] { x, y };
				}
			}
		}
		return null;
	}

	@Test
	void withinPartyPairSeenInOppositeOrderInAnotherBlockIsCountedOnce() {
		final Party partyB = new Party("B", false);
		final List<Record> fillers = new ArrayList<>();
		for (int k = 0; k < 40; k++) {
			fillers.add(record("f" + k, "gf" + k, partyB));
		}
		final Record[] pair = findPairWithFlippingOrder(partyB, fillers);
		assertNotNull(pair, "nessun paio con ordine di iterazione instabile trovato");

		final Block small = blockWith(partyB, pair[0], pair[1], List.of());
		final Block big = blockWith(partyB, pair[0], pair[1], fillers);
		assertNotEquals(iterationOrder(small, partyB).indexOf(pair[0].getId()) < iterationOrder(small, partyB)
			.indexOf(pair[1].getId()),
			iterationOrder(big, partyB).indexOf(pair[0].getId()) < iterationOrder(big, partyB).indexOf(pair[1].getId()));

		final int n = fillers.size() + 2;
		final long distinctPairs = (long) n * (n - 1) / 2;

		final BlockingEvaluationResult result = new BlockingEvaluator(new IdEqualityTrueMatchChecker())
			.evaluate(Arrays.asList(small, big, small), distinctPairs, 1);

		// tutte le coppie distinte del blocco grande, la coppia (x,y) contata una sola volta
		assertEquals(distinctPairs, result.getCandidatePairs());
		assertEquals(1, result.getTrueMatchesInBlocks());
		assertEquals(1.0, result.getPairsCompleteness(), 1e-9);
	}

	@Test
	void mixedCleanAndDirtyPartiesAreNotDoubleCountedAcrossBlocksWithDifferentOrder() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", false);
		final List<Record> fillers = new ArrayList<>();
		for (int k = 0; k < 40; k++) {
			fillers.add(record("f" + k, "gf" + k, partyB));
		}
		final Record[] pair = findPairWithFlippingOrder(partyB, fillers);
		assertNotNull(pair, "nessun paio con ordine di iterazione instabile trovato");
		final Record a1 = record("a1", "gx0", partyA);

		final Block small = blockWith(partyB, pair[0], pair[1], List.of());
		small.add(partyA, a1);
		final Block big = blockWith(partyB, pair[0], pair[1], fillers);
		big.add(partyA, a1);

		final int n = fillers.size() + 2;
		// within B: C(n,2); cross A-B: n coppie (a1 con ogni record di B), nessuna within A (A e' clean)
		final long distinctPairs = (long) n * (n - 1) / 2 + n;

		final BlockingEvaluationResult result = new BlockingEvaluator(new IdEqualityTrueMatchChecker())
			.evaluate(Arrays.asList(small, big), distinctPairs, 3);

		assertEquals(distinctPairs, result.getCandidatePairs());
		// veri match: x-y (within B), a1-x, a1-y (cross), tutti con GLOBAL_ID gx0
		assertEquals(3, result.getTrueMatchesInBlocks());
		assertEquals(1.0, result.getPairsCompleteness(), 1e-9);
	}
}
