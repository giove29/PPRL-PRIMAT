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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;


/**
 * 
 * @author mfranke
 *
 * @param  <V>
 * @param  <E>
 */
public class VertexDegreeScoring<V, E> implements VertexScoringAlgorithm<V, Integer> {

	private final Graph<V, E> graph;
	private Map<V, Integer> scores;

	public VertexDegreeScoring(Graph<V, E> graph) {
		this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
		this.scores = null;
	}

	private void compute() {
		final Set<V> vertices = this.graph.vertexSet();
		this.scores = new HashMap<V, Integer>(vertices.size());

		for (final V vertex : vertices) {
			final int vertexDegree = this.graph.degreeOf(vertex);
			this.scores.put(vertex, vertexDegree);
		}
	}

	// TODO: Differentiate between source and target vertices in order to
	// calculate fraction of vertices with degree 1 and all other vertices.

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<V, Integer> getScores() {

		if (scores == null) {
			compute();
		}
		return Collections.unmodifiableMap(scores);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getVertexScore(V v) {

		if (!graph.containsVertex(v)) {
			throw new IllegalArgumentException("Cannot return score of unknown vertex");
		}
		else if (scores == null) {
			compute();
		}
		return scores.get(v);
	}
}
