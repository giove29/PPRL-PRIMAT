/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

/**
 * Rilevatori non parametrici del "ginocchio" tra la popolazione dei non-match
 * e quella dei match su un istogramma di conteggi (bin uniformi su [0,1]).
 * Ognuno restituisce {@code NaN} quando non trova evidenza. Sono i membri
 * "senza modello" dell'ensemble di {@link ThresholdEstimator}:
 * <ul>
 * <li>{@link #triangle}: triangolo di Rosin / Kneedle sul log-istogramma,
 * adatto a un modo dominante con una spalla (altopiano) di match nella coda;</li>
 * <li>{@link #brokenStick}: regressione continua a due segmenti sul
 * log-istogramma, punto in cui la pendenza cambia verso l'alto (valle o
 * spalla);</li>
 * <li>{@link #logOtsu}: Otsu su {@code ln(1+conteggio)}, che attenua il bias
 * verso la classe numerosa;</li>
 * <li>{@link #gap}: vuoto di bin a conteggio zero tra le due masse (modi
 * perfettamente separati).</li>
 * </ul>
 */
final class KneeDetectors {

	private static final int SMOOTHING_RADIUS = 2;

	private KneeDetectors() {
	}

	/** Risultato di {@link #triangle}: posizione e "profondita'" (0..1) dell'angolo. */
	static final class Corner {
		final double position;
		final double depth;

		Corner(double position, double depth) {
			this.position = position;
			this.depth = depth;
		}
	}

	/** Risultato di {@link #gap}: bordi del vuoto. */
	static final class Gap {
		final double from;
		final double to;

		Gap(double from, double to) {
			this.from = from;
			this.to = to;
		}

		double middle() {
			return (from + to) / 2d;
		}
	}

	static double[] smooth(double[] counts) {
		final double[] s = new double[counts.length];
		for (int i = 0; i < s.length; i++) {
			double sum = 0;
			int n = 0;
			for (int k = Math.max(0, i - SMOOTHING_RADIUS); k <= Math.min(s.length - 1, i + SMOOTHING_RADIUS); k++) {
				sum += counts[k];
				n++;
			}
			s[i] = sum / n;
		}
		return s;
	}

