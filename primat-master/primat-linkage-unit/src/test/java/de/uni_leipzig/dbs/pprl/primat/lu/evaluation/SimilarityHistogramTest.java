package de.uni_leipzig.dbs.pprl.primat.lu.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimilarityHistogramTest {

	/** Non-match attorno a 0.3 (molti), match attorno a 0.85 (pochi), valle vuota in mezzo. */
	private static SimilarityHistogram bimodal() {
		final SimilarityHistogram histogram = new SimilarityHistogram();
		for (int i = 0; i < 2000; i++) {
			histogram.add(0.25 + 0.001 * (i % 100));
		}
		for (int i = 0; i < 300; i++) {
			histogram.add(0.80 + 0.001 * (i % 100));
		}
		return histogram;
	}

	@Test
	void binsAtTheEdgesAreClamped() {
		final SimilarityHistogram histogram = new SimilarityHistogram();
		histogram.add(0.0);
		histogram.add(1.0);
		histogram.add(-0.5);
		histogram.add(1.7);
		histogram.add(Double.NaN);

		assertEquals(4, histogram.getTotal());
		assertEquals(2, histogram.getCount(0));
		assertEquals(2, histogram.getCount(histogram.getBins() - 1));
		assertEquals(0.0, histogram.binLow(0), 1e-12);
		assertEquals(1.0, histogram.binHigh(histogram.getBins() - 1), 1e-12);
	}

	@Test
	void bimodalDistributionHasValleyAndOtsuBetweenTheTwoModes() {
		final SimilarityHistogram histogram = bimodal();

		assertTrue(histogram.isBimodal());
		final double valley = histogram.valleyThreshold();
		final double otsu = histogram.otsuThreshold();
		assertTrue(valley > 0.35 && valley < 0.80, "valle " + valley);
		assertTrue(otsu > 0.35 && otsu < 0.80, "otsu " + otsu);
	}

	@Test
	void statsSplitTheTwoModesAtTheValley() {
		final SimilarityHistogram histogram = bimodal();
		final double valley = histogram.valleyThreshold();

		final SimilarityHistogram.Stats low = histogram.statsBelow(valley);
		final SimilarityHistogram.Stats high = histogram.statsAtOrAbove(valley);

		assertEquals(2000, low.getCount());
		assertEquals(300, high.getCount());
		assertEquals(0.30, low.getMean(), 0.02);
		assertEquals(0.85, high.getMean(), 0.02);
	}

	@Test
	void unimodalDistributionIsNotBimodalAndHasNoValley() {
		final SimilarityHistogram histogram = new SimilarityHistogram();
		for (int i = 0; i < 1000; i++) {
			histogram.add(0.40 + 0.0005 * (i % 200));
		}

		assertFalse(histogram.isBimodal());
		assertTrue(Double.isNaN(histogram.valleyThreshold()));
	}

	@Test
	void emptyOrSingleBinHistogramHasNoThresholds() {
		final SimilarityHistogram empty = new SimilarityHistogram();
		assertTrue(Double.isNaN(empty.otsuThreshold()));
		assertTrue(Double.isNaN(empty.valleyThreshold()));
		assertFalse(empty.isBimodal());
		assertEquals(0, empty.statsBelow(0.5).getCount());
		assertTrue(Double.isNaN(empty.statsBelow(0.5).getMean()));

		final SimilarityHistogram single = new SimilarityHistogram();
		for (int i = 0; i < 10; i++) {
			single.add(0.5);
		}
		assertTrue(Double.isNaN(single.otsuThreshold()));
	}

	@Test
	void combineSumsBinsAndCountAtOrAboveUsesBinLowerEdges() {
		final SimilarityHistogram a = new SimilarityHistogram();
		final SimilarityHistogram b = new SimilarityHistogram();
		a.add(0.30);
		a.add(0.75);
		b.add(0.75);
		b.add(0.99);

		final SimilarityHistogram sum = SimilarityHistogram.combine(a, b);

		assertEquals(4, sum.getTotal());
		assertEquals(2, sum.getCount(75));
		assertEquals(3, sum.countAtOrAbove(0.75));
		assertEquals(4, sum.countAtOrAbove(0.0));
		assertEquals(0, sum.countAtOrAbove(1.0));
		assertThrows(IllegalArgumentException.class, () -> SimilarityHistogram.combine(a, new SimilarityHistogram(10)));
	}

	@Test
	void fewerThanTwoBinsIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new SimilarityHistogram(1));
	}

	@Test
	void addWithRepetitionsAndFromCountsAgreeWithSingleAdds() {
		final SimilarityHistogram repeated = new SimilarityHistogram(10);
		repeated.add(0.25, 3);
		repeated.add(0.95, 2);
		repeated.add(Double.NaN, 5);
		repeated.add(0.5, 0);

		final SimilarityHistogram single = new SimilarityHistogram(10);
		for (int i = 0; i < 3; i++) {
			single.add(0.25);
		}
		single.add(0.95);
		single.add(0.95);

		final SimilarityHistogram rebuilt = SimilarityHistogram.fromCounts(new long[] { 0, 0, 3, 0, 0, 0, 0, 0, 0, 2 });
		for (int bin = 0; bin < 10; bin++) {
			assertEquals(single.getCount(bin), repeated.getCount(bin));
			assertEquals(single.getCount(bin), rebuilt.getCount(bin));
		}
		assertEquals(5, repeated.getTotal());
		assertEquals(5, rebuilt.getTotal());
	}
}
