/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

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
			+ "  { \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"dataType\": \"TEXT\", \"hashFunctions\": 12, \"salt\": \"FN_\" },"
			+ "  { \"index\": 6, \"name\": \"YOB\", \"role\": \"QID\", \"dataType\": \"NUMERIC\", \"hashFunctions\": 13, \"salt\": \"YOB_\" }"
			+ "]}";

	private Path writeJson(String fileName, String content) throws IOException {
		final Path tempDir = Files.createTempDirectory("dataowner-config-test");
		tempDir.toFile().deleteOnExit();
		final Path path = tempDir.resolve(fileName);
		Files.writeString(path, content, StandardCharsets.UTF_8);
		path.toFile().deleteOnExit();
		return path;
	}

	@Test
	void loadsValidConfig() throws Exception {
		final Path path = writeJson("valid.json", VALID_JSON);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(path);

		assertEquals("A", config.getParty());
		assertEquals("tcp://localhost:1883", config.getMqttBrokerUrl());
		assertEquals("data-owner-A", config.getMqttClientId());
		assertEquals(DataSourceType.CSV, config.getDataSourceType());
		assertEquals("synthetic_ncvr/party_A.csv", config.getCsvFilePath());
		assertEquals(1024, config.getBloomFilterLength());
		assertEquals(5, config.getColumns().size());
		assertTrue(config.getHardener() instanceof NoHardener);
	}

	@Test
	void loadsValidConfigWithXorFoldHardening() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 }");
		final Path path = writeJson("valid-xorfold.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(path);

		assertTrue(config.getHardener() instanceof XorFolder);
	}

	@Test
	void loadsValidConfigWithBlipHardening() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"BLIP\", \"probability\": 0.2, \"seed\": 42 }");
		final Path path = writeJson("valid-blip.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(path);

		assertTrue(config.getHardener() instanceof RandomizedResponse);
	}

	@Test
	void loadsValidConfigWithBlipHardeningDefaultSeed() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"BLIP\", \"probability\": 0.2 }");
		final Path path = writeJson("valid-blip-default-seed.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(path);

		assertTrue(config.getHardener() instanceof RandomizedResponse);
	}

	@Test
	void blipWithoutProbabilityThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }", "{ \"type\": \"BLIP\" }");
		final Path path = writeJson("blip-no-probability.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(path));
		assertTrue(e.getMessage().contains("probability"));
	}

	@Test
	void blipWithOutOfRangeProbabilityThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("{ \"type\": \"NONE\" }",
				"{ \"type\": \"BLIP\", \"probability\": 1.5 }");
		final Path path = writeJson("blip-bad-probability.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void loadsValidConfigWithBlipThenXorFoldChain() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": ["
						+ "{ \"type\": \"BLIP\", \"probability\": 0.2, \"seed\": 42 },"
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 }" + "]");
		final Path path = writeJson("valid-chain.json", json);

		final DataOwnerConfig config = DataOwnerConfigLoader.load(path);

		assertTrue(config.getHardener() instanceof ChainedHardener);
	}

	@Test
	void xorFoldNotLastInChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": ["
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 },"
						+ "{ \"type\": \"BLIP\", \"probability\": 0.2, \"seed\": 42 }" + "]");
		final Path path = writeJson("chain-xorfold-not-last.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(path));
		assertTrue(e.getMessage().contains("ultimo step"));
	}

	@Test
	void duplicateXorFoldInChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": ["
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 1 },"
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 1 }" + "]");
		final Path path = writeJson("chain-duplicate-xorfold.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void bothHardeningAndHardeningChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardening\": { \"type\": \"NONE\" }, \"hardeningChain\": ["
						+ "{ \"type\": \"BLIP\", \"probability\": 0.2 }" + "]");
		final Path path = writeJson("chain-and-single.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(path));
		assertTrue(e.getMessage().contains("mutuamente esclusivi"));
	}

	@Test
	void noneTypeInsideChainThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hardening\": { \"type\": \"NONE\" }",
				"\"hardeningChain\": [" + "{ \"type\": \"NONE\" },"
						+ "{ \"type\": \"XOR_FOLD\", \"foldCount\": 2 }" + "]");
		final Path path = writeJson("chain-none-step.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void missingFileThrowsConfigException() throws IOException {
		final Path missing = Files.createTempDirectory("dataowner-config-test").resolve("does-not-exist.json");

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(missing));
		assertTrue(e.getMessage().contains("non trovato"));
	}

	@Test
	void malformedJsonThrowsConfigException() throws Exception {
		final Path path = writeJson("malformed.json", "{ \"party\": \"A\", ");

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void missingRequiredFieldThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"party\": \"A\",", "");
		final Path path = writeJson("no-party.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(path));
		assertTrue(e.getMessage().contains("party"));
	}

	@Test
	void duplicateColumnIndexThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 6, \"name\": \"YOB\", \"role\": \"QID\", \"dataType\": \"NUMERIC\", \"hashFunctions\": 13, \"salt\": \"YOB_\" }",
				"{ \"index\": 3, \"name\": \"YOB\", \"role\": \"QID\", \"dataType\": \"NUMERIC\", \"hashFunctions\": 13, \"salt\": \"YOB_\" }");
		final Path path = writeJson("dup-index.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void missingPartyRoleThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },", "");
		final Path path = writeJson("no-party-role.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void qidColumnWithoutDataTypeThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace(
				"{ \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"dataType\": \"TEXT\", \"hashFunctions\": 12, \"salt\": \"FN_\" },",
				"{ \"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"hashFunctions\": 12, \"salt\": \"FN_\" },");
		final Path path = writeJson("no-datatype.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(path));
		assertTrue(e.getMessage().contains("dataType"));
	}

	@Test
	void nonPositiveHashFunctionsThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"hashFunctions\": 12,", "\"hashFunctions\": 0,");
		final Path path = writeJson("bad-hashfunctions.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void nonPositiveBloomFilterLengthThrowsConfigException() throws Exception {
		final String json = VALID_JSON.replace("\"length\": 1024,", "\"length\": 0,");
		final Path path = writeJson("bad-length.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
	}

	@Test
	void incompatibleFoldCountThrowsConfigException() throws Exception {
		final String json = VALID_JSON
				.replace("\"length\": 1024,", "\"length\": 100,")
				.replace("{ \"type\": \"NONE\" }", "{ \"type\": \"XOR_FOLD\", \"foldCount\": 3 }");
		final Path path = writeJson("bad-foldcount.json", json);

		assertThrows(DataOwnerConfigException.class, () -> DataOwnerConfigLoader.load(path));
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
		final Path path = writeJson("db-no-table.json", json);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.load(path));
		assertTrue(e.getMessage().contains("tableName"));
	}
}
