/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation;

/**
 * Istogramma a bin uniformi delle similarita' su [0,1] delle coppie candidate
 * del blocking, per analizzare la separazione tra il modo dei non-match
 * (basso) e quello dei match (alto): il rumore (es. BLIP) sposta e allarga il
 * modo dei match sovrapponendolo all'altro. Classe pura, senza dipendenze dal
 * modello dei record.
 * <p>
 * Le soglie suggerite ({@link #otsuThreshold()}, {@link #valleyThreshold()})
 * sono solo diagnostiche: non sostituiscono la soglia configurata. Il blocking
 * tronca la coda bassa dei non-match, quindi il modo basso e' troncato ma la
 * valle resta visibile.
 */
public final class SimilarityHistogram {

	public static final int DEFAULT_BINS = 100;

	private static final int SMOOTHING_RADIUS = 2;
	/** Distanza minima tra i due picchi, in frazione dell'intervallo [0,1]. */
	private static final double MIN_PEAK_SEPARATION = 0.10;
	/** La valle deve stare sotto questa frazione del picco minore. */
	private static final double MAX_VALLEY_RATIO = 0.5;

	/** Conteggio, media e deviazione standard (sui centri dei bin) di una parte dell'istogramma. */
	public static final class Stats {
		private final long count;
		private final double mean;
		private final double stdDev;

		Stats(long count, double mean, double stdDev) {
			this.count = count;
			this.mean = mean;
			this.stdDev = stdDev;
		}

		public long getCount() {
			return count;
		}

		/** @return media, {@code NaN} se {@code count == 0}. */
		public double getMean() {
			return mean;
		}

		/** @return deviazione standard di popolazione, {@code NaN} se {@code count == 0}. */
		public double getStdDev() {
			return stdDev;
		}
	}

	private final long[] counts;
	private long total;

	public SimilarityHistogram() {
		this(DEFAULT_BINS);
	}

	public SimilarityHistogram(int bins) {
		if (bins < 2) {
			throw new IllegalArgumentException("bins deve essere >= 2, trovato " + bins);
		}
		this.counts = new long[bins];
	}

	/** Aggiunge una similarita'; valori fuori da [0,1] sono confinati ai bin estremi, {@code NaN} ignorato. */
	public void add(double similarity) {
		if (Double.isNaN(similarity)) {
			return;
		}
		final int bins = counts.length;
		final int bin = (int) Math.min(bins - 1, Math.max(0, Math.floor(similarity * bins)));
		counts[bin]++;
		total++;
	}

	/** Come {@link #add(double)}, ripetuto {@code times} volte (dati sintetici, ricostruzioni da conteggi). */
	public void add(double similarity, long times) {
		if (Double.isNaN(similarity) || times <= 0) {
			return;
		}
		final int bins = counts.length;
		final int bin = (int) Math.min(bins - 1, Math.max(0, Math.floor(similarity * bins)));
		counts[bin] += times;
		total += times;
	}

	/** Istogramma con i conteggi per bin dati (copiati; conteggi negativi trattati come 0). */
	public static SimilarityHistogram fromCounts(long[] binCounts) {
		final SimilarityHistogram histogram = new SimilarityHistogram(binCounts.length);
		for (int i = 0; i < binCounts.length; i++) {
			final long count = Math.max(0, binCounts[i]);
			histogram.counts[i] = count;
			histogram.total += count;
		}
		return histogram;
	}

	/** Somma bin a bin di due istogrammi con lo stesso numero di bin. */
	public static SimilarityHistogram combine(SimilarityHistogram a, SimilarityHistogram b) {
		if (a.counts.length != b.counts.length) {
			throw new IllegalArgumentException(
					"numero di bin diverso: " + a.counts.length + " vs " + b.counts.length);
		}
		final SimilarityHistogram sum = new SimilarityHistogram(a.counts.length);
		for (int i = 0; i < sum.counts.length; i++) {
			sum.counts[i] = a.counts[i] + b.counts[i];
		}
		sum.total = a.total + b.total;
		return sum;
	}

	/** Numero di similarita' nei bin il cui bordo inferiore e' pari o sopra {@code threshold}. */
	public long countAtOrAbove(double threshold) {
		long count = 0;
		for (int i = 0; i < counts.length; i++) {
			if (binLow(i) >= threshold - 1e-9) {
				count += counts[i];
			}
		}
		return count;
	}

	public int getBins() {
		return counts.length;
	}

	public long getCount(int bin) {
		return counts[bin];
	}

	public long getTotal() {
		return total;
	}

	public double binLow(int bin) {
		return (double) bin / counts.length;
	}

	public double binHigh(int bin) {
		return (double) (bin + 1) / counts.length;
	}

	private double binCenter(int bin) {
		return (binLow(bin) + binHigh(bin)) / 2;
	}

