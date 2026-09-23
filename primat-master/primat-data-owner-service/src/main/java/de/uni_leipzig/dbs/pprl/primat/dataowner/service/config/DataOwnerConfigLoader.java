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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.BloomFilterHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.ChainedHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.NoHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.RandomizedResponse;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.XorFolder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.DataOwnerConfig;

/**
 * Legge e valida il JSON di configurazione del Data Owner, producendo un
 * {@link DataOwnerConfig} pronto all'uso. Qui vive tutta la gestione degli
 * errori introdotti dalla configurazione via file (invece dei precedenti
 * argomenti posizionali da riga di comando): file mancante/non leggibile,
 * JSON malformato, campi obbligatori assenti o non validi, colonne
 * inconsistenti, parametri di tuning del Bloom Filter fuori range. Ogni
 * problema viene riportato come {@link DataOwnerConfigException} con un
 * messaggio comprensibile, mai come eccezione grezza di Gson/IO.
 */
public final class DataOwnerConfigLoader {

	/** Lunghezza dell'RBF in bit se la sezione {@code bloomFilter} o il campo {@code length} sono omessi. */
	public static final int DEFAULT_BF_LENGTH = 1024;

	/** Seed del PRNG di BLIP se il campo {@code seed} e' omesso nella sezione {@code hardening}. */
	public static final long DEFAULT_BLIP_SEED = 42L;

	private DataOwnerConfigLoader() {
	}

	/**
	 * @param jsonPath percorso del file JSON di configurazione
	 * @return la configurazione validata
	 * @throws DataOwnerConfigException se il file non e' leggibile, il JSON e'
	 *                                   malformato, o il contenuto non e' valido
	 */
	public static DataOwnerConfig load(Path jsonPath) throws DataOwnerConfigException {
		final String json = readFile(jsonPath);
		final DataOwnerJsonConfig raw = parseJson(json, jsonPath);

		validateTopLevel(raw, jsonPath);
		validateDataSource(raw.getDataSource(), jsonPath);
		validateColumns(raw.getColumns(), jsonPath);
		validateMissingValueHandling(raw.getMissingValueHandling(), raw.getColumns(), jsonPath);

		final int bfLength = resolveBloomFilterLength(raw.getBloomFilter(), jsonPath);
		final BloomFilterHardener hardener = resolveHardener(raw.getBloomFilter(), bfLength, jsonPath);
		final List<String> hardeningDescriptions = describeHardeningChain(raw.getBloomFilter());

		final DataSourceJsonConfig dataSource = raw.getDataSource();
		final String csvFilePath = dataSource.getType() == DataSourceType.CSV ? dataSource.getCsv().getFilePath()
				: null;
		final boolean csvHasHeader = dataSource.getType() == DataSourceType.CSV && dataSource.getCsv().isHasHeader();
		char csvDelimiter = ';';
		if (dataSource.getType() == DataSourceType.CSV) {
			final String d = dataSource.getCsv().getDelimiter();
			if (d.length() != 1) {
				throw new DataOwnerConfigException(
						"dataSource.csv.delimiter deve essere un solo carattere, trovato: \"" + d + "\" in " + jsonPath);
			}
			csvDelimiter = d.charAt(0);
		}
		final DbSourceConfig dbConfig = dataSource.getType() == DataSourceType.DB ? dataSource.getDb() : null;

		final MissingValueHandlingJsonConfig mvh = raw.getMissingValueHandling();
		final boolean missingValueHandlingEnabled = mvh != null && Boolean.TRUE.equals(mvh.getEnabled());
		final List<String> missingValueAnchorPriority = missingValueHandlingEnabled ? mvh.getAnchorPriority()
				: List.of();

		return new DataOwnerConfig(raw.getParty(), dataSource.getType(), csvFilePath, csvHasHeader, csvDelimiter, dbConfig,
				raw.getMqttBrokerUrl(), raw.getColumns(), bfLength, hardener, raw.isDebug(), missingValueHandlingEnabled,
				missingValueAnchorPriority, hardeningDescriptions);
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

	private static DataOwnerJsonConfig parseJson(String json, Path jsonPath) throws DataOwnerConfigException {
		final DataOwnerJsonConfig raw;
		try {
			raw = new Gson().fromJson(json, DataOwnerJsonConfig.class);
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

	private static void validateTopLevel(DataOwnerJsonConfig raw, Path jsonPath) throws DataOwnerConfigException {
		requireNonBlank(raw.getParty(), "party", jsonPath);
		requireNonBlank(raw.getMqttBrokerUrl(), "mqttBrokerUrl", jsonPath);
		if (raw.getDataSource() == null) {
			throw new DataOwnerConfigException("Campo obbligatorio 'dataSource' mancante in " + jsonPath);
		}
		if (raw.getColumns() == null || raw.getColumns().isEmpty()) {
			throw new DataOwnerConfigException("Campo obbligatorio 'columns' mancante o vuoto in " + jsonPath);
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
			if (column.getIndex() < 0) {
				throw new DataOwnerConfigException(
						"Indice di colonna negativo per '" + column.getName() + "' in " + jsonPath);
			}
			if (!seenIndexes.add(column.getIndex())) {
				throw new DataOwnerConfigException(
						"Indice di colonna duplicato: " + column.getIndex() + " in " + jsonPath);
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
		if (column.getDataType() == null) {
			throw new DataOwnerConfigException(
					"Colonna QID '" + column.getName() + "' priva del campo 'dataType' obbligatorio in " + jsonPath);
		}
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
				System.out.println("ATTENZIONE: XOR-Folding con foldCount=" + foldCount
						+ " e' rischioso a livello di performance e puo' degradare eccessivamente i dati (perdita di informazione nell'RBF).");
			}
			System.out.println(
					"ATTENZIONE: XOR-Folding attivo - impostare 'rbfSize' a " + (bfLength >> foldCount)
							+ " (= " + bfLength + " >> " + foldCount
							+ ") nella configurazione della Linkage Unit, cosi' il blocking (valueRange) e il controllo automatico a run-time corrispondono alla reale dimensione dell'RBF.");
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
