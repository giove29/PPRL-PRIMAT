package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;

class SimilarityHistogramCollectorTest {

	private static Record record(String id, String globalId, Party party) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setGlobalIdAttribute(new GlobalIdAttribute(globalId));
		record.setParty(party);
		return record;
	}

	private static SimilarityHistogramCollector collector() {
		return new SimilarityHistogramCollector(100, new IdEqualityTrueMatchChecker());
	}

	@Test
	void samePairSeenInSeveralBlocksAndBothOrderingsCountsOnce() {
		final Party a = new Party("A", true);
		final Party b = new Party("B", true);
		final Record a1 = record("A1", "g1", a);
		final Record b1 = record("B1", "g1", b);
		final SimilarityHistogramCollector collector = collector();

		collector.observe(a1, b1, 0.9);
		collector.observe(a1, b1, 0.9);
		collector.observe(b1, a1, 0.9);

		assertEquals(1, collector.getAll().getTotal());
	}

	@Test
	void withinAndCrossPartyPairsShareTheSameHistograms() {
		final Party a = new Party("A", false);
		final Party b = new Party("B", true);
		final Record a1 = record("A1", "g1", a);
		final Record a2 = record("A2", "g1", a);
		final Record b1 = record("B1", "g2", b);
		final SimilarityHistogramCollector collector = collector();

		collector.observe(a1, a2, 0.8); // within, match vero
		collector.observe(a1, b1, 0.2); // cross, non-match
		collector.observe(a2, b1, 0.3); // cross, non-match

		assertEquals(1, collector.getMatches().getTotal());
		assertEquals(2, collector.getNonMatches().getTotal());
		assertEquals(3, collector.getAll().getTotal());
	}

	@Test
	void pairsAreSplitByGroundTruthAndTheCombinedHistogramIsTheirSum() {
		final Party a = new Party("A", false);
		final Party b = new Party("B", true);
		final Record a1 = record("A1", "g1", a);
		final Record a2 = record("A2", "g1", a);
		final Record a3 = record("A3", "g3", a);
		final Record b1 = record("B1", "g1", b);
		final Record b2 = record("B2", "g2", b);
		final SimilarityHistogramCollector collector = collector();

		collector.observe(a1, b1, 0.9); // stesso GLOBAL_ID
		collector.observe(a1, a2, 0.85); // stesso GLOBAL_ID
		collector.observe(a1, b2, 0.4); // GLOBAL_ID diverso
		collector.observe(a3, b2, 0.5);
		collector.observe(a1, a3, 0.3);

		assertEquals(2, collector.getMatches().getTotal());
		assertEquals(3, collector.getNonMatches().getTotal());
		assertEquals(5, collector.getAll().getTotal());
		assertEquals(2, collector.getMatches().countAtOrAbove(0.75));
		assertEquals(0, collector.getNonMatches().countAtOrAbove(0.75));
	}
}
