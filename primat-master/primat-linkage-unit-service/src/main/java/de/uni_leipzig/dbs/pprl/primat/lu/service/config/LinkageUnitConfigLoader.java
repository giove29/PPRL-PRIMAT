/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterBlockingKeyStrategy;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterRepresentantStrategy;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.PreferenceConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures.CenterClusteringConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.data_structures.ClipConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.global_greedy.data_structures.GlobalGreedyConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.service.LinkageUnitConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.service.LinkageUnitOrchestrator;

/**
 * Legge e valida il JSON di configurazione della Linkage Unit, producendo un
 * {@link LinkageUnitConfig} pronto all'uso. Mirror esatto di
 * {@code DataOwnerConfigLoader}: ogni problema viene riportato come
 * {@link LinkageUnitConfigException} con un messaggio comprensibile, mai come
 * eccezione grezza di Gson/IO. La config JSON e' la sola fonte di verità sulla
 * strategia da eseguire — niente più auto-routing basato su dirty/clean.
 */
public final class LinkageUnitConfigLoader {

	private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.6;
	private static final int DEFAULT_LSH_KEY_SIZE = 4;
	private static final int DEFAULT_LSH_KEYS = 30;
	private static final int DEFAULT_LSH_VALUE_RANGE = 1024;
	private static final long DEFAULT_LSH_SEED = 42L;
	private static final long DEFAULT_BROKER_CONNECT_TIMEOUT_SECONDS = 30L;
	private static final long DEFAULT_RBF_COLLECTION_TIMEOUT_SECONDS = 30L;
	private static final long DEFAULT_RBF_REPUBLISH_INTERVAL_SECONDS = 15L;
	/**
	 * Nome storico, mantenuto identico per compatibilità con
	 * {@code python_evaluation/evaluate_mcl.py}, che legge questo path di
	 * default quando {@code persistence.csvOutputPath} non è specificato per MCL.
	 */
	private static final String DEFAULT_MCL_CSV_OUTPUT_PATH = "mcl_debug_output.csv";

	private LinkageUnitConfigLoader() {
	}

	/**
	 * @param jsonPath percorso del file JSON di configurazione
	 * @return la configurazione validata
	 * @throws LinkageUnitConfigException se il file non e' leggibile, il JSON
	 *                                     e' malformato, o il contenuto non e'
	 *                                     valido
	 */
	public static LinkageUnitConfig load(Path jsonPath) throws LinkageUnitConfigException {
		final String json = readFile(jsonPath);
		final LinkageUnitJsonConfig raw = parseJson(json, jsonPath);

		validateTopLevel(raw, jsonPath);
		final List<Party> parties = resolveParties(raw.getParties(), jsonPath);
		final ClusteringMethod method = raw.getClusteringMethod();

		if (method == ClusteringMethod.MSCD_AP && !LinkageUnitOrchestrator.anyPartyDuplicateFree(parties)) {
			throw new LinkageUnitConfigException(
					"'clusteringMethod' e' MSCD_AP ma nessuna party ha 'duplicateFree=true' in " + jsonPath);
		}
		if ((method == ClusteringMethod.GLOBAL_GREEDY || method == ClusteringMethod.CLIP)
				&& !LinkageUnitOrchestrator.allPartiesDuplicateFree(parties)) {
			throw new LinkageUnitConfigException(
					"'clusteringMethod' e' " + method + " ma non tutte le party hanno 'duplicateFree=true' in "
							+ jsonPath
							+ " (il vincolo source-consistency e' corretto solo se nessuna sorgente ha duplicati interni)");
		}

		final double similarityThreshold = resolveSimilarityThreshold(raw.getSimilarityThreshold(), jsonPath);
		final int[] lsh = resolveLsh(raw.getBlocking(), raw.getRbfSize(), jsonPath);
		final JaccardLshJsonConfig jaccardLsh = raw.getBlocking() != null ? raw.getBlocking().getJaccardLsh() : null;
		final long lshSeed = jaccardLsh != null && jaccardLsh.getSeed() != null ? jaccardLsh.getSeed()
				: DEFAULT_LSH_SEED;
		final MqttJsonConfig mqtt = raw.getMqtt();
		final String mqttBrokerUrl = raw.getMqttBrokerUrl();
		final long brokerConnectTimeoutSeconds = mqtt != null && mqtt.getBrokerConnectTimeoutSeconds() != null
				? mqtt.getBrokerConnectTimeoutSeconds()
				: DEFAULT_BROKER_CONNECT_TIMEOUT_SECONDS;
		final long rbfCollectionTimeoutSeconds = mqtt != null && mqtt.getRbfCollectionTimeoutSeconds() != null
				? mqtt.getRbfCollectionTimeoutSeconds()
				: DEFAULT_RBF_COLLECTION_TIMEOUT_SECONDS;
		final long rbfRepublishIntervalSeconds = mqtt != null && mqtt.getRbfRepublishIntervalSeconds() != null
				? mqtt.getRbfRepublishIntervalSeconds()
				: DEFAULT_RBF_REPUBLISH_INTERVAL_SECONDS;

		final ClusterFactory clusterFactory = resolveClusterFactory(raw.getCluster());

		validatePersistence(raw.getPersistence(), method, jsonPath);
		final boolean persistenceEnabled = resolvePersistenceEnabled(raw.getPersistence(), method);
		final String csvOutputPath = resolveCsvOutputPath(raw.getPersistence(), method);
		// [persistenceUnitName, url, user, password], tutti null se persistenceEnabled == false.
		// Solo validati qui: la DbConnection vera e propria viene costruita lazy
		// da LinkageUnitConfig.getDbConnection() (apre una EntityManagerFactory,
		// che con hbm2ddl.auto=update tocca subito la rete) cosi' il solo
		// caricamento della config resta un test puro senza Postgres.
		final String[] dbParams = resolveDatabaseParams(raw.getDatabase(), method, persistenceEnabled, jsonPath);

		final CenterClusteringConfig centerClusteringConfig = resolveCenterClusteringConfig(raw.getCenterClustering());
		final ApConfig apConfig = resolveApConfig(raw.getMscdAp());
		final MclConfig mclConfig = resolveMclConfig(raw.getMcl());
		final GlobalGreedyConfig globalGreedyConfig = resolveGlobalGreedyConfig(raw.getGlobalGreedy());
		final ClipConfig clipConfig = resolveClipConfig(raw.getClip());

		return new LinkageUnitConfig(parties, method, similarityThreshold, raw.getRbfSize(), lsh[0], lsh[1], lsh[2],
				lshSeed, mqttBrokerUrl, brokerConnectTimeoutSeconds, rbfCollectionTimeoutSeconds, rbfRepublishIntervalSeconds, clusterFactory,
				persistenceEnabled, csvOutputPath, centerClusteringConfig, apConfig, mclConfig,
				globalGreedyConfig, clipConfig, dbParams[0], dbParams[1], dbParams[2], dbParams[3]);
	}

