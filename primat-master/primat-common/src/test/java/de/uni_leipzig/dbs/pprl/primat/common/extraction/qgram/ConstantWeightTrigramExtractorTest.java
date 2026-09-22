/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;

class ConstantWeightTrigramExtractorTest {

	@Test
	void disabledIsEquivalentToPlainTrigramExtractor() {
		final QidAttribute<?> attr = new StringAttribute("ELEONORA");
		final List<String> expected = new TrigramExtractor(true, "_").extract(attr);

		final ConstantWeightTrigramExtractor cwe = new ConstantWeightTrigramExtractor(true, "_", "SALT_", false, 4,
				12);

		assertEquals(expected, cwe.extract(attr));
	}

	@Test
	void aboveMaxTrigramsIsTruncatedDeterministically() {
		final QidAttribute<?> attr = new StringAttribute("ELEONORA");
		final ConstantWeightTrigramExtractor cwe = new ConstantWeightTrigramExtractor(true, "_", "SALT_", true, 0, 3);

		final List<String> result1 = cwe.extract(attr);
		final List<String> result2 = cwe.extract(attr);

		assertEquals(3, result1.size());
		assertEquals(result1, result2);

		final Set<String> naturalTrigrams = new HashSet<>(new TrigramExtractor(true, "_").extract(attr));
		assertTrue(naturalTrigrams.containsAll(result1));
	}

	@Test
	void belowMinTrigramsIsPaddedDeterministically() {
		final QidAttribute<?> attr = new StringAttribute("AA");
		final ConstantWeightTrigramExtractor cwe = new ConstantWeightTrigramExtractor(true, "_", "SALT_", true, 5,
				100);

		final List<String> result1 = cwe.extract(attr);
		final List<String> result2 = cwe.extract(attr);

		assertEquals(5, result1.size());
		assertEquals(result1, result2);
		assertTrue(result1.containsAll(new TrigramExtractor(true, "_").extract(attr)));
	}

	@Test
	void inBandValueIsUntouched() {
		final QidAttribute<?> attr = new StringAttribute("MARY");
		final List<String> expected = new TrigramExtractor(true, "_").extract(attr);
		final ConstantWeightTrigramExtractor cwe = new ConstantWeightTrigramExtractor(true, "_", "SALT_", true, 1,
				100);

		assertEquals(new HashSet<>(expected), new HashSet<>(cwe.extract(attr)));
	}

	@Test
	void emptyValueIsDelegatedUnchanged() {
		final QidAttribute<?> attr = new StringAttribute("");
		final List<String> expected = new TrigramExtractor(true, "_").extract(attr);
		final ConstantWeightTrigramExtractor cwe = new ConstantWeightTrigramExtractor(true, "_", "SALT_", true, 5, 8);

		assertEquals(expected, cwe.extract(attr));
	}

	@Test
	void nullValueReturnsEmptyList() {
		final ConstantWeightTrigramExtractor cwe = new ConstantWeightTrigramExtractor(true, "_", "SALT_", true, 5, 8);

		assertEquals(List.of(), cwe.extract(new StringAttribute()));
		assertEquals(List.of(), cwe.extract(null));
	}
}
