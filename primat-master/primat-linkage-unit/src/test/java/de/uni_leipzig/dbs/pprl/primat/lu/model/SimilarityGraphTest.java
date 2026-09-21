package de.uni_leipzig.dbs.pprl.primat.lu.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;

import org.jgrapht.Graphs;
import org.jgrapht.graph.ParanoidGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

class SimilarityGraphTest {

	@Test
	public void sortedEdgesTest() {
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
		
		List<SimilarityVector> sortedEdges = graph.sortedEdgesOf(a4);
		assertEquals(2, sortedEdges.size());
		assertEquals(0.8d, sortedEdges.get(0).getAggregatedValue().doubleValue());
		assertEquals(0.7d, sortedEdges.get(1).getAggregatedValue().doubleValue());
		
	}
	
	
	@Test
	public void testSimFilteredSubgraph() {
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
		
		SimilarityGraph<Record> subgraph = graph.getSimilarityFilteredSubgraph(0.8d);
		
		assertEquals(6, subgraph.edges());
		assertEquals(9, graph.edges());
		assertEquals(13, graph.vertices());
		assertEquals(13, subgraph.vertices());
		
		SimilarityGraph<Record> subgraph2 = graph.getSimilarityFilteredSubgraph(0.7d);
		assertEquals(13, subgraph2.vertices());
		assertEquals(9, subgraph2.edges());
		
	}
	
	
	@Test
	public void graphScalingTest() {
//		SimpleWeightedGraph<Record,SimilarityVector> graph = 
//			new SimpleWeightedGraph<>(SimilarityVector.class);
		
		SimilarityGraph<Record> graph 
		= new SimilarityGraph<Record>();
		
		Record[] recordsA = new Record[1_000_000];
		
		for (int i = 0; i < recordsA.length; i++) {
			Record r = new Record();
			r.setIdAttribute(new IdAttribute("a" + i));
			recordsA[i] = r;
		}
		
		Record[] recordsB = new Record[800_000];
		
		for (int i = 0; i < recordsB.length; i++) {
			Record r = new Record();
			r.setIdAttribute(new IdAttribute("b" + i));
			recordsB[i] = r;
		}
		
		Random rnd = new Random(42L);
		for (int i = 0; i < 15_000_000; i++) {
			SimilarityVector simVec = new SimilarityVector(rnd.nextDouble());
			
			Record recA;
			Record recB;
			
			do {
				int a = rnd.nextInt(recordsA.length);
				int b = rnd.nextInt(recordsB.length);
				
				recA = recordsA[a];
				recB = recordsB[b];
			}
			while (graph.containsEdge(recA, recB));

			graph.addEdge(recA, recB, simVec, simVec.getAggregatedValue());
		}
		
		assertEquals(15_000_000  , graph.edges());
	
	}
	
	@Test
	public void graphTest() {
		SimpleWeightedGraph<Linkable,SimilarityVector> graph = new SimpleWeightedGraph<Linkable,SimilarityVector>(SimilarityVector.class);
		ParanoidGraph<Linkable, SimilarityVector> paranoidGraph = new ParanoidGraph<Linkable, SimilarityVector>(graph);
		
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
		
		paranoidGraph.addVertex(a1);
		paranoidGraph.addVertex(a2);
		paranoidGraph.addVertex(a3);
		paranoidGraph.addVertex(a4);
		paranoidGraph.addVertex(a5);
		paranoidGraph.addVertex(a6);
		paranoidGraph.addVertex(b1);
		paranoidGraph.addVertex(b2);
		paranoidGraph.addVertex(b3);
		paranoidGraph.addVertex(b4);
		paranoidGraph.addVertex(b4);
		paranoidGraph.addVertex(b5);
		paranoidGraph.addVertex(b6);
		paranoidGraph.addVertex(b7);
		
		paranoidGraph.addEdge(a1, b1, new SimilarityVector(1d));
		paranoidGraph.addEdge(a1, b1, new SimilarityVector(1d));
		paranoidGraph.addEdge(a1, b1, new SimilarityVector(1d));
		paranoidGraph.addEdge(a1, b7, new SimilarityVector(1d));
		
		paranoidGraph.addEdge(a2, b2, new SimilarityVector(0.8d));
		paranoidGraph.addEdge(a2, b3, new SimilarityVector(0.9d));
		paranoidGraph.addEdge(a3, b4, new SimilarityVector(0.85d));
		paranoidGraph.addEdge(a3, b5, new SimilarityVector(0.9d));
		paranoidGraph.addEdge(a4, b5, new SimilarityVector(0.8d));
		paranoidGraph.addEdge(a4, b6, new SimilarityVector(0.7d));
		paranoidGraph.addEdge(a5, b7, new SimilarityVector(0.75d));
		paranoidGraph.addEdge(a6, b7, new SimilarityVector(0.7d));	
		
		assertTrue(paranoidGraph.edgeSet().size() == 10);
	}
	
	@Test
	public void cloneTest() {
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
		
		SimilarityGraph<Record> clone1 = graph.clone();
		
		assertTrue(clone1.getGraph().equals(graph.getGraph()));
		assertTrue(clone1.getSource().equals(graph.getSource()));
		assertTrue(clone1.getTarget().equals(graph.getTarget()));
		assertTrue(clone1.edgeSet().equals(graph.edgeSet()));
		assertFalse(clone1 == graph);
		
		SimilarityGraph<Record> clone2 = graph.clone();		
		
		clone2.removeEdge(a2, b3);
		clone2.removeEdge(a3,a5);
		clone2.addEdge(a1, b7, new SimilarityVector(0.2d));
		clone2.addVertexToTarget(new Record());
		
		
		assertTrue(clone1.getGraph().equals(graph.getGraph()));
		assertTrue(clone1.getSource().equals(graph.getSource()));
		assertTrue(clone1.getTarget().equals(graph.getTarget()));
		assertTrue(clone1.edgeSet().equals(graph.edgeSet()));
		assertFalse(clone1 == graph);
		
		assertFalse(clone2.getGraph().equals(graph.getGraph()));
		assertTrue(clone2.getSource().equals(graph.getSource()));
		assertFalse(clone2.getTarget().equals(graph.getTarget()));
		assertFalse(clone2.edgeSet().equals(graph.edgeSet()));
		assertFalse(clone2 == graph);
	}
}