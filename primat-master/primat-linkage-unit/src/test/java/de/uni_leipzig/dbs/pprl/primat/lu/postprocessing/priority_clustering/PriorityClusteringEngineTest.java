package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.priority_clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PriorityClusteringEngineTest {

	@Test
	void collectEdgesOrdersByRawSimilarityDescendingWithTieBreak() {
		final double[][] sim = {
				{ 0.0, 0.7, 0.9 },
				{ 0.7, 0.0, 0.7 },
				{ 0.9, 0.7, 0.0 },
		};
		final int[] partyIds = { 0, 1, 2 };
		final PriorityClusteringEngine engine = new PriorityClusteringEngine(sim, partyIds, 3,
				EdgePriorityFunction.RAW_SIMILARITY, null);

		final List<int[]> edges = engine.collectEdges();

		assertEquals(3, edges.size());
		assertEquals(0, edges.get(0)[0]);
		assertEquals(2, edges.get(0)[1]); // similarità 0.9, priorità massima
		assertEquals(0, edges.get(1)[0]);
		assertEquals(1, edges.get(1)[1]); // similarità 0.7, tie-break (0,1) prima di (1,2)
		assertEquals(1, edges.get(2)[0]);
		assertEquals(2, edges.get(2)[1]);
	}

	@Test
	void mergesTwoNodesOfDifferentParties() {
		final double[][] sim = { { 0.0, 0.9 }, { 0.9, 0.0 } };
		final int[] partyIds = { 0, 1 };
		final PriorityClusteringEngine engine = new PriorityClusteringEngine(sim, partyIds, 2,
				EdgePriorityFunction.RAW_SIMILARITY, null);
		engine.fit();

		assertEquals(engine.getLabels()[0], engine.getLabels()[1]);
	}

	/**
	 * Catena 0(party0)-1(party1)-2(party0), archi (0,1) e (1,2) ma nessun
	 * (0,2): il merge di 1 e 2 va scartato perché unirebbe transitivamente due
	 * record della stessa party (0), anche se non esiste un arco diretto tra
	 * 0 e 2.
	 */
	@Test
	void rejectsTransitiveSourceConsistencyViolationThroughThirdParty() {
		final double[][] sim = {
				{ 0.0, 0.95, 0.0 },
				{ 0.95, 0.0, 0.9 },
				{ 0.0, 0.9, 0.0 },
		};
		final int[] partyIds = { 0, 1, 0 };
		final PriorityClusteringEngine engine = new PriorityClusteringEngine(sim, partyIds, 2,
				EdgePriorityFunction.RAW_SIMILARITY, null);
		engine.fit();

		final int[] labels = engine.getLabels();
		assertEquals(labels[0], labels[1]);
		assertNotEquals(labels[0], labels[2]);
	}

	@Test
	void rejectsDirectEdgeBetweenSameParty() {
		final double[][] sim = { { 0.0, 0.9 }, { 0.9, 0.0 } };
		final int[] partyIds = { 0, 0 };
		final PriorityClusteringEngine engine = new PriorityClusteringEngine(sim, partyIds, 1,
				EdgePriorityFunction.RAW_SIMILARITY, null);
		engine.fit();

		assertNotEquals(engine.getLabels()[0], engine.getLabels()[1]);
	}

	@Test
	void mergeThresholdFiltersEdgesBelowIt() {
		final double[][] sim = { { 0.0, 0.3 }, { 0.3, 0.0 } };
		final int[] partyIds = { 0, 1 };
		final PriorityClusteringEngine engine = new PriorityClusteringEngine(sim, partyIds, 2,
				EdgePriorityFunction.RAW_SIMILARITY, 0.5);
		engine.fit();

		assertNotEquals(engine.getLabels()[0], engine.getLabels()[1]);
	}

	@Test
	void negativeInfinityPriorityExcludesEdgeRegardlessOfSimilarity() {
		final double[][] sim = { { 0.0, 0.99 }, { 0.99, 0.0 } };
		final int[] partyIds = { 0, 1 };
		final EdgePriorityFunction alwaysExcluded = (matrix, rowMax, degree, i, j) -> Double.NEGATIVE_INFINITY;
		final PriorityClusteringEngine engine = new PriorityClusteringEngine(sim, partyIds, 2, alwaysExcluded, null);

		assertTrue(engine.collectEdges().isEmpty());
		engine.fit();
		assertNotEquals(engine.getLabels()[0], engine.getLabels()[1]);
	}
}
