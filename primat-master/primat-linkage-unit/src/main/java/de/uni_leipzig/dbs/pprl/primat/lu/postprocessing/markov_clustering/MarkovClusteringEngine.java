/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils.MatrixUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;

/**
 * Motore puro di Markov Clustering (MCL): nessuna dipendenza da
 * {@code Record}/{@code Party}/grafo, opera solo su una matrice di
 * similarità densa NxN, analogo a {@code SparseMscdAffinityPropagationSeq}.
 */
public class MarkovClusteringEngine {

	private static final Logger LOGGER = Logger.getLogger(MarkovClusteringEngine.class.getName());

	private final double[][] similarityMatrix;
	private final MclConfig config;
	private final int n;

	private double[][] matrix;
	private int[] labels;
	private boolean converged;
	private int iterCount;

	public MarkovClusteringEngine(double[][] similarityMatrix, MclConfig config) {
		this.similarityMatrix = similarityMatrix;
		this.config = config;
		this.n = similarityMatrix.length;
	}

	public void fit() {
		// fast-path per componenti banali: niente iterazione necessaria
		if (n <= 2) {
			this.labels = n == 1 ? new int[] { 0 } : new int[] { 0, 0 };
			this.converged = true;
			this.iterCount = 0;
			return;
		}

		this.matrix = columnNormalize(addSelfLoops(this.similarityMatrix));
		this.converged = false;

		for (this.iterCount = 0; this.iterCount < config.getMaxIterations(); this.iterCount++) {
			final double[][] next = prune(inflate(expand(this.matrix)));
			final boolean stable = hasConverged(this.matrix, next);
			this.matrix = next;
			if (stable) {
				this.converged = true;
				this.iterCount++;
				break;
			}
		}

		if (!this.converged) {
			LOGGER.warning("MCL non convergente entro " + config.getMaxIterations()
					+ " iterazioni: uso l'ultima matrice disponibile.");
		}

		this.labels = extractClusters(this.matrix);
	}

	public int[] getLabels() {
		return labels;
	}

	public boolean isConverged() {
		return converged;
	}

	public int getIterCount() {
		return iterCount;
	}

	double[][] addSelfLoops(double[][] m) {
		final double[][] result = MatrixUtils.copy(m);
		for (int i = 0; i < result.length; i++) {
			result[i][i] = config.getSelfLoopWeight();
		}
		return result;
	}

	/**
	 * Normalizza ogni colonna a somma 1. Una colonna a somma zero resta a
	 * zero (non genera {@code NaN}).
	 */
	double[][] columnNormalize(double[][] m) {
		final int rows = m.length;
		final int cols = m[0].length;
		final double[][] result = new double[rows][cols];
		for (int j = 0; j < cols; j++) {
			double sum = 0d;
			for (int i = 0; i < rows; i++) {
				sum += m[i][j];
			}
			if (sum == 0d) {
				continue;
			}
			for (int i = 0; i < rows; i++) {
				result[i][j] = m[i][j] / sum;
			}
		}
		return result;
	}

	double[][] expand(double[][] m) {
		final RealMatrix base = new Array2DRowRealMatrix(m);
		RealMatrix result = base;
		for (int e = 1; e < config.getExpansionPower(); e++) {
			result = result.multiply(base);
		}
		return result.getData();
	}

	double[][] inflate(double[][] m) {
		final int rows = m.length;
		final int cols = m[0].length;
		final double[][] result = new double[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				result[i][j] = Math.pow(m[i][j], config.getInflationExponent());
			}
		}
		return columnNormalize(result);
	}

	double[][] prune(double[][] m) {
		final int rows = m.length;
		final int cols = m[0].length;
		final double[][] result = new double[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				result[i][j] = m[i][j] < config.getPruningThreshold() ? 0d : m[i][j];
			}
		}
		return result;
	}

	boolean hasConverged(double[][] previous, double[][] current) {
		double maxDiff = 0d;
		for (int i = 0; i < previous.length; i++) {
			for (int j = 0; j < previous[i].length; j++) {
				maxDiff = Math.max(maxDiff, Math.abs(previous[i][j] - current[i][j]));
			}
		}
		return maxDiff < config.getConvergenceEpsilon();
	}

	/**
	 * Trova l'attrattore di ogni colonna (riusa {@link MatrixUtils#argMax})
	 * e raggruppa le colonne che condividono lo stesso attrattore.
	 */
	int[] extractClusters(double[][] m) {
		final int[] attractorOfColumn = MatrixUtils.argMax(m, MatrixUtils.Axis.COL);
		final Map<Integer, Integer> attractorToLabel = new HashMap<>();
		final int[] result = new int[attractorOfColumn.length];
		for (int col = 0; col < attractorOfColumn.length; col++) {
			final int attractor = attractorOfColumn[col];
			result[col] = attractorToLabel.computeIfAbsent(attractor, a -> attractorToLabel.size());
		}
		return result;
	}
}
