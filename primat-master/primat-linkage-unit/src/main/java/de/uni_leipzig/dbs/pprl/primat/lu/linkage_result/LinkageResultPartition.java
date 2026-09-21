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
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author     mfranke
 *
 * @param  <T>
 */
public class LinkageResultPartition<T extends Linkable> {

	private final PartyPair partyPair;
	private final MatchStrategy<T> matches;
	private final NonMatchStrategy<T> nonMatches;

	public LinkageResultPartition(PartyPair partyPair, MatchStrategyFactory<T> matchFactory,
		NonMatchStrategyFactory<T> nonMatchFactory) {
		this.partyPair = partyPair;
		this.matches = matchFactory.createMatchStrategy();
		this.nonMatches = nonMatchFactory.createNonMatchStrategy();
	}

	public MatchStrategy<T> getMatchStrategy() {
		return this.matches;
	}

	public NonMatchStrategy<T> getNonMatchStrategy() {
		return this.nonMatches;
	}

	public int matches() {
		return this.matches.size();
	}

	public int nonMatches() {
		return this.nonMatches.size();
	}

	public boolean addMatch(LinkedPair<T> pair) {
		return this.addMatch(pair.getLeftRecord(), pair.getRight(), pair.getSimVec(), pair.getAggregatedSim());
	}

	public boolean addNonMatch(LinkedPair<T> pair) {
		return this.addNonMatch(pair.getLeftRecord(), pair.getRight(), pair.getSimVec(), pair.getAggregatedSim());
	}

	public boolean addMatch(Record left, T right, SimilarityVector v, double weight) {
		return this.matches.add(left, right, v, weight);
	}

	public boolean addNonMatch(Record left, T right, SimilarityVector v, double weight) {
		return this.nonMatches.add(left, right, v, weight);
	}

	public PartyPair getPartyPair() {
		return partyPair;
	}
}