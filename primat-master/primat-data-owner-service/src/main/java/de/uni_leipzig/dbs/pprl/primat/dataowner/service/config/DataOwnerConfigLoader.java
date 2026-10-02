/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.BloomFilterHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.ChainedHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.NoHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.RandomizedResponse;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.XorFolder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.DataOwnerConfig;

/**
 * Legge e valida la configurazione del Data Owner, fusa da due file JSON
 * distinti - "locale" ({@code party}/{@code debug}/{@code dataSource}, mai
 * riscritto da questo processo) e "live" ({@code mqttBrokerUrl}/
 * {@code columns}/{@code bloomFilter}/{@code missingValueHandling}/
 * {@code hmacKey}/{@code version}, l'unico che {@code ConfigPush} puo'
 * riscrivere) - producendo un {@link DataOwnerConfig} pronto all'uso. Qui
 * vive tutta la gestione degli errori introdotti dalla configurazione via
 * file: file mancante/non leggibile, JSON malformato, campi obbligatori
 * assenti o non validi, colonne inconsistenti, parametri di tuning del Bloom
 * Filter fuori range. Ogni problema viene riportato come {@link DataOwnerConfigException} con un
 * messaggio comprensibile, mai come eccezione grezza di Gson/IO.
 */
public final class DataOwnerConfigLoader {

	/** Lunghezza dell'RBF in bit se la sezione {@code bloomFilter} o il campo {@code length} sono omessi. */
	public static final int DEFAULT_BF_LENGTH = 1024;

	/** Seed del PRNG di BLIP se il campo {@code seed} e' omesso nella sezione {@code hardening}. */
	public static final long DEFAULT_BLIP_SEED = 42L;

	/** Numero di record RBF per messaggio MQTT se {@code rbfChunkSize} e' omesso nel file locale. */
	public static final int DEFAULT_RBF_CHUNK_SIZE = 2000;

	/**
	 * Contenuto scritto nel file live quando viene creato perche' assente
	 * all'avvio (vedi {@link #loadBootstrap(Path)}): nessuna chiave oltre
	 * {@code version}, cosi' un qualunque checkVersion/confronto risulta
	 * "fuori fase" finche' non arriva una {@code ConfigPush} reale.
	 */
	private static final String PENDING_LIVE_FILE_CONTENT = "{\n  \"version\": \"NOT_FOUND\"\n}\n";

	private DataOwnerConfigLoader() {
	}

