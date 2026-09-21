/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import java.util.List;

/**
 * DTO grezzo 1:1 del JSON di configurazione della Linkage Unit, popolato da
 * Gson per riflessione (nessuna validazione qui, tutta in
 * {@link LinkageUnitConfigLoader}). Mirror di {@code DataOwnerJsonConfig}.
 */
public class LinkageUnitJsonConfig {

	private List<PartyJsonConfig> parties;
	private ClusteringMethod clusteringMethod;
	private Double similarityThreshold;
	private Integer rbfSize;
	private BlockingJsonConfig blocking;
	private MqttJsonConfig mqtt;
	private ClusterJsonConfig cluster;
	private PersistenceJsonConfig persistence;
	private DatabaseJsonConfig database;
	private CenterClusteringJsonConfig centerClustering;
	private ApJsonConfig mscdAp;
	private MclJsonConfig mcl;
	private GlobalGreedyJsonConfig globalGreedy;
	private ClipJsonConfig clip;

	public List<PartyJsonConfig> getParties() {
		return parties;
	}

	public ClusteringMethod getClusteringMethod() {
		return clusteringMethod;
	}

	public Double getSimilarityThreshold() {
		return similarityThreshold;
	}

	/** @return dimensione attesa dell'RBF in bit (informativa, es. per verificare la coerenza con l'XOR-Folding lato Data Owner), {@code null} se non impostata. */
	public Integer getRbfSize() {
		return rbfSize;
	}

	public BlockingJsonConfig getBlocking() {
		return blocking;
	}

	public MqttJsonConfig getMqtt() {
		return mqtt;
	}

	public ClusterJsonConfig getCluster() {
		return cluster;
	}

	public PersistenceJsonConfig getPersistence() {
		return persistence;
	}

	public DatabaseJsonConfig getDatabase() {
		return database;
	}

	public CenterClusteringJsonConfig getCenterClustering() {
		return centerClustering;
	}

	public ApJsonConfig getMscdAp() {
		return mscdAp;
	}

	public MclJsonConfig getMcl() {
		return mcl;
	}

	public GlobalGreedyJsonConfig getGlobalGreedy() {
		return globalGreedy;
	}

	public ClipJsonConfig getClip() {
		return clip;
	}
}
