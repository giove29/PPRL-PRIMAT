/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.LshPassProbability;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.OracleThreshold;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimate;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;

/**
 * Confronta la soglia automatica con la soglia oracolo su istogrammi reali
 * scritti dalla LU con {@code debug: true}
 * ({@code python_evaluation/similarity_histogram.csv}, che contiene match veri
 * e non-match separati). Per ogni file stima la soglia dalla sola somma delle
 * due classi (come farebbe la LU senza ground truth) e stampa: soglia oracolo
 * (F1 massima), ginocchio stimato, F1 ottenuta dal ginocchio, da una soglia
 * fissa 0.75, da Otsu e dalla valle, affidabilita' e regime. Serve a tarare le
 * costanti di {@link ThresholdEstimator} e a verificare che l'affidabilita'
 * cresca quando l'errore diminuisce.
 * <p>
 * Uso: {@code ThresholdBenchmark <keySize> <keys> <csv>...}
 * ({@code keySize}/{@code keys} sono r e b del blocking con cui e' stato
 * generato il CSV; {@code 0 0} per nessun filtro LSH).
 */
public final class ThresholdBenchmark {

	private static final double FIXED_BASELINE = 0.75;

	private ThresholdBenchmark() {
	}

	/** Istogrammi per classe letti da {@code truth,bin_low,bin_high,count}. */
	static SimilarityHistogram[] read(Path csv) throws IOException {
		final List<Long> matches = new ArrayList<>();
		final List<Long> nonMatches = new ArrayList<>();
		final List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
		for (int i = 1; i < lines.size(); i++) {
			final String[] cells = lines.get(i).split(",");
			if (cells.length < 4) {
				continue;
			}
			("match".equals(cells[0]) ? matches : nonMatches).add(Long.parseLong(cells[3].trim()));
		}
		return new SimilarityHistogram[] { toHistogram(matches), toHistogram(nonMatches) };
	}

	private static SimilarityHistogram toHistogram(List<Long> counts) {
		final long[] array = new long[counts.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = counts.get(i);
		}
		return SimilarityHistogram.fromCounts(array);
	}

	private static double f1At(SimilarityHistogram matches, SimilarityHistogram nonMatches, double threshold) {
		if (Double.isNaN(threshold)) {
			return Double.NaN;
		}
		final double tp = matches.countAtOrAbove(threshold);
		final double fp = nonMatches.countAtOrAbove(threshold);
		final double fn = matches.getTotal() - tp;
		return 2 * tp + fp + fn > 0 ? 2 * tp / (2 * tp + fp + fn) : 0d;
	}

	private static String format(double value) {
		return Double.isNaN(value) ? "  n/d" : String.format(Locale.ROOT, "%5.3f", value);
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println("Uso: ThresholdBenchmark <keySize> <keys> <similarity_histogram.csv>...");
			System.exit(1);
		}
		final LshPassProbability lsh = new LshPassProbability(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		final ThresholdEstimator estimator = new ThresholdEstimator(lsh);
		System.out.println("file | coppie | oracolo t/F1 | ginocchio (F1) | fisso 0.75 (F1) | Otsu (F1) | valle (F1)"
				+ " | affidabilita' | regime | scarto");
		for (int i = 2; i < args.length; i++) {
			final Path csv = Path.of(args[i]);
			final SimilarityHistogram[] classes = read(csv);
			final SimilarityHistogram matches = classes[0];
			final SimilarityHistogram nonMatches = classes[1];
			final SimilarityHistogram all = SimilarityHistogram.combine(matches, nonMatches);
			final ThresholdEstimate estimate = estimator.estimate(all, ThresholdMode.AUTO, ThresholdEstimator.DEFAULT_EPSILON);
			final OracleThreshold.Result oracle = OracleThreshold.bestF1(matches, nonMatches, matches.getTotal());
			final double otsu = all.otsuThreshold();
			final double valley = all.valleyThreshold();
			System.out.printf(Locale.ROOT, "%s | %d | %s / %s | %s (%s) | %.2f (%s) | %s (%s) | %s (%s) | %.2f %s | %s | %+.3f%n",
					csv.getFileName(), all.getTotal(), format(oracle.getThreshold()), format(oracle.getF1()),
					format(estimate.getKnee()), format(f1At(matches, nonMatches, estimate.getKnee())), FIXED_BASELINE,
					format(f1At(matches, nonMatches, FIXED_BASELINE)), format(otsu), format(f1At(matches, nonMatches, otsu)),
					format(valley), format(f1At(matches, nonMatches, valley)), estimate.getReliability(),
					estimate.getReliabilityLevel(), estimate.getRegime(), estimate.getKnee() - oracle.getThreshold());
			System.out.println("    " + estimate.describe());
			System.out.println("    stimatori: " + estimate.describeEstimators());
		}
	}
}
