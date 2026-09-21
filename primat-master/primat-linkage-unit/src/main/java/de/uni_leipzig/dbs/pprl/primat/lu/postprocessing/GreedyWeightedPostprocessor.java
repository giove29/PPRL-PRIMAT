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

import org.jgrapht.alg.interfaces.MatchingAlgorithm;
import org.jgrapht.alg.matching.GreedyWeightedMatching;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * ---> Maximal cardinality matching, if an edge is added no more longer a
 * matching, maybe max weight
 * 
 * @author mfranke
 *
 */
public class GreedyWeightedPostprocessor<T extends Linkable> extends JGraphTPostprocessor<T> {

	@Override
	protected MatchingAlgorithm<Linkable, SimilarityVector> getMatchingAlgorithm(SimilarityGraph<T> graph) {
		return new GreedyWeightedMatching<Linkable, SimilarityVector>(graph.getGraph(), true);

	}

	@Override
	public String toString() {
		return "Greedy Weighted";
	}

}