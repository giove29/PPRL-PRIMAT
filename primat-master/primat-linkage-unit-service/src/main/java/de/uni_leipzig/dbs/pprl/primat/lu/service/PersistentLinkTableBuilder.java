/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.database.DbConnection;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ProgressListener;

/**
 * Variante persistente di {@link LinkTableBuilder}: oltre a chiudere
 * transitivamente i {@code linkedPairs} di questo run via union-find, seeda
 * l'union-find con l'appartenenza ai {@link Cluster} già persistiti (cosi' un
 * link stabilito in un run precedente non può mai essere smontato
 * silenziosamente da questo run — i cluster possono solo fondersi, mai
 * dividersi) e riconcilia ogni componente connessa risultante con lo storico
 * su database tramite {@link DbConnection}. Non modifica
 * {@link LinkTableBuilder}, che resta il path non persistente usato dalla
 * strategia MCL (non persistente per design, vedi {@code MultiSourceLinkage#runMcl}).
 */
public final class PersistentLinkTableBuilder {

	private PersistentLinkTableBuilder() {
	}

	/**
	 * @param linkedPairs    coppie linkate aggregate da tutte le
	 *                       {@code PartyPair} di questo run (solo strategia
	 *                       MSCD-AP)
	 * @param allRecords     tutti i record di questo run: sia lo storico già
	 *                       persistito (con {@link Record#getCluster()}
	 *                       valorizzato) sia i record freschi mai visti prima
	 *                       ({@code getCluster() == null})
	 * @param clusterFactory factory usata per costruire eventuali cluster
	 *                       nuovi
	 * @param dbConnection   connessione al DB dedicato alla strategia che sta
	 *                       chiamando (Center Clustering / MSCD-AP / Global Greedy / CLIP
	 *                       hanno ciascuna il proprio, vedi {@code LinkageUnitConfig})
	 * @return un cluster per ogni componente connessa, riconciliato con lo
	 *         storico persistito (nuovo, esteso o risultato di una fusione)
	 */
	public static Set<Cluster> build(Collection<LinkedPair<Record>> linkedPairs, Collection<Record> allRecords,
			ClusterFactory clusterFactory, DbConnection dbConnection, ProgressListener progress) {
		final Map<Record, Record> parent = new IdentityHashMap<>();
		for (final Record record : allRecords) {
			parent.put(record, record);
		}

		seedWithExistingClusters(parent, allRecords);

		for (final LinkedPair<Record> pair : linkedPairs) {
			union(parent, pair.getLeftRecord(), pair.getRight());
		}

		final Map<Record, List<Record>> componentsByRoot = new IdentityHashMap<>();
		for (final Record record : allRecords) {
			final Record root = find(parent, record);
			componentsByRoot.computeIfAbsent(root, r -> new ArrayList<>()).add(record);
		}

		final Set<Cluster> linkTable = new HashSet<>();
		final long totalComponents = componentsByRoot.size();
		final List<List<Record>> newComponents = new ArrayList<>();
		long reconciled = 0;
		for (final List<Record> component : componentsByRoot.values()) {
			if (hasExistingCluster(component)) {
				linkTable.add(reconcile(component, clusterFactory, dbConnection));
				progress.update(++reconciled, totalComponents);
			}
			else {
				newComponents.add(component);
			}
		}
		final long alreadyDone = reconciled;
		linkTable.addAll(dbConnection.persistNewClusters(clusterFactory, newComponents,
			(done, total) -> progress.update(alreadyDone + done, totalComponents)));
		return linkTable;
	}

	private static void seedWithExistingClusters(Map<Record, Record> parent, Collection<Record> allRecords) {
		final Map<Integer, List<Record>> byExistingCluster = new HashMap<>();
		for (final Record record : allRecords) {
			if (record.getCluster() != null) {
				byExistingCluster.computeIfAbsent(record.getCluster().getId(), id -> new ArrayList<>()).add(record);
			}
		}
		for (final List<Record> members : byExistingCluster.values()) {
			for (int i = 1; i < members.size(); i++) {
				union(parent, members.get(0), members.get(i));
			}
		}
	}

	/**
	 * Riconcilia una componente connessa con lo storico persistito, a seconda
	 * di quanti {@link Cluster} distinti sono già referenziati dai suoi
	 * membri: nessuno -> nuovo cluster; uno -> lo estende; due o più -> li
	 * fonde (target = id più basso).
	 */
	private static Cluster reconcile(List<Record> component, ClusterFactory clusterFactory, DbConnection dbConnection) {
		final Set<Cluster> existingClusters = new HashSet<>();
		for (final Record record : component) {
			if (record.getCluster() != null) {
				existingClusters.add(record.getCluster());
			}
		}

		if (existingClusters.isEmpty()) {
			return dbConnection.persistNewCluster(clusterFactory, component);
		}

		if (existingClusters.size() == 1) {
			final Cluster cluster = existingClusters.iterator().next();
			dbConnection.extendCluster(cluster, newRecordsOf(component));
			return cluster;
		}

		final List<Cluster> sortedByIdAscending = new ArrayList<>(existingClusters);
		sortedByIdAscending.sort(Comparator.comparingInt(Cluster::getId));
		final Cluster target = sortedByIdAscending.get(0);
		final List<Cluster> losers = sortedByIdAscending.subList(1, sortedByIdAscending.size());
		return dbConnection.mergeClusters(target, losers, newRecordsOf(component));
	}

	private static boolean hasExistingCluster(List<Record> component) {
		for (final Record record : component) {
			if (record.getCluster() != null) {
				return true;
			}
		}
		return false;
	}

	private static List<Record> newRecordsOf(List<Record> component) {
		final List<Record> newRecords = new ArrayList<>();
		for (final Record record : component) {
			if (record.getCluster() == null) {
				newRecords.add(record);
			}
		}
		return newRecords;
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
