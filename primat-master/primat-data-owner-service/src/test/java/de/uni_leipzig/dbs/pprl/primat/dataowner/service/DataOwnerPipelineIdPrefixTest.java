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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.CsvRecordSource;

class DataOwnerPipelineIdPrefixTest {

	private List<String> publishedIds(String party) throws Exception {
		return publishedIds(party, false);
	}

	private List<String> publishedIds(String party, boolean withHeader) throws Exception {
		final Path dir = Files.createTempDirectory("dataowner-id-test");
		dir.toFile().deleteOnExit();
		final Path csv = dir.resolve("data.csv");
		Files.writeString(csv, (withHeader ? "PARTY;GLOBAL_ID;ID;FN\n" : "") + party + ";1;1;JOHN\n" + party
				+ ";2;2;MARY\n", StandardCharsets.UTF_8);
		csv.toFile().deleteOnExit();

		final String json = "{\"party\": \"" + party + "\", \"mqttBrokerUrl\": \"tcp://localhost:1883\","
				+ "\"dataSource\": {\"type\": \"CSV\", \"csv\": {\"filePath\": \""
				+ csv.toString().replace("\\", "\\\\") + "\"" + (withHeader ? ", \"hasHeader\": true" : "") + "}},"
				+ "\"columns\": ["
				+ "{\"index\": 0, \"name\": \"PARTY\", \"role\": \"PARTY\"},"
				+ "{\"index\": 1, \"name\": \"GLOBAL_ID\", \"role\": \"GLOBAL_ID\"},"
				+ "{\"index\": 2, \"name\": \"ID\", \"role\": \"ID\"},"
				+ "{\"index\": 3, \"name\": \"FN\", \"role\": \"QID\", \"dataType\": \"TEXT\"}]}";
		final Path jsonPath = dir.resolve("config.json");
		Files.writeString(jsonPath, json, StandardCharsets.UTF_8);
		jsonPath.toFile().deleteOnExit();

		final DataOwnerConfig config = DataOwnerConfigLoader.load(jsonPath);
		return new DataOwnerPipeline(new CsvRecordSource(config.getCsvFilePath(), config.isCsvHasHeader(), config.getCsvDelimiter()), config).run().stream()
				.map(Record::getId).collect(Collectors.toList());
	}

	@Test
	void prefixesLocalIdsWithPartyName() throws Exception {
		assertEquals(List.of("A1", "A2"), publishedIds("A"));
	}

	@Test
	void hasHeaderSkipsFirstRow() throws Exception {
		assertEquals(List.of("A1", "A2"), publishedIds("A", true));
	}

	@Test
	void sameLocalIdsAreUniqueAcrossParties() throws Exception {
		assertEquals(List.of("B1", "B2"), publishedIds("B"));
	}
}
