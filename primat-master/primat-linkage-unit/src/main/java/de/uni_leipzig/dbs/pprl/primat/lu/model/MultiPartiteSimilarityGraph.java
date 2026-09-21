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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.alg.scoring.ClosenessCentrality;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class MultiPartiteSimilarityGraph {

	private SimpleWeightedGraph<Record, SimilarityVector> graph;
	
	private Map<Party, Set<Record>> partitions;

	public static MultiPartiteSimilarityGraph from(LinkageResult<Record> linkRes) {
		final MultiPartiteSimilarityGraph mpsg = new MultiPartiteSimilarityGraph();
		
		final List<LinkedPair<Record>> matches = linkRes.getMatches();
		
		for (final LinkedPair<Record> pair : matches) {
			mpsg.addEdge(pair.getLeftRecord(), pair.getRight(), pair.getSimVec(), pair.getAggregatedSim());
		}
		
		return mpsg;
	}
	
	public MultiPartiteSimilarityGraph() {
		this.graph = new SimpleWeightedGraph<>(SimilarityVector.class);
		this.partitions = new HashMap<>();
	}

	public boolean addVertex(Record record) {
		final Party party = record.getParty();

		if (this.partitions.containsKey(party)) {
			this.partitions.get(party).add(record);
		}
		else {
			final Set<Record> set = new HashSet<>();
			set.add(record);
			this.partitions.put(party, set);
		}
		return this.graph.addVertex(record);
	}

	public boolean addEdge(Record sourceVertex, Record targetVertex, SimilarityVector edge) {
		return this.addEdge(sourceVertex, targetVertex, edge, 1d);
	}

	public boolean addEdge(Record sourceVertex, Record targetVertex, SimilarityVector edge, double weight) {
		this.addVertex(sourceVertex);
		this.addVertex(targetVertex);
		final boolean insert = this.graph.addEdge(sourceVertex, targetVertex, edge);

		if (insert) {
			this.graph.setEdgeWeight(edge, weight);
		}
		return insert;
	}

	public double getEdgeWeight(SimilarityVector edge) {
		return this.graph.getEdgeWeight(edge);
	}

	public Set<Record> getPartition(Party party) {
		return this.partitions.get(party);
	}

	public boolean removeEdge(SimilarityVector edge) {
		return this.graph.removeEdge(edge);
	}

	public SimilarityVector removeEdge(Record sourceVertex, Record targetVertex) {
		return this.graph.removeEdge(sourceVertex, targetVertex);
	}

	public boolean removeAllEdgesOf(Record record) {
		final Set<SimilarityVector> edges = this.edgesOf(record);
		return this.graph.removeAllEdges(edges);
	}

	public void retainEdgeOf(Record record, SimilarityVector edgeToRetain) {
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

	public Set<SimilarityVector> getAllEdges(Record source, Record target) {
		return this.graph.getAllEdges(source, target);
	}

	public void removeAllEdges() {
		this.graph.removeAllEdges(this.edgeSet());
	}

	public boolean removeAllEdges(Collection<? extends SimilarityVector> edges) {
		return this.graph.removeAllEdges(edges);
	}

	public void removeVertex(Record vertex) {
		final Party party = vertex.getParty();

		if (this.partitions.containsKey(party)) {
			this.partitions.get(party).remove(vertex);
			this.graph.removeVertex(vertex);
		}
		else {
			// TODO: Error handling?
		}
	}

	public AsUnmodifiableGraph<Record, SimilarityVector> getGraph() {
		return new AsUnmodifiableGraph<Record, SimilarityVector>(this.graph);
	}

	public Set<SimilarityVector> edgesOf(Record record) {
		return this.graph.edgesOf(record);
	}

	public List<SimilarityVector> sortedEdgesOf(Record record) {
		final List<SimilarityVector> edges = new ArrayList<>(this.edgesOf(record));
		Collections.sort(edges, Collections.reverseOrder());
		return edges;
	}

	public SimilarityVector maxEdgeOf(Record record) {
		final Set<SimilarityVector> edges = this.edgesOf(record);
		return Collections.max(edges);
	}

	/**
	 * Returns the degree of the specified vertex.
	 * 
	 * The degree of a vertex is the number of edges touching that vertex.
	 * 
	 * @param  record
	 * @return
	 */
	public int getVertexDegree(Record vertex) {
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
		final Record source = this.graph.getEdgeSource(edge);
		final Record target = this.graph.getEdgeTarget(edge);
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
	 * @param  e
	 * @return
	 */
	public int getEdgeGrade(SimilarityVector edge) {
		final Record source = this.graph.getEdgeSource(edge);
		final Record target = this.graph.getEdgeTarget(edge);
		final int sourceDegree = this.getVertexDegree(source);
		final int targetDegree = this.getVertexDegree(target);
		return Math.max(sourceDegree, targetDegree);
	}

	public boolean containsVertex(Record vertex) {
		return this.graph.containsVertex(vertex);
	}

	public boolean containsEdge(Record sourceVertex, Record targetVertex) {
		return this.graph.containsEdge(sourceVertex, targetVertex);
	}

	public boolean containsEdge(SimilarityVector edge) {
		return this.graph.containsEdge(edge);
	}

	public Record getEdgeSource(SimilarityVector edge) {
		final Linkable source = this.graph.getEdgeSource(edge);
		return (Record) source;
	}

	public Record getEdgeTarget(SimilarityVector edge) {
		return this.graph.getEdgeTarget(edge);
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

	public int partitionVertices(Party party) {
		return this.partitions.get(party).size();
	}

	public int unmappedPartitionVertices(Party party) {
		final Set<Record> partition = this.partitions.get(party);
		return (int) partition.stream().filter(x -> this.graph.edgesOf(x).isEmpty()).count();
	}

	public Set<SimilarityVector> edgeSet() {
		return this.graph.edgeSet();
	}

	public Set<Record> vertexSet() {
		return this.graph.vertexSet();
	}

	@SuppressWarnings("unchecked")
	@Override
	public MultiPartiteSimilarityGraph clone() {
		final MultiPartiteSimilarityGraph clone = new MultiPartiteSimilarityGraph();

		clone.partitions = new HashMap<>(this.partitions);
		clone.graph = (SimpleWeightedGraph<Record, SimilarityVector>) this.graph.clone();

		return clone;
	}

	public Map<Record, Double> getPageRankScores() {
		final PageRank<Record, SimilarityVector> pageRank = new PageRank<>(this.graph);
		return pageRank.getScores();
	}

	public Map<Record, Double> getBetweennessCentralityScores() {
		final BetweennessCentrality<Record, SimilarityVector> betweennessCentrality = new BetweennessCentrality<>(
			this.graph);
		return betweennessCentrality.getScores();
	}

	public Map<Record, Double> getClosenessCentralityScores() {
		final ClosenessCentrality<Record, SimilarityVector> closenessCentrality = new ClosenessCentrality<>(this.graph);
		return closenessCentrality.getScores();
	}

	public Map<Record, Integer> getVertexDegreeScores() {
		final VertexDegreeScoring<Record, SimilarityVector> vertexDegree = new VertexDegreeScoring<>(this.graph);
		return vertexDegree.getScores();
	}

	public Map<Record, Double> getVertexSelectivityScores() {
		final VertexSelectivityScoring<Record, SimilarityVector> vertexSelectivity = new VertexSelectivityScoring<>(
			this.graph);
		return vertexSelectivity.getScores();
	}

	public Map<Record, Double> getVertexStrictSelectivityScores() {
		final VertexStrictSelectivityScoring<Record, SimilarityVector> vertexStrictSelectivity = new VertexStrictSelectivityScoring<>(
			this.graph);
		return vertexStrictSelectivity.getScores();
	}

	public Map<SimilarityVector, Integer> getEdgeGradeScoring() {
		final EdgeGradeScoring<Record, SimilarityVector> edgeDegree = new EdgeGradeScoring<>(this.graph);
		return edgeDegree.getScores();
	}

	public int singleLinks() {
		final EdgeGradeScoring<Record, SimilarityVector> edgeDegree = new EdgeGradeScoring<>(this.graph);
		return edgeDegree.singleLinks();
	}

	public int multiLinks() {
		final EdgeGradeScoring<Record, SimilarityVector> edgeDegree = new EdgeGradeScoring<>(this.graph);
		return edgeDegree.multiLinks();
	}

	public DescriptiveStatistics getEdgeWeightStatistic() {
		final DescriptiveStatistics stats = new DescriptiveStatistics();

		for (final SimilarityVector edge : this.edgeSet()) {
			final double weight = this.graph.getEdgeWeight(edge);
			stats.addValue(weight);
		}
		return stats;
	}

	public Set<Graph<Record, SimilarityVector>> getConnectedComponents() {
		final BiconnectivityInspector<Record, SimilarityVector> inspector = new BiconnectivityInspector<>(this.graph);
		// TODO: also there are blocks, bridges, cutpoints etc.
		return inspector.getConnectedComponents();
	}

	public Set<MultiPartiteSimilarityGraph> getConnectedComponentsSimGraph() {
		final BiconnectivityInspector<Record, SimilarityVector> inspector = new BiconnectivityInspector<>(this.graph);
		// TODO: also there are blocks, bridges, cutpoints etc.
		final Set<Graph<Record, SimilarityVector>> ccSet = inspector.getConnectedComponents();

		final Set<MultiPartiteSimilarityGraph> result = new HashSet<MultiPartiteSimilarityGraph>();

		for (final Graph<Record, SimilarityVector> cc : ccSet) {

			final MultiPartiteSimilarityGraph simGraph = new MultiPartiteSimilarityGraph();

			for (final SimilarityVector simVec : cc.edgeSet()) {
				final Record source = cc.getEdgeSource(simVec);
				final Record target = cc.getEdgeTarget(simVec);

				simGraph.addEdge(source, target, simVec);
			}

			result.add(simGraph);
		}

		return result;
	}

	@Override
	public String toString() {
		return this.graph.toString();
	}
}