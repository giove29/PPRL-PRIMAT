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


/**
 * 
 * @author mfranke
 *
 * @param  <V>
 * @param  <E>
 */
public class EdgeGradeScoring<V, E> implements EdgeScoringAlgorithm<E, Integer> {

	private final Graph<V, E> graph;
	private Map<E, Integer> scores;

	public EdgeGradeScoring(Graph<V, E> graph) {
		this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
		this.scores = null;
	}

	private void compute() {
		final Set<E> edges = this.graph.edgeSet();
		this.scores = new HashMap<E, Integer>(edges.size());

		for (final E edge : edges) {
			final V sourceVertex = this.graph.getEdgeSource(edge);
			final V targetVertex = this.graph.getEdgeTarget(edge);

			final int sourceDegree = this.graph.degreeOf(sourceVertex);
			final int targetDegree = this.graph.degreeOf(targetVertex);

			final int edgeGrade = Math.max(sourceDegree, targetDegree);

			this.scores.put(edge, edgeGrade);
		}
	}

	public int singleLinks() {
		final Map<E, Integer> scores = this.getScores();
		return scores.values().stream().filter(v -> v == 1).mapToInt(v -> 1).sum();
	}

	public int multiLinks() {
		final Map<E, Integer> scores = this.getScores();
		return scores.values().stream().filter(v -> v > 1).mapToInt(v -> 1).sum();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<E, Integer> getScores() {

		if (scores == null) {
			compute();
		}
		return Collections.unmodifiableMap(scores);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer getEdgeScore(E e) {

		if (!graph.containsEdge(e)) {
			throw new IllegalArgumentException("Cannot return score of unknown edge");
		}
		else if (scores == null) {
			compute();
		}
		return scores.get(e);
	}

}