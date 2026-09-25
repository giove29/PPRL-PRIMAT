/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

import java.util.Arrays;

/**
 * Mistura di distribuzioni Beta sulle similarita' di un istogramma, con
 * {@code nonMatchComponents} componenti per i non-match (le piu' basse) e
 * {@code matchComponents} per i match (le piu' alte), fittata con EM sul
 * modello di Poisson delle osservazioni <b>assottigliate dal blocking</b>:
 * nel bin {@code i} il conteggio atteso e' {@code N * f(x_i) * P(x_i)}, dove
 * {@code f} e' la mistura sulla popolazione completa e {@code P} la
 * probabilita' che una coppia superi il blocking
 * ({@link LshPassProbability}). Le coppie scartate dal blocking sono i "dati
 * mancanti" dell'EM: cosi' la coda bassa (troncata) dei non-match e i match
 * persi dal blocking sono trattati dal modello invece che ignorati.
 * <p>
 * Ogni componente e' una distribuzione discreta sui centri dei bin (Beta
 * normalizzata sui bin), con forme {@code a, b >= 1} (unimodale) fittate per
 * momenti pesati. Deterministico: nessun generatore casuale.
 */
final class BetaMixtureModel {

	static final double MIN_SHAPE = 1d;
	static final double MAX_SHAPE = 5000d;
	private static final double MIN_VARIANCE = 1e-5;
	private static final double MAX_POPULATION = 1e13;
	private static final double[] LANCZOS = { 0.99999999999980993, 676.5203681218851, -1259.1392167224028,
			771.32342877765313, -176.61502916214059, 12.507343278686905, -0.13857109526572012,
			9.9843695780195716e-6, 1.5056327351493116e-7 };

	private final double[] centers;
	private final double binWidth;
	private final double[] observed;
	private final double[] pass;

	final int nonMatchComponents;
	final int matchComponents;
	final double[] alpha;
	final double[] beta;
	final double[] weight;
	double population;
	double logLikelihood;
	int iterations;

	/** Massa di ciascuna componente su ciascun bin, {@code [k][i]}, ricalcolata a fine fit. */
	private double[][] mass;

	private BetaMixtureModel(double[] centers, double binWidth, double[] observed, double[] pass, int nonMatchComponents,
			int matchComponents) {
		this.centers = centers;
		this.binWidth = binWidth;
		this.observed = observed;
		this.pass = pass;
		this.nonMatchComponents = nonMatchComponents;
		this.matchComponents = matchComponents;
		final int k = nonMatchComponents + matchComponents;
		this.alpha = new double[k];
		this.beta = new double[k];
		this.weight = new double[k];
	}

	int components() {
		return alpha.length;
	}

	/** Numero di parametri liberi: forme (2K), pesi (K-1), popolazione (1). */
	int parameterCount() {
		return 3 * components();
	}

	double bic(double observations) {
		return -2d * logLikelihood + parameterCount() * Math.log(Math.max(2d, observations));
	}

	static double logGamma(double x) {
		if (x < 0.5) {
			return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - logGamma(1d - x);
		}
		final double z = x - 1d;
		double a = LANCZOS[0];
		final double t = z + 7.5;
		for (int i = 1; i < 9; i++) {
			a += LANCZOS[i] / (z + i);
		}
		return 0.5 * Math.log(2 * Math.PI) + (z + 0.5) * Math.log(t) - t + Math.log(a);
	}

	static double logBetaPdf(double x, double a, double b) {
		return logGamma(a + b) - logGamma(a) - logGamma(b) + (a - 1d) * Math.log(x) + (b - 1d) * Math.log(1d - x);
	}

	static double betaMean(double a, double b) {
		return a / (a + b);
	}

	static double betaVariance(double a, double b) {
		return a * b / ((a + b) * (a + b) * (a + b + 1d));
	}

	/**
	 * Fitta il modello.
	 *
	 * @param centers  centri dei bin
	 * @param observed conteggi osservati per bin
	 * @param pass     probabilita' di passare il blocking per bin
	 * @param split    punto che divide le due popolazioni nell'inizializzazione
	 * @param warm     modello con la stessa struttura da cui ripartire ({@code null} = inizializza da {@code split})
	 */
	static BetaMixtureModel fit(double[] centers, double binWidth, double[] observed, double[] pass,
			int nonMatchComponents, int matchComponents, double split, BetaMixtureModel warm, int maxIterations) {
		final BetaMixtureModel model = new BetaMixtureModel(centers, binWidth, observed, pass, nonMatchComponents,
				matchComponents);
		if (warm != null && warm.nonMatchComponents == nonMatchComponents && warm.matchComponents == matchComponents) {
			System.arraycopy(warm.alpha, 0, model.alpha, 0, model.alpha.length);
			System.arraycopy(warm.beta, 0, model.beta, 0, model.beta.length);
			System.arraycopy(warm.weight, 0, model.weight, 0, model.weight.length);
			model.population = warm.population;
		}
		else {
			model.initialize(split);
		}
		model.runEm(maxIterations);
		model.relabelByMean();
		return model;
	}

