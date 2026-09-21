/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.MultipartiteClusteringStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

/**
 * Wiring di {@link MarkovClusteringEngine} su {@link MultiPartiteSimilarityGraph},
 * analogo a {@code AffinityPropagationPostprocessor}. Nessun vincolo
 * clean-source da rispettare: MCL gira solo quando tutte le sorgenti del run
 * sono dirty (per costruzione del routing in {@code LinkageUnitOrchestrator}).
 */
public class MarkovClusteringPostprocessor implements MultipartiteClusteringStrategy {

	private final MclConfig config;

	public MarkovClusteringPostprocessor(MclConfig config) {
		this.config = config;
	}

	@Override
	public List<LinkedPair<Record>> cluster(MultiPartiteSimilarityGraph graph) {
		final List<LinkedPair<Record>> matches = new ArrayList<>();

		for (final MultiPartiteSimilarityGraph component : graph.getConnectedComponentsSimGraph()) {
			final Map<Record, Integer> recordIndexMap = new HashMap<>();
			final Map<Integer, Record> indexRecordMap = new HashMap<>();
			for (final Record record : component.vertexSet()) {
				final int idx = recordIndexMap.size();
				recordIndexMap.put(record, idx);
				indexRecordMap.put(idx, record);
			}

			// simmetrica: a differenza di AP (che scrive solo [i][j]), qui
			// serve un grafo non orientato correttamente rappresentato,
			// perché la normalizzazione per colonna di MCL lo richiede.
			final double[][] simMatrix = new double[recordIndexMap.size()][recordIndexMap.size()];
			for (final SimilarityVector sv : component.edgeSet()) {
				final Record source = component.getEdgeSource(sv);
				final Record target = component.getEdgeTarget(sv);
				final int i = recordIndexMap.get(source);
				final int j = recordIndexMap.get(target);
				simMatrix[i][j] = sv.getAggregatedValue();
				simMatrix[j][i] = sv.getAggregatedValue();
			}

			final MarkovClusteringEngine engine = new MarkovClusteringEngine(simMatrix, config);
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
}
