/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.SimilarityObserver;

/**
 * Alimenta gli {@link SimilarityHistogram} delle similarita' di tutte le
 * coppie confrontate dalla classificazione, divise solo per ground truth
 * (match vero secondo il {@link TrueMatchChecker} / non-match). Nessuna
 * distinzione cross-party/within-party: quali coppie esistono lo decide gia'
 * il contesto ({@code ComparisonStrategy.DIRTY_AWARE}: in un contesto clean
 * non ci sono coppie within-party) e la soglia di similarita' e' una sola per
 * tutte, quindi l'istogramma e' quello delle coppie effettivamente
 * classificate.
 * <p>
 * Ogni coppia conta una sola volta: con {@code MATCH_TWICE} e piu' chiavi LSH
 * la stessa coppia e' confrontata in ogni blocco condiviso, e le coppie molto
 * simili co-occorrono in piu' blocchi (l'istogramma risulterebbe sbilanciato
 * verso l'alto). Stessa deduplicazione, per identita' non ordinata dei due
 * record, di
 * {@link de.uni_leipzig.dbs.pprl.primat.lu.evaluation.BlockingEvaluator}.
 */
public final class SimilarityHistogramCollector implements SimilarityObserver {

	private final TrueMatchChecker trueMatchChecker;
	private final SimilarityHistogram matches;
	private final SimilarityHistogram nonMatches;
	private final Set<Map.Entry<Record, Record>> seenPairs = new HashSet<>();

	public SimilarityHistogramCollector(int bins, TrueMatchChecker trueMatchChecker) {
		this.trueMatchChecker = trueMatchChecker;
		this.matches = new SimilarityHistogram(bins);
		this.nonMatches = new SimilarityHistogram(bins);
	}

	@Override
	public void observe(Record left, Record right, double similarity) {
		final boolean ordered = left.getId().compareTo(right.getId()) <= 0;
		final Map.Entry<Record, Record> key = ordered ? new SimpleImmutableEntry<>(left, right)
				: new SimpleImmutableEntry<>(right, left);
		if (!seenPairs.add(key)) {
			return;
		}
		(trueMatchChecker.isTrueMatch(left, right) ? matches : nonMatches).add(similarity);
	}

	/** @return le coppie che sono match veri secondo la ground truth. */
	public SimilarityHistogram getMatches() {
		return matches;
	}

	/** @return le coppie che non sono match veri. */
	public SimilarityHistogram getNonMatches() {
		return nonMatches;
	}

	/** @return tutte le coppie confrontate (match veri + non-match). */
	public SimilarityHistogram getAll() {
		return SimilarityHistogram.combine(matches, nonMatches);
	}
}
