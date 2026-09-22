/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter;

import java.util.ArrayList;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DeterministicHashing;
import de.uni_leipzig.dbs.pprl.primat.common.utils.QidAttributeUtils;

/**
 * Genera i token sintetici per un attributo QID vuoto, al posto dei
 * trigrammi che {@code TrigramExtractor}/{@code ConstantWeightTrigramExtractor}
 * produrrebbero da un padding fisso condiviso (es. {@code "___"}, identico
 * per ogni record con quell'attributo vuoto). L'attributo "anchor" (il primo
 * attributo QID non vuoto del record, secondo una lista di priorita'
 * configurabile) determina deterministicamente un bucket tra
 * {@link #NUM_BUCKETS}; i token dipendono solo dal bucket, non dal nome o dal
 * salt dell'attributo mancante, perche' la disambiguazione tra attributi
 * diversi e' gia' garantita a valle dal salt per-attributo applicato in
 * {@code BloomFilterEncoder.getBloomFilterPositions}.
 */
public final class MissingValueBucketing {

	/** Numero di bucket, fisso e non esposto in configurazione. */
	public static final int NUM_BUCKETS = 64;

	private MissingValueBucketing() {
		throw new RuntimeException();
	}

	/**
	 * @param record                     il record cui appartiene l'attributo vuoto
	 * @param anchorPriorityColumnNames  nomi di colonna QID in ordine di priorita', usati per
	 *                                   scegliere l'anchor (il primo non vuoto tra questi)
	 * @param tokenCount                 numero di token sintetici da generare
	 * @return {@code tokenCount} token deterministici, identici per ogni record che condivide lo stesso anchor
	 */
	public static List<String> generateTokens(Record record, List<String> anchorPriorityColumnNames,
			int tokenCount) {
		final String anchorValue = resolveAnchor(record, anchorPriorityColumnNames);
		final int bucket = DeterministicHashing.toBucket(anchorValue, NUM_BUCKETS);
		final String seed = "MISSING_B" + bucket;

		final List<String> tokens = new ArrayList<>(tokenCount);
		for (int i = 0; i < tokenCount; i++) {
			tokens.add("MISSING_T" + DeterministicHashing.toPositiveInt(seed + "_" + i));
		}
		return tokens;
	}

	private static String resolveAnchor(Record record, List<String> anchorPriorityColumnNames) {
		for (final String columnName : anchorPriorityColumnNames) {
			final QidAttribute<?> attr = record.getQidAttribute(columnName);
			if (!QidAttributeUtils.isEmpty(attr)) {
				return attr.getStringValue();
			}
		}
		// Fallback: tutti gli attributi QID del record sono vuoti.
		return record.getId();
	}
}
