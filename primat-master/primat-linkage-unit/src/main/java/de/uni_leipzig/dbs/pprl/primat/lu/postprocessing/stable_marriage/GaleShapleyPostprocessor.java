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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class GaleShapleyPostprocessor<T extends Linkable> implements Postprocessor<T> {

	public GaleShapleyPostprocessor() {
	}

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<T>();
		matches.accept(vis);
		final SimilarityGraph<T> graph = vis.getSimilarityGraph();

		final Deque<Record> freeMen = new ArrayDeque<>(graph.getSource());
		final Map<T, SimilarityVector> marriedWoman = new HashMap<>();

		while (!freeMen.isEmpty()) {
			final Record man = freeMen.pop();
			final List<SimilarityVector> manPreferences = new ArrayList<>(graph.getGraph().outgoingEdgesOf(man));
			Collections.sort(manPreferences, Collections.reverseOrder());

			for (final SimilarityVector proposal : manPreferences) {
				final T woman = graph.getEdgeTarget(proposal);

				if (!marriedWoman.containsKey(woman)) {
					marriedWoman.put(woman, proposal);
					break;
				}
				else {
					final SimilarityVector currentMan = marriedWoman.get(woman);

					if (currentMan.compareTo(proposal) < 0) {
						// divorce and new marriage
						marriedWoman.put(woman, proposal);
						final Record divorcedMan = graph.getEdgeSource(currentMan);
						freeMen.addLast(divorcedMan);
						break;
					}
					else {
						// current man is better
						continue;
					}
				}
			}
		}

		graph.retainAllEdges(marriedWoman.values());

		return null;
	}

	@Override
	public String toString() {
		return "Stable Marriage (Gale-Shapley)";
	}

}