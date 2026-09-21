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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification;

import java.util.Set;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.utils.SetUtils;

/**
 * 
 * @author mfranke
 *
 */
public enum ComparisonStrategy {

	SOURCE_CONSISTENT {
		@Override
		public Set<PartyPair> getPartyPairs(Set<Party> parties) {
			return SetUtils.getIrreflexiveClosure(parties).stream().map(p -> new PartyPair(p))
				.collect(Collectors.toSet());
		}
	},

	SOURCE_INCONSISTENT {
		@Override
		public Set<PartyPair> getPartyPairs(Set<Party> parties) {
			return SetUtils.orderedAntiSymmetricCartesianSquare(parties).stream().map(p -> new PartyPair(p))
				.collect(Collectors.toSet());
		}
	};

	public abstract Set<PartyPair> getPartyPairs(Set<Party> parties);
}