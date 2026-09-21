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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.stable_marriage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.Postprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class GreedyPostprocessor<T extends Linkable> implements Postprocessor<T> {

	public GreedyPostprocessor() {
	}

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<>();
		matches.accept(vis);
		final SimilarityGraph<T> graph = vis.getSimilarityGraph();

		final Set<SimilarityVector> edges = graph.edgeSet();
		final List<SimilarityVector> sortedEdges = new ArrayList<>(edges);
		Collections.sort(sortedEdges, Collections.reverseOrder());

		final Set<Record> leftVisits = new HashSet<>();
		final Set<T> rightVisits = new HashSet<>();

		for (final SimilarityVector edge : sortedEdges) {

			if (!graph.containsEdge(edge)) {
				continue;
			}
			else {
				final Record left = graph.getEdgeSource(edge);
				final T right = graph.getEdgeTarget(edge);

				if (leftVisits.contains(left) || rightVisits.contains(right)) {
					continue;
				}
				else {
					final Set<SimilarityVector> leftEdges = graph.edgesOf(left);
					final Set<SimilarityVector> rightEdges = graph.edgesOf(right);
					final Set<SimilarityVector> allEdges = new HashSet<>(leftEdges);
					allEdges.addAll(rightEdges);
					allEdges.remove(edge);
					graph.removeAllEdges(allEdges);
				}

				leftVisits.add(left);
				rightVisits.add(right);
			}
		}

		final SimilarityGraphMatchStrategy<T> result = new SimilarityGraphMatchStrategy<>();
		result.setSimilarityGraph(vis.getSimilarityGraph());
		return result;
	}

	@Override
	public String toString() {
		return "Stable Marriage (Greedy)";
	}
}