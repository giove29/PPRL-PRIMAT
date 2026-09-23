package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;

/**
 * Verifica diretta delle formule di {@link MultiSourceLinkage#countGroundTruthMatches}
 * e {@link MultiSourceLinkage#countsForOutcome} nel caso misto (N>=2 party,
 * un sottoinsieme dirty) — scenario non coperto prima della generalizzazione
 * a {@link de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy#DIRTY_AWARE},
 * quando le coppie within-party erano ammesse solo per un'unica party in
 * input, indipendentemente da {@code duplicateFree}.
 */
class MultiSourceLinkageMetricsTest {

	private static Record record(String id, String globalId, Party party) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setGlobalIdAttribute(new GlobalIdAttribute(globalId));
		record.setParty(party);
		return record;
	}

	/**
	 * 3 party: A clean, B dirty, C dirty.
	 * <ul>
	 * <li>gruppo "g1": A1 (A) + B1,B2 (B, dirty) => within-B 1 + cross A-B 2 = 3</li>
	 * <li>gruppo "g2": B3 (B) + C1,C2 (C, dirty) => within-C 1 + cross B-C 2 = 3</li>
	 * </ul>
	 * Totale atteso: 6. Nessun termine within-party per A (clean).
	 */
	@Test
	void countGroundTruthMatchesCountsWithinPartyOnlyForDirtyPartiesAcrossTheWholeGroup() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", false);
		final Party partyC = new Party("C", false);

		final Record a1 = record("a1", "g1", partyA);
		final Record b1 = record("b1", "g1", partyB);
		final Record b2 = record("b2", "g1", partyB);
		final Record b3 = record("b3", "g2", partyB);
		final Record c1 = record("c1", "g2", partyC);
		final Record c2 = record("c2", "g2", partyC);

		final Map<Party, Collection<Record>> input = new LinkedHashMap<>();
		input.put(partyA, List.of(a1));
		input.put(partyB, List.of(b1, b2, b3));
		input.put(partyC, List.of(c1, c2));

		final long total = MultiSourceLinkage.countGroundTruthMatches(input);

		assertEquals(6, total);
	}

	@Test
	void countGroundTruthMatchesMatchesOldFormulaWhenAllPartiesAreClean() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", true);

		final Record a1 = record("a1", "g1", partyA);
		final Record a2 = record("a2", "g1", partyA);
		final Record b1 = record("b1", "g1", partyB);

		final Map<Party, Collection<Record>> input = new LinkedHashMap<>();
		input.put(partyA, List.of(a1, a2));
		input.put(partyB, List.of(b1));

		// solo cross-party: A(2)*B(1) = 2, mai within-A perche' A e' clean
		assertEquals(2, MultiSourceLinkage.countGroundTruthMatches(input));
	}

	@Test
	void countGroundTruthMatchesMatchesOldFormulaForASingleDirtyParty() {
		final Party partyA = new Party("A", false);

		final List<Record> records = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			records.add(record("a" + i, "g1", partyA));
		}

		final Map<Party, Collection<Record>> input = new LinkedHashMap<>();
		input.put(partyA, records);

		// un solo party dirty: tutte le C(4,2)=6 coppie del gruppo sono within-party
		assertEquals(6, MultiSourceLinkage.countGroundTruthMatches(input));
	}

	@Test
	void countsForOutcomeAcceptsCrossPartyAndWithinDirtyButRejectsWithinClean() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", false);

		final Record a1 = record("a1", "g1", partyA);
		final Record a2 = record("a2", "g1", partyA);
		final Record b1 = record("b1", "g1", partyB);
		final Record b2 = record("b2", "g1", partyB);

		assertTrue(MultiSourceLinkage.countsForOutcome(new LinkedPair<>(a1, b1)), "cross-party conta sempre");
		assertTrue(MultiSourceLinkage.countsForOutcome(new LinkedPair<>(b1, b2)),
				"within-party di una party dirty conta");
		assertFalse(MultiSourceLinkage.countsForOutcome(new LinkedPair<>(a1, a2)),
				"within-party di una party clean non deve mai contare");
	}
}