	private void initialize(double split) {
		final int n = centers.length;
		double total = 0;
		for (final double h : observed) {
			total += h;
		}
		int splitBin = 0;
		while (splitBin < n && centers[splitBin] < split) {
			splitBin++;
		}
		if (matchComponents == 0) {
			splitBin = n;
		}
		population = Math.max(1d, total);
		int k = 0;
		k = initGroup(k, nonMatchComponents, 0, splitBin, total);
		initGroup(k, matchComponents, splitBin, n, total);
	}

	/** Inizializza {@code count} componenti sui bin {@code [from, to)} dividendo la massa osservata a meta'. */
	private int initGroup(int firstComponent, int count, int from, int to, double grandTotal) {
		if (count == 0) {
			return firstComponent;
		}
		double groupMass = 0;
		for (int i = from; i < to; i++) {
			groupMass += observed[i];
		}
		int cut = to;
		if (count == 2) {
			double acc = 0;
			cut = from;
			while (cut < to && acc < groupMass / 2) {
				acc += observed[cut];
				cut++;
			}
			cut = Math.min(Math.max(cut, from + 1), Math.max(from + 1, to - 1));
		}
		final int[][] ranges = count == 1 ? new int[][] { { from, to } } : new int[][] { { from, cut }, { cut, to } };
		for (int c = 0; c < count; c++) {
			final int lo = ranges[c][0];
			final int hi = Math.max(ranges[c][1], lo + 1);
			double m = 0;
			double sx = 0;
			double sxx = 0;
			for (int i = lo; i < Math.min(hi, centers.length); i++) {
				m += observed[i];
				sx += observed[i] * centers[i];
				sxx += observed[i] * centers[i] * centers[i];
			}
			final double mean;
			final double variance;
			if (m > 0) {
				mean = sx / m;
				variance = Math.max(MIN_VARIANCE, sxx / m - mean * mean - binWidth * binWidth / 12d);
			}
			else {
				final double a = lo < centers.length ? centers[lo] : 1d;
				final double b = centers[Math.min(hi, centers.length) - 1];
				mean = (a + b) / 2d;
				variance = Math.max(MIN_VARIANCE, Math.pow((b - a) / 4d, 2));
			}
			setShapes(firstComponent + c, mean, variance);
			weight[firstComponent + c] = grandTotal > 0 ? Math.max(1e-6, m / grandTotal) : 1d / components();
		}
		return firstComponent + count;
	}

	private void setShapes(int k, double mean, double variance) {
		final double m = Math.min(0.999, Math.max(0.001, mean));
		final double v = Math.max(MIN_VARIANCE, variance);
		double concentration = m * (1d - m) / v - 1d;
		if (concentration <= 0) {
			concentration = 2d;
		}
		alpha[k] = Math.min(MAX_SHAPE, Math.max(MIN_SHAPE, m * concentration));
		beta[k] = Math.min(MAX_SHAPE, Math.max(MIN_SHAPE, (1d - m) * concentration));
	}