	/**
	 * Soglia di Otsu (massimizza la varianza tra le due classi).
	 *
	 * @return bordo superiore del bin di taglio, {@code NaN} se l'istogramma e'
	 *         vuoto o tutte le similarita' cadono in un solo bin
	 */
	public double otsuThreshold() {
		if (total == 0) {
			return Double.NaN;
		}
		double sumAll = 0;
		for (int i = 0; i < counts.length; i++) {
			sumAll += binCenter(i) * counts[i];
		}
		double weightBelow = 0;
		double sumBelow = 0;
		final double[] variances = new double[counts.length - 1];
		double bestVariance = 0;
		for (int t = 0; t < variances.length; t++) {
			weightBelow += counts[t];
			sumBelow += binCenter(t) * counts[t];
			final double weightAbove = total - weightBelow;
			if (weightBelow == 0 || weightAbove == 0) {
				continue;
			}
			final double meanBelow = sumBelow / weightBelow;
			final double meanAbove = (sumAll - sumBelow) / weightAbove;
			variances[t] = weightBelow * weightAbove * (meanBelow - meanAbove) * (meanBelow - meanAbove);
			bestVariance = Math.max(bestVariance, variances[t]);
		}
		if (bestVariance == 0) {
			return Double.NaN;
		}
		// Ogni taglio dentro un tratto vuoto tra i due modi ha la stessa varianza:
		// si prende il punto medio del plateau, non il suo primo bordo.
		int first = -1;
		int last = -1;
		for (int t = 0; t < variances.length; t++) {
			if (variances[t] >= bestVariance * (1 - 1e-9)) {
				first = first < 0 ? t : first;
				last = t;
			}
		}
		return (binHigh(first) + binHigh(last)) / 2;
	}

	/**
	 * @return {@code true} se, sull'istogramma smussato, esistono due picchi
	 *         separati da almeno {@value #MIN_PEAK_SEPARATION} con una valle
	 *         sotto {@value #MAX_VALLEY_RATIO} del picco minore
	 */
	public boolean isBimodal() {
		return findValley() != null;
	}

	/**
	 * Centro della valle tra i due picchi principali dell'istogramma smussato
	 * (per un plateau di minimo, il suo punto medio).
	 *
	 * @return la soglia, {@code NaN} se l'istogramma non e' bimodale
	 */
	public double valleyThreshold() {
		final double[] valley = findValley();
		return valley == null ? Double.NaN : valley[0];
	}

	/** @return {@code {soglia}} oppure {@code null} se non bimodale. */
	private double[] findValley() {
		if (total == 0) {
			return null;
		}
		final double[] s = smoothed();
		int p1 = 0;
		for (int i = 1; i < s.length; i++) {
			if (s[i] > s[p1]) {
				p1 = i;
			}
		}
		final int minSeparation = (int) Math.ceil(MIN_PEAK_SEPARATION * counts.length);
		int bestP2 = -1;
		int bestA = -1;
		int bestB = -1;
		for (int p2 = 0; p2 < s.length; p2++) {
			if (Math.abs(p2 - p1) < minSeparation || !isLocalMax(s, p2) || s[p2] <= 0) {
				continue;
			}
			final int from = Math.min(p1, p2);
			final int to = Math.max(p1, p2);
			double min = Double.MAX_VALUE;
			for (int i = from; i <= to; i++) {
				min = Math.min(min, s[i]);
			}
			if (min > MAX_VALLEY_RATIO * Math.min(s[p1], s[p2])) {
				continue;
			}
			if (bestP2 < 0 || s[p2] > s[bestP2]) {
				bestP2 = p2;
				int a = from;
				while (s[a] > min + 1e-12) {
					a++;
				}
				int b = to;
				while (s[b] > min + 1e-12) {
					b--;
				}
				bestA = a;
				bestB = b;
			}
		}
		if (bestP2 < 0) {
			return null;
		}
		return new double[] { (binLow(bestA) + binHigh(bestB)) / 2 };
	}

	private static boolean isLocalMax(double[] s, int i) {
		final double left = i > 0 ? s[i - 1] : Double.NEGATIVE_INFINITY;
		final double right = i < s.length - 1 ? s[i + 1] : Double.NEGATIVE_INFINITY;
		return s[i] >= left && s[i] >= right;
	}

	private double[] smoothed() {
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

	/** Statistiche dei bin il cui centro e' strettamente sotto {@code threshold}. */
	public Stats statsBelow(double threshold) {
		return stats(threshold, true);
	}

	/** Statistiche dei bin il cui centro e' pari o sopra {@code threshold}. */
	public Stats statsAtOrAbove(double threshold) {
		return stats(threshold, false);
	}

	private Stats stats(double threshold, boolean below) {
		long count = 0;
		double sum = 0;
		for (int i = 0; i < counts.length; i++) {
			if (below == (binCenter(i) < threshold)) {
				count += counts[i];
				sum += binCenter(i) * counts[i];
			}
		}
		if (count == 0) {
			return new Stats(0, Double.NaN, Double.NaN);
		}
		final double mean = sum / count;
		double squares = 0;
		for (int i = 0; i < counts.length; i++) {
			if (below == (binCenter(i) < threshold)) {
				squares += counts[i] * (binCenter(i) - mean) * (binCenter(i) - mean);
			}
		}
		return new Stats(count, mean, Math.sqrt(squares / count));
	}
}
