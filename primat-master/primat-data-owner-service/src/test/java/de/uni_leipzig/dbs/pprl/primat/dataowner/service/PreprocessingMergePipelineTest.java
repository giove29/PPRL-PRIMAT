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

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.CsvRecordSource;

/**
 * Verifica end-to-end la proprieta' che rende utile un primo step MERGE nel
 * {@code preprocessing} di una colonna QID virtuale: un DO con given_name/
 * surname separati (dichiarati {@code role: RAW}, fusi in full_name tramite
 * {"type":"MERGE",...} come primo step di 'preprocessing') deve produrre
 * esattamente lo stesso RBF di un DO che ha gia' un'unica colonna
 * {@code full_name} nella sorgente dati - cioe' i due schemi diventano
 * davvero congruenti dopo la trasformazione, non solo "simili".
 */
class PreprocessingMergePipelineTest {

	private static final String MERGE_CONFIG_JSON = "{"
			+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
			+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"%CSV%\" } },"
			+ "\"bloomFilter\": { \"length\": 256, \"hardeningChain\": [] },"
			+ "\"columns\": ["
			+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
			+ "  { \"index\": 1, \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\" },"
			+ "  { \"index\": 2, \"name\": \"ID\", \"role\": \"ID\" },"
			+ "  { \"index\": 3, \"name\": \"given_name\", \"role\": \"RAW\" },"
			+ "  { \"index\": 4, \"name\": \"surname\", \"role\": \"RAW\" },"
			+ "  { \"name\": \"full_name\", \"role\": \"QID\", \"preprocessing\": ["
			+ "      {\"type\":\"MERGE\",\"sources\":[\"given_name\",\"surname\"],\"merger\":{\"type\":\"BLANK\"}},"
			+ "      {\"type\":\"TRIM\"}, {\"type\":\"UPPERCASE\"}"
			+ "    ], \"hashFunctions\": 8, \"salt\": \"FN_\" }"
			+ "]}";

	private static final String PLAIN_CONFIG_JSON = "{"
			+ "\"party\": \"A\",\"mqttBrokerUrl\": \"tcp://localhost:1883\","
			+ "\"dataSource\": { \"type\": \"CSV\", \"csv\": { \"filePath\": \"%CSV%\" } },"
			+ "\"bloomFilter\": { \"length\": 256, \"hardeningChain\": [] },"
			+ "\"columns\": ["
			+ "  { \"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\" },"
			+ "  { \"index\": 1, \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\" },"
			+ "  { \"index\": 2, \"name\": \"ID\", \"role\": \"ID\" },"
			+ "  { \"index\": 3, \"name\": \"full_name\", \"role\": \"QID\", \"preprocessing\": ["
			+ "      {\"type\":\"TRIM\"}, {\"type\":\"UPPERCASE\"}"
			+ "    ], \"hashFunctions\": 8, \"salt\": \"FN_\" }"
			+ "]}";

	@Test
	void mergeProducesSameRbfAsAlreadyMergedSource() throws Exception {
		final Path dir = Files.createTempDirectory("field-transform-test");
		dir.toFile().deleteOnExit();

		final Path mergeCsv = dir.resolve("merge.csv");
		Files.writeString(mergeCsv, "A;1;1;John;Smith\n", StandardCharsets.UTF_8);
		mergeCsv.toFile().deleteOnExit();
		final Record mergedRecord = runPipeline(dir, "merge-config.json",
				MERGE_CONFIG_JSON.replace("%CSV%", csvPath(mergeCsv)));

		final Path plainCsv = dir.resolve("plain.csv");
		Files.writeString(plainCsv, "A;1;1;John Smith\n", StandardCharsets.UTF_8);
		plainCsv.toFile().deleteOnExit();
		final Record plainRecord = runPipeline(dir, "plain-config.json",
				PLAIN_CONFIG_JSON.replace("%CSV%", csvPath(plainCsv)));

		assertEquals(plainRecord.getAttributes().get(0).getStringValue(),
				mergedRecord.getAttributes().get(0).getStringValue());
	}

	private static String csvPath(Path csv) {
		return csv.toString().replace("\\", "\\\\");
	}

	private Record runPipeline(Path dir, String fileName, String json) throws Exception {
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
		final Path localPath = dir.resolve(fileName + "_local.json");
		final Path livePath = dir.resolve(fileName + "_live.json");
		Files.writeString(localPath, local.toString(), StandardCharsets.UTF_8);
		Files.writeString(livePath, live.toString(), StandardCharsets.UTF_8);
		localPath.toFile().deleteOnExit();
		livePath.toFile().deleteOnExit();

		final DataOwnerConfig config = DataOwnerConfigLoader.load(localPath, livePath);
		final List<Record> records = new DataOwnerPipeline(
				new CsvRecordSource(config.getCsvFilePath(), config.isCsvHasHeader(), config.getCsvDelimiter()),
				config).run();
		return records.get(0);
	}
}
