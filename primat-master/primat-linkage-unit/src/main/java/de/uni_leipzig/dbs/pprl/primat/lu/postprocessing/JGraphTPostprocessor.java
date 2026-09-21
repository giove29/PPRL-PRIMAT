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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public abstract class JGraphTPostprocessor<T extends Linkable> implements Postprocessor<T> {

	protected abstract MatchingAlgorithm<Linkable, SimilarityVector> getMatchingAlgorithm(SimilarityGraph<T> graph);

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<>();
		matches.accept(vis);
		final SimilarityGraph<T> graph = vis.getSimilarityGraph();
		final Matching<Linkable, SimilarityVector> matching = this.getMatchingAlgorithm(graph).getMatching();
		final Set<SimilarityVector> matchingEdges = new HashSet<>(matching.getEdges());
		final Set<SimilarityVector> edgesToRemove = new HashSet<>(graph.getGraph().edgeSet());
		edgesToRemove.removeAll(matchingEdges);

		graph.getGraph().removeAllEdges(edgesToRemove);

		return null;
	}
}