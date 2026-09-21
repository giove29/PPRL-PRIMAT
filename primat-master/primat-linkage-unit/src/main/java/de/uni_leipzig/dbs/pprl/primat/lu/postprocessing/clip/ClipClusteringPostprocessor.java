/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.MultipartiteClusteringStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.data_structures.ClipConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.priority_clustering.PriorityClusteringEngine;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

/**
 * Wiring di {@link PriorityClusteringEngine} (con priorità
 * {@link ClipEdgePriorityFunction}) su {@link MultiPartiteSimilarityGraph},
 * analogo a {@code GlobalGreedyClusteringPostprocessor}/
 * {@code CenterClusteringPostprocessor}. Implementazione completa
 * dell'algoritmo CLIP (letteratura FAMER/Leipzig DBS) come clustering N-ario
 * source-consistent, a differenza della classe legacy
 * {@code postprocessing.CLIP} (scoring corretto ma esecuzione finale
 * bipartita, mai completata) — quella classe non viene toccata/riusata qui.
 */
public class ClipClusteringPostprocessor implements MultipartiteClusteringStrategy {

	private final ClipConfig config;

	public ClipClusteringPostprocessor(ClipConfig config) {
		this.config = config;
	}

	@Override
	public List<LinkedPair<Record>> cluster(MultiPartiteSimilarityGraph graph) {
		final List<LinkedPair<Record>> matches = new ArrayList<>();

		for (final MultiPartiteSimilarityGraph component : graph.getConnectedComponentsSimGraph()) {
			// jgrapht non garantisce un ordine di iterazione stabile di
			// vertexSet() tra run/JVM: ordiniamo con una chiave stabile
			// (party#id) perché l'algoritmo deve essere riproducibile per la
			// persistenza.
			final List<Record> orderedRecords = new ArrayList<>(component.vertexSet());
			orderedRecords.sort(Comparator.comparing(ClipClusteringPostprocessor::stableKey));

			final Map<Record, Integer> recordIndexMap = new HashMap<>();
			final Map<Integer, Record> indexRecordMap = new HashMap<>();
			for (final Record record : orderedRecords) {
				final int idx = recordIndexMap.size();
				recordIndexMap.put(record, idx);
				indexRecordMap.put(idx, record);
			}

			final List<Party> orderedParties = orderedRecords.stream().map(Record::getParty).distinct()
					.sorted(Comparator.comparing(Party::getName)).collect(Collectors.toList());
			final Map<Party, Integer> partyIndexMap = new HashMap<>();
			for (final Party party : orderedParties) {
				partyIndexMap.put(party, partyIndexMap.size());
			}

			final int[] partyIds = new int[orderedRecords.size()];
			for (final Record record : orderedRecords) {
				partyIds[recordIndexMap.get(record)] = partyIndexMap.get(record.getParty());
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

			final PriorityClusteringEngine engine = new PriorityClusteringEngine(simMatrix, partyIds,
					orderedParties.size(), new ClipEdgePriorityFunction(config), config.getMergeThreshold());
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