	private static String readFile(Path jsonPath) throws LinkageUnitConfigException {
		try {
			return Files.readString(jsonPath, StandardCharsets.UTF_8);
		}
		catch (NoSuchFileException e) {
			throw new LinkageUnitConfigException("File di configurazione non trovato: " + jsonPath, e);
		}
		catch (IOException e) {
			throw new LinkageUnitConfigException("Impossibile leggere il file di configurazione: " + jsonPath, e);
		}
	}

	private static LinkageUnitJsonConfig parseJson(String json, Path jsonPath) throws LinkageUnitConfigException {
		final LinkageUnitJsonConfig raw;
		try {
			raw = new Gson().fromJson(json, LinkageUnitJsonConfig.class);
		}
		catch (JsonParseException e) {
			throw new LinkageUnitConfigException(
					"JSON di configurazione malformato in " + jsonPath + ": " + e.getMessage(), e);
		}
		if (raw == null) {
			throw new LinkageUnitConfigException("File di configurazione vuoto: " + jsonPath);
		}
		return raw;
	}

	private static void validateTopLevel(LinkageUnitJsonConfig raw, Path jsonPath) throws LinkageUnitConfigException {
		if (raw.getMqttBrokerUrl() == null || raw.getMqttBrokerUrl().isBlank()) {
			throw new LinkageUnitConfigException("Campo obbligatorio 'mqttBrokerUrl' mancante o vuoto in " + jsonPath);
		}
		if (raw.getParties() == null || raw.getParties().isEmpty()) {
			throw new LinkageUnitConfigException("Campo obbligatorio 'parties' mancante o vuoto in " + jsonPath);
		}
		if (raw.getClusteringMethod() == null) {
			throw new LinkageUnitConfigException("Campo obbligatorio 'clusteringMethod' mancante in " + jsonPath);
		}
	}

