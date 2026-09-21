package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures.CenterClusteringConfig;

class CenterClusteringEngineTest {

	@Test
	void simplePairTest() {
		final double[][] sim = { { 0.0, 0.9 }, { 0.9, 0.0 } };
		final CenterClusteringEngine engine = new CenterClusteringEngine(sim, new CenterClusteringConfig());
		engine.fit();

		final int[] labels = engine.getLabels();
		assertEquals(labels[0], labels[1]);
	}

	/**
	 * Hub (0) + 3 spoke, nessun arco tra gli spoke: tutti finiscono sotto lo
	 * stesso centro (l'hub, indice più basso).
	 */
	@Test
	void starHubAndThreeSpokesTest() {
		final double[][] sim = {
				{ 0.0, 0.9, 0.8, 0.7 },
				{ 0.9, 0.0, 0.0, 0.0 },
				{ 0.8, 0.0, 0.0, 0.0 },
				{ 0.7, 0.0, 0.0, 0.0 },
		};
		final CenterClusteringEngine engine = new CenterClusteringEngine(sim, new CenterClusteringConfig());
		engine.fit();

		assertArrayEquals(new int[] { 0, 0, 0, 0 }, engine.getLabels());
	}

	@Test
	void twoDisjointEdgesTest() {
		final double[][] sim = {
				{ 0.0, 0.9, 0.0, 0.0 },
				{ 0.9, 0.0, 0.0, 0.0 },
				{ 0.0, 0.0, 0.0, 0.9 },
				{ 0.0, 0.0, 0.9, 0.0 },
		};
		final CenterClusteringEngine engine = new CenterClusteringEngine(sim, new CenterClusteringConfig());
		engine.fit();

		final int[] labels = engine.getLabels();
		assertEquals(labels[0], labels[1]);
		assertEquals(labels[2], labels[3]);
		assertNotEquals(labels[0], labels[2]);
	}

	/**
	 * Catena 0-1-2 senza arco diretto 0-2: il nodo 2 non deve essere
	 * assorbito per transitività nel cluster di 0 — Center Clustering produce
	 * cluster "a stella", non la chiusura transitiva del grafo.
	 */
	@Test
	void chainWithoutTransitiveClosureTest() {
		final double[][] sim = {
				{ 0.0, 0.9, 0.0 },
				{ 0.9, 0.0, 0.9 },
				{ 0.0, 0.9, 0.0 },
		};
		final CenterClusteringEngine engine = new CenterClusteringEngine(sim, new CenterClusteringConfig());
		engine.fit();

		final int[] labels = engine.getLabels();
		assertEquals(labels[0], labels[1]);
		assertEquals(2, labels[2]);
		assertNotEquals(labels[0], labels[2]);
	}

	@Test
	void edgeBelowThresholdIgnoredTest() {
		final double[][] sim = { { 0.0, 0.3 }, { 0.3, 0.0 } };
		final CenterClusteringConfig config = new CenterClusteringConfig();
		config.setCenterAssignmentThreshold(0.5);
		final CenterClusteringEngine engine = new CenterClusteringEngine(sim, config);
		engine.fit();

		assertArrayEquals(new int[] { 0, 1 }, engine.getLabels());
	}

	@Test
	void emptyGraphTest() {
		final CenterClusteringEngine engine = new CenterClusteringEngine(new double[0][0], new CenterClusteringConfig());
		engine.fit();

		assertArrayEquals(new int[0], engine.getLabels());
	}

	@Test
	void deterministicAcrossFreshInstancesTest() {
		final double[][] sim = {
				{ 0.0, 0.9, 0.8, 0.0 },
				{ 0.9, 0.0, 0.0, 0.6 },
				{ 0.8, 0.0, 0.0, 0.0 },
				{ 0.0, 0.6, 0.0, 0.0 },
		};
		final int[] first = runFresh(sim);
		final int[] second = runFresh(sim);

		assertArrayEquals(first, second);
	}

	private static int[] runFresh(double[][] sim) {
		final CenterClusteringEngine engine = new CenterClusteringEngine(sim, new CenterClusteringConfig());
		engine.fit();
		return engine.getLabels();
	}

	/**
	 * Due archi di peso identico verso lo stesso nodo (0-2 e 1-2): il
	 * tie-break canonico su {@code (i,j)} ascendente deve far vincere
	 * deterministicamente l'arco (0,2), rendendo 0 il centro e 1 un
	 * singoletto separato (non assegnato a 0, perché l'arco (0,1) non esiste).
	 */
	@Test
	void tieBreakOnEqualWeightsTest() {
		final double[][] sim = {
				{ 0.0, 0.0, 0.9 },
				{ 0.0, 0.0, 0.9 },
				{ 0.9, 0.9, 0.0 },
		};
		final CenterClusteringEngine engine = new CenterClusteringEngine(sim, new CenterClusteringConfig());
		engine.fit();

		final int[] labels = engine.getLabels();
		assertEquals(0, labels[0]);
		assertEquals(0, labels[2]);
		assertEquals(1, labels[1]);
	}
}
