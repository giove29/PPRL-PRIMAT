/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.binary.BinarySimilarity;

/**
 * Logica pura (nessun DB) con cui {@link PersistentLinkTableBuilder} decide
 * dove finiscono i record freschi di un run rispetto ai {@link Cluster} già
 * persistiti.
 * <p>
 * Regole:
 * <ul>
 * <li>i cluster storici sono <b>congelati</b>: non si fondono mai per effetto
 * di un run e i {@code linkedPairs} storico-storico vengono ignorati;</li>
 * <li>un gruppo di record freschi ({@code S}: componente connessa dei soli
 * record freschi tramite i {@code linkedPairs} fresco-fresco) entra in UN solo
 * cluster storico, scelto tra quelli collegati a {@code S} dall'algoritmo di
 * clustering del run;</li>
 * <li>tra più candidati vince il miglior {@link Score}: max Jaccard, poi
 * media Jaccard, poi blocking key in comune, poi id cluster più basso;</li>
 * <li>vincolo clean-source: un record di una party {@code duplicateFree}
 * non può entrare in un cluster che contiene già un record della stessa party
 * (storico o assegnato in questo run); se il candidato migliore viola il
 * vincolo si prova il successivo, se nessuno è ammissibile nasce un cluster
 * nuovo.</li>
 * </ul>
 * Senza storico (primo run) i gruppi {@code S} sono le componenti connesse dei
 * {@code linkedPairs}, cioè lo stesso risultato di {@link LinkTableBuilder}.
 */
final class ClusterAssignmentPlanner {

	/** Due valori di similarità entro questa soglia sono considerati pari. */
	private static final double SIMILARITY_EPSILON = 1e-9;

	private ClusterAssignmentPlanner() {
	}

	/** Esito della pianificazione, da applicare poi al DB. */
	static final class Plan {

		/** Record freschi da aggiungere a ciascun cluster storico esistente. */
		final Map<Cluster, List<Record>> extensions = new LinkedHashMap<>();

		/** Gruppi di record freschi che formano un cluster nuovo. */
		final List<List<Record>> newComponents = new ArrayList<>();

		/** Tutti i cluster storici referenziati dai record in input (invariati o estesi). */
		final Set<Cluster> historicClusters = new LinkedHashSet<>();
	}

	/** Punteggio di un candidato, confrontato nell'ordine dei campi. */
	static final class Score {

		final double maxSimilarity;
		final double averageSimilarity;
		final int sharedBlockingKeys;
		final int clusterId;

		Score(double maxSimilarity, double averageSimilarity, int sharedBlockingKeys, int clusterId) {
			this.maxSimilarity = maxSimilarity;
			this.averageSimilarity = averageSimilarity;
			this.sharedBlockingKeys = sharedBlockingKeys;
			this.clusterId = clusterId;
		}

		/** Ordine "migliore prima": max desc, media desc, chiavi comuni desc, id cluster asc. */
		static final Comparator<Score> BEST_FIRST = (a, b) -> {
			int c = compareDescending(a.maxSimilarity, b.maxSimilarity);
			if (c != 0) {
				return c;
			}
			c = compareDescending(a.averageSimilarity, b.averageSimilarity);
			if (c != 0) {
				return c;
			}
			c = Integer.compare(b.sharedBlockingKeys, a.sharedBlockingKeys);
			if (c != 0) {
				return c;
			}
			return Integer.compare(a.clusterId, b.clusterId);
		};

		private static int compareDescending(double a, double b) {
			if (Math.abs(a - b) < SIMILARITY_EPSILON) {
				return 0;
			}
			return a > b ? -1 : 1;
		}
	}

	private static final class Candidate {

		final int group;
		final Cluster cluster;
		final Score score;

		Candidate(int group, Cluster cluster, Score score) {
			this.group = group;
			this.cluster = cluster;
			this.score = score;
		}
	}