	private static List<Party> resolveParties(List<PartyJsonConfig> rawParties, Path jsonPath)
			throws LinkageUnitConfigException {
		final Set<String> seenNames = new HashSet<>();
		final List<Party> parties = new ArrayList<>();
		for (final PartyJsonConfig rawParty : rawParties) {
			if (rawParty.getName() == null || rawParty.getName().isBlank()) {
				throw new LinkageUnitConfigException("Party con 'name' mancante o vuoto in " + jsonPath);
			}
			if (!seenNames.add(rawParty.getName().toUpperCase())) {
				throw new LinkageUnitConfigException("Nome di party duplicato: " + rawParty.getName() + " in " + jsonPath);
			}
			parties.add(new Party(rawParty.getName(), rawParty.isDuplicateFreeOrDefault()));
		}
		return parties;
	}

	private static double resolveSimilarityThreshold(Double raw, Path jsonPath) throws LinkageUnitConfigException {
		final double threshold = raw != null ? raw : DEFAULT_SIMILARITY_THRESHOLD;
		if (threshold <= 0 || threshold > 1) {
			throw new LinkageUnitConfigException(
					"'similarityThreshold' deve essere in (0,1], trovato " + threshold + " in " + jsonPath);
		}
		return threshold;
	}

	/**
	 * @return {@code [keySize, keys, valueRange]}, gia' validati positivi.
	 *         {@code valueRange}, se omesso nel JSON, usa {@code rbfSize} come
	 *         default (invece del fisso {@code DEFAULT_LSH_VALUE_RANGE}) quando
	 *         dichiarato: e' cosi' che il blocking campiona davvero la dimensione
	 *         reale dell'RBF (es. dimezzata da un hardening XOR-fold) invece di
	 *         restare sempre a 1024 a prescindere da cosa produce il Data Owner.
	 */
	private static int[] resolveLsh(BlockingJsonConfig blocking, Integer rbfSize, Path jsonPath)
			throws LinkageUnitConfigException {
		final JaccardLshJsonConfig jaccardLsh = blocking != null ? blocking.getJaccardLsh() : null;
		final int keySize = jaccardLsh != null && jaccardLsh.getKeySize() != null ? jaccardLsh.getKeySize()
				: DEFAULT_LSH_KEY_SIZE;
		final int keys = jaccardLsh != null && jaccardLsh.getKeys() != null ? jaccardLsh.getKeys() : DEFAULT_LSH_KEYS;
		final int valueRangeDefault = rbfSize != null ? rbfSize : DEFAULT_LSH_VALUE_RANGE;
		final int valueRange = jaccardLsh != null && jaccardLsh.getValueRange() != null ? jaccardLsh.getValueRange()
				: valueRangeDefault;
		if (keySize <= 0 || keys <= 0 || valueRange <= 0) {
			throw new LinkageUnitConfigException(
					"'blocking.jaccardLsh.{keySize,keys,valueRange}' devono essere positivi in " + jsonPath);
		}
		return new int[] { keySize, keys, valueRange };
	}

	private static ClusterFactory resolveClusterFactory(ClusterJsonConfig cluster) {
		final ClusterBlockingKeyStrategy blockingKeyStrategy = cluster != null && cluster.getBlockingKeyStrategy() != null
				? cluster.getBlockingKeyStrategy()
				: ClusterBlockingKeyStrategy.UNION;
		final ClusterRepresentantStrategy representantStrategy = cluster != null
				&& cluster.getRepresentantStrategy() != null ? cluster.getRepresentantStrategy()
						: ClusterRepresentantStrategy.RETAIN_FIRST;
		return new ClusterFactory(blockingKeyStrategy, representantStrategy);
	}

	private static void validatePersistence(PersistenceJsonConfig persistence, ClusteringMethod method, Path jsonPath)
			throws LinkageUnitConfigException {
		if (persistence != null && Boolean.TRUE.equals(persistence.getEnabled()) && method == ClusteringMethod.MCL) {
			throw new LinkageUnitConfigException(
					"'persistence.enabled=true' non e' compatibile con 'clusteringMethod: MCL' (MCL non persiste per design) in "
							+ jsonPath);
		}
	}

	/**
	 * @return {@code true} se {@code persistence.enabled} è specificato esplicitamente
	 *         nel JSON, altrimenti il default: {@code true} per ogni metodo diverso da
	 *         MCL (comportamento storico invariato), {@code false} per MCL.
	 */
	private static boolean resolvePersistenceEnabled(PersistenceJsonConfig persistence, ClusteringMethod method) {
		if (persistence != null && persistence.getEnabled() != null) {
			return persistence.getEnabled();
		}
		return method != ClusteringMethod.MCL;
	}

