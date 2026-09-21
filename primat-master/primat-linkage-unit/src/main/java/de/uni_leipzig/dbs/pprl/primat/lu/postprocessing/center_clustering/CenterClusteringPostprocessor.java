/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ProgressListener;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.MultipartiteClusteringStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures.CenterClusteringConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

/**
 * Wiring di {@link CenterClusteringEngine} su {@link MultiPartiteSimilarityGraph},
 * analogo a {@code MarkovClusteringPostprocessor}. Emette la clique completa
 * di {@link LinkedPair} per ogni gruppo risultante (stessa convenzione di
 * output di AP/MCL, richiesta da {@code PersistentLinkTableBuilder}), anche
 * se internamente il clustering è "a stella" (vedi {@link CenterClusteringEngine}).
 */
public class CenterClusteringPostprocessor implements MultipartiteClusteringStrategy {

	private final CenterClusteringConfig config;

	public CenterClusteringPostprocessor(CenterClusteringConfig config) {
		this.config = config;
	}

	@Override
	public void setProgressListener(ProgressListener listener) {
		this.progressListener = listener;
	}

	private ProgressListener progressListener = ProgressListener.NOOP;

	@Override
	public List<LinkedPair<Record>> cluster(MultiPartiteSimilarityGraph graph) {
		final List<LinkedPair<Record>> matches = new ArrayList<>();

		for (final MultiPartiteSimilarityGraph component : ProgressListener.tracking(graph.getConnectedComponentsSimGraph(), progressListener)) {
			// jgrapht non garantisce un ordine di iterazione stabile di
			// vertexSet() tra run/JVM: ordiniamo con una chiave stabile
			// (party#id, non il solo id per evitare collisioni cross-party)
			// perché l'algoritmo deve essere riproducibile per la persistenza.
			final List<Record> orderedRecords = new ArrayList<>(component.vertexSet());
			orderedRecords.sort(Comparator.comparing(CenterClusteringPostprocessor::stableKey));

			final Map<Record, Integer> recordIndexMap = new HashMap<>();
			final Map<Integer, Record> indexRecordMap = new HashMap<>();
			for (final Record record : orderedRecords) {
				final int idx = recordIndexMap.size();
				recordIndexMap.put(record, idx);
				indexRecordMap.put(idx, record);
			}

			final double[][] simMatrix = new double[orderedRecords.size()][orderedRecords.size()];
			for (final SimilarityVector sv : component.edgeSet()) {
				final Record source = component.getEdgeSource(sv);
				final Record target = component.getEdgeTarget(sv);
				final int i = recordIndexMap.get(source);
				final int j = recordIndexMap.get(target);
				simMatrix[i][j] = sv.getAggregatedValue();
				simMatrix[j][i] = sv.getAggregatedValue();
			}

			final CenterClusteringEngine engine = new CenterClusteringEngine(simMatrix, config);
			engine.fit();
			final int[] labels = engine.getLabels();

			final Map<Integer, List<Record>> clusters = new HashMap<>();
			for (int idx = 0; idx < labels.length; idx++) {
				clusters.computeIfAbsent(labels[idx], l -> new ArrayList<>()).add(indexRecordMap.get(idx));
			}

			for (final List<Record> members : clusters.values()) {
				for (int i = 0; i < members.size(); i++) {
					for (int j = i + 1; j < members.size(); j++) {
						matches.add(new LinkedPair<>(members.get(i), members.get(j)));
					}
				}
			}
		}

		return matches;
	}

	private static String stableKey(Record record) {
		return record.getParty().getName() + "#" + record.getId();
	}
}
