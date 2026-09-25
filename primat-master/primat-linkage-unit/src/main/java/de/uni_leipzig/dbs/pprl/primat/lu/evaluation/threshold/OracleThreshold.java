/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;

/**
 * Soglia "oracolo": conoscendo la ground truth, la soglia che massimizza la F1
 * sulle coppie candidate. Serve solo a validare {@link ThresholdEstimator}
 * (debug e benchmark), mai a decidere la soglia di un run.
 */
public final class OracleThreshold {

	/** Soglia oracolo e F1 raggiunta. */
	public static final class Result {
		private final double threshold;
		private final double f1;

		Result(double threshold, double f1) {
			this.threshold = threshold;
			this.f1 = f1;
		}

		public double getThreshold() {
			return threshold;
		}

		public double getF1() {
			return f1;
		}
	}

	private OracleThreshold() {
	}

	/**
	 * @param matches          istogramma delle coppie che sono match veri
	 * @param nonMatches       istogramma delle altre coppie candidate
	 * @param totalTrueMatches match veri totali nel ground truth (i match persi dal blocking contano come FN)
	 * @return la soglia (bordo inferiore di bin; punto medio di un eventuale plateau) con F1 massima
	 */
	public static Result bestF1(SimilarityHistogram matches, SimilarityHistogram nonMatches, long totalTrueMatches) {
		final int bins = matches.getBins();
		final double[] f1 = new double[bins + 1];
		double best = -1;
		for (int j = 0; j <= bins; j++) {
			final double threshold = (double) j / bins;
			final double tp = matches.countAtOrAbove(threshold);
			final double fp = nonMatches.countAtOrAbove(threshold);
			final double fn = Math.max(0d, totalTrueMatches - tp);
			final double denominator = 2 * tp + fp + fn;
			f1[j] = denominator > 0 ? 2 * tp / denominator : 0d;
			best = Math.max(best, f1[j]);
		}
		int first = -1;
		int last = -1;
		for (int j = 0; j <= bins; j++) {
			if (f1[j] >= best - 1e-12) {
				first = first < 0 ? j : first;
				last = j;
			}
		}
		return new Result(((double) first / bins + (double) last / bins) / 2d, best);
	}
}
