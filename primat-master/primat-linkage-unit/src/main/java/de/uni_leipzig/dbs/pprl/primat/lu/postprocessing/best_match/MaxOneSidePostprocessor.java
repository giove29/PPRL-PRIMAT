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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.Postprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

/**
 * 
 * @author mfranke
 *
 */
public abstract class MaxOneSidePostprocessor<T extends Linkable> implements Postprocessor<T> {

	public MaxOneSidePostprocessor() {
	}

	protected abstract Set<? extends Linkable> getManySideVertices(SimilarityGraph<T> graph);

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<>();
		matches.accept(vis);
		final SimilarityGraph<T> graph = vis.getSimilarityGraph();

		final Set<? extends Linkable> records = this.getManySideVertices(graph);

		for (final Linkable rec : records) {
			final Set<SimilarityVector> edges = graph.edgesOf(rec);

			if (edges == null || edges.size() < 2) {
				continue;
			}
			else {
				final SimilarityVector maxEdge = Collections.max(edges);
				final Set<SimilarityVector> edgesToRemove = new HashSet<>(edges);
				edgesToRemove.remove(maxEdge);
				graph.removeAllEdges(edgesToRemove);
			}
		}
		// System.out.println("After: " + graph.edges());

		return null;
	}
}