	static Plan plan(Collection<LinkedPair<Record>> linkedPairs, Collection<Record> allRecords) {
		final Plan plan = new Plan();

		final Map<Record, Record> parent = new IdentityHashMap<>();
		final List<Record> fresh = new ArrayList<>();
		for (final Record record : allRecords) {
			if (record.getCluster() == null) {
				parent.put(record, record);
				fresh.add(record);
			}
			else {
				plan.historicClusters.add(record.getCluster());
			}
		}

		// Un solo passaggio: fresco-fresco unisce, fresco-storico e' evidenza per
		// il candidato, storico-storico e' ignorato (cluster congelati).
		final List<Record[]> evidence = new ArrayList<>();
		for (final LinkedPair<Record> pair : linkedPairs) {
			final Record left = pair.getLeftRecord();
			final Record right = pair.getRight();
			final boolean leftFresh = parent.containsKey(left);
			final boolean rightFresh = parent.containsKey(right);
			if (leftFresh && rightFresh) {
				union(parent, left, right);
			}
			else if (leftFresh && right.getCluster() != null) {
				evidence.add(new Record[] { left, right });
			}
			else if (rightFresh && left.getCluster() != null) {
				evidence.add(new Record[] { right, left });
			}
		}

		final Map<Record, Integer> groupOfRoot = new IdentityHashMap<>();
		final List<List<Record>> groups = new ArrayList<>();
		for (final Record record : fresh) {
			final Record root = find(parent, record);
			Integer group = groupOfRoot.get(root);
			if (group == null) {
				group = groups.size();
				groupOfRoot.put(root, group);
				groups.add(new ArrayList<>());
			}
			groups.get(group).add(record);
		}

		final Map<Integer, Set<Cluster>> candidateClusters = new LinkedHashMap<>();
		for (final Record[] freshAndHistoric : evidence) {
			final int group = groupOfRoot.get(find(parent, freshAndHistoric[0]));
			candidateClusters.computeIfAbsent(group, g -> new LinkedHashSet<>()).add(freshAndHistoric[1].getCluster());
		}

		final List<Candidate> candidates = new ArrayList<>();
		for (final Map.Entry<Integer, Set<Cluster>> entry : candidateClusters.entrySet()) {
			for (final Cluster cluster : entry.getValue()) {
				candidates.add(new Candidate(entry.getKey(), cluster, score(groups.get(entry.getKey()), cluster)));
			}
		}
		candidates.sort(Comparator.<Candidate, Score>comparing(c -> c.score, Score.BEST_FIRST)
				.thenComparingInt(c -> c.group));

		final Map<Cluster, Set<String>> cleanPartiesIn = new HashMap<>();
		final boolean[] assigned = new boolean[groups.size()];
		for (final Candidate candidate : candidates) {
			if (assigned[candidate.group]) {
				continue;
			}
			final List<Record> group = groups.get(candidate.group);
			final Set<String> occupied = cleanPartiesIn.computeIfAbsent(candidate.cluster,
					ClusterAssignmentPlanner::cleanPartiesOf);
			if (conflicts(group, occupied)) {
				continue;
			}
			for (final Record record : group) {
				if (record.getParty().isDuplicateFree()) {
					occupied.add(record.getParty().getName());
				}
			}
			assigned[candidate.group] = true;
			plan.extensions.computeIfAbsent(candidate.cluster, c -> new ArrayList<>()).addAll(group);
		}

		for (int group = 0; group < groups.size(); group++) {
			if (!assigned[group]) {
				plan.newComponents.add(groups.get(group));
			}
		}
		return plan;
	}

	/** Punteggio di {@code group} rispetto a {@code cluster}, calcolato esattamente su tutte le coppie. */
	static Score score(List<Record> group, Cluster cluster) {
		double max = 0d;
		double sum = 0d;
		int pairs = 0;
		for (final Record fresh : group) {
			for (final Record member : cluster.getRecords()) {
				final double similarity = jaccard(fresh, member);
				max = Math.max(max, similarity);
				sum += similarity;
				pairs++;
			}
		}
		final double average = pairs == 0 ? 0d : sum / pairs;

		int shared = 0;
		final Set<BlockingKeyAttribute> clusterKeys = cluster.getBlockingKeys();
		if (clusterKeys != null) {
			final Set<BlockingKeyAttribute> groupKeys = new HashSet<>();
			for (final Record fresh : group) {
				groupKeys.addAll(fresh.getBlockingKeys());
			}
			for (final BlockingKeyAttribute key : groupKeys) {
				if (clusterKeys.contains(key)) {
					shared++;
				}
			}
		}
		return new Score(max, average, shared, cluster.getId());
	}

	private static double jaccard(Record a, Record b) {
		final List<BitSetAttribute> left = a.getBitSetAttributes();
		final List<BitSetAttribute> right = b.getBitSetAttributes();
		if (left.isEmpty() || right.isEmpty()) {
			return 0d;
		}
		final BitSet leftBits = left.get(0).getValue();
		final BitSet rightBits = right.get(0).getValue();
		if (leftBits == null || rightBits == null) {
			return 0d;
		}
		return BinarySimilarity.JACCARD_SIMILARITY.calculateSimilarity(leftBits, rightBits);
	}

	private static Set<String> cleanPartiesOf(Cluster cluster) {
		final Set<String> parties = new HashSet<>();
		for (final Record member : cluster.getRecords()) {
			if (member.getParty().isDuplicateFree()) {
				parties.add(member.getParty().getName());
			}
		}
		return parties;
	}

	private static boolean conflicts(List<Record> group, Set<String> occupiedCleanParties) {
		for (final Record record : group) {
			if (record.getParty().isDuplicateFree() && occupiedCleanParties.contains(record.getParty().getName())) {
				return true;
			}
		}
		return false;
	}

	private static Record find(Map<Record, Record> parent, Record record) {
		Record root = record;
		while (parent.get(root) != root) {
			root = parent.get(root);
		}
		Record current = record;
		while (parent.get(current) != root) {
			final Record next = parent.get(current);
			parent.put(current, root);
			current = next;
		}
		return root;
	}

	private static void union(Map<Record, Record> parent, Record a, Record b) {
		final Record rootA = find(parent, a);
		final Record rootB = find(parent, b);
		if (rootA != rootB) {
			parent.put(rootA, rootB);
		}
	}
}
