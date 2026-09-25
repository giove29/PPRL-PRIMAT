package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.LshPassProbability;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimate;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;

class ThresholdArtifactsTest {

	private final Path tempDir = createTempDir();

	private static Path createTempDir() {
		try {
			final Path dir = Files.createTempDirectory("threshold-artifacts");
			dir.toFile().deleteOnExit();
			return dir;
		}
		catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Record record(String id, String globalId, Party party) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setGlobalIdAttribute(new GlobalIdAttribute(globalId));
		record.setParty(party);
		return record;
	}

	@Test
	void histogramCsvRoundTripsThroughTheBenchmarkReader() throws Exception {
		final Party a = new Party("A", true);
		final Party b = new Party("B", true);
		final SimilarityHistogramCollector collector = new SimilarityHistogramCollector(50, new IdEqualityTrueMatchChecker());
		for (int i = 0; i < 40; i++) {
			collector.observe(record("A" + i, "g" + i, a), record("B" + i, "g" + i, b), 0.9);
			collector.observe(record("A" + i, "g" + i, a), record("B" + (i + 100), "g" + (i + 100), b), 0.3);
		}
		final Path csv = tempDir.resolve("h.csv");
		SimilarityHistogramCsvWriter.write(collector, csv.toString());

		final SimilarityHistogram[] read = ThresholdBenchmark.read(csv);

		assertEquals(40, read[0].getTotal());
		assertEquals(40, read[1].getTotal());
		assertEquals(50, read[0].getBins());
		assertEquals(40, read[0].countAtOrAbove(0.8));
		assertEquals(0, read[1].countAtOrAbove(0.8));
	}

	@Test
	void thresholdCsvContainsTheEstimateForThePlot() throws Exception {
		final SimilarityHistogram h = new SimilarityHistogram(200);
		h.add(0.40, 50_000);
		h.add(0.42, 30_000);
		h.add(0.38, 30_000);
		h.add(0.90, 500);
		h.add(0.93, 400);
		h.add(0.96, 300);
		final ThresholdEstimate estimate = new ThresholdEstimator(LshPassProbability.NONE).estimate(h,
				ThresholdMode.AUTO_RECALL, 0.05);
		final Path csv = tempDir.resolve("t.csv");

		SimilarityThresholdCsvWriter.write(estimate, csv.toString());

		final String content = Files.readString(csv, StandardCharsets.UTF_8);
		assertTrue(content.startsWith("key,value"));
		assertTrue(content.contains("mode,auto_recall"));
		assertTrue(content.contains("epsilon,0.0500"));
		assertTrue(content.contains(String.format(java.util.Locale.ROOT, "threshold,%.4f", estimate.getThreshold())));
		assertTrue(content.contains("regime,"));
		assertTrue(content.contains("reliability,"));
	}
}
