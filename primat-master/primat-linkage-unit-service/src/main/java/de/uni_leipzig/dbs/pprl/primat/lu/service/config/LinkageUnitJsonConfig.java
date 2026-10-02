/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import com.google.gson.JsonElement;

/**
 * DTO grezzo 1:1 del JSON di configurazione della Linkage Unit, popolato da
 * Gson per riflessione (nessuna validazione qui, tutta in
 * {@link LinkageUnitConfigLoader}). Mirror di {@code DataOwnerJsonConfig}.
 * Nessun campo {@code parties}/{@code rbfSize}: dal 2026-10-02 sono puramente
 * runtime, spinti dalla SMU ad ogni run via {@code LuConfigPush} (si veda
 * {@code LinkageUnitConfigLoader#resolvePartyRoster}/{@code #validateRbfSize}),
 * mai letti dal JSON locale.
 */
public class LinkageUnitJsonConfig {

	private String mqttBrokerUrl;
	private ClusteringMethod clusteringMethod;
	private JsonElement similarityThreshold;
	private AutoThresholdJsonConfig autoThreshold;
	private RangeJsonConfig range;
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
	private Boolean debug;

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	public ClusteringMethod getClusteringMethod() {
		return clusteringMethod;
	}

	/** @return un numero in (0,1] o una stringa {@code auto}/{@code auto_precision}/{@code auto_recall}/{@code range} (validato dal loader). */
	public JsonElement getSimilarityThreshold() {
		return similarityThreshold;
	}

	public AutoThresholdJsonConfig getAutoThreshold() {
		return autoThreshold;
	}

	/** @return sezione {@code range} (ammessa solo con {@code similarityThreshold: "range"}), {@code null} se assente. */
	public RangeJsonConfig getRange() {
		return range;
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

	/** @return {@code true} abilita l'output diagnostico (istogramma delle similarita'), {@code null} se omesso (= {@code false}). */
	public Boolean getDebug() {
		return debug;
	}
}
