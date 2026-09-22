/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;

class MissingValueBucketingTest {

	@BeforeEach
	void setUpSchema() {
		RecordSchema.INSTANCE.clear();
		RecordSchema.INSTANCE.put("LN", QidAttributeType.STRING, 0);
		RecordSchema.INSTANCE.put("FN", QidAttributeType.STRING, 1);
	}

	@AfterEach
	void clearSchema() {
		RecordSchema.INSTANCE.clear();
	}

	private Record recordWith(String id, String ln, String fn) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.addQidAttribute(0, new StringAttribute(ln));
		record.addQidAttribute(1, new StringAttribute(fn));
		return record;
	}

	@Test
	void sameAnchorProducesSameTokens() {
		final Record r1 = recordWith("1", "SMITH", "");
		final Record r2 = recordWith("2", "SMITH", "");

		final List<String> tokens1 = MissingValueBucketing.generateTokens(r1, List.of("LN", "FN"), 6);
		final List<String> tokens2 = MissingValueBucketing.generateTokens(r2, List.of("LN", "FN"), 6);

		assertEquals(6, tokens1.size());
		assertEquals(tokens1, tokens2);
	}

	@Test
	void differentAnchorsProduceMoreThanOneBucketAcrossManySamples() {
		// Con NUM_BUCKETS=64 due anchor arbitrarie possono collidere per puro caso
		// (~1/64): un confronto a coppia singola sarebbe un test fragile. Su un
		// campione ampio la varieta' dei bucket deve emergere con probabilita'
		// schiacciante se l'anchor determina davvero il bucket (e non e' ignorato).
		final Set<List<String>> distinctTokenSets = new HashSet<>();
		for (int i = 0; i < 20; i++) {
			final Record record = recordWith(Integer.toString(i), "SURNAME_" + i, "");
			distinctTokenSets.add(MissingValueBucketing.generateTokens(record, List.of("LN", "FN"), 6));
		}

		assertTrue(distinctTokenSets.size() > 1);
	}

	@Test
	void fallsBackToRecordIdWhenAllAnchorsAreEmptyAndVariesAcrossManySamples() {
		final Set<List<String>> distinctTokenSets = new HashSet<>();
		for (int i = 0; i < 20; i++) {
			final Record record = recordWith("rec-" + i, "", "");
			distinctTokenSets.add(MissingValueBucketing.generateTokens(record, List.of("LN", "FN"), 4));
		}

		assertTrue(distinctTokenSets.size() > 1);
	}

	@Test
	void skipsEmptyAnchorsInPriorityOrder() {
		final Record r1 = recordWith("1", "", "MARY");
		final Record r2 = recordWith("2", "", "MARY");

		// LN e' vuoto in entrambi: l'anchor effettivo diventa FN ("MARY"), identico -> stesso bucket.
		final List<String> tokens1 = MissingValueBucketing.generateTokens(r1, List.of("LN", "FN"), 5);
		final List<String> tokens2 = MissingValueBucketing.generateTokens(r2, List.of("LN", "FN"), 5);

		assertEquals(tokens1, tokens2);
	}
}
