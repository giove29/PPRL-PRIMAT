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
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimate;

/**
 * Scrive la soglia automatica ({@link ThresholdEstimate}) come CSV
 * {@code key,value} accanto all'istogramma, per
 * {@code python_evaluation/plot_similarity_histogram.py} (che disegna la
 * soglia con la banda dell'intervallo di confidenza). Stesse convenzioni di
 * {@link SimilarityHistogramCsvWriter}: percorso relativo alla cwd, cartella
 * creata se manca.
 */
public final class SimilarityThresholdCsvWriter {

	public static final String DEFAULT_OUTPUT_PATH = "python_evaluation/similarity_threshold.csv";

	private SimilarityThresholdCsvWriter() {
	}

	public static void write(ThresholdEstimate estimate, String outputPath) throws IOException {
		final Path path = Path.of(outputPath);
		if (path.getParent() != null) {
			Files.createDirectories(path.getParent());
		}
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write("key,value");
			writer.newLine();
			row(writer, "mode", estimate.getMode().name().toLowerCase(Locale.ROOT));
			row(writer, "epsilon", number(estimate.getEpsilon()));
			row(writer, "threshold", number(estimate.getThreshold()));
			row(writer, "knee", number(estimate.getKnee()));
			row(writer, "ci_low", number(estimate.getCiLow()));
			row(writer, "ci_high", number(estimate.getCiHigh()));
			row(writer, "reliability", number(estimate.getReliability()));
			row(writer, "reliability_level", estimate.getReliabilityLevel().name());
			row(writer, "regime", estimate.getRegime().name());
			row(writer, "pairs", String.valueOf(estimate.getPairs()));
			for (final Map.Entry<String, Double> e : estimate.getEstimators().entrySet()) {
				row(writer, "estimator_" + e.getKey(), Double.isNaN(e.getValue()) ? "" : number(e.getValue()));
			}
		}
	}

	private static void row(BufferedWriter writer, String key, String value) throws IOException {
		writer.write(key + "," + value);
		writer.newLine();
	}

	private static String number(double value) {
		return String.format(Locale.ROOT, "%.4f", value);
	}
}
