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
import java.util.Locale;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;

/**
 * Scrive gli istogrammi di similarita' di {@link SimilarityHistogramCollector}
 * su CSV ({@code truth,bin_low,bin_high,count}, una riga per bin di ciascun
 * istogramma; {@code truth} = {@code match}/{@code non_match} secondo la
 * ground truth), letto da {@code python_evaluation/plot_similarity_histogram.py}.
 * Il percorso di default e' relativo alla cwd (root del progetto, come per gli
 * script Python) e mette il file accanto allo script che lo legge.
 */
public final class SimilarityHistogramCsvWriter {

	public static final String DEFAULT_OUTPUT_PATH = "python_evaluation/similarity_histogram.csv";

	private SimilarityHistogramCsvWriter() {
	}

	public static void write(SimilarityHistogramCollector collector, String outputPath) throws IOException {
		final Path path = Path.of(outputPath);
		if (path.getParent() != null) {
			Files.createDirectories(path.getParent());
		}
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write("truth,bin_low,bin_high,count");
			writer.newLine();
			writeRows(writer, "match", collector.getMatches());
			writeRows(writer, "non_match", collector.getNonMatches());
		}
	}

	private static void writeRows(BufferedWriter writer, String truth, SimilarityHistogram histogram)
			throws IOException {
		for (int i = 0; i < histogram.getBins(); i++) {
			writer.write(String.format(Locale.ROOT, "%s,%.4f,%.4f,%d", truth, histogram.binLow(i),
					histogram.binHigh(i), histogram.getCount(i)));
			writer.newLine();
		}
	}
}
