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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.SetUtils;
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
public class MaxBothPostprocessor<T extends Linkable> implements Postprocessor<T> {

	public MaxBothPostprocessor() {}

	public MatchStrategy<T> clean(SimilarityGraph<T> graph){		
		final Set<SimilarityVector> leftMax = new HashSet<>();
		final Set<SimilarityVector> rightMax = new HashSet<>();
		
		for (final Record rec : graph.getSource()) {
			final List<SimilarityVector> edges = graph.sortedEdgesOf(rec);
		
			if (edges != null && edges.size() > 0) {			
				final SimilarityVector maxEdge = edges.get(0);							
				
				if (edges.size() > 1) {
					final SimilarityVector top2 = edges.get(1);
					if (maxEdge.getAggregatedValue().compareTo(top2.getAggregatedValue()) == 0) {
						// HANDLE INDIFFERENCE
					}
					else {
						leftMax.add(maxEdge);
					}
				}
				else {
					leftMax.add(maxEdge);
				}
			}	
		}
		
		for (final T rec : graph.getTarget()) {
			final List<SimilarityVector> edges = graph.sortedEdgesOf(rec);
		
			if (edges != null && edges.size() > 0) {			
				final SimilarityVector maxEdge = edges.get(0);
				
				if (edges.size() > 1) {
					final SimilarityVector top2 = edges.get(1);
					if (maxEdge.getAggregatedValue().compareTo(top2.getAggregatedValue()) == 0) {
						// HANDLE INDIFFERENCE
					}
					else {
						rightMax.add(maxEdge);
					}
				}
				else {
					rightMax.add(maxEdge);
				}
				
				
			}	
		}		
		
		final Set<SimilarityVector> maxBoth = SetUtils.intersection(leftMax, rightMax);
		graph.retainAllEdges(maxBoth);
		graph.removeUnmappedVertices();
		
		return null;
	}
			
	
	
	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<>();
		matches.accept(vis);
		final SimilarityGraph<T> graph = vis.getSimilarityGraph();
		return clean(graph);
	}

	@Override
	public String toString() {
		return "Max1-both (Symetric Best Match)";
	}
}
