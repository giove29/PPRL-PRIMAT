/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.ChainedHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.NoHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.RandomizedResponse;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.XorFolder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.DataOwnerConfig;

/**
 * Copre il caricamento/validazione del JSON di configurazione del Data
 * Owner: un caso happy-path e un caso per ciascuna categoria di errore
 * gestita da {@link DataOwnerConfigLoader}. Niente {@code @TempDir}/
 * {@code assertInstanceOf}: il progetto e' fermo a JUnit Jupiter 5.1.0
 * (vedi root {@code pom.xml}), che non li include ancora.
 */
class DataOwnerConfigLoaderTest {

	private static final String VALID_JSON = "{"
			+ "\"party\": \"A\","
			+ "\"mqttBrokerUrl\": \"tcp://localhost:1883\","
			+ "\"dataSource\": {"
			+ "  \"type\": \"CSV\","
			+ "  \"csv\": { \"filePath\": \"synthetic_ncvr/party_A.csv\" }"
			+ "},"
			+ "\"bloomFilter\": {"
			+ "  \"length\": 1024,"
			+ "  \"hardening\": { \"type\": \"NONE\" }"
			+ "},"
			+ "\"columns\": ["
			+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
			+ "  { \"index\": 1, \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\" },"
			+ "  { \"index\": 2, \"name\": \"ID\", \"role\": \"ID\" },"
			+ "  { \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"TRIM\"}, {\"type\":\"UPPERCASE\"}, {\"type\":\"REMOVE_ACCENTS\"}, {\"type\":\"REMOVE_SPECIAL_CHARS\"}, {\"type\":\"TRUNCATE\",\"from\":0,\"to\":20} ], \"hashFunctions\": 12, \"salt\": \"FN_\" },"
			+ "  { \"index\": 6, \"name\": \"YOB\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"TRIM\"}, {\"type\":\"REMOVE_NON_DIGITS\"} ], \"hashFunctions\": 13, \"salt\": \"YOB_\" }"
			+ "]}";

	private static final String PREP_TEXT = "[ {\"type\":\"TRIM\"}, {\"type\":\"UPPERCASE\"}, {\"type\":\"REMOVE_ACCENTS\"}, {\"type\":\"REMOVE_SPECIAL_CHARS\"}, {\"type\":\"TRUNCATE\",\"from\":0,\"to\":20} ]";
	private static final String PREP_NUMERIC = "[ {\"type\":\"TRIM\"}, {\"type\":\"REMOVE_NON_DIGITS\"} ]";

