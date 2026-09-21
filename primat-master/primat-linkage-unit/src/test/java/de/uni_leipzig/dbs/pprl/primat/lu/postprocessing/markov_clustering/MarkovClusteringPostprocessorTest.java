package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.PartyAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

class MarkovClusteringPostprocessorTest {

	private static Record record(String id, Party party) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setPartyAttribute(new PartyAttribute(party));
		return record;
	}

	/**
	 * Grafo multi-partito con 3 party dirty: una componente a triangolo
	 * (a1,b1,c1, tutte alta similarità) e una componente separata a coppia
	 * (a2,b2). {@link MarkovClusteringPostprocessor#cluster} deve produrre
	 * la clique completa di {@link LinkedPair} per ciascuna componente.
	 */
	@Test
	void threeDirtyPartiesTwoComponentsTest() {
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

		final MarkovClusteringPostprocessor postprocessor = new MarkovClusteringPostprocessor(new MclConfig());
		final List<LinkedPair<Record>> matches = postprocessor.cluster(graph);

		assertEquals(4, matches.size());
		assertTrue(containsUnorderedPair(matches, a1, b1));
		assertTrue(containsUnorderedPair(matches, a1, c1));
		assertTrue(containsUnorderedPair(matches, b1, c1));
		assertTrue(containsUnorderedPair(matches, a2, b2));
	}

	private static boolean containsUnorderedPair(List<LinkedPair<Record>> matches, Record left, Record right) {
		return matches.stream().anyMatch(pair -> (pair.getLeftRecord() == left && pair.getRight() == right)
				|| (pair.getLeftRecord() == right && pair.getRight() == left));
	}
}
