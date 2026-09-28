/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;

/**
 * Copre {@link SimilarityThresholdSpec#rangeValues()}: generazione dei valori
 * di soglia per la modalita' di testing {@code "range"}, in particolare
 * l'assenza di drift in virgola mobile (calcolo via {@code BigDecimal}, non
 * addizione ripetuta) e l'inclusione/esclusione corretta dell'estremo
 * superiore quando {@code step} non divide esattamente {@code to - from}.
 */
class SimilarityThresholdSpecTest {

	@Test
	void defaultRangeProducesFiveEvenlySpacedValues() {
		final SimilarityThresholdSpec spec = SimilarityThresholdSpec.range(0.5, 0.9, 0.1);
		assertTrue(spec.isRange());
		assertFalse(spec.isAuto());
		assertEquals(List.of(0.5, 0.6, 0.7, 0.8, 0.9), spec.rangeValues());
	}

	@Test
	void customRangeWithSmallStepHasNoFloatingPointDrift() {
		final SimilarityThresholdSpec spec = SimilarityThresholdSpec.range(0.55, 0.9, 0.05);
		assertEquals(List.of(0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9), spec.rangeValues());
	}

	@Test
	void stepNotDividingRangeExcludesValuesPastTo() {
		final SimilarityThresholdSpec spec = SimilarityThresholdSpec.range(0.5, 0.83, 0.1);
		assertEquals(List.of(0.5, 0.6, 0.7, 0.8), spec.rangeValues());
	}

	@Test
	void singleStepRangeProducesExactlyTwoValues() {
		final SimilarityThresholdSpec spec = SimilarityThresholdSpec.range(0.6, 0.7, 0.1);
		assertEquals(List.of(0.6, 0.7), spec.rangeValues());
	}

	@Test
	void rangeValuesOnNonRangeSpecThrows() {
		assertThrows(IllegalStateException.class, () -> SimilarityThresholdSpec.fixed(0.7).rangeValues());
		assertThrows(IllegalStateException.class,
				() -> SimilarityThresholdSpec.auto(ThresholdMode.AUTO, 0.03).rangeValues());
	}

	@Test
	void labelAndToStringDescribeTheRange() {
		final SimilarityThresholdSpec spec = SimilarityThresholdSpec.range(0.55, 0.9, 0.05);
		assertEquals("range [0.55..0.90 step 0.05]", spec.label());
		assertEquals(spec.label(), spec.toString());
	}
}
