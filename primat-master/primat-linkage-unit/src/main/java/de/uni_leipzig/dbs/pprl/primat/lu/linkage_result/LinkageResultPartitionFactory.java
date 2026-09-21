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
package de.uni_leipzig.dbs.pprl.primat.lu.linkage_result;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;


/**
 * 
 * @author     mfranke
 *
 * @param  <T>
 */
public class LinkageResultPartitionFactory<T extends Linkable> {

	private MatchStrategyFactory<T> matchStrategyFactory;
	private NonMatchStrategyFactory<T> nonMatchStrategyFactory;

	public LinkageResultPartitionFactory(MatchStrategyFactory<T> matchStrategyFactory,
		NonMatchStrategyFactory<T> nonMatchStrategyFactory) {

		this.matchStrategyFactory = matchStrategyFactory;
		this.nonMatchStrategyFactory = nonMatchStrategyFactory;
	}

	public LinkageResultPartition<T> createLinkageResultPartition(PartyPair partyPair) {
		return new LinkageResultPartition<T>(partyPair, matchStrategyFactory, nonMatchStrategyFactory);
	}
}
