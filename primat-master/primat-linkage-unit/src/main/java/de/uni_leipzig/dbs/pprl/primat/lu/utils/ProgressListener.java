/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.utils;

import java.util.Iterator;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;

/**
 * Riceve gli avanzamenti di una fase lunga ({@code done} su {@code total}
 * unità di lavoro).
 */
@FunctionalInterface
public interface ProgressListener {

	ProgressListener NOOP = (done, total) -> {
	};

	void update(long done, long total);

	/**
	 * Itera sulle componenti connesse riportando l'avanzamento pesato sul
	 * numero di archi di ciascuna: il progresso di una componente viene
	 * riportato quando si richiede la successiva (o si esaurisce l'iteratore).
	 */
	static Iterable<MultiPartiteSimilarityGraph> tracking(Set<MultiPartiteSimilarityGraph> components,
			ProgressListener listener) {
		long total = 0;
		for (final MultiPartiteSimilarityGraph component : components) {
			total += component.edgeSet().size();
		}
		final long totalEdges = total;
		return () -> new Iterator<MultiPartiteSimilarityGraph>() {

			private final Iterator<MultiPartiteSimilarityGraph> delegate = components.iterator();
			private long doneEdges;
			private long lastEdges;

			@Override
			public boolean hasNext() {
				final boolean has = delegate.hasNext();
				if (!has) {
					flush();
				}
				return has;
			}

			@Override
			public MultiPartiteSimilarityGraph next() {
				flush();
				final MultiPartiteSimilarityGraph component = delegate.next();
				lastEdges = component.edgeSet().size();
				return component;
			}

			private void flush() {
				doneEdges += lastEdges;
				lastEdges = 0;
				listener.update(doneEdges, totalEdges);
			}
		};
	}
}
