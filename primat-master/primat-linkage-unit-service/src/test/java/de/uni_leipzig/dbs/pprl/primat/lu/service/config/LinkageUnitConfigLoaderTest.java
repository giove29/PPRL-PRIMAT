/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;
import de.uni_leipzig.dbs.pprl.primat.lu.service.LinkageUnitConfig;

/**
 * Copre il caricamento/validazione del JSON di configurazione della Linkage
 * Unit: un caso happy-path completo, uno minimale (verifica tutti i
 * default), e un caso per ciascuna regola di validazione gestita da
 * {@link LinkageUnitConfigLoader}. Mirror di {@code DataOwnerConfigLoaderTest}.
 *
 * <p>Nessun test qui chiama {@link LinkageUnitConfig#getDbConnection()}:
 * quel metodo apre una {@code EntityManagerFactory} reale (tocca la rete),
 * mentre questa classe verifica solo il caricamento/validazione della
 * config, che resta volutamente senza alcuna dipendenza da Postgres.
 */
class LinkageUnitConfigLoaderTest {

	private static final String MINIMAL_MCL_JSON = "{"
			+ "\"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" }, { \"name\": \"B\" } ],"
			+ "\"clusteringMethod\": \"MCL\""
			+ "}";

	private static final String FULL_CENTER_CLUSTERING_JSON = "{"
			+ "\"mqttBrokerUrl\": \"tcp://localhost:1884\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": false }, { \"name\": \"B\", \"duplicateFree\": false } ],"
			+ "\"clusteringMethod\": \"CENTER_CLUSTERING\","
			+ "\"similarityThreshold\": 0.75,"
			+ "\"blocking\": { \"jaccardLsh\": { \"keySize\": 5, \"keys\": 20, \"valueRange\": 2048, \"seed\": 7 } },"
			+ "\"mqtt\": { \"brokerConnectTimeoutSeconds\": 12, \"rbfCollectionTimeoutSeconds\": 45, \"rbfRepublishIntervalSeconds\": 5 },"
			+ "\"cluster\": { \"blockingKeyStrategy\": \"INTERSECTION\", \"representantStrategy\": \"REPLACE\" },"
			+ "\"persistence\": { \"enabled\": true },"
			+ "\"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_center_clustering\", \"user\": \"primat\", \"password\": \"primat\" },"
			+ "\"centerClustering\": { \"centerAssignmentThreshold\": 0.4 }"
			+ "}";

	private Path writeJson(String fileName, String content) throws IOException {
		final Path tempDir = Files.createTempDirectory("lu-config-test");
		tempDir.toFile().deleteOnExit();
		final Path path = tempDir.resolve(fileName);
		Files.writeString(path, content, StandardCharsets.UTF_8);
		path.toFile().deleteOnExit();
		return path;
	}

	@Test
	void loadsMinimalConfigWithAllDefaults() throws Exception {
		final Path path = writeJson("minimal.json", MINIMAL_MCL_JSON);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(2, config.getParties().size());
		assertTrue(config.getParties().stream().noneMatch(Party::isDuplicateFree));
		assertEquals(ClusteringMethod.MCL, config.getClusteringMethod());
		assertEquals(0.6, config.getSimilarityThreshold());
		assertEquals(4, config.getLshKeySize());
		assertEquals(30, config.getLshKeys());
		assertEquals(1024, config.getLshValueRange());
		assertEquals(42L, config.getLshSeed());
		assertEquals("tcp://localhost:1883", config.getMqttBrokerUrl());
		assertEquals(30L, config.getRbfCollectionTimeoutSeconds());
		assertEquals(15L, config.getRbfRepublishIntervalSeconds());
		assertFalse(config.isPersistenceEnabled());
		assertEquals("mcl_debug_output.csv", config.getCsvOutputPath());
		assertNull(config.getCenterClusteringConfig().getCenterAssignmentThreshold());
	}

	@Test
	void debugDefaultsToFalseAndCanBeEnabled() throws Exception {
		assertFalse(LinkageUnitConfigLoader.load(writeJson("no_debug.json", MINIMAL_MCL_JSON)).isDebug());

		final String withDebug = MINIMAL_MCL_JSON.replace("\"clusteringMethod\"", "\"debug\": true, \"clusteringMethod\"");
		assertTrue(LinkageUnitConfigLoader.load(writeJson("debug.json", withDebug)).isDebug());
	}

	@Test
	void loadsFullConfigWithOverrides() throws Exception {
		final Path path = writeJson("full.json", FULL_CENTER_CLUSTERING_JSON);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(ClusteringMethod.CENTER_CLUSTERING, config.getClusteringMethod());
		assertEquals(0.75, config.getSimilarityThreshold());
		assertEquals(5, config.getLshKeySize());
		assertEquals(20, config.getLshKeys());
		assertEquals(2048, config.getLshValueRange());
		assertEquals(7L, config.getLshSeed());
		assertEquals("tcp://localhost:1884", config.getMqttBrokerUrl());
		assertEquals(45L, config.getRbfCollectionTimeoutSeconds());
		assertEquals(5L, config.getRbfRepublishIntervalSeconds());
		assertEquals(0.4, config.getCenterClusteringConfig().getCenterAssignmentThreshold(), 1e-9);
	}

	@Test
	void valueRangeDefaultsToRbfSizeWhenOmitted() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" } ],"
				+ " \"clusteringMethod\": \"MCL\", \"rbfSize\": 512 }";
		final Path path = writeJson("value-range-from-rbfsize.json", json);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(512, config.getLshValueRange());
	}

	@Test
	void explicitValueRangeOverridesRbfSize() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" } ],"
				+ " \"clusteringMethod\": \"MCL\", \"rbfSize\": 512,"
				+ " \"blocking\": { \"jaccardLsh\": { \"valueRange\": 2048 } } }";
		final Path path = writeJson("value-range-explicit-override.json", json);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(2048, config.getLshValueRange());
	}

	@Test
	void valueRangeDefaultsTo1024WhenNeitherRbfSizeNorValueRangeDeclared() throws Exception {
		final Path path = writeJson("value-range-no-rbfsize.json", MINIMAL_MCL_JSON);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(1024, config.getLshValueRange());
	}

	@Test
	void missingFileThrowsConfigException() throws IOException {
		final Path missing = Files.createTempDirectory("lu-config-test").resolve("does-not-exist.json");

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(missing));
		assertTrue(e.getMessage().contains("non trovato"));
	}

	@Test
	void malformedJsonThrowsConfigException() throws Exception {
		final Path path = writeJson("malformed.json", "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ ");

		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader.load(path));
	}

	@Test
	void missingMqttBrokerUrlThrowsConfigException() throws Exception {
		final String json = "{ \"parties\": [ { \"name\": \"A\" } ], \"clusteringMethod\": \"MCL\" }";
		final Path path = writeJson("no-broker-url.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("mqttBrokerUrl"));
	}

	@Test
	void missingPartiesThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"clusteringMethod\": \"MCL\" }";
		final Path path = writeJson("no-parties.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("parties"));
	}

	@Test
	void missingClusteringMethodThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" } ] }";
		final Path path = writeJson("no-method.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("clusteringMethod"));
	}

	@Test
	void duplicatePartyNameThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" }, { \"name\": \"A\" } ], \"clusteringMethod\": \"MCL\" }";
		final Path path = writeJson("dup-party.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("duplicato"));
	}

	@Test
	void mscdApWithoutCleanPartyThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" }, { \"name\": \"B\" } ], \"clusteringMethod\": \"MSCD_AP\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_mscd_ap\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("mscd-ap-without-clean-party.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("MSCD_AP"));
	}

	@Test
	void loadsGlobalGreedyConfigWithAllCleanParties() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": true },"
				+ " { \"name\": \"B\", \"duplicateFree\": true }, { \"name\": \"C\", \"duplicateFree\": true } ],"
				+ " \"clusteringMethod\": \"GLOBAL_GREEDY\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_global_greedy\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("global-greedy-ok.json", json);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(ClusteringMethod.GLOBAL_GREEDY, config.getClusteringMethod());
		assertNull(config.getGlobalGreedyConfig().getMergeThreshold());
	}

	@Test
	void rejectsGlobalGreedyWithAnyDirtyParty() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": true },"
				+ " { \"name\": \"B\", \"duplicateFree\": false } ], \"clusteringMethod\": \"GLOBAL_GREEDY\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_global_greedy\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("global-greedy-dirty.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("GLOBAL_GREEDY"));
	}

	@Test
	void loadsClipConfigWithAllCleanParties() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": true },"
				+ " { \"name\": \"B\", \"duplicateFree\": true }, { \"name\": \"C\", \"duplicateFree\": true } ],"
				+ " \"clusteringMethod\": \"CLIP\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_clip\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("clip-ok.json", json);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(ClusteringMethod.CLIP, config.getClusteringMethod());
		assertEquals(0.5, config.getClipConfig().getWeightSimilarity(), 1e-9);
		assertTrue(config.getClipConfig().isIgnoreWeakLinks());
	}

	@Test
	void rejectsMclWithAnyCleanParty() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": false },"
				+ " { \"name\": \"B\", \"duplicateFree\": true } ], \"clusteringMethod\": \"MCL\" }";
		final Path path = writeJson("mcl-clean.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("MCL"));
		assertTrue(e.getMessage().contains("dirty"));
	}

	@Test
	void rejectsCenterClusteringWithAnyCleanParty() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": true },"
				+ " { \"name\": \"B\", \"duplicateFree\": false } ], \"clusteringMethod\": \"CENTER_CLUSTERING\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_center_clustering\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("center-clean.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("CENTER_CLUSTERING"));
		assertTrue(e.getMessage().contains("dirty"));
	}

	@Test
	void loadsCenterClusteringWithAllDirtyParties() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": false },"
				+ " { \"name\": \"B\" } ], \"clusteringMethod\": \"CENTER_CLUSTERING\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_center_clustering\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("center-dirty.json", json);

		assertEquals(ClusteringMethod.CENTER_CLUSTERING, LinkageUnitConfigLoader.load(path).getClusteringMethod());
	}

	@Test
	void rejectsClipWithAnyDirtyParty() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": true },"
				+ " { \"name\": \"B\", \"duplicateFree\": false } ], \"clusteringMethod\": \"CLIP\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_clip\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("clip-dirty.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("CLIP"));
	}

	@Test
	void persistenceEnabledWithMclThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" } ], \"clusteringMethod\": \"MCL\","
				+ " \"persistence\": { \"enabled\": true } }";
		final Path path = writeJson("mcl-persistence.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("MCL"));
	}

	@Test
	void missingDatabaseForPersistentMethodThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": true } ],"
				+ " \"clusteringMethod\": \"MSCD_AP\" }";
		final Path path = writeJson("no-database.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("database"));
	}

	@Test
	void lowercaseEnumValueThrowsReadableConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" } ], \"clusteringMethod\": \"mcl\" }";
		final Path path = writeJson("lowercase-enum.json", json);

		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader.load(path));
	}

	@Test
	void similarityThresholdOutOfRangeThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\" } ], \"clusteringMethod\": \"MCL\","
				+ " \"similarityThreshold\": 1.5 }";
		final Path path = writeJson("bad-threshold.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("similarityThreshold"));
	}

	private static String withThreshold(String thresholdJson, String autoThresholdJson) {
		return "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"parties\": [ { \"name\": \"A\", \"duplicateFree\": false } ],"
				+ " \"clusteringMethod\": \"MCL\", \"similarityThreshold\": " + thresholdJson
				+ (autoThresholdJson != null ? ", \"autoThreshold\": " + autoThresholdJson : "") + " }";
	}

	@Test
	void autoThresholdModesAreParsedWithDefaultOrCustomEpsilon() throws Exception {
		final SimilarityThresholdSpec exact = LinkageUnitConfigLoader
				.load(writeJson("auto.json", withThreshold("\"auto\"", null))).getThresholdSpec();
		assertTrue(exact.isAuto());
		assertEquals(ThresholdMode.AUTO, exact.getMode());
		assertEquals(0.03, exact.getEpsilon(), 1e-12);
		assertTrue(Double.isNaN(exact.getFixedValue()));

		final SimilarityThresholdSpec precision = LinkageUnitConfigLoader
				.load(writeJson("auto_p.json", withThreshold("\"auto_precision\"", null))).getThresholdSpec();
		assertEquals(ThresholdMode.AUTO_PRECISION, precision.getMode());
		assertEquals(0.03, precision.getEpsilon(), 1e-12);

		final SimilarityThresholdSpec recall = LinkageUnitConfigLoader
				.load(writeJson("auto_r.json", withThreshold("\"AUTO_RECALL\"", "{ \"epsilon\": 0.05 }"))).getThresholdSpec();
		assertEquals(ThresholdMode.AUTO_RECALL, recall.getMode());
		assertEquals(0.05, recall.getEpsilon(), 1e-12);
	}

	@Test
	void fixedThresholdIsStillANumberAndHasNoMode() throws Exception {
		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(writeJson("fixed.json", withThreshold("0.72", null)));
		assertFalse(config.getThresholdSpec().isAuto());
		assertEquals(0.72, config.getSimilarityThreshold(), 1e-12);
		assertNull(config.getThresholdSpec().getMode());
	}

	@Test
	void invalidAutoThresholdConfigurationsThrow() throws Exception {
		assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(writeJson("unknown.json", withThreshold("\"dynamic\"", null))));
		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader
				.load(writeJson("eps_zero.json", withThreshold("\"auto_precision\"", "{ \"epsilon\": 0 }"))));
		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader
				.load(writeJson("eps_big.json", withThreshold("\"auto_recall\"", "{ \"epsilon\": 0.25 }"))));
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader
				.load(writeJson("eps_fixed.json", withThreshold("0.7", "{ \"epsilon\": 0.03 }"))));
		assertTrue(e.getMessage().contains("autoThreshold"));
		assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(writeJson("bool.json", withThreshold("true", null))));
	}

	private static Path fullReferencePath() throws Exception {
		return Path.of(LinkageUnitConfigLoaderTest.class.getResource("/config/examples/full_reference.json").toURI());
	}

	@Test
	void fullReferenceExampleLoadsWithEverySectionRead() throws Exception {
		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(fullReferencePath());

		assertEquals(ClusteringMethod.MSCD_AP, config.getClusteringMethod());
		assertTrue(config.isDebug());
		assertEquals(3, config.getParties().size());
		assertEquals(ThresholdMode.AUTO_PRECISION, config.getThresholdSpec().getMode());
		assertEquals(0.03, config.getThresholdSpec().getEpsilon(), 1e-12);
		assertEquals(Integer.valueOf(1024), config.getRbfSize());
		assertEquals(6, config.getLshKeySize());
		assertEquals(20, config.getLshKeys());
		assertEquals(1024, config.getLshValueRange());
		assertEquals(42L, config.getLshSeed());
		assertEquals(30L, config.getBrokerConnectTimeoutSeconds());
		assertEquals(30L, config.getRbfCollectionTimeoutSeconds());
		assertEquals(15L, config.getRbfRepublishIntervalSeconds());
		assertTrue(config.isPersistenceEnabled());
		assertEquals("mscd_ap_output.csv", config.getCsvOutputPath());
		assertEquals(20000, config.getApConfig().getMaxApIteration());
		assertEquals(0.7, config.getCenterClusteringConfig().getCenterAssignmentThreshold(), 1e-12);
		assertEquals(100, config.getMclConfig().getMaxIterations());
		assertEquals(0.7, config.getGlobalGreedyConfig().getMergeThreshold(), 1e-12);
		assertEquals(0.4, config.getClipConfig().getWeightLinkStrength(), 1e-12);
	}

	@Test
	void fullReferenceExampleAlsoLoadsWithTheOtherThresholdVariants() throws Exception {
		final String original = Files.readString(fullReferencePath(), StandardCharsets.UTF_8);
		for (final String variant : new String[] { "auto", "auto_recall" }) {
			final JsonObject json = new Gson().fromJson(original, JsonObject.class);
			json.addProperty("similarityThreshold", variant);
			final LinkageUnitConfig config = LinkageUnitConfigLoader.load(writeJson("ref_" + variant + ".json", json.toString()));
			assertEquals(ThresholdMode.valueOf(variant.toUpperCase()), config.getThresholdSpec().getMode());
		}
		final JsonObject fixed = new Gson().fromJson(original, JsonObject.class);
		fixed.addProperty("similarityThreshold", 0.75);
		fixed.remove("autoThreshold");
		assertEquals(0.75,
				LinkageUnitConfigLoader.load(writeJson("ref_fixed.json", fixed.toString())).getSimilarityThreshold(), 1e-12);
	}

	@Test
	void autoThresholdExampleConfigsLoad() throws Exception {
		final LinkageUnitConfig clip = LinkageUnitConfigLoader.load(
				Path.of(LinkageUnitConfigLoaderTest.class.getResource("/config/examples/febrl4_clean_clip_auto.json").toURI()));
		assertEquals(ThresholdMode.AUTO, clip.getThresholdSpec().getMode());

		final LinkageUnitConfig center = LinkageUnitConfigLoader.load(Path.of(LinkageUnitConfigLoaderTest.class
				.getResource("/config/examples/febrl3_dirty_center_clustering_auto_recall.json").toURI()));
		assertEquals(ThresholdMode.AUTO_RECALL, center.getThresholdSpec().getMode());
		assertEquals(0.05, center.getThresholdSpec().getEpsilon(), 1e-12);
	}
}
