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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.IntegratedSource;
import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;


/**
 * 
 * @author     mfranke
 *
 * @param  <T>
 */
public class IncrementalLinkageResult<T extends Linkable> {

	private Map<Party, LinkageResultPartition<T>> result;

	public IncrementalLinkageResult(Set<Party> parties,
		LinkageResultPartitionFactory<T> linkageResultPartitionFactory) {
		this.result = new TreeMap<>();

		for (final Party party : parties) {
			this.result.put(party, linkageResultPartitionFactory
				.createLinkageResultPartition(new PartyPair(party, IntegratedSource.getInstance())));
		}
	}

	public LinkageResultPartition<T> getPartition(Party party) {
		return this.result.get(party);
	}

	public Collection<LinkageResultPartition<T>> getPartitions() {
		return this.result.values();
	}

	public Map<Party, LinkageResultPartition<T>> getPartitionMap() {
		return this.result;
	}

	public Set<Party> partitions() {
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