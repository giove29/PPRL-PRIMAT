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
package de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches;

import java.util.HashSet;
import java.util.Set;


import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 * @param  <T>
 */
public class SimilarityGraphMatchStrategy<T extends Linkable> implements MatchStrategy<T> {

	private SimilarityGraph<T> simGraph;

	public SimilarityGraphMatchStrategy() {
		this.simGraph = new SimilarityGraph<T>();
	}

	@Override
	public boolean isContained(Record left, T right) {
		return this.simGraph.containsEdge(left, right);
	}

	@Override
	public boolean add(Record left, T right, SimilarityVector v, double weight) {
		// final boolean contained = isContained(left, right);
		final boolean insert = this.simGraph.addEdge(left, right, v, weight);		
		return insert;
	}

	@Override
	public int size() {
		return this.simGraph.edges();
	}

	public SimilarityGraph<T> getSimilarityGraph() {
		return this.simGraph;
	}

	public void setSimilarityGraph(SimilarityGraph<T> simGraph) {
		this.simGraph = simGraph;
	}

	@Override
	public void accept(MatchStrategyVisitor<T> vis) {
		vis.visit(this);
	}

	@Override
	public Set<LinkedPair<T>> getMatches() {
		final Set<LinkedPair<T>> matches = new HashSet<>(simGraph.edges());

		// or use Graphs.neighborSetOf()
		for (final SimilarityVector edge : this.simGraph.edgeSet()) {
			final Record source = this.simGraph.getEdgeSource(edge);
			final T target = this.simGraph.getEdgeTarget(edge);
			final LinkedPair<T> match = new LinkedPair<T>(source, target, edge, edge.getAggregatedValue());
			matches.add(match);
		}

		return matches;
	}

	@Override
	public int sourceSize() {
		return this.simGraph.sourceVertices();
	}

	@Override
	public int targetSize() {
		return this.simGraph.targetVertices();
	}
}