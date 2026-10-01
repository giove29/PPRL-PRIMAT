/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.dataowner.service.DataOwnerConfig;

/**
 * Copre {@link DataOwnerConfigLoader#loadBootstrap(Path)}: il Data Owner e'
 * lanciato con il solo file locale, che dichiara al proprio interno dove
 * trovare (o dover creare) il file live. {@link DataOwnerConfigLoader#load}
 * (a due argomenti, usato da tutti gli altri test del modulo) resta invariato
 * e non e' oggetto di questa classe.
 */
class DataOwnerConfigLoaderBootstrapTest {

	private static final String PREP_TEXT = "[ {\"type\":\"TRIM\"}, {\"type\":\"UPPERCASE\"} ]";

	private Path localJson(Path dir, String liveConfigPath) throws IOException {
		final String json = "{"
				+ "\"party\": \"A\","
				+ "\"debug\": false,"
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"liveConfigPath\": \"" + liveConfigPath + "\","
				+ "\"bootstrapMqttBrokerUrl\": \"tcp://localhost:1883\""
				+ "}";
		final Path path = dir.resolve("config_local.json");
		Files.writeString(path, json, StandardCharsets.UTF_8);
		return path;
	}

	private String validLiveJson() {
		return "{"
				+ "\"mqttBrokerUrl\": \"tcp://localhost:1884\","
				+ "\"bloomFilter\": { \"length\": 1024, \"hardening\": { \"type\": \"NONE\" } },"
				+ "\"columns\": ["
				+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
				+ "  { \"index\": 1, \"name\": \"ID\", \"role\": \"ID\" },"
				+ "  { \"index\": 2, \"name\": \"FN\", \"role\": \"QID\", \"preprocessing\": " + PREP_TEXT + " }"
				+ "]}";
	}

	@Test
	void liveFileMissingReturnsPendingAndWritesPlaceholder() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final Path localPath = localJson(dir, "config_live.json");

		final DataOwnerConfigLoader.BootstrapResult result = DataOwnerConfigLoader.loadBootstrap(localPath);

		assertTrue(result.config.isPending());
		assertEquals("NOT_FOUND", result.config.getVersion());
		assertEquals("tcp://localhost:1883", result.config.getMqttBrokerUrl());
		assertEquals("A", result.config.getParty());
		assertEquals(dir.resolve("config_live.json"), result.livePath);

		final String written = Files.readString(result.livePath, StandardCharsets.UTF_8);
		assertEquals("{\n  \"version\": \"NOT_FOUND\"\n}\n", written);
	}

	@Test
	void secondBootstrapAfterPlaceholderAlreadyWrittenStillReturnsPending() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final Path localPath = localJson(dir, "config_live.json");

		DataOwnerConfigLoader.loadBootstrap(localPath);
		final DataOwnerConfigLoader.BootstrapResult second = DataOwnerConfigLoader.loadBootstrap(localPath);

		assertTrue(second.config.isPending());
		assertEquals("NOT_FOUND", second.config.getVersion());
	}

	@Test
	void liveFileAlreadyValidLoadsNormallyAndIsNotPending() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final Path localPath = localJson(dir, "config_live.json");
		Files.writeString(dir.resolve("config_live.json"), validLiveJson(), StandardCharsets.UTF_8);

		final DataOwnerConfigLoader.BootstrapResult result = DataOwnerConfigLoader.loadBootstrap(localPath);

		assertFalse(result.config.isPending());
		// Il mqttBrokerUrl live, non quello di bootstrap, e' quello autorevole.
		assertEquals("tcp://localhost:1884", result.config.getMqttBrokerUrl());
		assertEquals(1, result.config.getColumns().stream().filter(c -> c.getRole() == ColumnRole.QID).count());
	}

	@Test
	void incompleteLiveFileWithoutColumnsIsTreatedAsPending() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final Path localPath = localJson(dir, "config_live.json");
		Files.writeString(dir.resolve("config_live.json"), "{\"version\": \"7\"}", StandardCharsets.UTF_8);

		final DataOwnerConfigLoader.BootstrapResult result = DataOwnerConfigLoader.loadBootstrap(localPath);

		assertTrue(result.config.isPending());
		// Lo stato pending forza sempre NOT_FOUND, anche se il file incompleto
		// conteneva un altro valore di 'version': non deve mai sembrare allineato
		// a una versione reale della SMU.
		assertEquals("NOT_FOUND", result.config.getVersion());
	}

	@Test
	void missingLiveConfigPathThrowsConfigException() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final String json = "{"
				+ "\"party\": \"A\",\"debug\": false,"
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"bootstrapMqttBrokerUrl\": \"tcp://localhost:1883\""
				+ "}";
		final Path localPath = dir.resolve("config_local.json");
		Files.writeString(localPath, json, StandardCharsets.UTF_8);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.loadBootstrap(localPath));
		assertTrue(e.getMessage().contains("liveConfigPath"));
	}

	@Test
	void missingBootstrapMqttBrokerUrlThrowsConfigException() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final String json = "{"
				+ "\"party\": \"A\",\"debug\": false,"
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"liveConfigPath\": \"config_live.json\""
				+ "}";
		final Path localPath = dir.resolve("config_local.json");
		Files.writeString(localPath, json, StandardCharsets.UTF_8);

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.loadBootstrap(localPath));
		assertTrue(e.getMessage().contains("bootstrapMqttBrokerUrl"));
	}

	@Test
	void liveConfigPathPointingAtDirectoryThrowsConfigException() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		Files.createDirectory(dir.resolve("config_live.json"));
		final Path localPath = localJson(dir, "config_live.json");

		final DataOwnerConfigException e = assertThrows(DataOwnerConfigException.class,
				() -> DataOwnerConfigLoader.loadBootstrap(localPath));
		assertTrue(e.getMessage().contains("live"));
	}

	@Test
	void liveConfigPathResolvedRelativeToLocalFileDirectoryNotCwd() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final Path subDir = Files.createDirectory(dir.resolve("sub"));
		final Path localPath = subDir.resolve("config_local.json");
		final String json = "{"
				+ "\"party\": \"A\",\"debug\": false,"
				+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"x.csv\" } },"
				+ "\"liveConfigPath\": \"config_live.json\","
				+ "\"bootstrapMqttBrokerUrl\": \"tcp://localhost:1883\""
				+ "}";
		Files.writeString(localPath, json, StandardCharsets.UTF_8);

		final DataOwnerConfigLoader.BootstrapResult result = DataOwnerConfigLoader.loadBootstrap(localPath);

		assertEquals(subDir.resolve("config_live.json").toAbsolutePath().normalize(), result.livePath);
		assertTrue(Files.exists(result.livePath));
	}

	@Test
	void pendingConfigComputeConfigHashDoesNotThrow() throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-bootstrap-test");
		final Path localPath = localJson(dir, "config_live.json");

		final DataOwnerConfig config = DataOwnerConfigLoader.loadBootstrap(localPath).config;

		// Placeholder innocuo (nessuna colonna QID): deve restare calcolabile
		// senza eccezioni anche se non ha alcun senso usarlo finche' pending.
		config.computeConfigHash();
	}
}
