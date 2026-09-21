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

import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;

/**
 * 
 * @author mfranke
 *
 */
public class MaxRightPostprocessor<T extends Linkable> extends MaxOneSidePostprocessor<T> {

	public MaxRightPostprocessor() {
	}

	@Override
	protected Set<? extends Linkable> getManySideVertices(SimilarityGraph<T> graph) {
		return graph.getSource();
	}

	@Override
	public String toString() {
		return "Right Best Match";
	}
}