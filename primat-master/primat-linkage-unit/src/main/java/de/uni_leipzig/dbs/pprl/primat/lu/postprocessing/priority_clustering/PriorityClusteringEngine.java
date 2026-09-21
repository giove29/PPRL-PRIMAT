/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.priority_clustering;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Motore di clustering condiviso da Global Greedy e CLIP: nessuna dipendenza
 * da {@code Record}/{@code Party}/grafo, opera solo su una matrice di
 * similarità densa e simmetrica NxN, analogo a
 * {@code CenterClusteringEngine}/{@code MarkovClusteringEngine}.
 *
 * <p>A differenza di Center Clustering (cluster "a stella", non transitivi),
 * questo motore produce la chiusura transitiva del grafo tramite union-find,
 * con un vincolo esplicito di <b>source-consistency</b>: due cluster si
 * fondono solo se l'unione non conterrebbe mai due record della stessa
 * party, sia per un arco diretto sia per una catena transitiva attraverso
 * una terza party. Gli archi sono processati in ordine di priorità
 * decrescente secondo la {@link EdgePriorityFunction} passata al
 * costruttore — Global Greedy usa la similarità grezza
 * ({@link EdgePriorityFunction#RAW_SIMILARITY}), CLIP una priorità pesata
 * basata anche su link degree e link strength (vedi
 * {@code ClipEdgePriorityFunction}). La struttura union-find, invece, è
 * identica per entrambi: la priorità decide solo l'ordine di considerazione
 * degli archi, non la logica di fusione dei cluster.
 */
public class PriorityClusteringEngine {

	private final double[][] similarityMatrix;
	private final int[] partyIds;
	private final int numParties;
	private final EdgePriorityFunction priorityFunction;
	private final Double mergeThreshold;
	private final int n;

	private int[] labels;

	public PriorityClusteringEngine(double[][] similarityMatrix, int[] partyIds, int numParties,
			EdgePriorityFunction priorityFunction, Double mergeThreshold) {
		this.similarityMatrix = similarityMatrix;
		this.partyIds = partyIds;
		this.numParties = numParties;
		this.priorityFunction = priorityFunction;
		this.mergeThreshold = mergeThreshold;
		this.n = similarityMatrix.length;
	}

	public void fit() {
		final List<int[]> edges = collectEdges();

		final int[] parent = new int[n];
		final int[] rank = new int[n];
		final BitSet[] partiesInCluster = new BitSet[n]; // valido solo sui root
		for (int v = 0; v < n; v++) {
			parent[v] = v;
			partiesInCluster[v] = new BitSet(numParties);
			partiesInCluster[v].set(partyIds[v]);
		}

		for (final int[] edge : edges) {
			final int ri = find(parent, edge[0]);
			final int rj = find(parent, edge[1]);
			if (ri == rj) {
				continue; // già nello stesso cluster (via un altro arco)
			}
			if (partiesInCluster[ri].intersects(partiesInCluster[rj])) {
				continue; // violazione source-consistency (diretta o transitiva): arco scartato
			}
			union(parent, rank, partiesInCluster, ri, rj);
		}

		this.labels = new int[n];
		for (int v = 0; v < n; v++) {
			labels[v] = find(parent, v);
		}
	}

	public int[] getLabels() {
		return labels;
	}

	/**
	 * Precalcola {@code rowMax}/{@code vertexDegree}, raccoglie le coppie
	 * {@code i<j} con peso non nullo e non inferiore a {@code mergeThreshold},
	 * ne calcola la priorità (scartando quelle con priorità
	 * {@link Double#NEGATIVE_INFINITY}) e le ordina per priorità decrescente,
	 * a parità di priorità per {@code (i,j)} ascendente — stesso schema
	 * deterministico di {@code CenterClusteringEngine.collectEdges}.
	 */
	List<int[]> collectEdges() {
		final double threshold = mergeThreshold == null ? 0d : mergeThreshold;

		final double[] rowMax = new double[n];
		final int[] vertexDegree = new int[n];
		for (int i = 0; i < n; i++) {
			double max = 0d;
			int degree = 0;
			for (int j = 0; j < n; j++) {
				if (i == j) {
					continue;
				}
				final double weight = similarityMatrix[i][j];
				if (weight != 0d) {
					degree++;
					if (weight > max) {
						max = weight;
					}
				}
			}
			rowMax[i] = max;
			vertexDegree[i] = degree;
		}

		final List<double[]> scoredEdges = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				final double weight = similarityMatrix[i][j];
				if (weight == 0d || weight < threshold) {
					continue;
				}
				final double priority = priorityFunction.priority(similarityMatrix, rowMax, vertexDegree, i, j);
				if (priority == Double.NEGATIVE_INFINITY) {
					continue;
				}
				scoredEdges.add(new double[] { i, j, priority });
			}
		}

		scoredEdges.sort((a, b) -> {
			if (a[2] != b[2]) {
				return Double.compare(b[2], a[2]);
			}
			if (a[0] != b[0]) {
				return Double.compare(a[0], b[0]);
			}
			return Double.compare(a[1], b[1]);
		});

		final List<int[]> edges = new ArrayList<>(scoredEdges.size());
		for (final double[] scored : scoredEdges) {
			edges.add(new int[] { (int) scored[0], (int) scored[1] });
		}
		return edges;
	}

	private static int find(int[] parent, int v) {
		while (parent[v] != v) {
			parent[v] = parent[parent[v]]; // path halving
			v = parent[v];
		}
		return v;
	}

	private static void union(int[] parent, int[] rank, BitSet[] partiesInCluster, int ri, int rj) {
		if (rank[ri] < rank[rj]) {
			final int tmp = ri;
			ri = rj;
			rj = tmp;
		}
		parent[rj] = ri;
		partiesInCluster[ri].or(partiesInCluster[rj]);
		partiesInCluster[rj] = null;
		if (rank[ri] == rank[rj]) {
			rank[ri]++;
		}
	}
}
