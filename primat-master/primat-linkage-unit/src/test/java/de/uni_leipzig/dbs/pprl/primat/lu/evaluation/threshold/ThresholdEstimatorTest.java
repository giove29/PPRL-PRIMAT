package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimate.Regime;

class ThresholdEstimatorTest {

	private static final int BINS = 200;
	private static final LshPassProbability LSH_6_20 = new LshPassProbability(6, 20);
	private static final LshPassProbability LSH_8_20 = new LshPassProbability(8, 20);

	/** Popolazione Beta(mean, sd) di {@code size} coppie, assottigliata dal blocking, come conteggi attesi per bin. */
	private static double[] population(double size, double mean, double sd, LshPassProbability lsh) {
		final double concentration = mean * (1 - mean) / (sd * sd) - 1;
		final double a = mean * concentration;
		final double b = (1 - mean) * concentration;
		final double[] mass = new double[BINS];
		double sum = 0;
		for (int i = 0; i < BINS; i++) {
			final double x = (i + 0.5) / BINS;
			mass[i] = Math.exp(BetaMixtureModel.logBetaPdf(x, a, b));
			sum += mass[i];
		}
		final double[] counts = new double[BINS];
		for (int i = 0; i < BINS; i++) {
			counts[i] = size * mass[i] / sum * lsh.at((i + 0.5) / BINS);
		}
		return counts;
	}

	private static SimilarityHistogram histogram(long seed, double[]... parts) {
		final Random random = seed == 0 ? null : new Random(seed);
		final long[] counts = new long[BINS];
		for (final double[] part : parts) {
			for (int i = 0; i < BINS; i++) {
				counts[i] += random == null ? Math.round(part[i])
						: Math.max(0, Math.round(part[i] + Math.sqrt(part[i]) * random.nextGaussian()));
			}
		}
		return SimilarityHistogram.fromCounts(counts);
	}

	private static double oracle(double[] nonMatch, double[] match, double lostMatches) {
		final SimilarityHistogram m = histogram(0, match);
		final SimilarityHistogram n = histogram(0, nonMatch);
		return OracleThreshold.bestF1(m, n, m.getTotal() + Math.round(lostMatches)).getThreshold();
	}

	private static void print(String name, ThresholdEstimate e, double oracle) {
		System.out.printf("[%s] oracolo %.3f | %s | stimatori: %s%n", name, oracle, e.describe(), e.describeEstimators());
	}

