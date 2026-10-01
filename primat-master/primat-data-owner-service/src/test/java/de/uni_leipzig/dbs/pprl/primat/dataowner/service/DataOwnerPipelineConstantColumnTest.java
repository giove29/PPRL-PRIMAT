/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.CsvRecordSource;

/**
 * Verifica le colonne PARTY/GLOBAL_ID "a valore costante" (senza {@code index}):
 * il dataset CSV qui non ha affatto colonne fisiche per PARTY/GLOBAL_ID, solo
 * ID+un QID, mirror del caso di produzione citato dall'utente (nessuna colonna
 * PARTY nel dataset, nessun Ground Truth disponibile).
 */
class DataOwnerPipelineConstantColumnTest {

	private List<Record> runPipeline(String party, String partyColumnJson, String globalIdColumnJson)
			throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-constant-column-test");
		dir.toFile().deleteOnExit();
		final Path csv = dir.resolve("data.csv");
		Files.writeString(csv, "1;JOHN\n2;MARY\n", StandardCharsets.UTF_8);
		csv.toFile().deleteOnExit();

		final String json = "{\"party\": \"" + party + "\", \"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": {\"type\": \"CSV\", \"csv\": {\"filePath\": \""
				+ csv.toString().replace("\\", "\\\\") + "\"}},"
				+ "\"columns\": [" + partyColumnJson + "," + globalIdColumnJson + ","
				+ "{\"index\": 0, \"name\": \"ID\", \"role\": \"ID\"},"
				+ "{\"index\": 1, \"name\": \"FN\", \"role\": \"QID\", \"preprocessing\": [ {\"type\":\"TRIM\"}, {\"type\":\"UPPERCASE\"}, {\"type\":\"REMOVE_ACCENTS\"}, {\"type\":\"REMOVE_SPECIAL_CHARS\"}, {\"type\":\"TRUNCATE\",\"from\":0,\"to\":20} ]}]}";
		final JsonObject full = JsonParser.parseString(json).getAsJsonObject();
		final JsonObject local = new JsonObject();
		for (final String key : new String[] { "party", "debug", "dataSource" }) {
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
		final Path localPath = dir.resolve("config_local.json");
		final Path livePath = dir.resolve("config_live.json");
		Files.writeString(localPath, local.toString(), StandardCharsets.UTF_8);
		Files.writeString(livePath, live.toString(), StandardCharsets.UTF_8);
		localPath.toFile().deleteOnExit();
		livePath.toFile().deleteOnExit();

		final DataOwnerConfig config = DataOwnerConfigLoader.load(localPath, livePath);
		return new DataOwnerPipeline(
				new CsvRecordSource(config.getCsvFilePath(), config.isCsvHasHeader(), config.getCsvDelimiter()),
				config).run();
	}

	@Test
	void partyColumnWithoutIndexOrConstantValueDefaultsToConfigParty() throws Exception {
		final List<Record> records = runPipeline("org1", "{\"name\": \"PARTY\", \"role\": \"PARTY\"}",
				"{\"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\", \"constantValue\": \"\"}");

		assertEquals(List.of("org1", "org1"),
				records.stream().map(r -> r.getParty().getName()).collect(Collectors.toList()));
		// vincolo di non-rottura esplicito: l'ID pubblicato resta party+id locale,
		// indipendentemente da come e' risolta la colonna PARTY del record.
		assertEquals(List.of("org11", "org12"), records.stream().map(Record::getId).collect(Collectors.toList()));
	}

	@Test
	void globalIdColumnWithoutIndexOrConstantValueDefaultsToEmptyString() throws Exception {
		final List<Record> records = runPipeline("org1", "{\"name\": \"PARTY\", \"role\": \"PARTY\"}",
				"{\"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\"}");

		assertEquals(List.of("", ""), records.stream().map(Record::getGlobalId).collect(Collectors.toList()));
	}

	@Test
	void explicitConstantValueOverridesDefault() throws Exception {
		final List<Record> records = runPipeline("org1",
				"{\"name\": \"PARTY\", \"role\": \"PARTY\", \"constantValue\": \"other-party\"}",
				"{\"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\", \"constantValue\": \"UNKNOWN\"}");

		assertEquals(List.of("other-party", "other-party"),
				records.stream().map(r -> r.getParty().getName()).collect(Collectors.toList()));
		assertEquals(List.of("UNKNOWN", "UNKNOWN"),
				records.stream().map(Record::getGlobalId).collect(Collectors.toList()));
	}
}
