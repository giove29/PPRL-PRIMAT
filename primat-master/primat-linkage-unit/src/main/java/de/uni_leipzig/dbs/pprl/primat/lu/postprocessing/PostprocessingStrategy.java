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

import java.util.EnumMap;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.LinkageConstraint;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;


public class PostprocessingStrategy<T extends Linkable> {

	final EnumMap<LinkageConstraint, Postprocessor<T>> postprocessors;

	public PostprocessingStrategy() {
		this.postprocessors = new EnumMap<LinkageConstraint, Postprocessor<T>>(LinkageConstraint.class);
	}

	public PostprocessingStrategy(EnumMap<LinkageConstraint, Postprocessor<T>> postprocessors) {
		this.postprocessors = postprocessors;
	}

	public Postprocessor<T> setPostprocessor(LinkageConstraint linkageConstraint, Postprocessor<T> postprocessor) {
		return this.postprocessors.put(linkageConstraint, postprocessor);
	}

	public LinkageResultPartition<T> clean(LinkageResultPartition<T> partition) {
		final PartyPair partyPair = partition.getPartyPair();
		final LinkageConstraint linkageConstraint = partyPair.getLinkageConstraint();
		final Postprocessor<T> postprocessor = this.postprocessors.get(linkageConstraint);
		return postprocessor.clean(partition);
	}

}
