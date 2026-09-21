/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;

/**
 * Costruisce la Link Table finale multi-sorgente a partire dalle coppie
 * linkate raccolte da tutte le {@code PartyPair} di un run (2 o più party).
 * Realizza la chiusura transitiva via union-find sugli oggetti
 * {@link Record}: due record collegati anche indirettamente (es. A-B e B-C)
 * finiscono nello stesso {@link Cluster}. Non riusa
 * {@code SimpleClusterBuilder} di {@code primat-linkage-unit} perché quella
 * classe esistente ha un difetto noto (il {@code Set<Cluster>} restituito non
 * viene mai popolato) e il progetto non modifica classi esistenti dei moduli
 * originali.
 */
public final class LinkTableBuilder {

	private LinkTableBuilder() {
	}

	/**
	 * @param linkedPairs coppie linkate aggregate da tutte le {@code PartyPair}
	 *                    del run, per la strategia di postprocessing scelta
	 * @param allRecords  tutti i record del run (anche quelli senza alcun match,
	 *                    che formano cluster singoletto)
	 * @return un cluster per ogni componente connessa del grafo indotto dalle
	 *         coppie linkate
	 */
	public static Set<Cluster> build(Collection<LinkedPair<Record>> linkedPairs, Collection<Record> allRecords) {
		final Map<Record, Record> parent = new IdentityHashMap<>();
		for (final Record record : allRecords) {
			parent.put(record, record);
		}
		for (final LinkedPair<Record> pair : linkedPairs) {
			union(parent, pair.getLeftRecord(), pair.getRight());
		}

		final Map<Record, List<Record>> componentsByRoot = new IdentityHashMap<>();
		for (final Record record : allRecords) {
			final Record root = find(parent, record);
			componentsByRoot.computeIfAbsent(root, r -> new ArrayList<>()).add(record);
		}

		final Set<Cluster> linkTable = new HashSet<>();
		int clusterId = 0;
		for (final List<Record> component : componentsByRoot.values()) {
			final Cluster cluster = new Cluster();
			cluster.setId(clusterId++);
			for (final Record record : component) {
				cluster.addRecord(record);
			}
			linkTable.add(cluster);
		}
		return linkTable;
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
