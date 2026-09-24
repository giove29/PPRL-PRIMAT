/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.Collection;
import java.util.HashSet;
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
 * Variante persistente di {@link LinkTableBuilder}: riconcilia i record
 * freschi di questo run con i {@link Cluster} già persistiti su database
 * tramite {@link DbConnection}. La decisione (quale cluster, se esiste) è in
 * {@link ClusterAssignmentPlanner}: i cluster storici sono congelati (mai
 * fusi né divisi da un run), un gruppo di record freschi entra in un solo
 * cluster (il candidato con il punteggio migliore che rispetta il vincolo
 * clean-source) oppure ne forma uno nuovo. Non modifica
 * {@link LinkTableBuilder}, che resta il path non persistente usato dalla
 * strategia MCL (non persistente per design, vedi {@code MultiSourceLinkage#runMcl}).
 */
public final class PersistentLinkTableBuilder {

	private PersistentLinkTableBuilder() {
	}

	/**
	 * @param linkedPairs    coppie linkate dall'algoritmo di clustering di
	 *                       questo run (MSCD-AP, Center Clustering, Global
	 *                       Greedy o CLIP)
	 * @param allRecords     tutti i record di questo run: sia lo storico già
	 *                       persistito (con {@link Record#getCluster()}
	 *                       valorizzato) sia i record freschi mai visti prima
	 *                       ({@code getCluster() == null})
	 * @param clusterFactory factory usata per costruire eventuali cluster
	 *                       nuovi
	 * @param dbConnection   connessione al DB dedicato alla strategia che sta
	 *                       chiamando (Center Clustering / MSCD-AP / Global Greedy / CLIP
	 *                       hanno ciascuna il proprio, vedi {@code LinkageUnitConfig})
	 * @return i cluster storici referenziati dal run (invariati o estesi) più
	 *         quelli nuovi
	 */
	public static Set<Cluster> build(Collection<LinkedPair<Record>> linkedPairs, Collection<Record> allRecords,
			ClusterFactory clusterFactory, DbConnection dbConnection, ProgressListener progress) {
		final ClusterAssignmentPlanner.Plan plan = ClusterAssignmentPlanner.plan(linkedPairs, allRecords);

		final Set<Cluster> linkTable = new HashSet<>(plan.historicClusters);
		final long totalSteps = plan.extensions.size() + plan.newComponents.size();
		long done = 0;
		for (final Map.Entry<Cluster, List<Record>> extension : plan.extensions.entrySet()) {
			dbConnection.extendCluster(extension.getKey(), extension.getValue());
			progress.update(++done, totalSteps);
		}
		final long alreadyDone = done;
		linkTable.addAll(dbConnection.persistNewClusters(clusterFactory, plan.newComponents,
			(d, total) -> progress.update(alreadyDone + d, totalSteps)));
		return linkTable;
	}
}