	/**
	 * @param localPath percorso del file JSON "locale" (immutabile, mai
	 *                  riscritto da questo processo): {@code party}/
	 *                  {@code debug}/{@code dataSource}
	 * @param livePath  percorso del file JSON "live" (pushabile dalla SMU via
	 *                  {@code ConfigPush}, riscritto atomicamente ad ogni push
	 *                  accettata): {@code mqttBrokerUrl}/{@code columns}/
	 *                  {@code bloomFilter}/{@code missingValueHandling}/
	 *                  {@code hmacKey}/{@code version}
	 * @return la configurazione validata, fusione dei due file
	 * @throws DataOwnerConfigException se uno dei due file non e' leggibile, il
	 *                                   JSON e' malformato, i due file si
	 *                                   sovrappongono su una stessa chiave, o il
	 *                                   contenuto fuso non e' valido
	 */
	public static DataOwnerConfig load(Path localPath, Path livePath) throws DataOwnerConfigException {
		final JsonObject localObj = parseJsonObject(readFile(localPath), localPath);
		final JsonObject liveObj = parseJsonObject(readFile(livePath), livePath);

		for (final String key : localObj.keySet()) {
			if (liveObj.has(key)) {
				throw new DataOwnerConfigException("Campo '" + key + "' presente sia nel file locale (" + localPath
						+ ") sia nel file live (" + livePath + "): deve stare in uno solo dei due");
			}
		}
		final JsonObject merged = new JsonObject();
		for (final Map.Entry<String, JsonElement> entry : localObj.entrySet()) {
			merged.add(entry.getKey(), entry.getValue());
		}
		for (final Map.Entry<String, JsonElement> entry : liveObj.entrySet()) {
			merged.add(entry.getKey(), entry.getValue());
		}
		final DataOwnerJsonConfig raw = new Gson().fromJson(merged, DataOwnerJsonConfig.class);
		if (raw == null) {
			throw new DataOwnerConfigException("Configurazione vuota (file locale=" + localPath + ", live=" + livePath
					+ ")");
		}

		validateLocalTopLevel(raw, localPath);
		validateLiveTopLevel(raw, livePath);
		validateDataSource(raw.getDataSource(), localPath);
		validateColumns(raw.getColumns(), livePath);
		validateMissingValueHandling(raw.getMissingValueHandling(), raw.getColumns(), livePath);
		PreprocessingStepFactory.validateCrossColumnConsistency(raw.getColumns(), livePath);

		final int rbfChunkSize = resolveRbfChunkSize(raw.getRbfChunkSize());
		final int bfLength = resolveBloomFilterLength(raw.getBloomFilter(), livePath);
		final BloomFilterHardener hardener = resolveHardener(raw.getBloomFilter(), bfLength, livePath);
		final List<String> hardeningDescriptions = describeHardeningChain(raw.getBloomFilter());

		final DataSourceJsonConfig dataSource = raw.getDataSource();
		final ResolvedDataSource resolvedDataSource = resolveDataSource(dataSource, localPath);

		final MissingValueHandlingJsonConfig mvh = raw.getMissingValueHandling();
		final boolean missingValueHandlingEnabled = mvh != null && Boolean.TRUE.equals(mvh.getEnabled());
		final List<String> missingValueAnchorPriority = missingValueHandlingEnabled ? mvh.getAnchorPriority()
				: List.of();

		final String version = raw.getVersion() != null ? raw.getVersion() : "0";

		return new DataOwnerConfig(raw.getParty(), dataSource.getType(), resolvedDataSource.csvFilePath,
				resolvedDataSource.csvHasHeader, resolvedDataSource.csvDelimiter, resolvedDataSource.dbConfig,
				raw.getMqttBrokerUrl(), raw.getColumns(), bfLength, hardener, raw.isDebug(), missingValueHandlingEnabled,
				missingValueAnchorPriority, hardeningDescriptions, raw.getHmacKey(), version, false, rbfChunkSize);
	}

