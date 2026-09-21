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
package de.uni_leipzig.dbs.pprl.primat.lu.model;

import java.io.*;
import java.util.*;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.SetUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class SimilarityGraph<T extends Linkable> implements Serializable {

	private SimpleWeightedGraph<Linkable, SimilarityVector> graph;
	private Set<Record> source;
	private Set<T> target;

	public SimilarityGraph() {
		this.graph = new SimpleWeightedGraph<>(SimilarityVector.class);
		this.source = new HashSet<>();
		this.target = new HashSet<>();
	}

	public boolean addVertexToSource(Record record) {
		this.source.add(record);
		return this.graph.addVertex(record);
	}

	public boolean addVertexToTarget(T record) {
		this.target.add(record);
		return this.graph.addVertex(record);
	}

	public boolean addEdge(Record sourceVertex, T targetVertex, SimilarityVector edge) {
		return this.addEdge(sourceVertex, targetVertex, edge, 1d);
	}

	public boolean addEdge(Record sourceVertex, T targetVertex, SimilarityVector edge, double weight) {
		/*
		 * if (this.graph.containsEdge(sourceVertex, targetVertex)) { return
		 * false; } else {
		 */
//		System.out.println(sourceVertex + " --> " + targetVertex + " : " + weight);
		if(!(source.contains(sourceVertex) && target.contains(sourceVertex))) {
			this.addVertexToSource(sourceVertex);
		}
		if (!(target.contains(targetVertex) && source.contains(targetVertex))) {
			this.addVertexToTarget(targetVertex);
		}
			
		
		final boolean insert = this.graph.addEdge(sourceVertex, targetVertex, edge);

		if (insert) {
			this.graph.setEdgeWeight(edge, weight);
		}
		return insert;
		// }
	}

	public double getEdgeWeight(SimilarityVector edge) {
		return this.graph.getEdgeWeight(edge);
	}

	public Set<Record> getSource() {
		return this.source;
	}

	public Set<T> getTarget() {
		return this.target;
	}

	public boolean removeEdge(SimilarityVector edge) {
		return this.graph.removeEdge(edge);
	}

	public SimilarityVector removeEdge(Record sourceVertex, Record targetVertex) {
		return this.graph.removeEdge(sourceVertex, targetVertex);
	}

	public boolean removeAllEdgesOf(Linkable record) {
		final Set<SimilarityVector> edges = new HashSet<>(this.edgesOf(record));
		return this.graph.removeAllEdges(edges);
	}

	public void retainEdgeOf(Linkable record, SimilarityVector edgeToRetain) {
		final Set<SimilarityVector> edges = new HashSet<>(this.edgesOf(record));

		for (final SimilarityVector edge : edges) {

			if (edge.equals(edgeToRetain)) {
				continue;
			}
			else {
				this.graph.removeEdge(edge);
			}
		}
	}

	public Set<SimilarityVector> getAllEdges(Record source, T target) {
		return this.graph.getAllEdges(source, target);
	}

	public void removeAllEdges() {
		this.graph.removeAllEdges(new ArrayList<>(this.edgeSet()));
	}

	public void retainAllEdges(Collection<? extends SimilarityVector> edges) {
		final Set<SimilarityVector> edgesToRemove = new HashSet<>(graph.edgeSet());
		edgesToRemove.removeAll(edges);
		graph.removeAllEdges(edgesToRemove);
	}
	
	public boolean removeAllEdges(Collection<? extends SimilarityVector> edges) {
		return this.graph.removeAllEdges(edges);
	}

	public void removeUnmappedVertices() {
		final Set<Record> sourceVertices = new HashSet<>(this.source);
		for (final Record sourceVertex : sourceVertices) {
			if (this.graph.edgesOf(sourceVertex).isEmpty()) {
				this.removeVertexFromSource(sourceVertex);
			}
		}
		
		final Set<T> targetVertices = new HashSet<>(this.target);
		for (final T targetVertex : targetVertices) {
			if (this.graph.edgesOf(targetVertex).isEmpty()) {
				this.removeVertexFromTarget(targetVertex);
			}
		}
	}
	
	public void removeVerticesFromTarget(Set<T> vertices) {
		for (final T vertex : vertices) {
			this.removeVertexFromTarget(vertex);
		}
	}
	
	public void removeVerticesFromSource(Set<Record> vertices) {
		for (final Record vertex : vertices) {
			this.removeVertexFromSource(vertex);
		}
	}
		
	public void removeVertexFromSource(Record vertex) {
		this.source.remove(vertex);
		this.graph.removeVertex(vertex);
	}

	public void removeVertexFromTarget(T vertex) {
		this.target.remove(vertex);
		this.graph.removeVertex(vertex);
	}

	public AsUnmodifiableGraph<Linkable, SimilarityVector> getGraph() {
		return new AsUnmodifiableGraph<Linkable, SimilarityVector>(this.graph);
	}

	public Set<SimilarityVector> edgesOf(Linkable record) {
		return this.graph.edgesOf(record);
	}

	public List<SimilarityVector> sortedEdgesOf(Linkable record) {
		final List<SimilarityVector> edges = new ArrayList<>(this.edgesOf(record));
		Collections.sort(edges, Collections.reverseOrder());
		return edges;
	}

	public SimilarityVector maxEdgeOf(Linkable record) {
		final Set<SimilarityVector> edges = this.edgesOf(record);
		return Collections.max(edges);
	}

	/**
	 * Returns the degree of the specified vertex.
	 * 
	 * The degree of a vertex is the number of edges touching that vertex.
	 * 
	 * @param
	 * @return
	 */
	public int getVertexDegree(Linkable vertex) {
		return this.graph.degreeOf(vertex);
	}

	/**
	 * Distance between best match and next possible match, e.g. 1 - 0.8 = 0.2.
	 * A higher distance implies that the candidate match is certain.
	 * 
	 * @param  record
	 * @return
	 */
	public double getSelectivity(Record record) {
		final List<SimilarityVector> sortedEdges = this.sortedEdgesOf(record);

		if (sortedEdges.size() == 0) {
			return 0;
		}
		else if (sortedEdges.size() == 1) {
			return this.getEdgeWeight(sortedEdges.get(0));
		}
		else {
			return this.getEdgeWeight(sortedEdges.get(0)) - this.getEdgeWeight(sortedEdges.get(1));
		}
	}

	/**
	 * Returns the degree of the specified edge.
	 * 
	 * The degree of an edge is the minimum vertex degree of both vertex
	 * endpoints.
	 * 
	 * @param  edge
	 * @return
	 */
	public int getEdgeDegree(SimilarityVector edge) {
		final Linkable source = this.graph.getEdgeSource(edge);
		final Linkable target = this.graph.getEdgeTarget(edge);
		final int sourceDegree = this.getVertexDegree(source);
		final int targetDegree = this.getVertexDegree(target);
		return Math.min(sourceDegree, targetDegree);
	}

	/**
	 * Returns the grade of the specified edge.
	 * 
	 * The grade of an edge is the maximum vertex degree of both vertex
	 * endpoints.
	 * 
	 * @param  edge
	 * @return
	 */
	public int getEdgeGrade(SimilarityVector edge) {
		final Linkable source = this.graph.getEdgeSource(edge);
		final Linkable target = this.graph.getEdgeTarget(edge);
		final int sourceDegree = this.getVertexDegree(source);
		final int targetDegree = this.getVertexDegree(target);
		return Math.max(sourceDegree, targetDegree);
	}

	public boolean containsSourceVertex(Record vertex) {
		return this.source.contains(vertex);
	}

	public boolean containsTargetVertex(T vertex) {
		return this.target.contains(vertex);
	}

	public boolean containsVertex(Record vertex) {
		return this.graph.containsVertex(vertex);
	}

	public boolean containsEdge(Record sourceVertex, T targetVertex) {
		return this.graph.containsEdge(sourceVertex, targetVertex);
	}

	public boolean containsEdge(SimilarityVector edge) {
		return this.graph.containsEdge(edge);
	}

	public Record getEdgeSource(SimilarityVector edge) {
		final Linkable source = this.graph.getEdgeSource(edge);
		return (Record) source;
	}

	@SuppressWarnings("unchecked")
	public T getEdgeTarget(SimilarityVector edge) {
		final Linkable target = this.graph.getEdgeTarget(edge);
		return (T) target;
	}

	public int edges() {
		return this.graph.edgeSet().size();
	}

	public int links() {
		return this.edges();
	}

	public int vertices() {
		return this.graph.vertexSet().size();
	}

	public int sourceVertices() {
		return this.source.size();
	}

	public int mappedSourceVertices() {
		return (int) this.source.stream().filter(x -> ! this.graph.edgesOf(x).isEmpty()).count();
	}

	public int mappedTargetVertices() {
		return (int) this.target.stream().filter(x -> ! this.graph.edgesOf(x).isEmpty()).count();
	}
	
	public int unmappedSourceVertices() {
		return (int) this.source.stream().filter(x -> this.graph.edgesOf(x).isEmpty()).count();
	}

	public int unmappedTargetVertices() {
		return (int) this.target.stream().filter(x -> this.graph.edgesOf(x).isEmpty()).count();
	}

	public int targetVertices() {
		return this.target.size();
	}

	public Set<SimilarityVector> edgeSet() {
		return this.graph.edgeSet();
	}

	public Set<Linkable> vertexSet() {
		return this.graph.vertexSet();
	}

	@SuppressWarnings("unchecked")
	@Override
	public SimilarityGraph<T> clone() {
		final SimilarityGraph<T> clone = new SimilarityGraph<T>();

		clone.source = new HashSet<>(this.source);
		clone.target = new HashSet<>(this.target);
		clone.graph = (SimpleWeightedGraph<Linkable, SimilarityVector>) this.graph.clone();

		return clone;
	}

	public Map<Linkable, Double> getPageRankScores() {
		final PageRank<Linkable, SimilarityVector> pageRank = new PageRank<>(this.graph);
		return pageRank.getScores();
	}

	public Map<Linkable, Double> getBetweennessCentralityScores() {
		final BetweennessCentrality<Linkable, SimilarityVector> betweennessCentrality = new BetweennessCentrality<>(
			this.graph);
		return betweennessCentrality.getScores();
	}

	public Map<Linkable, Double> getClosenessCentralityScores() {
		final ClosenessCentrality<Linkable, SimilarityVector> closenessCentrality = new ClosenessCentrality<>(
			this.graph);
		return closenessCentrality.getScores();
	}

	public Map<Linkable, Integer> getVertexDegreeScores() {
		final VertexDegreeScoring<Linkable, SimilarityVector> vertexDegree = new VertexDegreeScoring<>(this.graph);
		return vertexDegree.getScores();
	}

	public Map<Linkable, Double> getVertexSelectivityScores() {
		final VertexSelectivityScoring<Linkable, SimilarityVector> vertexSelectivity = new VertexSelectivityScoring<>(
			this.graph);
		return vertexSelectivity.getScores();
	}

	public Map<Linkable, Double> getVertexStrictSelectivityScores() {
		final VertexStrictSelectivityScoring<Linkable, SimilarityVector> vertexStrictSelectivity = new VertexStrictSelectivityScoring<>(
			this.graph);
		return vertexStrictSelectivity.getScores();
	}

	public Map<SimilarityVector, Integer> getEdgeGradeScoring() {
		final EdgeGradeScoring<Linkable, SimilarityVector> edgeDegree = new EdgeGradeScoring<>(this.graph);
		return edgeDegree.getScores();
	}
	
	public Map<SimilarityVector, EdgeType> getEdgeTypeScoring() {
		final EdgeTypeScoring<Linkable, SimilarityVector> edgeType = new EdgeTypeScoring<>(this.graph);
		return edgeType.getScores();
	}

	public int singleLinks() {
		final EdgeTypeScoring<Linkable, SimilarityVector> edgeType = new EdgeTypeScoring<>(this.graph);
		return edgeType.singleLinks();
	}

	public int leftMultiLinks() {
		final EdgeTypeScoring<Linkable, SimilarityVector> edgeType = new EdgeTypeScoring<>(this.graph);
		return edgeType.leftMultiLinks();
	}
	
	public int rightMultiLinks() {
		final EdgeTypeScoring<Linkable, SimilarityVector> edgeType = new EdgeTypeScoring<>(this.graph);
		return edgeType.rightMultiLinks();
	}
	
	public int multiLinks() {
		final EdgeTypeScoring<Linkable, SimilarityVector> edgeType = new EdgeTypeScoring<>(this.graph);
		return edgeType.multiLinks();
	}

	public DescriptiveStatistics getEdgeWeightStatistic() {
		final DescriptiveStatistics stats = new DescriptiveStatistics();

		for (final SimilarityVector edge : this.edgeSet()) {
			final double weight = this.graph.getEdgeWeight(edge);
			stats.addValue(weight);
		}
		return stats;
	}
	
	public DescriptiveStatistics getEdgeStatistics() {
		final DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for (final SimilarityVector edge : this.edgeSet()) {
			final double sim = edge.getAggregatedValue();
			stats.addValue(sim);
		}
		
		return stats;
	}
	
	//TODO: test it!
	public Set<?> getAllEdgeInducedSubgraphs(){
		final Set<Graph<Linkable, SimilarityVector>> subgraphs = new HashSet<Graph<Linkable,SimilarityVector>>();
		final Set<Set<SimilarityVector>> powerSet = SetUtils.powerSet(this.graph.edgeSet());
		
		for (final Set<SimilarityVector> subset : powerSet) {
		    final Graph<Linkable, SimilarityVector> subgraph = new AsSubgraph<>(this.graph, new HashSet<Linkable>(), subset);
		    subgraphs.add(subgraph);
		}
		
		return subgraphs;
	}
	
	public Set<?> getAllVertexInducedSubgraphs(){
		final Set<Graph<Linkable, SimilarityVector>> subgraphs = new HashSet<Graph<Linkable,SimilarityVector>>();
		final Set<Set<Linkable>> powerSet = SetUtils.powerSet(this.graph.vertexSet());
		
		for (final Set<Linkable> subset : powerSet) {
		    final Graph<Linkable, SimilarityVector> subgraph = new AsSubgraph<>(this.graph, subset);
		    subgraphs.add(subgraph);
		}
		
		return subgraphs;
	}
	
	public double getEdgeAvgSim() {
		return this.getEdgeStatistics().getMean();
	}

	//TODO: Heap problems!
	@SuppressWarnings("unchecked")
	public Set<SimilarityGraph<T>> getConnectedComponentsSimGraph() {
		final BiconnectivityInspector<Linkable, SimilarityVector> inspector = new BiconnectivityInspector<>(this.graph);
		// TODO: also there are blocks, bridges, cutpoints etc.
		final Set<Graph<Linkable, SimilarityVector>> ccSet = inspector.getConnectedComponents();

		final Set<SimilarityGraph<T>> result = new HashSet<SimilarityGraph<T>>();
		Map<Integer, Integer> ccSize = new TreeMap<>();
		for (final Graph<Linkable, SimilarityVector> cc : ccSet) {
			if(cc.vertexSet().size()>1) {
				final SimilarityGraph<T> simGraph = new SimilarityGraph<>();

				for (final SimilarityVector simVec : cc.edgeSet()) {
					final Linkable source = cc.getEdgeSource(simVec);
					final Linkable target = cc.getEdgeTarget(simVec);
					simGraph.addEdge((Record) source, (T) target, simVec, simVec.getAggregatedValue());

				}
				Integer c = ccSize.putIfAbsent(simGraph.vertices(), 1);
				if(c != null){
					ccSize.put(simGraph.vertices(), c +1);
				}
				result.add(simGraph);
			}
		}
		return result;
	}
	
	//TODO: Heap problems!
	public Set<Graph<Linkable, SimilarityVector>> getConnectedComponents() {
		final BiconnectivityInspector<Linkable, SimilarityVector> inspector = new BiconnectivityInspector<>(this.graph);
		// TODO: also there are blocks, bridges, cutpoints etc.
		return inspector.getConnectedComponents();
	}

	
	public List<Set<Linkable>> getConnectedSets(){
		final ConnectivityInspector<Linkable, SimilarityVector> inspector = new ConnectivityInspector<>(this.graph);
		return inspector.connectedSets();
	}
	
	/* Own cc impl?
	    final Set<V> marked = new HashSet<>();   
		final Stack<V> stack = new Stack<>();

		final List<SimilarityGraph<Linkable>>
		
		for (V vertex : graph.vertexSet()) {
			if (!marked.contains(vertex)) {
				stack.push(vertex);
				marked.add(vertex);
				while (!stack.isEmpty()) {
					final V v = stack.pop();
					
					for (E edge : this.graph.edgesOf(v)) {
						final V nv = Graphs.getOppositeVertex(this.graph, edge, v);
						if (!marked.contains(nv)) {
							stack.push(nv);
							marked.add(v);
						}
					}
				}
			}		   
		}
	 */

	
	public SimilarityGraph<T> getSimilarityFilteredSubgraph(double similarity) {

		final Set<SimilarityVector> filteredEdges = new HashSet<>();
		
		for (final SimilarityVector edge : this.edgeSet()) {
			if (edge.getAggregatedValue().compareTo(similarity) >= 0) {
				filteredEdges.add(edge);
			}
		}
		
		final SimilarityGraph<T> subgraph = new SimilarityGraph<>();

		for (final Record r : this.getSource()) {
			subgraph.addVertexToSource(r);
		}
		
		for (final T r : this.getTarget()) {
			subgraph.addVertexToTarget(r);
		}	
		
		for (final SimilarityVector edge : filteredEdges) {
			final Record source = this.getEdgeSource(edge);
			final T target = this.getEdgeTarget(edge);
			
			final SimilarityVector edgeCopy = new SimilarityVector(edge);
			
			subgraph.addEdge(source, target, edgeCopy, edgeCopy.getAggregatedValue());
		}
				
		return subgraph;
	}

	private void writeObject(ObjectOutputStream oos)
			throws IOException {
		oos.writeObject(this.source);
		oos.writeObject(this.target);
		oos.writeObject(graph);
	}

	private void readObject(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {
		this.source = (Set<Record>) ois.readObject();
		this.target = (Set<T>) ois.readObject();
		this.graph = (SimpleWeightedGraph<Linkable, SimilarityVector>) ois.readObject();
	}


	@Override
	public String toString() {
		return this.graph.toString();
	}
}