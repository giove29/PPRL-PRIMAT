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

import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.HopcroftKarpMaximumCardinalityBipartiteMatching;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * ---> Maximum cardinality and thus also maximal
 * 
 * @author mfranke
 *
 */
public class HopcroftKarpMaximumCardinalityPostprocessor<T extends Linkable> extends JGraphTPostprocessor<T> {

	@Override
	protected MatchingAlgorithm<Linkable, SimilarityVector> getMatchingAlgorithm(SimilarityGraph<T> graph) {

		final Set<Record> sourceSpecific = graph.getSource();
		final Set<Linkable> source = sourceSpecific.stream().map(r -> (Linkable) r).collect(Collectors.toSet());

		final Set<T> targetSpecific = graph.getTarget();
		final Set<Linkable> target = targetSpecific.stream().map(r -> (Linkable) r).collect(Collectors.toSet());

		return new HopcroftKarpMaximumCardinalityBipartiteMatching<Linkable, SimilarityVector>(graph.getGraph(), source,
			target);
	}

	@Override
	public String toString() {
		return "Hopcroft-Karp Maximum Cardinality";
	}

}