	/**
	 * Legge+valida solo il file "locale" e risolve il percorso del file "live"
	 * dichiarato al suo interno ({@code liveConfigPath}). Se il file live non
	 * esiste ancora, lo crea con il solo placeholder {@code {"version":
	 * "NOT_FOUND"}} e ritorna una configurazione "pending" (connessione MQTT
	 * possibile via {@code bootstrapMqttBrokerUrl}, ma nessuna pipeline
	 * eseguibile finche' non arriva una {@code ConfigPush} reale). Se il file
	 * live esiste ma non e' leggibile per un motivo diverso da "non esiste"
	 * (permessi, ecc.), l'errore e' fatale. Se il file live esiste e contiene
	 * gia' una configurazione reale (chiave {@code columns} presente), il
	 * comportamento e' identico a {@link #load(Path, Path)}.
	 *
	 * @param localPath percorso del file JSON locale, unico argomento CLI di
	 *                  {@code DataOwnerService.main}
	 * @return la configurazione (pending o meno) e il percorso live risolto
	 * @throws DataOwnerConfigException se il file locale non e' valido, o il
	 *                                   file live esiste ma non e' leggibile/valido
	 */
	public static BootstrapResult loadBootstrap(Path localPath) throws DataOwnerConfigException {
		final JsonObject localObj = parseJsonObject(readFile(localPath), localPath);
		final DataOwnerJsonConfig localRaw = new Gson().fromJson(localObj, DataOwnerJsonConfig.class);
		if (localRaw == null) {
			throw new DataOwnerConfigException("File di configurazione locale vuoto: " + localPath);
		}
		validateLocalTopLevel(localRaw, localPath);
		validateDataSource(localRaw.getDataSource(), localPath);
		requireNonBlank(localRaw.getLiveConfigPath(), "liveConfigPath", localPath);
		requireNonBlank(localRaw.getBootstrapMqttBrokerUrl(), "bootstrapMqttBrokerUrl", localPath);

		final Path livePath = resolveLivePath(localPath, localRaw.getLiveConfigPath());

		final String liveJson;
		try {
			liveJson = Files.readString(livePath, StandardCharsets.UTF_8);
		}
		catch (NoSuchFileException e) {
			writePendingLiveFile(livePath);
			return new BootstrapResult(buildPendingConfig(localRaw, localPath), livePath);
		}
		catch (IOException e) {
			throw new DataOwnerConfigException("Impossibile leggere il file di configurazione live: " + livePath, e);
		}

		final JsonObject liveObj = parseJsonObject(liveJson, livePath);
		if (!liveObj.has("columns")) {
			// Placeholder (appena scritto o da un avvio precedente) o file live
			// incompleto trovato cosi' com'e': non ancora una configurazione
			// reale, nessun errore.
			return new BootstrapResult(buildPendingConfig(localRaw, localPath), livePath);
		}

		return new BootstrapResult(load(localPath, livePath), livePath);
	}

	private static Path resolveLivePath(Path localPath, String liveConfigPathValue) {
		final Path raw = Paths.get(liveConfigPathValue);
		if (raw.isAbsolute()) {
			return raw;
		}
		final Path localDir = localPath.toAbsolutePath().getParent();
		return localDir == null ? raw : localDir.resolve(raw).normalize();
	}

