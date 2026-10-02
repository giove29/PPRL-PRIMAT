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
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;
import de.uni_leipzig.dbs.pprl.primat.lu.service.LinkageUnitConfig;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.PartyPush;

/**
 * Copre il caricamento/validazione del JSON di configurazione della Linkage
 * Unit: un caso happy-path completo, uno minimale (verifica tutti i
 * default), e un caso per ciascuna regola di validazione gestita da
 * {@link LinkageUnitConfigLoader}. Mirror di {@code DataOwnerConfigLoaderTest}.
 *
 * <p>Dal 2026-10-02 {@code parties}/{@code rbfSize} non sono piu' campi JSON:
 * {@link LinkageUnitConfigLoader#load} non li tocca piu' affatto (un {@link
 * LinkageUnitConfig} appena caricato ha sempre roster vuoto e rbfSize 0), e
 * la loro validazione (compatibilita' dirty/clean-vs-{@code clusteringMethod},
 * rbfSize positivo) si testa direttamente sui due nuovi metodi standalone
 * {@link LinkageUnitConfigLoader#resolvePartyRoster}/{@link
 * LinkageUnitConfigLoader#validateRbfSize}, che operano sul roster cosi' come
 * arriva da un {@code LuConfigPush} della SMU, senza alcun file coinvolto.
 *
 * <p>Nessun test qui chiama {@link LinkageUnitConfig#getDbConnection()}:
 * quel metodo apre una {@code EntityManagerFactory} reale (tocca la rete),
 * mentre questa classe verifica solo il caricamento/validazione della
 * config, che resta volutamente senza alcuna dipendenza da Postgres.
 */
class LinkageUnitConfigLoaderTest {

	private static final String MINIMAL_MCL_JSON = "{"
			+ "\"mqttBrokerUrl\": \"tcp://localhost:1883\","
			+ "\"clusteringMethod\": \"MCL\""
			+ "}";

	private static final String FULL_CENTER_CLUSTERING_JSON = "{"
			+ "\"mqttBrokerUrl\": \"tcp://localhost:1884\","
			+ "\"clusteringMethod\": \"CENTER_CLUSTERING\","
			+ "\"similarityThreshold\": 0.75,"
			+ "\"blocking\": { \"jaccardLsh\": { \"keySize\": 5, \"keys\": 20, \"seed\": 7 } },"
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