	static int argMax(double[] values) {
		int best = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] > values[best]) {
				best = i;
			}
		}
		return best;
	}

	static int lastNonEmpty(double[] counts) {
		for (int i = counts.length - 1; i >= 0; i--) {
			if (counts[i] > 0) {
				return i;
			}
		}
		return -1;
	}

	private static double center(int bin, int bins) {
		return (bin + 0.5) / bins;
	}

	/**
	 * Angolo del log-istogramma smussato tra il picco dominante e l'ultimo bin
	 * non vuoto: il punto piu' distante sotto la corda picco-fine (Rosin 2001,
	 * Kneedle). Un decadimento esponenziale puro ha profondita' ~0, una spalla
	 * o una valle profondita' positiva.
	 */
	static Corner triangle(double[] counts) {
		final int bins = counts.length;
		final double[] smoothed = smooth(counts);
		final double[] y = new double[bins];
		for (int i = 0; i < bins; i++) {
			y[i] = Math.log1p(smoothed[i]);
		}
		final int peak = argMax(y);
		final int end = lastNonEmpty(counts);
		if (end - peak < 3 || y[peak] - y[end] < 1e-9) {
			return new Corner(Double.NaN, 0d);
		}
		double bestDepth = Double.NEGATIVE_INFINITY;
		int best = -1;
		for (int j = peak + 1; j < end; j++) {
			final double u = (double) (j - peak) / (end - peak);
			final double v = (y[j] - y[end]) / (y[peak] - y[end]);
			final double depth = 1d - u - v;
			if (depth > bestDepth) {
				bestDepth = depth;
				best = j;
			}
		}
		if (best < 0 || bestDepth <= 0) {
			return new Corner(Double.NaN, 0d);
		}
		return new Corner(center(best, bins), bestDepth);
	}

	/**
	 * Punto di rottura di una regressione continua a due segmenti sul
	 * log-istogramma smussato tra il picco dominante e l'ultimo bin non
	 * vuoto; {@code NaN} se la pendenza non aumenta (nessuna valle/spalla).
	 */
	static double brokenStick(double[] counts) {
		final int bins = counts.length;
		final double[] smoothed = smooth(counts);
		final int peak = argMax(smoothed);
		final int end = lastNonEmpty(counts);
		if (end - peak < 6) {
			return Double.NaN;
		}
		final int n = end - peak + 1;
		final double[] x = new double[n];
		final double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = (double) i / (n - 1);
			y[i] = Math.log1p(smoothed[peak + i]);
		}
		double bestSse = Double.POSITIVE_INFINITY;
		int bestBreak = -1;
		double bestSlopeGain = 0;
		for (int c = 2; c < n - 2; c++) {
			final double[] beta = fitHinge(x, y, x[c]);
			if (beta == null) {
				continue;
			}
			double sse = 0;
			for (int i = 0; i < n; i++) {
				final double fitted = beta[0] + beta[1] * x[i] + beta[2] * Math.max(0d, x[i] - x[c]);
				sse += (y[i] - fitted) * (y[i] - fitted);
			}
			if (sse < bestSse) {
				bestSse = sse;
				bestBreak = c;
				bestSlopeGain = beta[2];
			}
		}
		if (bestBreak < 0 || bestSlopeGain <= 0.5) {
			return Double.NaN;
		}
		return center(peak + bestBreak, bins);
	}

	/** Minimi quadrati per {@code y = b0 + b1 x + b2 max(0, x - knot)}; {@code null} se singolare. */
	private static double[] fitHinge(double[] x, double[] y, double knot) {
		final double[][] a = new double[3][4];
		for (int i = 0; i < x.length; i++) {
			final double[] row = { 1d, x[i], Math.max(0d, x[i] - knot) };
			for (int r = 0; r < 3; r++) {
				for (int c = 0; c < 3; c++) {
					a[r][c] += row[r] * row[c];
				}
				a[r][3] += row[r] * y[i];
			}
		}
		for (int p = 0; p < 3; p++) {
			int pivot = p;
			for (int r = p + 1; r < 3; r++) {
				if (Math.abs(a[r][p]) > Math.abs(a[pivot][p])) {
					pivot = r;
				}
			}
			if (Math.abs(a[pivot][p]) < 1e-12) {
				return null;
			}
			final double[] tmp = a[p];
			a[p] = a[pivot];
			a[pivot] = tmp;
			for (int r = p + 1; r < 3; r++) {
				final double factor = a[r][p] / a[p][p];
				for (int c = p; c < 4; c++) {
					a[r][c] -= factor * a[p][c];
				}
			}
		}
		final double[] beta = new double[3];
		for (int r = 2; r >= 0; r--) {
			double sum = a[r][3];
			for (int c = r + 1; c < 3; c++) {
				sum -= a[r][c] * beta[c];
			}
			beta[r] = sum / a[r][r];
		}
		return beta;
	}

	/**
	 * Otsu sui pesi {@code ln(1+conteggio)}: nei tratti che massimizzano la
	 * varianza tra classi in modo identico (vuoto tra i modi) prende il punto
	 * medio del plateau.
	 */
	static double logOtsu(double[] counts) {
		final int bins = counts.length;
		final double[] weight = new double[bins];
		double total = 0;
		double sumAll = 0;
		for (int i = 0; i < bins; i++) {
			weight[i] = Math.log1p(counts[i]);
			total += weight[i];
			sumAll += weight[i] * center(i, bins);
		}
		if (total <= 0) {
			return Double.NaN;
		}
		final double[] variance = new double[bins - 1];
		double below = 0;
		double sumBelow = 0;
		double best = 0;
		for (int t = 0; t < variance.length; t++) {
			below += weight[t];
			sumBelow += weight[t] * center(t, bins);
			final double above = total - below;
			if (below <= 0 || above <= 0) {
				continue;
			}
			final double diff = sumBelow / below - (sumAll - sumBelow) / above;
			variance[t] = below * above * diff * diff;
			best = Math.max(best, variance[t]);
		}
		if (best <= 0) {
			return Double.NaN;
		}
		int first = -1;
		int last = -1;
		for (int t = 0; t < variance.length; t++) {
			if (variance[t] >= best * (1 - 1e-9)) {
				first = first < 0 ? t : first;
				last = t;
			}
		}
		return ((first + 1d) / bins + (last + 1d) / bins) / 2d;
	}

	/**
	 * Il piu' lungo tratto di bin a conteggio zero (almeno {@code minGapBins})
	 * a destra del picco dominante con massa a sinistra e almeno
	 * {@code minRightMass} coppie a destra: i due modi sono perfettamente
	 * separati.
	 */
	static Gap gap(double[] counts, int minGapBins, double minRightMass) {
		final int bins = counts.length;
		final int peak = argMax(smooth(counts));
		final int end = lastNonEmpty(counts);
		if (end <= peak) {
			return null;
		}
		final double[] suffix = new double[bins + 1];
		for (int i = bins - 1; i >= 0; i--) {
			suffix[i] = suffix[i + 1] + counts[i];
		}
		int bestFrom = -1;
		int bestTo = -1;
		int i = peak;
		while (i < end) {
			if (counts[i] > 0) {
				i++;
				continue;
			}
			int j = i;
			while (j < end && counts[j] == 0) {
				j++;
			}
			// [i, j) e' una corsa di bin vuoti con massa non vuota sui due lati
			if (j - i >= minGapBins && suffix[j] >= minRightMass && (bestFrom < 0 || j - i > bestTo - bestFrom)) {
				bestFrom = i;
				bestTo = j;
			}
			i = j;
		}
		if (bestFrom < 0) {
			return null;
		}
		return new Gap((double) bestFrom / bins, (double) bestTo / bins);
	}
}