	private double[][] computeMass() {
		final int k = components();
		final int n = centers.length;
		final double[][] result = new double[k][n];
		final double[] logX = new double[n];
		final double[] log1mX = new double[n];
		for (int i = 0; i < n; i++) {
			logX[i] = Math.log(centers[i]);
			log1mX[i] = Math.log(1d - centers[i]);
		}
		for (int c = 0; c < k; c++) {
			final double norm = logGamma(alpha[c] + beta[c]) - logGamma(alpha[c]) - logGamma(beta[c]);
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < n; i++) {
				result[c][i] = norm + (alpha[c] - 1d) * logX[i] + (beta[c] - 1d) * log1mX[i];
				max = Math.max(max, result[c][i]);
			}
			double sum = 0;
			for (int i = 0; i < n; i++) {
				result[c][i] = Math.exp(result[c][i] - max);
				sum += result[c][i];
			}
			for (int i = 0; i < n; i++) {
				result[c][i] /= sum;
			}
		}
		return result;
	}

	private void runEm(int maxIterations) {
		final int k = components();
		final int n = centers.length;
		normalizeWeights();
		double previous = Double.NEGATIVE_INFINITY;
		for (iterations = 0; iterations < maxIterations; iterations++) {
			mass = computeMass();
			final double[] sumC = new double[k];
			final double[] sumX = new double[k];
			final double[] sumXX = new double[k];
			double ll = 0;
			for (int i = 0; i < n; i++) {
				double s = 0;
				for (int c = 0; c < k; c++) {
					s += weight[c] * mass[c][i];
				}
				if (s <= 0) {
					continue;
				}
				final double lambda = population * s * pass[i];
				if (lambda > 0) {
					ll += observed[i] * Math.log(lambda) - lambda;
				}
				final double missing = population * (1d - pass[i]);
				for (int c = 0; c < k; c++) {
					final double m = weight[c] * mass[c][i];
					final double expected = observed[i] * m / s + missing * m;
					sumC[c] += expected;
					sumX[c] += expected * centers[i];
					sumXX[c] += expected * centers[i] * centers[i];
				}
			}
			logLikelihood = ll;
			double total = 0;
			for (int c = 0; c < k; c++) {
				total += sumC[c];
			}
			if (total <= 0) {
				break;
			}
			population = Math.min(MAX_POPULATION, total);
			for (int c = 0; c < k; c++) {
				if (sumC[c] <= 1e-9 * total) {
					continue;
				}
				weight[c] = Math.max(1e-12, sumC[c] / total);
				final double mean = sumX[c] / sumC[c];
				final double variance = sumXX[c] / sumC[c] - mean * mean - binWidth * binWidth / 12d;
				setShapes(c, mean, variance);
			}
			normalizeWeights();
			if (Math.abs(ll - previous) <= 1e-9 * Math.abs(ll) + 1e-7) {
				iterations++;
				break;
			}
			previous = ll;
		}
		mass = computeMass();
		logLikelihood = currentLogLikelihood();
	}

	private void normalizeWeights() {
		double sum = 0;
		for (final double w : weight) {
			sum += w;
		}
		for (int c = 0; c < weight.length; c++) {
			weight[c] = sum > 0 ? weight[c] / sum : 1d / weight.length;
		}
	}

	private double currentLogLikelihood() {
		final double[] lambda = expectedObserved();
		double ll = 0;
		for (int i = 0; i < lambda.length; i++) {
			if (lambda[i] > 0) {
				ll += observed[i] * Math.log(lambda[i]) - lambda[i];
			}
		}
		return ll;
	}

	/** Riordina le componenti per media crescente: le prime {@code nonMatchComponents} sono non-match. */
	private void relabelByMean() {
		final int k = components();
		final Integer[] order = new Integer[k];
		for (int c = 0; c < k; c++) {
			order[c] = c;
		}
		Arrays.sort(order, (p, q) -> Double.compare(betaMean(alpha[p], beta[p]), betaMean(alpha[q], beta[q])));
		final double[] a = alpha.clone();
		final double[] b = beta.clone();
		final double[] w = weight.clone();
		for (int c = 0; c < k; c++) {
			alpha[c] = a[order[c]];
			beta[c] = b[order[c]];
			weight[c] = w[order[c]];
		}
		mass = computeMass();
		logLikelihood = currentLogLikelihood();
	}

	/** Conteggi attesi per bin nel modello (popolazione assottigliata dal blocking). */
	double[] expectedObserved() {
		final int n = centers.length;
		final double[] lambda = new double[n];
		for (int i = 0; i < n; i++) {
			double s = 0;
			for (int c = 0; c < components(); c++) {
				s += weight[c] * mass[c][i];
			}
			lambda[i] = population * s * pass[i];
		}
		return lambda;
	}

	/** Probabilita' a posteriori di "match" per bin (tra le coppie osservate). */
	double[] posteriorMatch() {
		final int n = centers.length;
		final double[] posterior = new double[n];
		for (int i = 0; i < n; i++) {
			double total = 0;
			double match = 0;
			for (int c = 0; c < components(); c++) {
				final double m = weight[c] * mass[c][i];
				total += m;
				if (c >= nonMatchComponents) {
					match += m;
				}
			}
			posterior[i] = total > 0 ? match / total : 0d;
		}
		return posterior;
	}

	/** Match della popolazione scartati dal blocking (mai osservati). */
	double lostMatches() {
		double lost = 0;
		for (int c = nonMatchComponents; c < components(); c++) {
			for (int i = 0; i < centers.length; i++) {
				lost += population * weight[c] * mass[c][i] * (1d - pass[i]);
			}
		}
		return lost;
	}

	/**
	 * Media e varianza della mistura dei match (o dei non-match) sulle coppie
	 * <b>osservate</b> (dopo il blocking): pesare per la popolazione completa
	 * darebbe ai match larghi una media artificiale nella regione che il
	 * blocking scarta.
	 */
	double[] groupMoments(boolean match) {
		final int from = match ? nonMatchComponents : 0;
		final int to = match ? components() : nonMatchComponents;
		double w = 0;
		double sx = 0;
		double sxx = 0;
		for (int c = from; c < to; c++) {
			for (int i = 0; i < centers.length; i++) {
				final double m = weight[c] * mass[c][i] * pass[i];
				w += m;
				sx += m * centers[i];
				sxx += m * centers[i] * centers[i];
			}
		}
		if (w <= 0) {
			return new double[] { Double.NaN, Double.NaN };
		}
		final double mean = sx / w;
		return new double[] { mean, Math.max(0d, sxx / w - mean * mean) };
	}

	double[] centers() {
		return centers;
	}

	double binWidth() {
		return binWidth;
	}

	double[] observed() {
		return observed;
	}
}