		assertTrue(config.getParties().isEmpty());
		assertEquals(0, config.getRbfSize());
		assertEquals(ClusteringMethod.MCL, config.getClusteringMethod());
		assertEquals(0.6, config.getSimilarityThreshold());
		assertEquals(4, config.getLshKeySize());
		assertEquals(30, config.getLshKeys());
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
		assertEquals(7L, config.getLshSeed());
		assertEquals("tcp://localhost:1884", config.getMqttBrokerUrl());
		assertEquals(45L, config.getRbfCollectionTimeoutSeconds());
		assertEquals(5L, config.getRbfRepublishIntervalSeconds());
		assertEquals(0.4, config.getCenterClusteringConfig().getCenterAssignmentThreshold(), 1e-9);
	}

	@Test
	void validateRbfSizeAcceptsPositiveValues() throws Exception {
		LinkageUnitConfigLoader.validateRbfSize(512);
	}

	@Test
	void validateRbfSizeRejectsNonPositive() {
		final LinkageUnitConfigException zero = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.validateRbfSize(0));
		assertTrue(zero.getMessage().contains("rbfSize"));
		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader.validateRbfSize(-1));
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
		final Path path = writeJson("malformed.json", "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"clusteringMethod\": [ ");

		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader.load(path));
	}

	@Test
	void missingMqttBrokerUrlThrowsConfigException() throws Exception {
		final String json = "{ \"clusteringMethod\": \"MCL\" }";
		final Path path = writeJson("no-broker-url.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("mqttBrokerUrl"));
	}

	@Test
	void missingClusteringMethodThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\" }";
		final Path path = writeJson("no-method.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("clusteringMethod"));
	}

	@Test
	void resolvePartyRosterRejectsEmptyRoster() {
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(List.of(), ClusteringMethod.MCL));
		assertTrue(e.getMessage().contains("vuoto"));
		assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(null, ClusteringMethod.MCL));
	}

	@Test
	void resolvePartyRosterRejectsDuplicateName() {
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(
						List.of(new PartyPush("A", false), new PartyPush("A", false)), ClusteringMethod.MCL));
		assertTrue(e.getMessage().contains("duplicato"));
	}

	@Test
	void mscdApWithoutCleanPartyThrowsConfigException() {
		final List<PartyPush> parties = List.of(new PartyPush("A", false), new PartyPush("B", false));
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(parties, ClusteringMethod.MSCD_AP));
		assertTrue(e.getMessage().contains("MSCD_AP"));
	}

	@Test
	void loadsGlobalGreedyConfigWithDefaults() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ " \"clusteringMethod\": \"GLOBAL_GREEDY\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_global_greedy\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("global-greedy-ok.json", json);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(ClusteringMethod.GLOBAL_GREEDY, config.getClusteringMethod());
		assertNull(config.getGlobalGreedyConfig().getMergeThreshold());
	}

	@Test
	void loadsClipConfigWithDefaults() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ " \"clusteringMethod\": \"CLIP\","
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_clip\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final Path path = writeJson("clip-ok.json", json);

		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(path);

		assertEquals(ClusteringMethod.CLIP, config.getClusteringMethod());
		assertEquals(0.5, config.getClipConfig().getWeightSimilarity(), 1e-9);
		assertTrue(config.getClipConfig().isIgnoreWeakLinks());
	}

	@Test
	void resolvePartyRosterAcceptsAllCleanForGlobalGreedyAndClip() throws Exception {
		final List<PartyPush> allClean = List.of(new PartyPush("A", true), new PartyPush("B", true),
				new PartyPush("C", true));
		assertEquals(3, LinkageUnitConfigLoader.resolvePartyRoster(allClean, ClusteringMethod.GLOBAL_GREEDY).size());
		assertEquals(3, LinkageUnitConfigLoader.resolvePartyRoster(allClean, ClusteringMethod.CLIP).size());
	}

	@Test
	void rejectsGlobalGreedyWithAnyDirtyParty() {
		final List<PartyPush> mixed = List.of(new PartyPush("A", true), new PartyPush("B", false));
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(mixed, ClusteringMethod.GLOBAL_GREEDY));
		assertTrue(e.getMessage().contains("GLOBAL_GREEDY"));
	}

	@Test
	void rejectsClipWithAnyDirtyParty() {
		final List<PartyPush> mixed = List.of(new PartyPush("A", true), new PartyPush("B", false));
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(mixed, ClusteringMethod.CLIP));
		assertTrue(e.getMessage().contains("CLIP"));
	}

	@Test
	void resolvePartyRosterAcceptsAllDirtyForMclAndCenterClustering() throws Exception {
		final List<PartyPush> allDirty = List.of(new PartyPush("A", false), new PartyPush("B", false));
		assertEquals(2, LinkageUnitConfigLoader.resolvePartyRoster(allDirty, ClusteringMethod.MCL).size());
		assertEquals(2, LinkageUnitConfigLoader.resolvePartyRoster(allDirty, ClusteringMethod.CENTER_CLUSTERING).size());
	}

	@Test
	void rejectsMclWithAnyCleanParty() {
		final List<PartyPush> mixed = List.of(new PartyPush("A", false), new PartyPush("B", true));
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(mixed, ClusteringMethod.MCL));
		assertTrue(e.getMessage().contains("MCL"));
		assertTrue(e.getMessage().contains("dirty"));
	}

	@Test
	void rejectsCenterClusteringWithAnyCleanParty() {
		final List<PartyPush> mixed = List.of(new PartyPush("A", true), new PartyPush("B", false));
		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.resolvePartyRoster(mixed, ClusteringMethod.CENTER_CLUSTERING));
		assertTrue(e.getMessage().contains("CENTER_CLUSTERING"));
		assertTrue(e.getMessage().contains("dirty"));
	}

	@Test
	void persistenceEnabledWithMclThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"clusteringMethod\": \"MCL\","
				+ " \"persistence\": { \"enabled\": true } }";
		final Path path = writeJson("mcl-persistence.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("MCL"));
	}

	@Test
	void missingDatabaseForPersistentMethodThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"clusteringMethod\": \"MSCD_AP\" }";
		final Path path = writeJson("no-database.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("database"));
	}

	@Test
	void lowercaseEnumValueThrowsReadableConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"clusteringMethod\": \"mcl\" }";
		final Path path = writeJson("lowercase-enum.json", json);

		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader.load(path));
	}

	@Test
	void similarityThresholdOutOfRangeThrowsConfigException() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"clusteringMethod\": \"MCL\","
				+ " \"similarityThreshold\": 1.5 }";
		final Path path = writeJson("bad-threshold.json", json);

		final LinkageUnitConfigException e = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(path));
		assertTrue(e.getMessage().contains("similarityThreshold"));
	}

	private static String withThreshold(String thresholdJson, String autoThresholdJson) {
		return "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\","
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
		assertTrue(config.getParties().isEmpty());
		assertEquals(0, config.getRbfSize());
		assertEquals(ThresholdMode.AUTO_PRECISION, config.getThresholdSpec().getMode());
		assertEquals(0.03, config.getThresholdSpec().getEpsilon(), 1e-12);
		assertEquals(6, config.getLshKeySize());
		assertEquals(20, config.getLshKeys());
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
		final String clipJson = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"debug\": true,"
				+ " \"clusteringMethod\": \"CLIP\", \"similarityThreshold\": \"auto\","
				+ " \"blocking\": { \"jaccardLsh\": { \"keySize\": 8, \"keys\": 20 } },"
				+ " \"persistence\": { \"enabled\": false, \"csvOutputPath\": \"clip_debug_output.csv\" } }";
		final LinkageUnitConfig clip = LinkageUnitConfigLoader.load(writeJson("clip_auto.json", clipJson));
		assertEquals(ThresholdMode.AUTO, clip.getThresholdSpec().getMode());

		final String centerJson = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\", \"debug\": true,"
				+ " \"clusteringMethod\": \"CENTER_CLUSTERING\", \"similarityThreshold\": \"auto_recall\","
				+ " \"autoThreshold\": { \"epsilon\": 0.05 },"
				+ " \"blocking\": { \"jaccardLsh\": { \"keySize\": 8, \"keys\": 20 } },"
				+ " \"persistence\": { \"enabled\": false, \"csvOutputPath\": \"center_clustering_debug_output.csv\" } }";
		final LinkageUnitConfig center = LinkageUnitConfigLoader.load(writeJson("center_auto_recall.json", centerJson));
		assertEquals(ThresholdMode.AUTO_RECALL, center.getThresholdSpec().getMode());
		assertEquals(0.05, center.getThresholdSpec().getEpsilon(), 1e-12);
	}

	@Test
	void rangeThresholdDefaultsWhenSectionOmitted() throws Exception {
		final SimilarityThresholdSpec spec = LinkageUnitConfigLoader
				.load(writeJson("range_default.json", withThreshold("\"range\"", null))).getThresholdSpec();
		assertTrue(spec.isRange());
		assertFalse(spec.isAuto());
		assertTrue(Double.isNaN(spec.getFixedValue()));
		assertEquals(0.5, spec.getRangeFrom(), 1e-12);
		assertEquals(0.9, spec.getRangeTo(), 1e-12);
		assertEquals(0.1, spec.getRangeStep(), 1e-12);
		assertEquals(List.of(0.5, 0.6, 0.7, 0.8, 0.9), spec.rangeValues());
	}

	private static String withRange(String rangeJson) {
		return "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ " \"clusteringMethod\": \"MCL\", \"similarityThreshold\": \"range\", \"range\": " + rangeJson + " }";
	}

	@Test
	void rangeThresholdCustomBoundsAreParsedWithoutDrift() throws Exception {
		final SimilarityThresholdSpec spec = LinkageUnitConfigLoader
				.load(writeJson("range_custom.json", withRange("{ \"from\": 0.55, \"to\": 0.9, \"step\": 0.05 }")))
				.getThresholdSpec();
		assertEquals(0.55, spec.getRangeFrom(), 1e-12);
		assertEquals(List.of(0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9), spec.rangeValues());
	}

	@Test
	void invalidRangeThresholdConfigurationsThrow() throws Exception {
		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader
				.load(writeJson("range_to_le_from.json", withRange("{ \"from\": 0.8, \"to\": 0.5 }"))));
		assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(writeJson("range_step_zero.json", withRange("{ \"step\": 0 }"))));
		assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(writeJson("range_from_oob.json", withRange("{ \"from\": 0 }"))));
		assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(writeJson("range_to_oob.json", withRange("{ \"to\": 1.5 }"))));
		assertThrows(LinkageUnitConfigException.class, () -> LinkageUnitConfigLoader.load(
				writeJson("range_too_many_steps.json", withRange("{ \"from\": 0.01, \"to\": 1.0, \"step\": 0.001 }"))));
		// autoThreshold e' ammesso solo con auto/auto_precision/auto_recall, non con "range"
		final LinkageUnitConfigException withAuto = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(writeJson("range_with_auto.json", withThreshold("\"range\"", "{ \"epsilon\": 0.03 }"))));
		assertTrue(withAuto.getMessage().contains("autoThreshold"));
		// 'range' e' ammesso solo con similarityThreshold: "range"
		final LinkageUnitConfigException rangeWithFixed = assertThrows(LinkageUnitConfigException.class,
				() -> LinkageUnitConfigLoader.load(writeJson("fixed_with_range.json",
						"{ \"mqttBrokerUrl\": \"tcp://localhost:1883\","
								+ " \"clusteringMethod\": \"MCL\", \"similarityThreshold\": 0.7, \"range\": { \"from\": 0.5, \"to\": 0.9 } }")));
		assertTrue(rangeWithFixed.getMessage().contains("range"));
	}

	@Test
	void rangeThresholdForcesPersistenceOffEvenIfExplicitlyEnabled() throws Exception {
		final String json = "{ \"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ " \"clusteringMethod\": \"MSCD_AP\", \"similarityThreshold\": \"range\","
				+ " \"persistence\": { \"enabled\": true },"
				+ " \"database\": { \"url\": \"jdbc:postgresql://localhost:5432/primat_mscd_ap\", \"user\": \"primat\", \"password\": \"primat\" } }";
		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(writeJson("range_persistence.json", json));
		assertTrue(config.getThresholdSpec().isRange());
		assertFalse(config.isPersistenceEnabled());
	}

	@Test
	void fullReferenceExampleAlsoLoadsWithRangeThresholdVariant() throws Exception {
		final String original = Files.readString(fullReferencePath(), StandardCharsets.UTF_8);
		final JsonObject json = new Gson().fromJson(original, JsonObject.class);
		json.addProperty("similarityThreshold", "range");
		json.remove("autoThreshold");
		final LinkageUnitConfig config = LinkageUnitConfigLoader.load(writeJson("ref_range.json", json.toString()));
		assertTrue(config.getThresholdSpec().isRange());
		assertFalse(config.isPersistenceEnabled());
	}
}