	/**
	 * @return {@code persistence.csvOutputPath} se specificato, altrimenti un default
	 *         per-metodo: il nome storico {@link #DEFAULT_MCL_CSV_OUTPUT_PATH} per MCL,
	 *         {@code <method-lowercase>_output.csv} per gli altri 5.
	 */
	private static String resolveCsvOutputPath(PersistenceJsonConfig persistence, ClusteringMethod method) {
		if (persistence != null && persistence.getCsvOutputPath() != null && !persistence.getCsvOutputPath().isBlank()) {
			return persistence.getCsvOutputPath();
		}
		return method == ClusteringMethod.MCL ? DEFAULT_MCL_CSV_OUTPUT_PATH
				: method.name().toLowerCase() + "_output.csv";
	}

	/** @return {@code [persistenceUnitName, url, user, password]}, tutti {@code null} se {@code !persistenceEnabled}. */
	private static String[] resolveDatabaseParams(DatabaseJsonConfig database, ClusteringMethod method,
			boolean persistenceEnabled, Path jsonPath) throws LinkageUnitConfigException {
		if (!persistenceEnabled) {
			return new String[4];
		}
		if (database == null) {
			throw new LinkageUnitConfigException(
					"Campo obbligatorio 'database' mancante (richiesto per clusteringMethod=" + method + ") in "
							+ jsonPath);
		}
		if (database.getUrl() == null || database.getUrl().isBlank()) {
			throw new LinkageUnitConfigException("Campo obbligatorio 'database.url' mancante o vuoto in " + jsonPath);
		}
		if (database.getUser() == null || database.getUser().isBlank()) {
			throw new LinkageUnitConfigException("Campo obbligatorio 'database.user' mancante o vuoto in " + jsonPath);
		}
		if (database.getPassword() == null) {
			throw new LinkageUnitConfigException("Campo obbligatorio 'database.password' mancante in " + jsonPath);
		}
		return new String[] { persistenceUnitNameFor(method), database.getUrl(), database.getUser(),
				database.getPassword() };
	}

	private static String persistenceUnitNameFor(ClusteringMethod method) {
		switch (method) {
			case CENTER_CLUSTERING:
				return "PrimatCenterClustering";
			case MSCD_AP:
				return "PrimatMscdAp";
			case GLOBAL_GREEDY:
				return "PrimatGlobalGreedy";
			case CLIP:
				return "PrimatClip";
			default:
				throw new IllegalArgumentException("Nessuna persistence-unit per " + method);
		}
	}

	private static CenterClusteringConfig resolveCenterClusteringConfig(CenterClusteringJsonConfig raw)
			throws LinkageUnitConfigException {
		final CenterClusteringConfig config = new CenterClusteringConfig();
		if (raw != null && raw.getCenterAssignmentThreshold() != null) {
			config.setCenterAssignmentThreshold(raw.getCenterAssignmentThreshold());
		}
		try {
			config.checkConfigCorrectness();
		}
		catch (IllegalArgumentException e) {
			throw new LinkageUnitConfigException("'centerClustering' non valido: " + e.getMessage(), e);
		}
		return config;
	}

	private static GlobalGreedyConfig resolveGlobalGreedyConfig(GlobalGreedyJsonConfig raw)
			throws LinkageUnitConfigException {
		final GlobalGreedyConfig config = new GlobalGreedyConfig();
		if (raw != null && raw.getMergeThreshold() != null) {
			config.setMergeThreshold(raw.getMergeThreshold());
		}
		try {
			config.checkConfigCorrectness();
		}
		catch (IllegalArgumentException e) {
			throw new LinkageUnitConfigException("'globalGreedy' non valido: " + e.getMessage(), e);
		}
		return config;
	}

	private static ClipConfig resolveClipConfig(ClipJsonConfig raw) throws LinkageUnitConfigException {
		final ClipConfig config = new ClipConfig();
		if (raw != null) {
			if (raw.getWeightSimilarity() != null) {
				config.setWeightSimilarity(raw.getWeightSimilarity());
			}
			if (raw.getWeightLinkDegree() != null) {
				config.setWeightLinkDegree(raw.getWeightLinkDegree());
			}
			if (raw.getWeightLinkStrength() != null) {
				config.setWeightLinkStrength(raw.getWeightLinkStrength());
			}
			if (raw.getValueStrong() != null) {
				config.setValueStrong(raw.getValueStrong());
			}
			if (raw.getValueNormal() != null) {
				config.setValueNormal(raw.getValueNormal());
			}
			if (raw.getValueWeak() != null) {
				config.setValueWeak(raw.getValueWeak());
			}
			if (raw.getIgnoreWeakLinks() != null) {
				config.setIgnoreWeakLinks(raw.getIgnoreWeakLinks());
			}
			if (raw.getMergeThreshold() != null) {
				config.setMergeThreshold(raw.getMergeThreshold());
			}
		}
		try {
			config.checkConfigCorrectness();
		}
		catch (IllegalArgumentException e) {
			throw new LinkageUnitConfigException("'clip' non valido: " + e.getMessage(), e);
		}
		return config;
	}