	/**
	 * Stessa configurazione (tutte e 6 le colonne QID di febrl4_1_mixed, gia'
	 * fuse da schema+encoding, {@code hmacKey} incluso) che
	 * {@code primat-smu/smu.py} pubblica per il DO "org". Il digest atteso e'
	 * stato calcolato lato Python con {@code compute_config_hash} sulla stessa
	 * configurazione (vedi {@code primat-smu/config/do_org.json}/{@code
	 * encoding.json}): verifica la parita' byte-esatta richiesta dal design
	 * della SMU (vedi CLAUDE.md, changelog 2026-09-29).
	 */
	private static final String SMU_FEBRL4_1_MIXED_TEST_JSON = "{"
			+ "\"party\": \"org\","
			+ "\"mqttBrokerUrl\": \"tcp://localhost:1883\","
			+ "\"dataSource\": {"
			+ "  \"type\": \"CSV\","
			+ "  \"csv\": { \"filePath\": \"primat-examples/src/main/resources/febrl4_1_mixed/febrl4_1_clean.csv\" }"
			+ "},"
			+ "\"hmacKey\": \"SMU_FEBRL4_1_MIXED_TEST_KEY\","
			+ "\"bloomFilter\": { \"length\": 1024, \"hardeningChain\": [] },"
			+ "\"missingValueHandling\": { \"enabled\": false, \"anchorPriority\": [] },"
			+ "\"columns\": ["
			+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
			+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
			+ "  { \"index\": 2, \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\" },"
			+ "  { \"index\": 3, \"name\": \"given_name\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 25, \"salt\": \"GIVEN_NAME_\","
			+ "    \"constantWeightEncoding\": { \"enabled\": false, \"minTrigrams\": 6, \"maxTrigrams\": 12 }, \"missingValueTokenCount\": 6 },"
			+ "  { \"index\": 4, \"name\": \"surname\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 25, \"salt\": \"SURNAME_\","
			+ "    \"constantWeightEncoding\": { \"enabled\": false, \"minTrigrams\": 6, \"maxTrigrams\": 12 }, \"missingValueTokenCount\": 6 },"
			+ "  { \"index\": 8, \"name\": \"suburb\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 6, \"salt\": \"SUBURB_\","
			+ "    \"constantWeightEncoding\": { \"enabled\": false, \"minTrigrams\": 6, \"maxTrigrams\": 12 }, \"missingValueTokenCount\": 6 },"
			+ "  { \"index\": 9, \"name\": \"postcode\", \"role\": \"QID\", \"preprocessing\": " + PREP_NUMERIC + ", \"hashFunctions\": 6, \"salt\": \"POSTCODE_\","
			+ "    \"constantWeightEncoding\": { \"enabled\": false, \"minTrigrams\": 6, \"maxTrigrams\": 12 }, \"missingValueTokenCount\": 6 },"
			+ "  { \"index\": 10, \"name\": \"state\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 3, \"salt\": \"STATE_\","
			+ "    \"constantWeightEncoding\": { \"enabled\": false, \"minTrigrams\": 6, \"maxTrigrams\": 12 }, \"missingValueTokenCount\": 6 },"
			+ "  { \"index\": 11, \"name\": \"date_of_birth\", \"role\": \"QID\", \"preprocessing\": " + PREP_NUMERIC + ", \"hashFunctions\": 25, \"salt\": \"DATE_OF_BIRTH_\","
			+ "    \"constantWeightEncoding\": { \"enabled\": false, \"minTrigrams\": 6, \"maxTrigrams\": 12 }, \"missingValueTokenCount\": 6 }"
			+ "]}";

	/**
	 * Stessa forma di {@code MERGE_CONFIG_JSON} in
	 * {@code PreprocessingMergePipelineTest} (given_name+surname RAW fuse
	 * tramite un primo step MERGE nel 'preprocessing' di full_name): verifica
	 * che il digest resti riproducibile byte-identico anche quando una colonna
	 * QID ha un MERGE come primo step, non solo nel caso base gia' coperto da
	 * {@link #smuDigestMatchesPythonReferenceForFebrl41MixedTestSchema()}.
	 */
	private static final String MERGE_TRANSFORM_TEST_JSON = "{"
			+ "\"party\": \"org\","
			+ "\"mqttBrokerUrl\": \"tcp://localhost:1883\","
			+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
			+ "\"bloomFilter\": { \"length\": 1024, \"hardeningChain\": [] },"
			+ "\"missingValueHandling\": { \"enabled\": false, \"anchorPriority\": [] },"
			+ "\"columns\": ["
			+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
			+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
			+ "  { \"index\": 2, \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\" },"
			+ "  { \"index\": 3, \"name\": \"given_name\", \"role\": \"RAW\" },"
			+ "  { \"index\": 4, \"name\": \"surname\", \"role\": \"RAW\" },"
			+ "  { \"name\": \"full_name\", \"role\": \"QID\", \"preprocessing\": ["
			+ "      {\"type\":\"MERGE\",\"sources\":[\"given_name\",\"surname\"],\"merger\":{\"type\":\"BLANK\"}},"
			+ "      {\"type\":\"TRIM\"}, {\"type\":\"UPPERCASE\"}, {\"type\":\"REMOVE_ACCENTS\"}, {\"type\":\"REMOVE_SPECIAL_CHARS\"}, {\"type\":\"TRUNCATE\",\"from\":0,\"to\":20}"
			+ "    ], \"hashFunctions\": 25, \"salt\": \"FULL_NAME_\","
			+ "    \"constantWeightEncoding\": { \"enabled\": false, \"minTrigrams\": 6, \"maxTrigrams\": 12 }, \"missingValueTokenCount\": 6 }"
			+ "]}";

