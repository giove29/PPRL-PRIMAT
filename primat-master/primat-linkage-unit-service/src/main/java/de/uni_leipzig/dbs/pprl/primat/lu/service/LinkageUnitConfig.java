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
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.SimilarityThresholdSpec;
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

	private List<Party> parties;
	private final ClusteringMethod clusteringMethod;
	private final SimilarityThresholdSpec thresholdSpec;
	private int rbfSize;
	private final int lshKeySize;
	private final int lshKeys;
	private final long lshSeed;
	private String mqttBrokerUrl;
	private final long brokerConnectTimeoutSeconds;
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
	private final boolean debug;
	private DbConnection dbConnection;

	/**
	 * Costruito esclusivamente da {@link LinkageUnitConfigLoader} dopo la
	 * validazione del JSON: nessun controllo aggiuntivo viene fatto qui,
	 * questa classe e' altrimenti un semplice contenitore. {@code parties}
	 * (roster) e {@code rbfSize} NON sono parametri del costruttore: dal
	 * 2026-10-02 sono campi puramente runtime, mai letti dal JSON locale —
	 * inizializzati qui a vuoto/0 ("non ancora configurato") e popolati solo
	 * da {@link #setParties}/{@link #setRbfSize} ad ogni {@code LuConfigPush}
	 * della SMU, poi azzerati di nuovo da {@code LinkageUnitOrchestrator}
	 * subito dopo ogni run.
	 *
	 * @param dbPersistenceUnitName nome della persistence-unit dedicata alla
	 *                              strategia scelta, {@code null} se
	 *                              {@code clusteringMethod == MCL}
	 * @param dbUrl                 url JDBC del DB dedicato, {@code null} se MCL
	 * @param dbUser                utente del DB dedicato, {@code null} se MCL
	 * @param dbPassword            password del DB dedicato, {@code null} se MCL
	 */
	public LinkageUnitConfig(ClusteringMethod clusteringMethod, SimilarityThresholdSpec thresholdSpec,
			int lshKeySize, int lshKeys, long lshSeed, String mqttBrokerUrl,
			long brokerConnectTimeoutSeconds, long rbfCollectionTimeoutSeconds, long rbfRepublishIntervalSeconds, ClusterFactory clusterFactory,
			boolean persistenceEnabled, String csvOutputPath, CenterClusteringConfig centerClusteringConfig,
			ApConfig apConfig, MclConfig mclConfig, GlobalGreedyConfig globalGreedyConfig, ClipConfig clipConfig,
			String dbPersistenceUnitName, String dbUrl, String dbUser, String dbPassword, boolean debug) {
		this.parties = List.of();
		this.clusteringMethod = clusteringMethod;
		this.thresholdSpec = thresholdSpec;
		this.rbfSize = 0;
		this.lshKeySize = lshKeySize;
		this.lshKeys = lshKeys;
		this.lshSeed = lshSeed;
		this.mqttBrokerUrl = mqttBrokerUrl;
		this.brokerConnectTimeoutSeconds = brokerConnectTimeoutSeconds;
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
		this.debug = debug;
	}

	/** @return {@code true} se il JSON ha {@code debug: true}: abilita l'istogramma delle similarita' (vedi {@link SimilarityHistogramCollector}). */
	public boolean isDebug() {
		return debug;
	}

	/** @return il roster corrente (nomi + clean/dirty), vuoto se nessun run e' in corso (vedi {@link #setParties}). */
	public List<Party> getParties() {
		return parties;
	}

	/**
	 * Imposta il roster dei party (nomi + clean/dirty) per il run in corso,
	 * sostituendo l'intera lista per riferimento (mai una mutazione in-place,
	 * cosi' un {@code executeRun}/{@code waitForRbf} gia' in corso non vede
	 * mai uno stato a meta'): chiamato da {@code
	 * LinkageUnitOrchestrator#handleLuConfigPush} con il roster appena
	 * validato da {@code LinkageUnitConfigLoader#resolvePartyRoster} contro
	 * ogni nuovo {@code LuConfigPush} della SMU, e azzerato di nuovo (lista
	 * vuota) subito dopo la fine del run — mai letto da un JSON locale, mai
	 * persistito, mai valido oltre la durata di un singolo run.
	 */
	public void setParties(List<Party> parties) {
		this.parties = parties;
	}

	public ClusteringMethod getClusteringMethod() {
		return clusteringMethod;
	}

	/** @return la soglia fissa configurata, {@code NaN} se e' stata chiesta una soglia automatica (vedi {@link #getThresholdSpec()}). */
	public double getSimilarityThreshold() {
		return thresholdSpec.getFixedValue();
	}

	/** @return la soglia di similarita' come dichiarata in JSON: fissa oppure automatica (con epsilon). */
	public SimilarityThresholdSpec getThresholdSpec() {
		return thresholdSpec;
	}

	/**
	 * @return dimensione autorevole dell'RBF in bit per il run in corso: unica
	 *         fonte di verita' per il blocking (vedi {@code
	 *         LinkageUnitOrchestrator}, che la usa direttamente come {@code
	 *         valueRange} del generatore di chiavi LSH); {@code 0} se nessun
	 *         run e' in corso (vedi {@link #setRbfSize}).
	 */
	public int getRbfSize() {
		return rbfSize;
	}

	/**
	 * Imposta la dimensione autorevole dell'RBF per il run in corso: chiamato
	 * da {@code LinkageUnitOrchestrator#handleLuConfigPush} con il valore
	 * appena validato da {@code LinkageUnitConfigLoader#validateRbfSize}
	 * contro ogni nuovo {@code LuConfigPush} della SMU, e azzerato di nuovo
	 * ({@code 0}) subito dopo la fine del run — mai letto da un JSON locale,
	 * mai persistito, mai valido oltre la durata di un singolo run.
	 */
	public void setRbfSize(int rbfSize) {
		this.rbfSize = rbfSize;
	}

	public int getLshKeySize() {
		return lshKeySize;
	}

	public int getLshKeys() {
		return lshKeys;
	}

	public long getLshSeed() {
		return lshSeed;
	}

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	/**
	 * Riconfigura a caldo il broker MQTT, sovrascrivendo il valore caricato dal
	 * JSON locale: usato da {@code LinkageUnitOrchestrator#handleLuBrokerPush}
	 * dopo che il nuovo broker e' stato verificato raggiungibile e il file
	 * locale e' gia' stato aggiornato — mirror esatto di {@link #setRbfSize}.
	 */
	public void setMqttBrokerUrl(String mqttBrokerUrl) {
		this.mqttBrokerUrl = mqttBrokerUrl;
	}

	public long getBrokerConnectTimeoutSeconds() {
		return brokerConnectTimeoutSeconds;
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

	/**
	 * @return riepilogo leggibile della configurazione corrente (strategia,
	 *         tuning blocking/MQTT, persistenza — questi dal JSON locale;
	 *         party e RBF size invece solo se un run e' in corso, altrimenti
	 *         vuoto/0: non fanno piu' parte del JSON, arrivano dalla SMU ad
	 *         ogni push), pensato per essere stampato a schermo sia
	 *         all'avvio della Linkage Unit sia ad ogni {@code LuConfigPush}
	 *         ricevuto, mirror di {@code DataOwnerConfig.describe()}. Nessun
	 *         dato in chiaro dei record, solo tuning/struttura (la password
	 *         del DB non è inclusa).
	 */
	public String describe() {
		final StringBuilder sb = new StringBuilder();
		sb.append("==================== Linkage Unit ====================\n");
		sb.append("Broker MQTT:            ").append(mqttBrokerUrl).append('\n');
		sb.append("Strategia:              ").append(clusteringMethod).append('\n');
		sb.append("Soglia similarita':     ").append(thresholdSpec).append('\n');
		sb.append("RBF size:               ").append(rbfSize).append(" bit\n");
		sb.append("Blocking (JaccardLSH):  keySize=").append(lshKeySize).append(", keys=").append(lshKeys)
				.append(", valueRange=").append(rbfSize).append(", seed=").append(lshSeed).append('\n');
		final String rangeTestingDisabled = "DISABILITATA (modalita' TEST range: nessun DB/CSV/incremento)";
		sb.append("Persistenza:            ");
		if (thresholdSpec.isRange()) {
			sb.append(rangeTestingDisabled);
		}
		else if (persistenceEnabled) {
			sb.append("DB (url=").append(dbUrl).append(", user=").append(dbUser).append(')');
		}
		else {
			sb.append("CSV (path=").append(csvOutputPath).append(')');
		}
		sb.append('\n');
		sb.append("MQTT tuning:            brokerConnectTimeout=").append(brokerConnectTimeoutSeconds)
				.append("s, rbfCollectionTimeout=").append(rbfCollectionTimeoutSeconds)
				.append("s, rbfRepublishInterval=").append(rbfRepublishIntervalSeconds).append("s\n");
		sb.append("Debug:                  ");
		if (thresholdSpec.isRange()) {
			sb.append(rangeTestingDisabled);
		}
		else {
			sb.append(debug ? "On (istogramma similarita' -> " + SimilarityHistogramCsvWriter.DEFAULT_OUTPUT_PATH + ")"
					: "Off");
		}
		sb.append('\n');
		sb.append("Party:\n");
		for (final Party party : parties) {
			sb.append("  - ").append(party.getName()).append(" [duplicateFree=").append(party.isDuplicateFree())
					.append("]\n");
		}
		sb.append("=======================================================");
		return sb.toString();
	}
}
