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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;


/**
 * 
 * @author     mfranke
 *
 * @param  <T>
 */
public class LinkageResult<T extends Linkable> {

	private Map<PartyPair, LinkageResultPartition<T>> result;

	public LinkageResult(Set<PartyPair> parties, LinkageResultPartitionFactory<T> linkageResultPartitionFactory) {
		this.result = new HashMap<>();

		for (final PartyPair partyPair : parties) {
			this.result.put(partyPair, linkageResultPartitionFactory.createLinkageResultPartition(partyPair));
		}
	}

	public LinkageResultPartition<T> getPartition(PartyPair partyPair) {
		return this.result.get(partyPair);
	}

	public Collection<LinkageResultPartition<T>> getPartitions() {
		return this.result.values();
	}

	public Set<PartyPair> partitions() {
		return this.result.keySet();
	}

	public List<LinkedPair<T>> getMatches() {
		return this.result.values().stream().map(part -> part.getMatchStrategy())
			.flatMap(matchStrat -> matchStrat.getMatches().stream()).collect(Collectors.toList());
	}

	public List<LinkedPair<T>> getNonMatches() {
		return this.result.values().stream().map(part -> part.getNonMatchStrategy())
			.flatMap(nonMatchStrat -> nonMatchStrat.getNonMatches().stream()).collect(Collectors.toList());
	}

	public int matches() {
		int result = 0;

		for (final LinkageResultPartition<T> lrp : this.result.values()) {
			result = result + lrp.getMatchStrategy().size();
		}
		return result;
	}

	public int nonMatches() {
		int result = 0;

		for (final LinkageResultPartition<T> lrp : this.result.values()) {
			result = result + lrp.getNonMatchStrategy().size();
		}
		return result;
	}
}