	private static ApConfig resolveApConfig(ApJsonConfig raw) throws LinkageUnitConfigException {
		final ApConfig config = new ApConfig();
		if (raw != null) {
			if (raw.getMaxApIteration() != null) {
				config.setMaxApIteration(raw.getMaxApIteration());
			}
			if (raw.getMaxAdaptionIteration() != null) {
				config.setMaxAdaptionIteration(raw.getMaxAdaptionIteration());
			}
			if (raw.getConvergenceIter() != null) {
				config.setConvergenceIter(raw.getConvergenceIter());
			}
			if (raw.getDampingFactor() != null) {
				config.setDampingFactor(raw.getDampingFactor());
			}
			if (raw.getDampingAdaptionStep() != null) {
				config.setDampingAdaptionStep(raw.getDampingAdaptionStep());
			}
			if (raw.getAllSameSimClusteringThreshold() != null) {
				config.setAllSameSimClusteringThreshold(raw.getAllSameSimClusteringThreshold());
			}
			if (raw.getNoiseDecimalPlace() != null) {
				config.setNoiseDecimalPlace(raw.getNoiseDecimalPlace());
			}
			if (raw.getSingletonsForUnconvergedComponents() != null) {
				config.setSingletonsForUnconvergedComponents(raw.getSingletonsForUnconvergedComponents());
			}
			config.setPreferenceConfig(resolvePreferenceConfig(raw.getPreference()));
		}
		try {
			config.checkConfigCorrectness();
		}
		catch (IllegalArgumentException e) {
			throw new LinkageUnitConfigException("'mscdAp' non valido: " + e.getMessage(), e);
		}
		return config;
	}

	private static PreferenceConfig resolvePreferenceConfig(PreferenceJsonConfig raw) {
		final PreferenceConfig config = new PreferenceConfig();
		if (raw == null) {
			return config;
		}
		if (raw.getPreferenceUseMinSimilarityDirtySrc() != null) {
			config.setPreferenceUseMinSimilarityDirtySrc(raw.getPreferenceUseMinSimilarityDirtySrc());
		}
		if (raw.getPreferenceFixValueDirtySrc() != null) {
			config.setPreferenceFixValueDirtySrc(raw.getPreferenceFixValueDirtySrc());
		}
		if (raw.getPreferencePercentileDirtySrc() != null) {
			config.setPreferencePercentileDirtySrc(raw.getPreferencePercentileDirtySrc());
		}
		if (raw.getPreferenceUseMinSimilarityCleanSrc() != null) {
			config.setPreferenceUseMinSimilarityCleanSrc(raw.getPreferenceUseMinSimilarityCleanSrc());
		}
		if (raw.getPreferenceFixValueCleanSrc() != null) {
			config.setPreferenceFixValueCleanSrc(raw.getPreferenceFixValueCleanSrc());
		}
		if (raw.getPreferencePercentileCleanSrc() != null) {
			config.setPreferencePercentileCleanSrc(raw.getPreferencePercentileCleanSrc());
		}
		if (raw.getPreferenceAdaptionStep() != null) {
			config.setPreferenceAdaptionStep(raw.getPreferenceAdaptionStep());
		}
		return config;
	}

	private static MclConfig resolveMclConfig(MclJsonConfig raw) throws LinkageUnitConfigException {
		final MclConfig config = new MclConfig();
		if (raw != null) {
			if (raw.getExpansionPower() != null) {
				config.setExpansionPower(raw.getExpansionPower());
			}
			if (raw.getInflationExponent() != null) {
				config.setInflationExponent(raw.getInflationExponent());
			}
			if (raw.getPruningThreshold() != null) {
				config.setPruningThreshold(raw.getPruningThreshold());
			}
			if (raw.getMaxIterations() != null) {
				config.setMaxIterations(raw.getMaxIterations());
			}
			if (raw.getConvergenceEpsilon() != null) {
				config.setConvergenceEpsilon(raw.getConvergenceEpsilon());
			}
			if (raw.getSelfLoopWeight() != null) {
				config.setSelfLoopWeight(raw.getSelfLoopWeight());
			}
		}
		try {
			config.checkConfigCorrectness();
		}
		catch (IllegalArgumentException e) {
			throw new LinkageUnitConfigException("'mcl' non valido: " + e.getMessage(), e);
		}
		return config;
	}
}
