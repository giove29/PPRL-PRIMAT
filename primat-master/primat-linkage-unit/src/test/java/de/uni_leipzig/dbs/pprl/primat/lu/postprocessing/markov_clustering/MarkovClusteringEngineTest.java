package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;

class MarkovClusteringEngineTest {

	/**
	 * Due blocchi densi (similarità 0.9 intra-blocco) quasi disconnessi tra
	 * loro (similarità 0.05 cross-blocco): MCL deve separarli in due cluster
	 * coerenti.
	 */
	@Test
	void dumbbellTwoClustersTest() {
		final double intra = 0.9;
		final double cross = 0.05;
		final double[][] sim = {
				{ 0.0, intra, intra, cross, cross, cross },
				{ intra, 0.0, intra, cross, cross, cross },
				{ intra, intra, 0.0, cross, cross, cross },
				{ cross, cross, cross, 0.0, intra, intra },
				{ cross, cross, cross, intra, 0.0, intra },
				{ cross, cross, cross, intra, intra, 0.0 },
		};

		final MclConfig config = new MclConfig();
		final MarkovClusteringEngine engine = new MarkovClusteringEngine(sim, config);
		engine.fit();

		assertTrue(engine.isConverged());
		assertTrue(engine.getIterCount() <= config.getMaxIterations());

		final int[] labels = engine.getLabels();
		assertEquals(labels[0], labels[1]);
		assertEquals(labels[0], labels[2]);
		assertEquals(labels[3], labels[4]);
		assertEquals(labels[3], labels[5]);
		assertNotEquals(labels[0], labels[3]);
	}

	@Test
	void fastPathSingleNodeTest() {
		final MarkovClusteringEngine engine = new MarkovClusteringEngine(new double[][] { { 1.0 } }, new MclConfig());
		engine.fit();

		assertTrue(engine.isConverged());
		assertEquals(0, engine.getIterCount());
		assertArrayEquals(new int[] { 0 }, engine.getLabels());
	}

	@Test
	void fastPathTwoNodesTest() {
		final double[][] sim = { { 0.0, 0.7 }, { 0.7, 0.0 } };
		final MarkovClusteringEngine engine = new MarkovClusteringEngine(sim, new MclConfig());
		engine.fit();

		assertTrue(engine.isConverged());
		assertEquals(0, engine.getIterCount());
		assertArrayEquals(new int[] { 0, 0 }, engine.getLabels());
	}
}