	private static void writePendingLiveFile(Path livePath) throws DataOwnerConfigException {
		try {
			if (livePath.toAbsolutePath().getParent() != null) {
				Files.createDirectories(livePath.toAbsolutePath().getParent());
			}
			Files.writeString(livePath, PENDING_LIVE_FILE_CONTENT, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new DataOwnerConfigException("Impossibile creare il file di configurazione live mancante: "
					+ livePath, e);
		}
	}

	private static DataOwnerConfig buildPendingConfig(DataOwnerJsonConfig localRaw, Path localPath)
			throws DataOwnerConfigException {
		final ResolvedDataSource resolvedDataSource = resolveDataSource(localRaw.getDataSource(), localPath);
		final int rbfChunkSize = resolveRbfChunkSize(localRaw.getRbfChunkSize());
		return new DataOwnerConfig(localRaw.getParty(), localRaw.getDataSource().getType(),
				resolvedDataSource.csvFilePath, resolvedDataSource.csvHasHeader, resolvedDataSource.csvDelimiter,
				resolvedDataSource.dbConfig, localRaw.getBootstrapMqttBrokerUrl(), List.of(), DEFAULT_BF_LENGTH,
				new NoHardener(), localRaw.isDebug(), false, List.of(), List.of(), null, "NOT_FOUND", true,
				rbfChunkSize);
	}

	/** Risultato di {@link #loadBootstrap(Path)}: la configurazione (pending o meno) e il percorso live risolto. */
	public static final class BootstrapResult {
		public final DataOwnerConfig config;
		public final Path livePath;

		private BootstrapResult(DataOwnerConfig config, Path livePath) {
			this.config = config;
			this.livePath = livePath;
		}
	}

	private static final class ResolvedDataSource {
		final String csvFilePath;
		final boolean csvHasHeader;
		final char csvDelimiter;
		final DbSourceConfig dbConfig;

		ResolvedDataSource(String csvFilePath, boolean csvHasHeader, char csvDelimiter, DbSourceConfig dbConfig) {
			this.csvFilePath = csvFilePath;
			this.csvHasHeader = csvHasHeader;
			this.csvDelimiter = csvDelimiter;
			this.dbConfig = dbConfig;
		}
	}

	/**
	 * Risolve i campi derivati di {@code dataSource}, condiviso da {@link #load}
	 * e da {@link #buildPendingConfig} per non duplicare questa logica.
	 *
	 * @param errorPath percorso da citare in un eventuale messaggio d'errore
	 *                  (il delimiter CSV e' validato anche qui, non solo da
	 *                  {@link #validateDataSource})
	 */
	private static ResolvedDataSource resolveDataSource(DataSourceJsonConfig dataSource, Path errorPath)
			throws DataOwnerConfigException {
		final String csvFilePath = dataSource.getType() == DataSourceType.CSV ? dataSource.getCsv().getFilePath()
				: null;
		final boolean csvHasHeader = dataSource.getType() == DataSourceType.CSV && dataSource.getCsv().isHasHeader();
		char csvDelimiter = ';';
		if (dataSource.getType() == DataSourceType.CSV) {
			final String d = dataSource.getCsv().getDelimiter();
			if (d.length() != 1) {
				throw new DataOwnerConfigException(
						"dataSource.csv.delimiter deve essere un solo carattere, trovato: \"" + d + "\" in " + errorPath);
			}
			csvDelimiter = d.charAt(0);
		}
		final DbSourceConfig dbConfig = dataSource.getType() == DataSourceType.DB ? dataSource.getDb() : null;
		return new ResolvedDataSource(csvFilePath, csvHasHeader, csvDelimiter, dbConfig);
	}

	private static String readFile(Path jsonPath) throws DataOwnerConfigException {
		try {
			return Files.readString(jsonPath, StandardCharsets.UTF_8);
		}
		catch (NoSuchFileException e) {
			throw new DataOwnerConfigException("File di configurazione non trovato: " + jsonPath, e);
		}
		catch (IOException e) {
			throw new DataOwnerConfigException("Impossibile leggere il file di configurazione: " + jsonPath, e);
		}
	}

	private static JsonObject parseJsonObject(String json, Path jsonPath) throws DataOwnerConfigException {
		final JsonObject raw;
		try {
			raw = new Gson().fromJson(json, JsonObject.class);
		}
		catch (JsonParseException e) {
			throw new DataOwnerConfigException("JSON di configurazione malformato in " + jsonPath + ": "
					+ e.getMessage(), e);
		}
		if (raw == null) {
			throw new DataOwnerConfigException("File di configurazione vuoto: " + jsonPath);
		}
		return raw;
	}

	private static void requireNonBlank(String value, String fieldName, Path jsonPath)
			throws DataOwnerConfigException {
		if (value == null || value.isBlank()) {
			throw new DataOwnerConfigException(
					"Campo obbligatorio '" + fieldName + "' mancante o vuoto in " + jsonPath);
		}
	}

	/** Valida i campi di competenza del file locale: {@code party}/{@code dataSource}/{@code rbfChunkSize}. */
	private static void validateLocalTopLevel(DataOwnerJsonConfig raw, Path localPath)
			throws DataOwnerConfigException {
		requireNonBlank(raw.getParty(), "party", localPath);
		if (raw.getDataSource() == null) {
			throw new DataOwnerConfigException("Campo obbligatorio 'dataSource' mancante in " + localPath);
		}
		if (raw.getRbfChunkSize() != null && raw.getRbfChunkSize() <= 0) {
			throw new DataOwnerConfigException(
					"'rbfChunkSize' deve essere positivo, trovato " + raw.getRbfChunkSize() + " in " + localPath);
		}
	}

	/**
	 * @return {@link #DEFAULT_RBF_CHUNK_SIZE} se {@code rbfChunkSize} e' omesso
	 *         nel file locale, altrimenti il valore gia' validato da
	 *         {@link #validateLocalTopLevel}.
	 */
	private static int resolveRbfChunkSize(Integer rbfChunkSize) {
		return rbfChunkSize == null ? DEFAULT_RBF_CHUNK_SIZE : rbfChunkSize;
	}

	/** Valida i campi di competenza del file live: {@code mqttBrokerUrl}/{@code columns}. */
	private static void validateLiveTopLevel(DataOwnerJsonConfig raw, Path livePath)
			throws DataOwnerConfigException {
		requireNonBlank(raw.getMqttBrokerUrl(), "mqttBrokerUrl", livePath);
		if (raw.getColumns() == null || raw.getColumns().isEmpty()) {
			throw new DataOwnerConfigException("Campo obbligatorio 'columns' mancante o vuoto in " + livePath);
		}
	}

	private static void validateDataSource(DataSourceJsonConfig dataSource, Path jsonPath)
			throws DataOwnerConfigException {
		if (dataSource.getType() == null) {
			throw new DataOwnerConfigException("Campo obbligatorio 'dataSource.type' mancante in " + jsonPath);
		}
		switch (dataSource.getType()) {
			case CSV:
				if (dataSource.getCsv() == null) {
					throw new DataOwnerConfigException(
							"'dataSource.type' e' CSV ma la sezione 'dataSource.csv' e' assente in " + jsonPath);
				}
				requireNonBlank(dataSource.getCsv().getFilePath(), "dataSource.csv.filePath", jsonPath);
				break;
			case DB:
				if (dataSource.getDb() == null) {
					throw new DataOwnerConfigException(
							"'dataSource.type' e' DB ma la sezione 'dataSource.db' e' assente in " + jsonPath);
				}
				// Solo il nome tabella e' validato qui (obbligatorio): jdbcUrl/username/
				// password sono richiesti dalla connessione JDBC vera e propria aperta
				// da JdbcRecordSource.readAll(...) a runtime, non dal loader.
				requireNonBlank(dataSource.getDb().getTableName(), "dataSource.db.tableName", jsonPath);
				break;
			default:
				throw new DataOwnerConfigException("Valore di 'dataSource.type' non gestito: " + dataSource.getType());
		}
	}

	private static void validateColumns(List<ColumnConfig> columns, Path jsonPath) throws DataOwnerConfigException {
		final Set<Integer> seenIndexes = new HashSet<>();
		final Set<String> seenNames = new HashSet<>();
		int partyCount = 0;
		int idCount = 0;
		int globalIdCount = 0;
		int qidCount = 0;

		for (final ColumnConfig column : columns) {
			if (column.getRole() == null) {
				throw new DataOwnerConfigException("Colonna con 'role' mancante in " + jsonPath);
			}
			requireNonBlank(column.getName(), "columns[].name", jsonPath);

			// 'index' e' obbligatorio per ogni ruolo tranne QID (virtuale, valore dal
			// primo step MERGE/SPLIT del proprio 'preprocessing') e PARTY/GLOBAL_ID "a
			// valore costante" (valore da 'constantValue', o da un default risolto in
			// Java - vedi DataOwnerPipeline.applyConstantColumns): per questi ultimi due
			// la colonna non e' letta fisicamente dalla sorgente dati.
			final boolean indexOptional = column.getRole() == ColumnRole.QID || column.getRole() == ColumnRole.PARTY
					|| column.getRole() == ColumnRole.GLOBAL_ID;
			if (!indexOptional && column.getIndex() == null) {
				throw new DataOwnerConfigException("Colonna '" + column.getName() + "' (role=" + column.getRole()
						+ ") priva del campo 'index' obbligatorio in " + jsonPath);
			}
			if (column.getConstantValue() != null && column.getRole() != ColumnRole.PARTY
					&& column.getRole() != ColumnRole.GLOBAL_ID) {
				throw new DataOwnerConfigException("Colonna '" + column.getName() + "' (role=" + column.getRole()
						+ ") non supporta 'constantValue' (solo PARTY/GLOBAL_ID) in " + jsonPath);
			}
			if (column.getIndex() != null) {
				if (column.getIndex() < 0) {
					throw new DataOwnerConfigException(
							"Indice di colonna negativo per '" + column.getName() + "' in " + jsonPath);
				}
				if (!seenIndexes.add(column.getIndex())) {
					throw new DataOwnerConfigException(
							"Indice di colonna duplicato: " + column.getIndex() + " in " + jsonPath);
				}
			}
			if (!seenNames.add(column.getName().toUpperCase())) {
				throw new DataOwnerConfigException("Nome di colonna duplicato: " + column.getName() + " in " + jsonPath);
			}

			switch (column.getRole()) {
				case PARTY:
					partyCount++;
					break;
				case ID:
					idCount++;
					break;
				case GLOBAL_ID:
					globalIdCount++;
					break;
				case QID:
					qidCount++;
					validateQidColumn(column, jsonPath);
					break;
				case RAW:
					break;
			}
		}

		if (partyCount != 1) {
			throw new DataOwnerConfigException(
					"Deve esserci esattamente una colonna con role=PARTY (trovate " + partyCount + ") in " + jsonPath);
		}
		if (idCount != 1) {
			throw new DataOwnerConfigException(
					"Deve esserci esattamente una colonna con role=ID (trovate " + idCount + ") in " + jsonPath);
		}
		if (globalIdCount > 1) {
			throw new DataOwnerConfigException(
					"Puo' esserci al massimo una colonna con role=GLOBAL_ID (trovate " + globalIdCount + ") in "
							+ jsonPath);
		}
		if (qidCount < 1) {
			throw new DataOwnerConfigException("Deve esserci almeno una colonna con role=QID in " + jsonPath);
		}
	}

	private static void validateQidColumn(ColumnConfig column, Path jsonPath) throws DataOwnerConfigException {
		PreprocessingStepFactory.validateColumnSteps(column.getPreprocessing(), column, jsonPath);
		if (column.getHashFunctions() != null && column.getHashFunctions() <= 0) {
			throw new DataOwnerConfigException("Colonna QID '" + column.getName()
					+ "': 'hashFunctions' deve essere positivo, trovato " + column.getHashFunctions() + " in "
					+ jsonPath);
		}
		if (column.getMissingValueTokenCount() != null && column.getMissingValueTokenCount() <= 0) {
			throw new DataOwnerConfigException("Colonna QID '" + column.getName()
					+ "': 'missingValueTokenCount' deve essere positivo, trovato " + column.getMissingValueTokenCount()
					+ " in " + jsonPath);
		}
		if (column.isConstantWeightEncodingEnabled()) {
			final ConstantWeightEncodingJsonConfig cwe = column.getConstantWeightEncoding();
			final Integer min = cwe.getMinTrigrams();
			final Integer max = cwe.getMaxTrigrams();
			if (min == null || max == null || min <= 0 || max <= 0) {
				throw new DataOwnerConfigException("Colonna QID '" + column.getName()
						+ "': 'constantWeightEncoding.minTrigrams'/'maxTrigrams' devono essere presenti e positivi in "
						+ jsonPath);
			}
			if (min > max) {
				throw new DataOwnerConfigException("Colonna QID '" + column.getName()
						+ "': 'constantWeightEncoding.minTrigrams' (" + min + ") non puo' superare 'maxTrigrams' (" + max
						+ ") in " + jsonPath);
			}
		}
	}

	private static void validateMissingValueHandling(MissingValueHandlingJsonConfig missingValueHandling,
			List<ColumnConfig> columns, Path jsonPath) throws DataOwnerConfigException {
		if (missingValueHandling == null || !Boolean.TRUE.equals(missingValueHandling.getEnabled())) {
			return;
		}
		final List<String> anchorPriority = missingValueHandling.getAnchorPriority();
		if (anchorPriority == null || anchorPriority.isEmpty()) {
			throw new DataOwnerConfigException(
					"'missingValueHandling.enabled' e' true ma 'anchorPriority' e' assente o vuoto in " + jsonPath);
		}
		final Set<String> qidColumnNames = new HashSet<>();
		for (final ColumnConfig column : columns) {
			if (column.getRole() == ColumnRole.QID) {
				qidColumnNames.add(column.getName());
			}
		}
		for (final String anchorName : anchorPriority) {
			if (!qidColumnNames.contains(anchorName)) {
				throw new DataOwnerConfigException("'missingValueHandling.anchorPriority' referenzia '" + anchorName
						+ "', che non corrisponde a nessuna colonna QID dichiarata in 'columns' (match case-sensitive) in "
						+ jsonPath);
			}
		}
	}

	private static int resolveBloomFilterLength(BloomFilterJsonConfig bloomFilter, Path jsonPath)
			throws DataOwnerConfigException {
		if (bloomFilter == null || bloomFilter.getLength() == null) {
			return DEFAULT_BF_LENGTH;
		}
		final int length = bloomFilter.getLength();
		if (length <= 0) {
			throw new DataOwnerConfigException("'bloomFilter.length' deve essere positivo, trovato " + length + " in "
					+ jsonPath);
		}
		return length;
	}

	/**
	 * Descrive in forma leggibile la catena di hardening gia' validata da
	 * {@link #resolveHardener}, per uso esclusivamente di stampa (es.
	 * {@link DataOwnerConfig#describe()}) - non partecipa alla codifica.
	 *
	 * @return lista ordinata delle descrizioni di ciascuno step, vuota se nessun hardening e' configurato
	 */
	private static List<String> describeHardeningChain(BloomFilterJsonConfig bloomFilter) {
		if (bloomFilter == null) {
			return List.of();
		}
		final List<HardeningJsonConfig> chain = bloomFilter.getHardeningChain();
		if (chain != null && !chain.isEmpty()) {
			final List<String> descriptions = new ArrayList<>();
			for (final HardeningJsonConfig step : chain) {
				descriptions.add(describeHardening(step));
			}
			return descriptions;
		}
		final HardeningJsonConfig single = bloomFilter.getHardening();
		if (single != null && single.getType() != null && single.getType() != HardeningType.NONE) {
			return List.of(describeHardening(single));
		}
		return List.of();
	}

	private static String describeHardening(HardeningJsonConfig hardening) {
		switch (hardening.getType()) {
			case BLIP:
				final long seed = hardening.getSeed() != null ? hardening.getSeed() : DEFAULT_BLIP_SEED;
				return "BLIP(probability=" + hardening.getProbability() + ", seed=" + seed + ")";
			case XOR_FOLD:
				return "XOR_FOLD(foldCount=" + hardening.getFoldCount() + ")";
			default:
				return hardening.getType().toString();
		}
	}

	private static BloomFilterHardener resolveHardener(BloomFilterJsonConfig bloomFilter, int bfLength,
			Path jsonPath) throws DataOwnerConfigException {
		if (bloomFilter == null) {
			return new NoHardener();
		}

		final HardeningJsonConfig hardening = bloomFilter.getHardening();
		final List<HardeningJsonConfig> hardeningChain = bloomFilter.getHardeningChain();
		final boolean hasChain = hardeningChain != null && !hardeningChain.isEmpty();

		if (hardening != null && hasChain) {
			throw new DataOwnerConfigException(
					"'bloomFilter' specifica sia 'hardening' che 'hardeningChain': sono mutuamente esclusivi in "
							+ jsonPath);
		}

		if (hasChain) {
			return resolveHardenerChain(hardeningChain, bfLength, jsonPath);
		}

		if (hardening == null || hardening.getType() == null || hardening.getType() == HardeningType.NONE) {
			return new NoHardener();
		}

		return resolveSingleHardener(hardening, bfLength, jsonPath);
	}

	private static BloomFilterHardener resolveHardenerChain(List<HardeningJsonConfig> hardeningChain, int bfLength,
			Path jsonPath) throws DataOwnerConfigException {
		for (int i = 0; i < hardeningChain.size(); i++) {
			final HardeningType type = hardeningChain.get(i).getType();
			if (type == null || type == HardeningType.NONE) {
				throw new DataOwnerConfigException("'bloomFilter.hardeningChain[" + i
						+ "].type' e' assente o NONE: non e' un valore valido all'interno di una catena in "
						+ jsonPath);
			}
			// XorFolder dimezza la lunghezza dell'RBF: applicare un altro hardener dopo
			// il fold opererebbe su una rappresentazione non piu' direttamente
			// confrontabile con l'RBF originale, e romperebbe il contratto di
			// XorBitSetAttribute (la cardinalita' catturata deve riflettere l'ultimo
			// step). XOR_FOLD e' quindi ammesso solo come ultimo step della catena.
			if (type == HardeningType.XOR_FOLD && i != hardeningChain.size() - 1) {
				throw new DataOwnerConfigException(
						"'bloomFilter.hardeningChain': XOR_FOLD deve essere l'ultimo step della catena (trovato in posizione "
								+ i + " di " + hardeningChain.size() + ") in " + jsonPath);
			}
		}

		final List<BloomFilterHardener> steps = new ArrayList<>();
		for (final HardeningJsonConfig step : hardeningChain) {
			steps.add(resolveSingleHardener(step, bfLength, jsonPath));
		}

		return steps.size() == 1 ? steps.get(0) : new ChainedHardener(steps);
	}

	private static BloomFilterHardener resolveSingleHardener(HardeningJsonConfig hardening, int bfLength,
			Path jsonPath) throws DataOwnerConfigException {
		if (hardening.getType() == HardeningType.XOR_FOLD) {
			final Integer foldCount = hardening.getFoldCount();
			if (foldCount == null || foldCount <= 0) {
				throw new DataOwnerConfigException(
						"'bloomFilter.hardening.type' e' XOR_FOLD ma 'foldCount' e' assente o non positivo in "
								+ jsonPath);
			}
			// XorFolder dimezza la lunghezza dell'RBF ad ogni ripiegamento: un
			// foldCount incompatibile con bfLength produrrebbe un BitSet vuoto o
			// un'eccezione profonda dentro XorFolder invece di un errore di
			// configurazione leggibile, quindi lo intercettiamo qui.
			if (foldCount >= 31 || (bfLength >> foldCount) < 1 || bfLength % (1 << foldCount) != 0) {
				throw new DataOwnerConfigException("'bloomFilter.hardening.foldCount' (" + foldCount
						+ ") incompatibile con 'bloomFilter.length' (" + bfLength + ") in " + jsonPath);
			}
			if (foldCount > 1) {
				System.out.println("Attenzione: XOR-Folding con foldCount=" + foldCount
						+ " e' rischioso a livello di performance e puo' degradare eccessivamente i dati (perdita di informazione nell'RBF).");
			}
			return new XorFolder(foldCount);
		}

		if (hardening.getType() == HardeningType.BLIP) {
			final Double probability = hardening.getProbability();
			if (probability == null || probability <= 0 || probability > 1) {
				throw new DataOwnerConfigException(
						"'bloomFilter.hardening.type' e' BLIP ma 'probability' e' assente o fuori dall'intervallo (0, 1] in "
								+ jsonPath);
			}
			final long seed = hardening.getSeed() != null ? hardening.getSeed() : DEFAULT_BLIP_SEED;
			return new RandomizedResponse(probability, seed);
		}

		throw new DataOwnerConfigException("Valore di 'bloomFilter.hardening.type' non gestito: " + hardening.getType());
	}
}
