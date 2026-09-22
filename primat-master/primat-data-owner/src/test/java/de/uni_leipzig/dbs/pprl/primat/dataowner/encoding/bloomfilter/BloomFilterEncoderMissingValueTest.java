/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram.TrigramExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.RandomFactory;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.NoHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.RandomHashing;

/**
 * Verifica il bypass del missing-value bucketing direttamente al livello di
 * {@link BloomFilterEncoder}, non solo a livello unitario di
 * {@link MissingValueBucketing}: un attributo vuoto deve produrre un RBF
 * deterministico in base al solo anchor (non al padding fisso "_" di
 * {@link TrigramExtractor}), identico per record che condividono l'anchor e
 * diverso da un valore realmente popolato.
 */
class BloomFilterEncoderMissingValueTest {

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

	private Record recordWith(String ln, String fn) {
		final Record record = new Record();
		record.addQidAttribute(0, new StringAttribute(ln));
		record.addQidAttribute(1, new StringAttribute(fn));
		return record;
	}

	private BloomFilterDefinition rbfDefinition() {
		final BloomFilterExtractorDefinition lnDef = new BloomFilterExtractorDefinition();
		lnDef.setColumnsByName("LN");
		lnDef.setExtractors(new TrigramExtractor(true, "_"));
		lnDef.setNumberOfHashFunctions(10);
		lnDef.setSalt("LN_");

		final BloomFilterExtractorDefinition fnDef = new BloomFilterExtractorDefinition();
		fnDef.setColumnsByName("FN");
		fnDef.setExtractors(new TrigramExtractor(true, "_"));
		fnDef.setNumberOfHashFunctions(10);
		fnDef.setSalt("FN_");
		fnDef.setMissingValueTokenCount(6);

		final BloomFilterDefinition def = new BloomFilterDefinition();
		def.setName("RBF");
		def.setBfLength(1024);
		def.setHashingMethod(new RandomHashing(1024, RandomFactory.SECURE_RANDOM));
		def.setFeatureExtractors(List.of(lnDef, fnDef));
		def.setHardener(new NoHardener());
		def.setMissingValueHandlingEnabled(true);
		def.setMissingValueAnchorPriority(List.of("LN"));
		return def;
	}

	private BitSetAttribute encode(Record record) {
		final BloomFilterEncoder encoder = new BloomFilterEncoder(List.of(rbfDefinition()));
		final Record encoded = encoder.encode(record);
		return (BitSetAttribute) encoded.getAttributes().get(0);
	}

	@Test
	void sameAnchorProducesIdenticalRbfForMissingField() {
		final BitSetAttribute rbf1 = encode(recordWith("SMITH", ""));
		final BitSetAttribute rbf2 = encode(recordWith("SMITH", ""));

		assertEquals(rbf1.getValue(), rbf2.getValue());
	}

	@Test
	void emptyFieldDiffersFromPresentField() {
		final BitSetAttribute rbfMissing = encode(recordWith("SMITH", ""));
		final BitSetAttribute rbfPresent = encode(recordWith("SMITH", "MARY"));

		assertNotEquals(rbfMissing.getValue(), rbfPresent.getValue());
	}
}
