/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.lu.database.DbConnection;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures.CenterClusteringConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.data_structures.ClipConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.global_greedy.data_structures.GlobalGreedyConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.ClusteringMethod;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.LinkageUnitConfigLoader;

/**
 * Configurazione completa di un run della Linkage Unit: caricata da un file
 * JSON tramite {@link LinkageUnitConfigLoader}, sostituisce i campi
 * hardcoded che c'erano prima in {@code LinkageUnitOrchestrator}. E' la sola
 * fonte di verità su quale delle 4 strategie di clustering eseguire — niente
 * più auto-routing basato su dirty/clean (vedi {@code
 * LinkageUnitOrchestrator#anyPartyDuplicateFree}, usato solo in validazione).
 */
public class LinkageUnitConfig {

	private final List<Party> parties;
	private final ClusteringMethod clusteringMethod;
	private final double similarityThreshold;
	private final Integer rbfSize;
	private final int lshKeySize;
	private final int lshKeys;
	private final int lshValueRange;
	private final long lshSeed;
	private final String mqttBrokerUrl;
	private final long rbfCollectionTimeoutSeconds;
	private final long rbfRepublishIntervalSeconds;
	private final ClusterFactory clusterFactory;
	private final boolean persistenceEnabled;
	private final String csvOutputPath;
	private final CenterClusteringConfig centerClusteringConfig;
	private final ApConfig apConfig;
	private final MclConfig mclConfig;
	private final GlobalGreedyConfig globalGreedyConfig;
	private final ClipConfig clipConfig;
	private final String dbPersistenceUnitName;
	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;
	private DbConnection dbConnection;

	/**
	 * Costruito esclusivamente da {@link LinkageUnitConfigLoader} dopo la
	 * validazione del JSON: nessun controllo aggiuntivo viene fatto qui,
	 * questa classe e' altrimenti un semplice contenitore immutabile.
	 *
	 * @param dbPersistenceUnitName nome della persistence-unit dedicata alla
	 *                              strategia scelta, {@code null} se
	 *                              {@code clusteringMethod == MCL}
	 * @param dbUrl                 url JDBC del DB dedicato, {@code null} se MCL
	 * @param dbUser                utente del DB dedicato, {@code null} se MCL
	 * @param dbPassword            password del DB dedicato, {@code null} se MCL
	 */
	public LinkageUnitConfig(List<Party> parties, ClusteringMethod clusteringMethod, double similarityThreshold,
			Integer rbfSize, int lshKeySize, int lshKeys, int lshValueRange, long lshSeed, String mqttBrokerUrl,
			long rbfCollectionTimeoutSeconds, long rbfRepublishIntervalSeconds, ClusterFactory clusterFactory,
			boolean persistenceEnabled, String csvOutputPath, CenterClusteringConfig centerClusteringConfig,
			ApConfig apConfig, MclConfig mclConfig, GlobalGreedyConfig globalGreedyConfig, ClipConfig clipConfig,
			String dbPersistenceUnitName, String dbUrl, String dbUser, String dbPassword) {
		this.parties = parties;
		this.clusteringMethod = clusteringMethod;
		this.similarityThreshold = similarityThreshold;
		this.rbfSize = rbfSize;
		this.lshKeySize = lshKeySize;
		this.lshKeys = lshKeys;
		this.lshValueRange = lshValueRange;
		this.lshSeed = lshSeed;
		this.mqttBrokerUrl = mqttBrokerUrl;
		this.rbfCollectionTimeoutSeconds = rbfCollectionTimeoutSeconds;
		this.rbfRepublishIntervalSeconds = rbfRepublishIntervalSeconds;
		this.clusterFactory = clusterFactory;
		this.persistenceEnabled = persistenceEnabled;
		this.csvOutputPath = csvOutputPath;
		this.centerClusteringConfig = centerClusteringConfig;
		this.apConfig = apConfig;
		this.mclConfig = mclConfig;
		this.globalGreedyConfig = globalGreedyConfig;
		this.clipConfig = clipConfig;
		this.dbPersistenceUnitName = dbPersistenceUnitName;
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
	}

	public List<Party> getParties() {
		return parties;
	}

	public ClusteringMethod getClusteringMethod() {
		return clusteringMethod;
	}

	public double getSimilarityThreshold() {
		return similarityThreshold;
	}

	/** @return dimensione attesa dell'RBF in bit (informativa), {@code null} se non impostata in JSON. */
	public Integer getRbfSize() {
		return rbfSize;
	}

	public int getLshKeySize() {
		return lshKeySize;
	}

	public int getLshKeys() {
		return lshKeys;
	}

	public int getLshValueRange() {
		return lshValueRange;
	}

	public long getLshSeed() {
		return lshSeed;
	}

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	public long getRbfCollectionTimeoutSeconds() {
		return rbfCollectionTimeoutSeconds;
	}

	public long getRbfRepublishIntervalSeconds() {
		return rbfRepublishIntervalSeconds;
	}

	public ClusterFactory getClusterFactory() {
		return clusterFactory;
	}

	/** @return {@code true} se la Link Table va persistita su Postgres, {@code false} se va esportata su CSV. */
	public boolean isPersistenceEnabled() {
		return persistenceEnabled;
	}

	/** @return percorso del CSV di export, rilevante solo se {@code !isPersistenceEnabled()}. */
	public String getCsvOutputPath() {
		return csvOutputPath;
	}

	public CenterClusteringConfig getCenterClusteringConfig() {
		return centerClusteringConfig;
	}

	public ApConfig getApConfig() {
		return apConfig;
	}

	public MclConfig getMclConfig() {
		return mclConfig;
	}

	public GlobalGreedyConfig getGlobalGreedyConfig() {
		return globalGreedyConfig;
	}

	public ClipConfig getClipConfig() {
		return clipConfig;
	}

	/**
	 * Costruisce (e mette in cache, una sola volta) la connessione al DB
	 * dedicato alla strategia scelta, {@code null} se {@code clusteringMethod
	 * == MCL}. Deliberatamente lazy invece che costruita dal loader: aprire
	 * l'{@code EntityManagerFactory} è un'operazione che tocca la rete
	 * (Hibernate con {@code hbm2ddl.auto=update} interroga subito lo schema
	 * del DB), quindi il solo caricamento/validazione della config JSON
	 * ({@code LinkageUnitConfigLoaderTest}) resta un test puro senza Postgres.
	 * Mai più di un {@link DbConnection} vivo per istanza di questa classe.
	 */
	public synchronized DbConnection getDbConnection() {
		if (dbPersistenceUnitName == null) {
			return null;
		}
		if (dbConnection == null) {
			dbConnection = new DbConnection(dbPersistenceUnitName, dbUrl, dbUser, dbPassword);
		}
		return dbConnection;
	}
}
