/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.service.MultiSourceLinkage.LinkageOutcome;

/**
 * Scrive una {@code LinkageOutcome#getLinkTable()} su CSV ({@code
 * cluster_id,party,record_id,global_id}, una riga per record), usato da
 * {@link LinkageUnitOrchestrator} per qualunque strategia quando {@code
 * persistence.enabled=false} in config (nessun DB toccato). Prima di questa
 * classe l'export CSV era cablato solo nel ramo MCL di {@code runOnce()};
 * ora e' generico, riusabile da tutte e 6 le strategie.
 */
public final class ClusterCsvWriter {

	private ClusterCsvWriter() {
	}

	/**
	 * Sovrascrive {@code outputPath} con la Link Table di {@code outcome}.
	 *
	 * @param outcome    esito del run la cui {@code linkTable} va esportata
	 * @param outputPath percorso del CSV da (sovra)scrivere
	 * @throws IOException se la scrittura del file fallisce
	 */
	public static void write(LinkageOutcome outcome, String outputPath) throws IOException {
		final Path path = Path.of(outputPath);
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write("cluster_id,party,record_id,global_id");
			writer.newLine();
			for (final Cluster cluster : outcome.getLinkTable()) {
				for (final Record record : cluster.getRecords()) {
					writer.write(cluster.getId() + "," + csvField(record.getParty().getName()) + ","
							+ csvField(record.getId()) + "," + csvField(record.getGlobalId()));
					writer.newLine();
				}
			}
		}
		System.out.println("=== Link Table scritta su CSV (persistence disabled): " + path.toAbsolutePath() + " ===");
	}

	private static String csvField(String value) {
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}
