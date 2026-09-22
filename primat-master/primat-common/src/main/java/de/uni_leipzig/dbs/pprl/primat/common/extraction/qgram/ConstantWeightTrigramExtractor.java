/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DeterministicHashing;
import de.uni_leipzig.dbs.pprl.primat.common.utils.QidAttributeUtils;

/**
 * Trigram extractor con Constant Weight Encoding: normalizza il numero di
 * trigrammi unici estratti per un attributo dentro una banda
 * {@code [minTrigrams, maxTrigrams]}, senza forzare un valore esatto (una
 * variazione naturale dentro banda non viene toccata). Se il conteggio supera
 * {@code maxTrigrams}, seleziona un sottoinsieme deterministico via
 * hash-ranking (mai troncatura posizionale, che favorirebbe stringhe con
 * prefisso comune). Se il conteggio e' inferiore a {@code minTrigrams} e il
 * valore non e' vuoto, aggiunge trigrammi filler derivati dall'hash del
 * valore stesso (non un padding fisso condiviso tra record diversi).
 *
 * Un attributo vuoto non viene mai toccato da questa classe: la sua gestione
 * e' delegata interamente a {@code MissingValueBucketing}, che bypassa questo
 * estrattore a monte in {@code BloomFilterEncoder}. Con {@code enabled=false}
 * il comportamento e' identico byte-per-byte a {@link TrigramExtractor}.
 */
public class ConstantWeightTrigramExtractor extends TrigramExtractor {

	private final String attributeSalt;
	private final boolean enabled;
	private final int minTrigrams;
	private final int maxTrigrams;

	public ConstantWeightTrigramExtractor(boolean padding, String paddingCharacter, String attributeSalt,
			boolean enabled, int minTrigrams, int maxTrigrams) {
		super(padding, paddingCharacter);
		this.attributeSalt = attributeSalt;
		this.enabled = enabled;
		this.minTrigrams = minTrigrams;
		this.maxTrigrams = maxTrigrams;
	}

	@Override
	public List<String> extract(QidAttribute<?> attr) {
		if (attr == null || attr.getStringValue() == null) {
			return List.of();
		}

		final List<String> trigrams = super.extract(attr);
		if (!this.enabled || QidAttributeUtils.isEmpty(attr)) {
			return trigrams;
		}

		final Set<String> unique = new LinkedHashSet<>(trigrams);
		if (unique.size() > this.maxTrigrams) {
			return rankAndTruncate(unique);
		}
		if (unique.size() < this.minTrigrams) {
			return padWithFillers(unique, attr.getStringValue());
		}
		return new ArrayList<>(unique);
	}

	private List<String> rankAndTruncate(Set<String> trigrams) {
		return trigrams.stream()
				.sorted(Comparator.comparingInt(t -> DeterministicHashing.toPositiveInt(t + this.attributeSalt)))
				.limit(this.maxTrigrams)
				.collect(Collectors.toList());
	}

	private List<String> padWithFillers(Set<String> trigrams, String value) {
		final Set<String> result = new LinkedHashSet<>(trigrams);
		int i = 0;
		final int maxAttempts = this.minTrigrams * 10; // guardia anti-loop, collisione praticamente impossibile
		while (result.size() < this.minTrigrams && i < maxAttempts) {
			result.add("CWE_FILL_" + DeterministicHashing.toPositiveInt(value + this.attributeSalt + "_" + i));
			i++;
		}
		return new ArrayList<>(result);
	}
}
