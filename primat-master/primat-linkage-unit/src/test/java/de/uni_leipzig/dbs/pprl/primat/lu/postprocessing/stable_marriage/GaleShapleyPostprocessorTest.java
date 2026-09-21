package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.stable_marriage;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

class GaleShapleyPostprocessorTest {

	@Test
	public void cleanTest() {
		GaleShapleyPostprocessor<Record> gsp = new GaleShapleyPostprocessor<>();

		SimilarityGraph<Record> graph = new SimilarityGraph<Record>();

		Record m1 = new Record();
		m1.setIdAttribute(new IdAttribute("m1"));

		Record m2 = new Record();
		m2.setIdAttribute(new IdAttribute("m2"));

		Record w1 = new Record();
		w1.setIdAttribute(new IdAttribute("w1"));

		Record w2 = new Record();
		w2.setIdAttribute(new IdAttribute("w2"));

		// m2 outranks m1 for w1 -> triggers a divorce: m1 gets bumped to w2
		graph.addEdge(m1, w1, new SimilarityVector(0.9d));
		graph.addEdge(m1, w2, new SimilarityVector(0.3d));
		graph.addEdge(m2, w1, new SimilarityVector(0.95d));
		graph.addEdge(m2, w2, new SimilarityVector(0.4d));

		SimilarityGraphMatchStrategy<Record> matchStrategy = new SimilarityGraphMatchStrategy<>();
		matchStrategy.setSimilarityGraph(graph);

		gsp.clean(matchStrategy);

		assertEquals(2, graph.edges());

		assertTrue(graph.containsEdge(m2, w1));
		assertTrue(graph.containsEdge(m1, w2));

		assertFalse(graph.containsEdge(m1, w1));
		assertFalse(graph.containsEdge(m2, w2));
	}

}