	@Test
	void smuDigestMatchesPythonReferenceForMergeTransform() throws Exception {
		final ConfigPaths paths = writeJson("merge-transform.json", MERGE_TRANSFORM_TEST_JSON);
		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);
		assertEquals("nBkvX8iZZkAYtUcOTSS7I1Zc87VaGoHhLYetPdUAjWATKmnYr5E0gssJ/Z/2L9La", config.computeConfigHash());
	}

	@Test
	void smuDigestMatchesPythonReferenceForFebrl41MixedTestSchema() throws Exception {
		final ConfigPaths paths = writeJson("smu-febrl4_1_mixed.json", SMU_FEBRL4_1_MIXED_TEST_JSON);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertEquals("vOPtAqrLKl+0fe2Xwnmcmnr2ITi7DGbeHSJNUAlK/2+aQMRIncY13F2eMsyQQBs/", config.computeConfigHash());
	}

	/** Percorsi dei due file (locale/live) prodotti da {@link #writeJson(String, String)}. */
	private static final class ConfigPaths {
		final Path local;
		final Path live;

		ConfigPaths(Path local, Path live) {
			this.local = local;
			this.live = live;
		}
	}

	/**
	 * Scrive un JSON "a file singolo" come usato ovunque in questa classe (stile
	 * pre-split, piu' comodo per costruire casi di test) dividendolo
	 * automaticamente in coppia locale/live secondo la stessa partizione usata
	 * in produzione ({@code party}/{@code debug}/{@code dataSource} -&gt;
	 * locale, tutto il resto -&gt; live) e scrivendo i due file risultanti.
	 */
	private ConfigPaths writeJson(String fileName, String content) throws IOException {
		final Path tempDir = Files.createTempDirectory("dataowner-config-test");
		tempDir.toFile().deleteOnExit();

		final JsonObject full = JsonParser.parseString(content).getAsJsonObject();
		final JsonObject local = new JsonObject();
		for (final String key : new String[] { "party", "debug", "dataSource", "rbfChunkSize" }) {
			if (full.has(key)) {
				local.add(key, full.get(key));
			}
		}
		final JsonObject live = new JsonObject();
		for (final Map.Entry<String, JsonElement> entry : full.entrySet()) {
			if (!local.has(entry.getKey())) {
				live.add(entry.getKey(), entry.getValue());
			}
		}

		final Path localPath = tempDir.resolve(fileName + "_local.json");
		final Path livePath = tempDir.resolve(fileName + "_live.json");
		Files.writeString(localPath, local.toString(), StandardCharsets.UTF_8);
		Files.writeString(livePath, live.toString(), StandardCharsets.UTF_8);
		localPath.toFile().deleteOnExit();
		livePath.toFile().deleteOnExit();
		return new ConfigPaths(localPath, livePath);
	}

	@Test
	void loadsValidConfig() throws Exception {
		final ConfigPaths paths = writeJson("valid.json", VALID_JSON);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertEquals("A", config.getParty());
		assertEquals("tcp://localhost:1883", config.getMqttBrokerUrl());
		assertEquals("data-owner-A", config.getMqttClientId());
		assertEquals(DataSourceType.CSV, config.getDataSourceType());
		assertEquals("synthetic_ncvr/party_A.csv", config.getCsvFilePath());
		assertEquals(1024, config.getBloomFilterLength());
		assertEquals(5, config.getColumns().size());
		assertTrue(config.getHardener() instanceof NoHardener);
		assertEquals(1024, config.computeEffectiveRbfBitLength());
	}

	@Test
	void rbfChunkSizeDefaultsTo2000WhenOmitted() throws Exception {
		final ConfigPaths paths = writeJson("rbf-chunk-default.json", VALID_JSON);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertEquals(DataOwnerConfigLoader.DEFAULT_RBF_CHUNK_SIZE, config.getRbfChunkSize());
		assertEquals(2000, config.getRbfChunkSize());
	}

	@Test
	void rbfChunkSizeExplicitValueIsHonored() throws Exception {
		final String json = VALID_JSON.replace("\"party\": \"A\",", "\"party\": \"A\", \"rbfChunkSize\": 500,");
		final ConfigPaths paths = writeJson("rbf-chunk-explicit.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertEquals(500, config.getRbfChunkSize());
	}

	@Test
	void rbfChunkSizeZeroOrNegativeThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"party\": \"A\",", "\"party\": \"A\", \"rbfChunkSize\": 0,");
		final ConfigPaths paths = writeJson("rbf-chunk-invalid.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("rbfChunkSize"));
	}

	@Test
	void loadsValidConfigWithXorFoldHardening() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 }");
		final ConfigPaths paths = writeJson("valid-xorfold.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertTrue(config.getHardener() instanceof XorFolder);
		// bloomFilter.length=1024 dimezzato 2 volte dal foldCount: la dimensione
		// effettiva trasmessa alla LU deve riflettere il post-hardening, non i
		// 1024 bit originali.
		assertEquals(256, config.computeEffectiveRbfBitLength());
	}

	@Test
	void loadsValidConfigWithBlipHardening() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"BLIP\", \"probability\": 0.2, \"seed\": 42 }");
		final ConfigPaths paths = writeJson("valid-blip.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertTrue(config.getHardener() instanceof RandomizedResponse);
	}

	@Test
	void loadsValidConfigWithBlipHardeningDefaultSeed() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"BLIP\", \"probability\": 0.2 }");
		final ConfigPaths paths = writeJson("valid-blip-default-seed.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertTrue(config.getHardener() instanceof RandomizedResponse);
	}

	@Test
	void blipWithoutProbabilityThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }", "{ \"type\": \"BLIP\" }");
		final ConfigPaths paths = writeJson("blip-no-probability.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("probability"));
	}

	@Test
	void blipWithOutOfRangeProbabilityThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"BLIP\", \"probability\": 1.5 }");
		final ConfigPaths paths = writeJson("blip-bad-probability.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void loadsValidConfigWithBlipThenXorFoldChain() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": ["
						+ "{ \"type\": \"BLIP\", \"probability\": 0.2, \"seed\": 42 },"
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 }" + "]");
		final ConfigPaths paths = writeJson("valid-chain.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertTrue(config.getHardener() instanceof ChainedHardener);
	}

	@Test
	void xorFoldNotLastInChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": ["
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 },"
						+ "{ \"type\": \"BLIP\", \"probability\": 0.2, \"seed\": 42 }" + "]");
		final ConfigPaths paths = writeJson("chain-xorfold-not-last.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("ultimo step"));
	}

	@Test
	void duplicateXorFoldInChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": ["
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 1 },"
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 1 }" + "]");
		final ConfigPaths paths = writeJson("chain-duplicate-xorfold.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void bothHardeningAndHardeningChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardening\": { \"type\": \"NONE\" }, \"hardeningChain\": ["
						+ "{ \"type\": \"BLIP\", \"probability\": 0.2 }" + "]");
		final ConfigPaths paths = writeJson("chain-and-single.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("mutuamente esclusivi"));
	}

	@Test
	void noneTypeInsideChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": [" + "{ \"type\": \"NONE\" },"
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 }" + "]");
		final ConfigPaths paths = writeJson("chain-none-step.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void missingFileThrowsConfigException() throws IOException {
		final Path missing = Files.createTempDirectory("dataowner-config-test").resolve("does-not-exist.json");

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(missing, missing));
		assertTrue(e.getMessage().contains("non trovato"));
	}

	@Test
	void malformedJsonThrowsConfigException() throws Exception {
		final Path tempDir = Files.createTempDirectory("dataowner-config-test");
		tempDir.toFile().deleteOnExit();
		final Path malformedLocal = tempDir.resolve("malformed_local.json");
		Files.writeString(malformedLocal, "{ \"party\": \"A\", ", StandardCharsets.UTF_8);
		malformedLocal.toFile().deleteOnExit();
		final Path emptyLive = tempDir.resolve("malformed_live.json");
		Files.writeString(emptyLive, "{}", StandardCharsets.UTF_8);
		emptyLive.toFile().deleteOnExit();

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(malformedLocal, emptyLive));
	}

	@Test
	void missingRequiredFieldThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"party\": \"A\",", "");
		final ConfigPaths paths = writeJson("no-party.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("party"));
	}

	@Test
	void duplicateColumnIndexThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 6, \"name\": \"YOB\", \"role\": \"QID\", \"preprocessing\": " + PREP_NUMERIC + ", \"hashFunctions\": 13, \"salt\": \"YOB_\" }",
				"{ \"index\": 3, \"name\": \"YOB\", \"role\": \"QID\", \"preprocessing\": " + PREP_NUMERIC + ", \"hashFunctions\": 13, \"salt\": \"YOB_\" }");
		final ConfigPaths paths = writeJson("dup-index.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void missingPartyRoleThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },", "");
		final ConfigPaths paths = writeJson("no-party-role.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void partyColumnWithoutIndexLoadsFineAndDefersToConfigPartyAtRuntime() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },",
				"{ \"name\": \"PARTY\", \"role\": \"PARTY\" },");
		final ConfigPaths paths = writeJson("party-no-index.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		final ColumnConfig partyColumn = config.getColumns().stream()
				.filter(c -> c.getRole() == ColumnRole.PARTY).findFirst().orElseThrow();
		assertNull(partyColumn.getIndex());
		assertNull(partyColumn.getConstantValue());
	}

	@Test
	void globalIdColumnWithoutIndexLoadsFineAndDefersToEmptyStringAtRuntime() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 1, \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\" },",
				"{ \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\" },");
		final ConfigPaths paths = writeJson("global-id-no-index.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		final ColumnConfig globalIdColumn = config.getColumns().stream()
				.filter(c -> c.getRole() == ColumnRole.GLOBAL_ID).findFirst().orElseThrow();
		assertNull(globalIdColumn.getIndex());
		assertNull(globalIdColumn.getConstantValue());
	}

	@Test
	void idColumnWithoutIndexStillThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 2, \"name\": \"ID\", \"role\": \"ID\" },",
				"{ \"name\": \"ID\", \"role\": \"ID\" },");
		final ConfigPaths paths = writeJson("id-no-index.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void constantValueOnQidColumnThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 12, \"salt\": \"FN_\" },",
				"{ \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 12, \"salt\": \"FN_\", \"constantValue\": \"x\" },");
		final ConfigPaths paths = writeJson("constant-value-on-qid.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void qidColumnWithoutPreprocessingThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 12, \"salt\": \"FN_\" },",
				"{ \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"hashFunctions\": 12, \"salt\": \"FN_\" },");
		final ConfigPaths paths = writeJson("no-preprocessing.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("preprocessing"));
	}

	@Test
	void qidColumnWithUnknownPreprocessingStepThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"\"preprocessing\": " + PREP_TEXT + ", \"hashFunctions\": 12, \"salt\": \"FN_\"",
				"\"preprocessing\": [ {\"type\":\"NOT_A_STEP\"} ], \"hashFunctions\": 12, \"salt\": \"FN_\"");
		final ConfigPaths paths = writeJson("bad-preprocessing.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("preprocessing"));
	}

	@Test
	void nonPositiveHashFunctionsThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hashFunctions\": 12,", "\"hashFunctions\": 0,");
		final ConfigPaths paths = writeJson("bad-hashfunctions.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void nonPositiveBloomFilterLengthThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"length\": 1024,", "\"length\": 0,");
		final ConfigPaths paths = writeJson("bad-length.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void incompatibleFoldCountThrowsConfigException() throws Exception {
		final String json = VALID_JSON
				.replace("\"length\": 1024,", "\"length\": 100,")
				.replace("{ \"type\": \"NONE\" }", "{ \"type\": \"XOR_FOLD\", \"foldCount\": 3 }");
		final ConfigPaths paths = writeJson("bad-foldcount.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(paths.local, paths.live));
	}

	@Test
	void dbSourceWithoutTableNameThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"\"dataSource\": {"
						+ "  \"type\": \"CSV\","
						+ "  \"csv\": { \"filePath\": \"synthetic_ncvr/party_A.csv\" }"
						+ "},",
				"\"dataSource\": {"
						+ "  \"type\": \"DB\","
						+ "  \"db\": { \"jdbcUrl\": \"jdbc:postgresql://localhost:5432/primat\" }"
						+ "},");
		final ConfigPaths paths = writeJson("db-no-table.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("tableName"));
	}

	@Test
	void rawColumnNotReferencedByAnyTransformThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"given_name\", \"role\": \"RAW\" },"
				+ "  { \"index\": 3, \"name\": \"surname\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + " }"
				+ "]}";
		final ConfigPaths paths = writeJson("raw-not-referenced.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("RAW"));
	}

	@Test
	void rawColumnReferencedByTwoStepsThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"given_name\", \"role\": \"RAW\" },"
				+ "  { \"index\": 3, \"name\": \"surname\", \"role\": \"RAW\" },"
				+ "  { \"index\": 4, \"name\": \"middle_name\", \"role\": \"RAW\" },"
				+ "  { \"name\": \"a\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"MERGE\",\"sources\":[\"given_name\",\"surname\"],\"merger\":{\"type\":\"BLANK\"}} ] },"
				+ "  { \"name\": \"b\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"MERGE\",\"sources\":[\"given_name\",\"middle_name\"],\"merger\":{\"type\":\"BLANK\"}} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("raw-referenced-twice.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("piu' di uno step"));
	}

	@Test
	void virtualQidColumnWithoutMergeOrSplitFirstStepThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"name\": \"full_name\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"TRIM\"} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("virtual-qid-no-transform-step.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("MERGE o SPLIT"));
	}

	@Test
	void physicalQidColumnWithMergeStepThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"given_name\", \"role\": \"RAW\" },"
				+ "  { \"index\": 3, \"name\": \"surname\", \"role\": \"RAW\" },"
				+ "  { \"index\": 4, \"name\": \"full_name\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"MERGE\",\"sources\":[\"given_name\",\"surname\"],\"merger\":{\"type\":\"BLANK\"}} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("physical-qid-with-merge.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("colonna fisica"));
	}

	@Test
	void simpleMergerWithoutSeparatorThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"given_name\", \"role\": \"RAW\" },"
				+ "  { \"index\": 3, \"name\": \"surname\", \"role\": \"RAW\" },"
				+ "  { \"name\": \"full_name\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"MERGE\",\"sources\":[\"given_name\",\"surname\"],\"merger\":{\"type\":\"SIMPLE\"}} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("simple-merger-no-separator.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("separator"));
	}

	@Test
	void loadsValidSplitConfig() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"date_of_birth\", \"role\": \"RAW\" },"
				+ "  { \"name\": \"dob_day\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":3,\"part\":0}, {\"type\":\"TRIM\"} ] },"
				+ "  { \"name\": \"dob_month\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":3,\"part\":1}, {\"type\":\"TRIM\"} ] },"
				+ "  { \"name\": \"dob_year\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":3,\"part\":2}, {\"type\":\"TRIM\"} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("valid-split.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(paths.local, paths.live);

		assertEquals(6, config.getColumns().size());
	}

	@Test
	void splitDoesNotCoverAllPartsThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"date_of_birth\", \"role\": \"RAW\" },"
				+ "  { \"name\": \"dob_day\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":3,\"part\":0} ] },"
				+ "  { \"name\": \"dob_month\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":3,\"part\":1} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("split-incomplete.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("non copre tutte le"));
	}

	@Test
	void splitDuplicatePartThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"date_of_birth\", \"role\": \"RAW\" },"
				+ "  { \"name\": \"dob_day\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":2,\"part\":0} ] },"
				+ "  { \"name\": \"dob_month\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":2,\"part\":0} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("split-duplicate-part.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("dichiarata da piu' di una colonna"));
	}

	@Test
	void splitInconsistentSplitterAcrossFragmentsThrowsConfigException() throws Exception {
		final String json = "{"
				+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"date_of_birth\", \"role\": \"RAW\" },"
				+ "  { \"name\": \"dob_day\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"BLANK\"},\"parts\":2,\"part\":0} ] },"
				+ "  { \"name\": \"dob_month\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"SPLIT\",\"source\":\"date_of_birth\",\"splitter\":{\"type\":\"COMMA\"},\"parts\":2,\"part\":1} ] }"
				+ "]}";
		final ConfigPaths paths = writeJson("split-inconsistent-splitter.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(paths.local, paths.live));
		assertTrue(e.getMessage().contains("incoerenti"));
	}
}
