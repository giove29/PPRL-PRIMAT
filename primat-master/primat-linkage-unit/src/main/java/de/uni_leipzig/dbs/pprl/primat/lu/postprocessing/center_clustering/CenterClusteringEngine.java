/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering;

import java.util.ArrayList;
import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.center_clustering.data_structures.CenterClusteringConfig;

/**
 * Motore puro di Center Clustering: nessuna dipendenza da
 * {@code Record}/{@code Party}/grafo, opera solo su una matrice di
 * similarità densa e simmetrica NxN, analogo a {@code MarkovClusteringEngine}.
 *
 * <p>A differenza di MCL, questo algoritmo è greedy e a singola passata: non
 * itera e non converge (nessun {@code isConverged()}/{@code getIterCount()}
 * qui, non per dimenticanza ma perché non c'è nulla da iterare). Gli archi
 * vengono ordinati in modo deterministico (similarità decrescente, poi
 * {@code (i,j)} ascendente a parità di peso) e consumati una sola volta:
 * il primo arco libero su entrambi gli estremi crea un nuovo centro
 * (l'estremo con indice più basso), un arco con un estremo già centro e
 * l'altro libero assegna quest'ultimo come foglia, ogni altro arco viene
 * scartato. Questo produce cluster "a stella" (centro + foglie dirette),
 * <b>non</b> la chiusura transitiva del grafo: due foglie dello stesso
 * centro non vengono a loro volta collegate tra loro, e un nodo raggiungibile
 * solo transitivamente da un centro (es. catena 0-1-2 senza arco diretto
 * 0-2) resta un centro singoletto a sé.
 */
public class CenterClusteringEngine {

	private final double[][] similarityMatrix;
	private final CenterClusteringConfig config;
	private final int n;

	private int[] labels;

	public CenterClusteringEngine(double[][] similarityMatrix, CenterClusteringConfig config) {
		this.similarityMatrix = similarityMatrix;
		this.config = config;
		this.n = similarityMatrix.length;
	}

	public void fit() {
		final double threshold = config.getCenterAssignmentThreshold() == null ? 0d
				: config.getCenterAssignmentThreshold();
		final List<int[]> edges = collectEdges(threshold);

		final int[] center = new int[n];
		final boolean[] isCenter = new boolean[n];
		for (int v = 0; v < n; v++) {
			center[v] = -1;
		}

		for (final int[] edge : edges) {
			final int i = edge[0];
			final int j = edge[1];
			if (center[i] == -1 && center[j] == -1) {
				isCenter[i] = true;
				center[i] = i;
				center[j] = i;
			} else if (isCenter[i] && center[j] == -1) {
				center[j] = i;
			} else if (isCenter[j] && center[i] == -1) {
				center[i] = j;
			}
			// altrimenti: entrambi gli estremi già consumati, arco scartato
		}

		this.labels = new int[n];
		for (int v = 0; v < n; v++) {
			this.labels[v] = center[v] == -1 ? v : center[v];
		}
	}

	public int[] getLabels() {
		return labels;
	}

	/**
	 * Raccoglie tutti gli archi {@code i<j} con peso non nullo e non
	 * inferiore alla soglia, ordinati per similarità decrescente e, a parità
	 * di peso, per {@code (i,j)} ascendente — ordine canonico e deterministico
	 * indipendente da come è stata popolata {@code similarityMatrix}.
	 */
	List<int[]> collectEdges(double threshold) {
		final List<int[]> edges = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				final double weight = similarityMatrix[i][j];
				if (weight != 0d && weight >= threshold) {
					edges.add(new int[] { i, j });
				}
			}
		}
		edges.sort((a, b) -> {
			final double wa = similarityMatrix[a[0]][a[1]];
			final double wb = similarityMatrix[b[0]][b[1]];
			if (wa != wb) {
				return Double.compare(wb, wa);
			}
			if (a[0] != b[0]) {
				return Integer.compare(a[0], b[0]);
			}
			return Integer.compare(a[1], b[1]);
		});
		return edges;
	}
}