	@Test
	void overlappingValleyIsFoundNearTheOracle() {
		final double[] nonMatch = population(20_000_000, 0.40, 0.045, LSH_6_20);
		final double[] match = population(5_000, 0.80, 0.09, LSH_6_20);
		final SimilarityHistogram h = histogram(7, nonMatch, match);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_6_20).estimate(h, ThresholdMode.AUTO, 0.03);
		final double oracle = oracle(nonMatch, match, 0);
		print("valle", e, oracle);
		assertEquals(oracle, e.getKnee(), 0.06);
		assertTrue(e.getRegime() == Regime.VALLE || e.getRegime() == Regime.ALTOPIANO);
	}

	@Test
	void separatedModesAreFoundInsideTheGap() {
		final double[] nonMatch = population(20_000_000, 0.40, 0.03, LSH_6_20);
		final double[] match = population(5_000, 0.90, 0.04, LSH_6_20);
		final SimilarityHistogram h = histogram(0, nonMatch, match);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_6_20).estimate(h, ThresholdMode.AUTO, 0.03);
		print("separati", e, oracle(nonMatch, match, 0));
		assertTrue(e.getKnee() > 0.55 && e.getKnee() < 0.8, "ginocchio " + e.getKnee());
		assertTrue(e.getReliability() >= 0.75, "affidabilita' " + e.getReliability());
	}

	@Test
	void plateauOfMatchesWithoutAValleyIsHandled() {
		final double[] nonMatch = population(20_000_000, 0.40, 0.045, LSH_6_20);
		final double[] noisyMatches = population(6_000, 0.72, 0.13, LSH_6_20);
		final SimilarityHistogram h = histogram(11, nonMatch, noisyMatches);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_6_20).estimate(h, ThresholdMode.AUTO, 0.03);
		final double oracle = oracle(nonMatch, noisyMatches, 0);
		print("altopiano", e, oracle);
		assertEquals(oracle, e.getKnee(), 0.08);
	}

	@Test
	void onlyNonMatchesIsIndeterminateWithLowReliability() {
		final double[] nonMatch = population(20_000_000, 0.40, 0.045, LSH_6_20);
		final SimilarityHistogram h = histogram(3, nonMatch);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_6_20).estimate(h, ThresholdMode.AUTO, 0.03);
		print("solo non-match", e, Double.NaN);
		assertEquals(Regime.INDETERMINATO, e.getRegime());
		assertTrue(e.getReliability() <= 0.25);
	}

	@Test
	void tooFewPairsFallsBackWithZeroReliability() {
		final SimilarityHistogram h = new SimilarityHistogram(BINS);
		h.add(0.5, 10);
		h.add(0.9, 10);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_6_20).estimate(h, ThresholdMode.AUTO, 0.03);
		assertEquals(0d, e.getReliability(), 1e-12);
		assertEquals(ThresholdEstimator.FALLBACK_KNEE, e.getThreshold(), 1e-9);
		assertTrue(!e.getWarnings().isEmpty());
	}

	@Test
	void epsilonShiftsTheThresholdInTheRequestedDirection() {
		final double[] nonMatch = population(20_000_000, 0.40, 0.03, LSH_8_20);
		final double[] match = population(5_000, 0.90, 0.04, LSH_8_20);
		final SimilarityHistogram h = histogram(0, nonMatch, match);
		final ThresholdEstimator estimator = new ThresholdEstimator(LSH_8_20);
		final ThresholdEstimate exact = estimator.estimate(h, ThresholdMode.AUTO, 0.03);
		final ThresholdEstimate precision = estimator.estimate(h, ThresholdMode.AUTO_PRECISION, 0.03);
		final ThresholdEstimate recall = estimator.estimate(h, ThresholdMode.AUTO_RECALL, 0.03);
		assertEquals(exact.getKnee(), exact.getThreshold(), 1e-9);
		assertEquals(exact.getKnee() + 0.03, precision.getThreshold(), 1e-9);
		assertEquals(exact.getKnee() - 0.03, recall.getThreshold(), 1e-9);
		assertEquals(exact.getKnee(), precision.getKnee(), 1e-9);
	}

	@Test
	void resultIsDeterministic() {
		final double[] nonMatch = population(20_000_000, 0.40, 0.045, LSH_6_20);
		final double[] match = population(5_000, 0.80, 0.09, LSH_6_20);
		final SimilarityHistogram h = histogram(7, nonMatch, match);
		final ThresholdEstimator estimator = new ThresholdEstimator(LSH_6_20);
		final ThresholdEstimate first = estimator.estimate(h, ThresholdMode.AUTO_PRECISION, 0.03);
		final ThresholdEstimate second = estimator.estimate(h, ThresholdMode.AUTO_PRECISION, 0.03);
		assertEquals(first.getThreshold(), second.getThreshold());
		assertEquals(first.getReliability(), second.getReliability());
		assertEquals(first.getCiLow(), second.getCiLow());
	}

	@Test
	void unbalancedClassesDoNotBiasTheKneeTowardTheLargeMode() {
		final double[] nonMatch = population(400_000_000, 0.40, 0.045, LSH_8_20);
		final double[] match = population(3_000, 0.82, 0.08, LSH_8_20);
		final SimilarityHistogram h = histogram(5, nonMatch, match);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_8_20).estimate(h, ThresholdMode.AUTO, 0.03);
		final double oracle = oracle(nonMatch, match, 0);
		print("sbilanciato", e, oracle);
		assertEquals(oracle, e.getKnee(), 0.07);
	}

	@Test
	void reliabilityIsOrderedByDifficulty() {
		final ThresholdEstimator estimator = new ThresholdEstimator(LSH_6_20);
		final double[] nonMatch = population(20_000_000, 0.40, 0.03, LSH_6_20);
		final ThresholdEstimate separated = estimator.estimate(histogram(0, nonMatch, population(5_000, 0.90, 0.04, LSH_6_20)),
				ThresholdMode.AUTO, 0.03);
		final ThresholdEstimate overlapped = estimator.estimate(
				histogram(7, population(20_000_000, 0.40, 0.045, LSH_6_20), population(5_000, 0.70, 0.12, LSH_6_20)),
				ThresholdMode.AUTO, 0.03);
		final ThresholdEstimate none = estimator.estimate(histogram(3, population(20_000_000, 0.40, 0.045, LSH_6_20)),
				ThresholdMode.AUTO, 0.03);
		print("ord-sep", separated, Double.NaN);
		print("ord-ovl", overlapped, Double.NaN);
		print("ord-none", none, Double.NaN);
		assertTrue(separated.getReliability() > overlapped.getReliability(),
				separated.getReliability() + " vs " + overlapped.getReliability());
		assertTrue(overlapped.getReliability() > none.getReliability(),
				overlapped.getReliability() + " vs " + none.getReliability());
	}

	/** Match che non seguono una Beta: altopiano uniforme tra due soglie piu' un picco di duplicati esatti. */
	private static double[] uniformPlateau(double size, double from, double to, LshPassProbability lsh) {
		final double[] counts = new double[BINS];
		int inside = 0;
		for (int i = 0; i < BINS; i++) {
			final double x = (i + 0.5) / BINS;
			if (x >= from && x < to) {
				inside++;
			}
		}
		for (int i = 0; i < BINS; i++) {
			final double x = (i + 0.5) / BINS;
			counts[i] = x >= from && x < to ? size / inside * lsh.at(x) : 0;
		}
		return counts;
	}

	private static double[] plus(double[] a, double[] b) {
		final double[] sum = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			sum[i] = a[i] + b[i];
		}
		return sum;
	}

	@Test
	void nonBetaMatchPlateauPlusExactDuplicatesIsHandled() {
		final double[] nonMatch = plus(population(15_000_000, 0.40, 0.04, LSH_6_20),
				population(5_000_000, 0.46, 0.05, LSH_6_20));
		final double[] match = plus(uniformPlateau(2_500, 0.62, 0.90, LSH_6_20), population(2_500, 0.98, 0.015, LSH_6_20));
		final SimilarityHistogram h = histogram(21, nonMatch, match);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_6_20).estimate(h, ThresholdMode.AUTO, 0.03);
		final double oracle = oracle(nonMatch, match, 0);
		print("non-beta", e, oracle);
		assertEquals(oracle, e.getKnee(), 0.09);
	}

	@Test
	void heavyNoiseWithoutLshFilteringStillGivesAKneeInsideTheOverlap() {
		final double[] nonMatch = population(3_000_000, 0.35, 0.10, LshPassProbability.NONE);
		final double[] match = population(60_000, 0.75, 0.09, LshPassProbability.NONE);
		final SimilarityHistogram h = histogram(4, nonMatch, match);
		final ThresholdEstimate e = new ThresholdEstimator(LshPassProbability.NONE).estimate(h, ThresholdMode.AUTO, 0.03);
		final double oracle = oracle(nonMatch, match, 0);
		print("senza-lsh", e, oracle);
		assertEquals(oracle, e.getKnee(), 0.07);
	}

	private static boolean hasBoundWarning(ThresholdEstimate e) {
		return e.getWarnings().stream().anyMatch(w -> w.startsWith("Epsilon non applicato"));
	}

	@Test
	void epsilonNeverPushesTheThresholdAboveOne() {
		final double[] nonMatch = population(1_000_000, 0.70, 0.04, LshPassProbability.NONE);
		final double[] match = population(3_000, 0.985, 0.008, LshPassProbability.NONE);
		final SimilarityHistogram h = histogram(0, nonMatch, match);
		final ThresholdEstimate e = new ThresholdEstimator(LshPassProbability.NONE).estimate(h,
				ThresholdMode.AUTO_PRECISION, 0.2);
		print("limite-alto", e, Double.NaN);
		assertTrue(e.getKnee() > 0.85, "ginocchio " + e.getKnee());
		assertEquals(1d, e.getThreshold(), 1e-12);
		assertTrue(hasBoundWarning(e), e.getWarnings().toString());
		assertTrue(e.getWarnings().stream().anyMatch(w -> w.contains("praticamente identiche")));
	}

	@Test
	void epsilonNeverPushesTheThresholdBelowTheNonMatchPeakOrZero() {
		final double[] nonMatch = population(1_000_000, 0.10, 0.02, LshPassProbability.NONE);
		final double[] match = population(3_000, 0.32, 0.03, LshPassProbability.NONE);
		final SimilarityHistogram h = histogram(0, nonMatch, match);
		final ThresholdEstimate e = new ThresholdEstimator(LshPassProbability.NONE).estimate(h,
				ThresholdMode.AUTO_RECALL, 0.2);
		print("limite-basso", e, Double.NaN);
		assertTrue(e.getKnee() < 0.3, "ginocchio " + e.getKnee());
		assertTrue(e.getThreshold() > 0d);
		assertTrue(e.getThreshold() >= 0.10, "soglia " + e.getThreshold());
		assertTrue(hasBoundWarning(e), e.getWarnings().toString());
	}

	@Test
	void boundedThresholdCoversBothEndsAndTheFallbackPath() {
		final java.util.List<String> warnings = new java.util.ArrayList<>();
		assertEquals(1d, ThresholdEstimator.boundedThreshold(0.9, ThresholdMode.AUTO_PRECISION, 0.2, 0d, warnings), 1e-12);
		assertEquals(0.001, ThresholdEstimator.boundedThreshold(0.1, ThresholdMode.AUTO_RECALL, 0.2, 0d, warnings), 1e-12);
		assertEquals(0.4, ThresholdEstimator.boundedThreshold(0.5, ThresholdMode.AUTO_RECALL, 0.2, 0.4, warnings), 1e-12);
		assertEquals(3, warnings.stream().filter(w -> w.startsWith("Epsilon non applicato")).count());

		final java.util.List<String> quiet = new java.util.ArrayList<>();
		assertEquals(0.64, ThresholdEstimator.boundedThreshold(0.61, ThresholdMode.AUTO_PRECISION, 0.03, 0.4, quiet), 1e-12);
		assertEquals(0.61, ThresholdEstimator.boundedThreshold(0.61, ThresholdMode.AUTO, 0.03, 0.4, quiet), 1e-12);
		assertTrue(quiet.isEmpty(), quiet.toString());
	}

	@Test
	void noBoundWarningWhenEpsilonFitsInsideTheDomain() {
		final double[] nonMatch = population(20_000_000, 0.40, 0.03, LSH_8_20);
		final double[] match = population(5_000, 0.90, 0.04, LSH_8_20);
		final ThresholdEstimate e = new ThresholdEstimator(LSH_8_20).estimate(histogram(0, nonMatch, match),
				ThresholdMode.AUTO_RECALL, 0.05);
		assertTrue(!hasBoundWarning(e), e.getWarnings().toString());
	}
}
