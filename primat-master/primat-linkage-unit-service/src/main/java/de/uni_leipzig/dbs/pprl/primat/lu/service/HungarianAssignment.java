/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service;

import java.util.Arrays;

/**
 * Algoritmo ungherese (Kuhn-Munkres, O(n^3)) per il problema di assegnazione
 * bipartita a peso massimo su matrici rettangolari. Usato da
 * {@link ClusterAssignmentPlanner} per assegnare gruppi di record freschi ai
 * cluster storici candidati in modo globalmente ottimale invece che greedy
 * (vedi {@code SOTA_RICALCOLO_CLUSTER_INCREMENTALE.md}, sezione 9).
 */
final class HungarianAssignment {

	/**
	 * Soglia sotto la quale una cella e' considerata non ammissibile (nessun
	 * legame candidato tra riga e colonna). I punteggi reali stanno in [0,1],
	 * quindi -1 non e' mai un punteggio valido.
	 */
	static final double UNASSIGNED_WEIGHT = -1d;

	private HungarianAssignment() {
	}

	/**
	 * Risolve l'assegnazione di massimo peso totale tra righe e colonne.
	 *
	 * @param weights matrice [righe][colonne]; una cella con valore
	 *                {@code <= UNASSIGNED_WEIGHT} e' trattata come non
	 *                ammissibile (quella riga non finira' mai su quella
	 *                colonna nel risultato)
	 * @return per ogni riga, l'indice di colonna assegnato, o -1 se nessuna
	 *         assegnazione ammissibile e' stata trovata per quella riga
	 */
	static int[] solveMaximize(double[][] weights) {
		final int rows = weights.length;
		if (rows == 0) {
			return new int[0];
		}
		final int cols = weights[0].length;
		if (cols == 0) {
			final int[] none = new int[rows];
			Arrays.fill(none, -1);
			return none;
		}
		final int n = Math.max(rows, cols);

		double maxWeight = 1d;
		for (final double[] row : weights) {
			for (final double w : row) {
				maxWeight = Math.max(maxWeight, w);
			}
		}
		final double big = maxWeight + 1d;

		// Costo = big - peso, per convertire la massimizzazione in una
		// minimizzazione; celle non ammesse e celle di padding (righe/colonne
		// fittizie per quadrare la matrice) ottengono un costo piu' alto di
		// qualunque cella ammessa, cosi' l'algoritmo le sceglie solo se non ha
		// altra scelta (riga senza alcun candidato ammissibile).
		final double[][] cost = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i < rows && j < cols && weights[i][j] > UNASSIGNED_WEIGHT) {
					cost[i][j] = big - weights[i][j];
				}
				else {
					cost[i][j] = big + 1d;
				}
			}
		}

		final int[] colOfRow = solveMinCostSquare(cost, n);

		final int[] result = new int[rows];
		for (int i = 0; i < rows; i++) {
			final int j = colOfRow[i];
			result[i] = (j < cols && weights[i][j] > UNASSIGNED_WEIGHT) ? j : -1;
		}
		return result;
	}

	/**
	 * Algoritmo ungherese O(n^3) con potenziali su matrice quadrata di costi
	 * (minimizzazione). Implementazione standard (cammini aumentanti con
	 * riduzione dei potenziali), 1-indicizzata internamente.
	 */
	private static int[] solveMinCostSquare(double[][] cost, int n) {
		final double inf = Double.MAX_VALUE / 2;
		final double[] u = new double[n + 1];
		final double[] v = new double[n + 1];
		final int[] p = new int[n + 1];
		final int[] way = new int[n + 1];

		for (int i = 1; i <= n; i++) {
			p[0] = i;
			int j0 = 0;
			final double[] minv = new double[n + 1];
			final boolean[] used = new boolean[n + 1];
			Arrays.fill(minv, inf);

			do {
				used[j0] = true;
				final int i0 = p[j0];
				double delta = inf;
				int j1 = -1;
				for (int j = 1; j <= n; j++) {
					if (!used[j]) {
						final double cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
						if (cur < minv[j]) {
							minv[j] = cur;
							way[j] = j0;
						}
						if (minv[j] < delta) {
							delta = minv[j];
							j1 = j;
						}
					}
				}
				for (int j = 0; j <= n; j++) {
					if (used[j]) {
						u[p[j]] += delta;
						v[j] -= delta;
					}
					else {
						minv[j] -= delta;
					}
				}
				j0 = j1;
			} while (p[j0] != 0);

			do {
				final int j1 = way[j0];
				p[j0] = p[j1];
				j0 = j1;
			} while (j0 != 0);
		}

		final int[] colOfRow = new int[n];
		for (int j = 1; j <= n; j++) {
			if (p[j] != 0) {
				colOfRow[p[j] - 1] = j - 1;
			}
		}
		return colOfRow;
	}
}
