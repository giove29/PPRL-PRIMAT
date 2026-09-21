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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.hungarian;

import java.util.HashSet;

import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
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
public final class MaximumWeightMatching<T extends Linkable> implements Postprocessor<T> {

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<>();
		matches.accept(vis);
		final SimilarityGraph<T> simGraph = vis.getSimilarityGraph();

		final Set<Record> sourceSpecific = simGraph.getSource();
		final Set<Linkable> source = sourceSpecific.stream().map(r -> (Linkable) r).collect(Collectors.toSet());

		final Set<T> targetSpecific = simGraph.getTarget();
		final Set<Linkable> target = targetSpecific.stream().map(r -> (Linkable) r).collect(Collectors.toSet());

		final MatchingAlgorithm<Linkable, SimilarityVector> mwmAlg = new MaximumWeightBipartiteMatching<Linkable, SimilarityVector>(
			simGraph.getGraph(), source, target);

		final Set<SimilarityVector> matchingEdges = mwmAlg.getMatching().getEdges();
		final Set<SimilarityVector> edgesToRemove = new HashSet<>(simGraph.edgeSet());
		edgesToRemove.removeAll(matchingEdges);

		simGraph.removeAllEdges(edgesToRemove);

		return null;
	}

	@Override
	public String toString() {
		return "Maximum Weight Matching";
	}
}