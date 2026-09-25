package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;

class OracleThresholdTest {

	@Test
	void separatedClassesGivePerfectF1AtTheMiddleOfTheGap() {
		final SimilarityHistogram matches = new SimilarityHistogram(100);
		matches.add(0.85, 50);
		final SimilarityHistogram nonMatches = new SimilarityHistogram(100);
		nonMatches.add(0.35, 1000);

		final OracleThreshold.Result result = OracleThreshold.bestF1(matches, nonMatches, 50);

		assertEquals(1d, result.getF1(), 1e-12);
		assertTrue(result.getThreshold() > 0.36 && result.getThreshold() < 0.85, "soglia " + result.getThreshold());
	}

	@Test
	void matchesLostByBlockingLowerTheAchievableF1() {
		final SimilarityHistogram matches = new SimilarityHistogram(100);
		matches.add(0.85, 50);
		final SimilarityHistogram nonMatches = new SimilarityHistogram(100);
		nonMatches.add(0.35, 1000);

		final OracleThreshold.Result result = OracleThreshold.bestF1(matches, nonMatches, 100);

		assertEquals(2d * 50 / (2 * 50 + 50), result.getF1(), 1e-12);
	}

	@Test
	void overlapPicksTheThresholdThatMaximisesF1() {
		final SimilarityHistogram matches = new SimilarityHistogram(100);
		matches.add(0.55, 10);
		matches.add(0.75, 90);
		final SimilarityHistogram nonMatches = new SimilarityHistogram(100);
		nonMatches.add(0.55, 200);
		nonMatches.add(0.35, 5000);

		final OracleThreshold.Result result = OracleThreshold.bestF1(matches, nonMatches, 100);

		// tagliare sopra 0.55 perde 10 match ma evita 200 falsi positivi
		assertTrue(result.getThreshold() > 0.56 && result.getThreshold() <= 0.75, "soglia " + result.getThreshold());
		assertEquals(2d * 90 / (2 * 90 + 0 + 10), result.getF1(), 1e-12);
	}
}
