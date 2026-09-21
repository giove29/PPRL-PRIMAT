package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.PartyAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures.CenterClusteringConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

class CenterClusteringPostprocessorTest {

	private static Record record(String id, Party party) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setPartyAttribute(new PartyAttribute(party));
		return record;
	}

	/**
	 * Cluster a stella (centro + 2 foglie, nessun arco diretto tra le
	 * foglie): l'output deve comunque essere la clique completa dei 3
	 * membri, non solo i 2 archi centro-foglia realmente presenti nel grafo.
	 */
	@Test
	void fullCliqueOnStarClusterTest() {
		final Party partyA = new Party("A", false);
		final Party partyB = new Party("B", false);
		final Party partyC = new Party("C", false);

		final Record a1 = record("a1", partyA);
		final Record b1 = record("b1", partyB);
		final Record c1 = record("c1", partyC);

		final MultiPartiteSimilarityGraph graph = new MultiPartiteSimilarityGraph();
		graph.addEdge(a1, b1, new SimilarityVector(0.9), 0.9);
		graph.addEdge(a1, c1, new SimilarityVector(0.8), 0.8);

		final CenterClusteringPostprocessor postprocessor = new CenterClusteringPostprocessor(new CenterClusteringConfig());
		final List<LinkedPair<Record>> matches = postprocessor.cluster(graph);

		assertEquals(3, matches.size());
		assertTrue(containsUnorderedPair(matches, a1, b1));
		assertTrue(containsUnorderedPair(matches, a1, c1));
		assertTrue(containsUnorderedPair(matches, b1, c1));
	}

	@Test
	void separateComponentsDoNotMixTest() {
		final Party partyA = new Party("A", false);
		final Party partyB = new Party("B", false);
		final Party partyC = new Party("C", false);

		final Record a1 = record("a1", partyA);
		final Record b1 = record("b1", partyB);
		final Record c1 = record("c1", partyC);
		final Record a2 = record("a2", partyA);
		final Record b2 = record("b2", partyB);

		final MultiPartiteSimilarityGraph graph = new MultiPartiteSimilarityGraph();
		graph.addEdge(a1, b1, new SimilarityVector(0.9), 0.9);
		graph.addEdge(a1, c1, new SimilarityVector(0.9), 0.9);
		graph.addEdge(b1, c1, new SimilarityVector(0.9), 0.9);
		graph.addEdge(a2, b2, new SimilarityVector(0.85), 0.85);

		final CenterClusteringPostprocessor postprocessor = new CenterClusteringPostprocessor(new CenterClusteringConfig());
		final List<LinkedPair<Record>> matches = postprocessor.cluster(graph);

		assertEquals(4, matches.size());
		assertTrue(containsUnorderedPair(matches, a1, b1));
		assertTrue(containsUnorderedPair(matches, a1, c1));
		assertTrue(containsUnorderedPair(matches, b1, c1));
		assertTrue(containsUnorderedPair(matches, a2, b2));
	}

	/**
	 * Stesso grafo, ordine di inserimento degli archi diverso: il risultato
	 * deve essere identico grazie alla chiave di ordinamento stabile su
	 * {@code party#id} (jgrapht non garantisce un ordine di iterazione
	 * stabile di {@code vertexSet()} altrimenti).
	 */
	@Test
	void resultStableAcrossInsertionOrderTest() {
		final Party partyA = new Party("A", false);
		final Party partyB = new Party("B", false);
		final Party partyC = new Party("C", false);

		final Record a1 = record("a1", partyA);
		final Record b1 = record("b1", partyB);
		final Record c1 = record("c1", partyC);

		final MultiPartiteSimilarityGraph graphOne = new MultiPartiteSimilarityGraph();
		graphOne.addEdge(a1, b1, new SimilarityVector(0.9), 0.9);
		graphOne.addEdge(a1, c1, new SimilarityVector(0.8), 0.8);

		final MultiPartiteSimilarityGraph graphTwo = new MultiPartiteSimilarityGraph();
		graphTwo.addEdge(a1, c1, new SimilarityVector(0.8), 0.8);
		graphTwo.addEdge(a1, b1, new SimilarityVector(0.9), 0.9);

		final CenterClusteringPostprocessor postprocessor = new CenterClusteringPostprocessor(new CenterClusteringConfig());
		final Set<String> resultOne = toUnorderedPairKeys(postprocessor.cluster(graphOne));
		final Set<String> resultTwo = toUnorderedPairKeys(postprocessor.cluster(graphTwo));

		assertEquals(resultOne, resultTwo);
	}

	private static Set<String> toUnorderedPairKeys(List<LinkedPair<Record>> matches) {
		final Set<String> keys = new HashSet<>();
		for (final LinkedPair<Record> pair : matches) {
			final String left = pair.getLeftRecord().getId();
			final String right = pair.getRight().getId();
			keys.add(left.compareTo(right) <= 0 ? left + "|" + right : right + "|" + left);
		}
		return keys;
	}

	private static boolean containsUnorderedPair(List<LinkedPair<Record>> matches, Record left, Record right) {
		return matches.stream().anyMatch(pair -> (pair.getLeftRecord() == left && pair.getRight() == right)
				|| (pair.getLeftRecord() == right && pair.getRight() == left));
	}
}
