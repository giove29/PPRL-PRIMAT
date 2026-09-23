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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;

/**
 * Verifica che {@link ComparisonStrategy#DIRTY_AWARE} generi sempre tutte le
 * coppie cross-party, più una coppia identità per ogni party dirty, mai per
 * una party clean — a differenza di {@link ComparisonStrategy#SOURCE_CONSISTENT}
 * (mai identity pair) e {@link ComparisonStrategy#SOURCE_INCONSISTENT} (identity
 * pair per ogni party, anche quelle clean).
 */
class ComparisonStrategyTest {

	private static boolean containsCrossPair(Set<PartyPair> pairs, Party x, Party y) {
		return pairs.stream().anyMatch(p -> (p.getLeftParty().equals(x) && p.getRightParty().equals(y))
				|| (p.getLeftParty().equals(y) && p.getRightParty().equals(x)));
	}

	private static boolean containsIdentityPair(Set<PartyPair> pairs, Party x) {
		return pairs.contains(new PartyPair(x, x));
	}

	@Test
	void dirtyAwareAddsIdentityPairsOnlyForDirtyParties() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", false);
		final Party partyC = new Party("C", false);

		final Set<PartyPair> pairs = ComparisonStrategy.DIRTY_AWARE.getPartyPairs(Set.of(partyA, partyB, partyC));

		// 3 coppie cross-party (invariate rispetto a SOURCE_CONSISTENT) + 2 identity pair (B, C)
		assertEquals(5, pairs.size());
		assertTrue(containsCrossPair(pairs, partyA, partyB));
		assertTrue(containsCrossPair(pairs, partyA, partyC));
		assertTrue(containsCrossPair(pairs, partyB, partyC));
		assertTrue(containsIdentityPair(pairs, partyB));
		assertTrue(containsIdentityPair(pairs, partyC));
		assertFalse(containsIdentityPair(pairs, partyA), "una party clean non deve mai avere una identity pair");
	}

	@Test
	void dirtyAwareCollapsesToSourceConsistentWhenAllPartiesAreClean() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", true);

		final Set<PartyPair> dirtyAware = ComparisonStrategy.DIRTY_AWARE.getPartyPairs(Set.of(partyA, partyB));
		final Set<PartyPair> sourceConsistent = ComparisonStrategy.SOURCE_CONSISTENT
				.getPartyPairs(Set.of(partyA, partyB));

		assertEquals(sourceConsistent, dirtyAware);
	}

	@Test
	void dirtyAwareCollapsesToSourceInconsistentForASingleDirtyParty() {
		final Party partyA = new Party("A", false);

		final Set<PartyPair> dirtyAware = ComparisonStrategy.DIRTY_AWARE.getPartyPairs(Set.of(partyA));
		final Set<PartyPair> sourceInconsistent = ComparisonStrategy.SOURCE_INCONSISTENT.getPartyPairs(Set.of(partyA));

		assertEquals(sourceInconsistent, dirtyAware);
	}
}
