package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

class MaxBothPostprocessorTest {
		
	@Test
	public void cleanTest() {
		MaxBothPostprocessor<Record> mbp = new MaxBothPostprocessor<>();	
		
		SimilarityGraph<Record> graph = new SimilarityGraph<Record>();
		
		Record a1 = new Record();
		a1.setIdAttribute(new IdAttribute("a1"));
		
		Record a2 = new Record();
		a2.setIdAttribute(new IdAttribute("a2"));
		
		Record a3 = new Record();
		a3.setIdAttribute(new IdAttribute("a3"));
		
		Record a4 = new Record();
		a4.setIdAttribute(new IdAttribute("a4"));
		
		Record a5 = new Record();
		a5.setIdAttribute(new IdAttribute("a5"));
		
		Record a6 = new Record();
		a6.setIdAttribute(new IdAttribute("a6"));
		
		Record b1 = new Record();
		b1.setIdAttribute(new IdAttribute("b1"));
		
		Record b2 = new Record();
		b2.setIdAttribute(new IdAttribute("b2"));
		
		Record b3 = new Record();
		b3.setIdAttribute(new IdAttribute("b3"));
		
		Record b4 = new Record();
		b4.setIdAttribute(new IdAttribute("b4"));
		
		Record b5 = new Record();
		b5.setIdAttribute(new IdAttribute("b5"));
		
		Record b6 = new Record();
		b6.setIdAttribute(new IdAttribute("b6"));
		
		Record b7 = new Record();
		b7.setIdAttribute(new IdAttribute("b7"));
		
		graph.addEdge(a1, b1, new SimilarityVector(1d));
		graph.addEdge(a2, b2, new SimilarityVector(0.8d));
		graph.addEdge(a2, b3, new SimilarityVector(0.9d));
		graph.addEdge(a3, b4, new SimilarityVector(0.85d));
		graph.addEdge(a3, b5, new SimilarityVector(0.9d));
		graph.addEdge(a4, b5, new SimilarityVector(0.8d));
		graph.addEdge(a4, b6, new SimilarityVector(0.7d));
		graph.addEdge(a5, b7, new SimilarityVector(0.75d));
		graph.addEdge(a6, b7, new SimilarityVector(0.7d));
		
		mbp.clean(graph);
		
		assertEquals(4, graph.edges());
		
		assertTrue(graph.containsEdge(a1, b1));
		assertTrue(graph.containsEdge(a2, b3));
		assertTrue(graph.containsEdge(a3, b5));
		assertTrue(graph.containsEdge(a5, b7));
		
		assertFalse(graph.containsEdge(a2, b2));
		assertFalse(graph.containsEdge(a3, b4));
		assertFalse(graph.containsEdge(a4, b5));
		assertFalse(graph.containsEdge(a4, b6));
		assertFalse(graph.containsEdge(a6, b7));
		
	}

}
