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

import java.util.HashSet;
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
	},

	/**
	 * Confronto cross-party sempre (come {@link #SOURCE_CONSISTENT}) più una
	 * coppia identità party-con-se-stessa per ogni party con
	 * {@code duplicateFree=false}, indipendentemente da quante altre party
	 * sono presenti: a differenza di {@link #SOURCE_INCONSISTENT} (che
	 * genererebbe una identity pair per OGNI party, anche quelle clean),
	 * questa strategia guarda {@link Party#isDuplicateFree()} di ciascuna
	 * party singolarmente, non la cardinalità dell'insieme. Per un insieme di
	 * una sola party (sempre dirty, per costruzione: la config loader rifiuta
	 * un'unica party clean) collassa a {@link #SOURCE_INCONSISTENT}; per un
	 * insieme di 2+ party tutte clean collassa a {@link #SOURCE_CONSISTENT}.
	 */
	DIRTY_AWARE {
		@Override
		public Set<PartyPair> getPartyPairs(Set<Party> parties) {
			final Set<PartyPair> pairs = SetUtils.getIrreflexiveClosure(parties).stream().map(p -> new PartyPair(p))
					.collect(Collectors.toCollection(HashSet::new));
			parties.stream().filter(p -> !p.isDuplicateFree()).forEach(p -> pairs.add(new PartyPair(p, p)));
			return pairs;
		}
	};

	public abstract Set<PartyPair> getPartyPairs(Set<Party> parties);
}