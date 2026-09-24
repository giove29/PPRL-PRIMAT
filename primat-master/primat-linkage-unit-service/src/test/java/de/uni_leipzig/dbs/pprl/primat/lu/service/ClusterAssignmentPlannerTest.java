package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;

/**
 * Logica pura di assegnazione dei record freschi ai cluster storici: un solo
 * cluster per record ambiguo, cluster storici mai fusi, vincolo clean-source.
 */
class ClusterAssignmentPlannerTest {

	private static final Party A_CLEAN = new Party("A", true);
	private static final Party B_DIRTY = new Party("B", false);
	private static final Party C_DIRTY = new Party("C", false);

	private static BitSet bits(int from, int toInclusive) {
		final BitSet bitSet = new BitSet();
		bitSet.set(from, toInclusive + 1);
		return bitSet;
	}

	private static Record record(String id, Party party, BitSet bitSet) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setParty(party);
		record.addQidAttribute(new BitSetAttribute(bitSet));
		return record;
	}

	private static Cluster cluster(int id, Record... members) {
		final Cluster cluster = new Cluster();
		cluster.setId(id);
		for (final Record member : members) {
			cluster.addRecord(member);
		}
		return cluster;
	}

	private static LinkedPair<Record> pair(Record left, Record right) {
		return new LinkedPair<>(left, right);
	}

	private static List<Record> all(Record... records) {
		return new ArrayList<>(List.of(records));
	}

	/** Query = bit 0..9; 0.8 con {0..7}; 0.5 con {0..5,10,11}. */
	private static final BitSet QUERY = bits(0, 9);
	private static final BitSet CLOSE = bits(0, 7);

	private static BitSet farther() {
		final BitSet bitSet = bits(0, 5);
		bitSet.set(10);
		bitSet.set(11);
		return bitSet;
	}

	@Test
	void bridgeRecordGoesToOnlyTheBestCluster() {
		final Record r = record("R", B_DIRTY, QUERY);
		final Record x1 = record("X1", B_DIRTY, CLOSE);
		final Record y1 = record("Y1", C_DIRTY, farther());
		final Cluster x = cluster(1, x1);
		final Cluster y = cluster(2, y1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r, x1), pair(r, y1)), all(r, x1, y1));

		assertEquals(Set.of(x), plan.extensions.keySet());
		assertEquals(List.of(r), plan.extensions.get(x));
		assertTrue(plan.newComponents.isEmpty());
		assertEquals(Set.of(x, y), plan.historicClusters);
		assertEquals(1, x.getRecords().size());
		assertEquals(1, y.getRecords().size());
	}

	@Test
	void tieOnMaxSimilarityIsBrokenByAverageSimilarity() {
		final Record r = record("R", B_DIRTY, QUERY);
		final Record x1 = record("X1", B_DIRTY, CLOSE);
		final Record x2 = record("X2", B_DIRTY, bits(20, 29));
		final Record y1 = record("Y1", C_DIRTY, CLOSE);
		final Cluster x = cluster(1, x1, x2);
		final Cluster y = cluster(2, y1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r, x1), pair(r, y1)), all(r, x1, x2, y1));

		assertEquals(Set.of(y), plan.extensions.keySet());
	}

	@Test
	void tieOnSimilarityIsBrokenBySharedBlockingKeysBeforeClusterId() {
		final Record r = record("R", B_DIRTY, QUERY);
		r.addBlockingKey(new BlockingKeyAttribute(0, "k0"));
		r.addBlockingKey(new BlockingKeyAttribute(1, "k1"));
		final Record x1 = record("X1", B_DIRTY, CLOSE);
		final Record y1 = record("Y1", C_DIRTY, CLOSE);
		final Cluster x = cluster(5, x1);
		final Cluster y = cluster(2, y1);
		x.setBlockingKeys(new HashSet<>(Set.of(new BlockingKeyAttribute(0, "k0"), new BlockingKeyAttribute(1, "k1"))));
		y.setBlockingKeys(new HashSet<>(Set.of(new BlockingKeyAttribute(0, "k0"))));

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r, x1), pair(r, y1)), all(r, x1, y1));

		assertEquals(Set.of(x), plan.extensions.keySet());
	}

	@Test
	void fullTieIsBrokenByLowestClusterId() {
		final Record r = record("R", B_DIRTY, QUERY);
		final Record x1 = record("X1", B_DIRTY, CLOSE);
		final Record y1 = record("Y1", C_DIRTY, CLOSE);
		final Cluster x = cluster(5, x1);
		final Cluster y = cluster(2, y1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r, x1), pair(r, y1)), all(r, x1, y1));

		assertEquals(Set.of(y), plan.extensions.keySet());
		assertTrue(x.getRecords().contains(x1));
	}

	@Test
	void cleanSourceConflictFallsBackToNextCandidate() {
		final Record r = record("R", A_CLEAN, QUERY);
		final Record a1 = record("A1", A_CLEAN, bits(40, 49));
		final Record b1 = record("B1", B_DIRTY, CLOSE);
		final Record b2 = record("B2", B_DIRTY, farther());
		final Cluster x = cluster(1, a1, b1);
		final Cluster y = cluster(2, b2);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r, b1), pair(r, b2)), all(r, a1, b1, b2));

		assertEquals(Set.of(y), plan.extensions.keySet());
		assertEquals(List.of(r), plan.extensions.get(y));
		assertTrue(plan.newComponents.isEmpty());
	}

	@Test
	void noEligibleCandidateBecomesNewComponent() {
		final Record r = record("R", A_CLEAN, QUERY);
		final Record a1 = record("A1", A_CLEAN, bits(40, 49));
		final Record b1 = record("B1", B_DIRTY, CLOSE);
		cluster(1, a1, b1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r, b1)), all(r, a1, b1));

		assertTrue(plan.extensions.isEmpty());
		assertEquals(1, plan.newComponents.size());
		assertEquals(List.of(r), plan.newComponents.get(0));
	}

	@Test
	void twoFreshRecordsOfTheSameCleanPartyDoNotShareACluster() {
		final Record r1 = record("R1", A_CLEAN, QUERY);
		final Record r2 = record("R2", A_CLEAN, bits(0, 5)); // 0.75 con CLOSE, R1 0.8
		final Record b1 = record("B1", B_DIRTY, CLOSE);
		final Cluster x = cluster(1, b1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r1, b1), pair(r2, b1)), all(r1, r2, b1));

		assertEquals(Set.of(x), plan.extensions.keySet());
		assertEquals(List.of(r1), plan.extensions.get(x));
		assertEquals(1, plan.newComponents.size());
		assertEquals(List.of(r2), plan.newComponents.get(0));
	}

	@Test
	void dirtyPartyRecordsAreNotConstrained() {
		final Record r1 = record("R1", B_DIRTY, QUERY);
		final Record r2 = record("R2", B_DIRTY, bits(0, 6));
		final Record b1 = record("B1", B_DIRTY, CLOSE);
		final Cluster x = cluster(1, b1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r1, b1), pair(r2, b1)), all(r1, r2, b1));

		assertEquals(Set.of(x), plan.extensions.keySet());
		assertEquals(2, plan.extensions.get(x).size());
		assertTrue(plan.newComponents.isEmpty());
	}

	@Test
	void historicToHistoricLinksNeverMergeClusters() {
		final Record x1 = record("X1", B_DIRTY, CLOSE);
		final Record y1 = record("Y1", C_DIRTY, CLOSE);
		final Cluster x = cluster(1, x1);
		final Cluster y = cluster(2, y1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(x1, y1)), all(x1, y1));

		assertTrue(plan.extensions.isEmpty());
		assertTrue(plan.newComponents.isEmpty());
		assertEquals(Set.of(x, y), plan.historicClusters);
	}

	@Test
	void freshGroupIsAssignedAsAUnit() {
		final Record r1 = record("R1", B_DIRTY, QUERY);
		final Record r2 = record("R2", B_DIRTY, QUERY);
		final Record x1 = record("X1", C_DIRTY, CLOSE);
		final Record y1 = record("Y1", C_DIRTY, farther());
		final Cluster x = cluster(1, x1);
		cluster(2, y1);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(r1, r2), pair(r1, x1), pair(r2, y1)), all(r1, r2, x1, y1));

		assertEquals(Set.of(x), plan.extensions.keySet());
		assertEquals(2, plan.extensions.get(x).size());
		assertTrue(plan.newComponents.isEmpty());
	}

	@Test
	void withoutHistoryNewComponentsAreTheConnectedComponentsOfThePairs() {
		final Record a = record("a", A_CLEAN, QUERY);
		final Record b = record("b", B_DIRTY, QUERY);
		final Record c = record("c", A_CLEAN, QUERY);
		final Record d = record("d", B_DIRTY, QUERY);
		final Record e = record("e", C_DIRTY, QUERY);

		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(
				List.of(pair(a, b), pair(c, d)), all(a, b, c, d, e));

		assertTrue(plan.extensions.isEmpty());
		assertTrue(plan.historicClusters.isEmpty());
		assertEquals(3, plan.newComponents.size());
		final List<Integer> sizes = new ArrayList<>();
		for (final List<Record> component : plan.newComponents) {
			sizes.add(component.size());
		}
		sizes.sort(Integer::compare);
		assertEquals(List.of(1, 2, 2), sizes);
	}

	@Test
	void keepPairsWithinFinalClustersDropsPairsSplitAcrossClusters() {
		final Record x1 = record("X1", B_DIRTY, CLOSE);
		final Record x2 = record("X2", B_DIRTY, CLOSE);
		final Record y1 = record("Y1", C_DIRTY, CLOSE);
		cluster(1, x1, x2);
		cluster(2, y1);

		final List<LinkedPair<Record>> kept = MultiSourceLinkage
				.keepPairsWithinFinalClusters(List.of(pair(x1, x2), pair(x2, y1)));

		assertEquals(1, kept.size());
		assertEquals(x1, kept.get(0).getLeftRecord());
	}
}
