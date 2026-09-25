/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.SimilarityHistogram;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimate.Regime;

/**
 * Stima non supervisionata della soglia di matching dalla distribuzione delle
 * similarita' delle coppie candidate del blocking, con una misura di
 * affidabilita'.
 * <p>
 * Un ensemble di stimatori propone il "ginocchio" tra la popolazione dei
 * non-match e quella dei match:
 * <ul>
 * <li><b>mix</b>: mistura di Beta consapevole del blocking LSH
 * ({@link BetaMixtureModel}); ginocchio = soglia che massimizza la F1 stimata;</li>
 * <li><b>triangolo</b>, <b>cambio</b>, <b>otsu-log</b>: rilevatori non
 * parametrici ({@link KneeDetectors}) per valli, spalle/altopiani;</li>
 * <li><b>valle</b>: minimo tra i due picchi
 * ({@link SimilarityHistogram#valleyThreshold()});</li>
 * <li><b>vuoto</b>: tratto di bin a zero tra i due modi (modi separati).</li>
 * </ul>
 * Il consenso sceglie il ginocchio e classifica il regime
 * ({@link Regime}); un bootstrap di Poisson dell'istogramma (seme fisso,
 * deterministico) da' l'intervallo di confidenza; l'affidabilita' e' la media
 * geometrica pesata di separazione, bonta' del fit, accordo tra stimatori,
 * stabilita' e supporto. La soglia finale sposta il ginocchio di epsilon
 * secondo {@link ThresholdMode}.
 */
public final class ThresholdEstimator {

	public static final double DEFAULT_EPSILON = 0.03;
	public static final double MAX_EPSILON = 0.2;
	/** Ginocchio di ripiego quando l'istogramma e' troppo povero per qualsiasi stima. */
	public static final double FALLBACK_KNEE = 0.75;

	private static final int MIN_PAIRS = 200;
	private static final int MIN_NON_EMPTY_BINS = 5;
	private static final double CONSENSUS_TOLERANCE = 0.05;
	private static final double MIN_BIC_GAIN = 10d;
	private static final double MIN_CORNER_DEPTH = 0.05;
	private static final double MIN_MODE_SEPARATION = 0.10;
	/** Distanza di variazione totale (oltre il rumore di Poisson) sotto cui il modello di mistura e' considerato affidabile. */
	private static final double TRUSTED_FIT_EXCESS = 0.10;
	private static final int BOOTSTRAP_REPLICATES = 100;
	private static final long BOOTSTRAP_SEED = 20260925L;
	private static final int FULL_FIT_ITERATIONS = 1500;
	private static final int WARM_FIT_ITERATIONS = 200;
	private static final double GRID_STEP = 0.001;
	/** Soglia minima applicabile: mai 0 o negativa, qualunque sia il ginocchio e l'epsilon. */
	private static final double MIN_THRESHOLD = 1e-3;
	/** Da qui in su passano solo coppie praticamente identiche. */
	private static final double DEGENERATE_THRESHOLD = 0.999;

	private final LshPassProbability lsh;

	public ThresholdEstimator(LshPassProbability lsh) {
		this.lsh = lsh == null ? LshPassProbability.NONE : lsh;
	}

	/** Risultato intermedio dell'ensemble su un vettore di conteggi. */
	private static final class Core {
		double knee = FALLBACK_KNEE;
		Regime regime = Regime.INDETERMINATO;
		final Map<String, Double> estimators = new LinkedHashMap<>();
		BetaMixtureModel model;
		double bicGain = Double.NaN;
		boolean modelBimodal;
		KneeDetectors.Gap gap;
		KneeDetectors.Corner corner = new KneeDetectors.Corner(Double.NaN, 0d);
		double matchObserved = Double.NaN;
		double fitExcess = Double.NaN;
	}

	public ThresholdEstimate estimate(SimilarityHistogram histogram, ThresholdMode mode, double epsilon) {
		if (epsilon < 0 || epsilon > MAX_EPSILON) {
			throw new IllegalArgumentException("epsilon deve essere in [0, " + MAX_EPSILON + "], trovato " + epsilon);
		}
		final int bins = histogram.getBins();
		final double[] counts = new double[bins];
		int nonEmpty = 0;
		for (int i = 0; i < bins; i++) {
			counts[i] = histogram.getCount(i);
			if (counts[i] > 0) {
				nonEmpty++;
			}
		}
		final List<String> warnings = new ArrayList<>();
		if (histogram.getTotal() < MIN_PAIRS || nonEmpty < MIN_NON_EMPTY_BINS) {
			warnings.add("Troppe poche coppie candidate (" + histogram.getTotal() + ") per stimare la soglia: ginocchio di ripiego "
					+ FALLBACK_KNEE);
			final double threshold = boundedThreshold(FALLBACK_KNEE, mode, epsilon, 0d, warnings);
			return new ThresholdEstimate(mode, epsilon, FALLBACK_KNEE, threshold, FALLBACK_KNEE, FALLBACK_KNEE, 0d,
					Regime.INDETERMINATO, new LinkedHashMap<>(), new LinkedHashMap<>(), Double.NaN, Double.NaN,
					Double.NaN, Double.NaN, Double.NaN, histogram.getTotal(), warnings);
		}

		final double[] centers = centers(bins);
		final double[] pass = new double[bins];
		for (int i = 0; i < bins; i++) {
			pass[i] = lsh.at(centers[i]);
		}
		final double width = 1d / bins;
		final double total = histogram.getTotal();

		final Core core = computeCore(counts, centers, width, pass, null, null);

		final double[] sample = bootstrapKnees(counts, centers, width, pass, core);
		final double sigma = standardDeviation(sample);
		final double ciLow = sample.length >= 20 ? percentile(sample, 0.025) : core.knee;
		final double ciHigh = sample.length >= 20 ? percentile(sample, 0.975) : core.knee;

		final Map<String, Double> components = new LinkedHashMap<>();
		final double reliability = reliability(core, counts, total, sigma, components);

		final double peakCenter = centers[KneeDetectors.argMax(KneeDetectors.smooth(counts))];
		final double lowerBound = peakCenter <= core.knee ? peakCenter + width : 0d;
		final double threshold = boundedThreshold(core.knee, mode, epsilon, lowerBound, warnings);

		double precision = Double.NaN;
		double recall = Double.NaN;
		double f1 = Double.NaN;
		double lost = Double.NaN;
		if (core.model != null) {
			final Quality quality = new Quality(core.model, counts);
			final double[] tpFpFn = quality.at(threshold);
			precision = tpFpFn[0] + tpFpFn[1] > 0 ? tpFpFn[0] / (tpFpFn[0] + tpFpFn[1]) : Double.NaN;
			recall = tpFpFn[0] + tpFpFn[2] > 0 ? tpFpFn[0] / (tpFpFn[0] + tpFpFn[2]) : Double.NaN;
			f1 = quality.f1(tpFpFn);
			lost = quality.lost;
		}

		if (reliability < 0.5) {
			warnings.add(String.format(java.util.Locale.ROOT,
					"Affidabilita' bassa (%.2f): la soglia stimata e' applicata comunque, da verificare", reliability));
		}
		if (core.regime == Regime.INDETERMINATO) {
			warnings.add("Nessuna evidenza di due popolazioni (match / non-match) nella distribuzione: la soglia e' solo un'ipotesi");
		}
		if (!Double.isNaN(core.matchObserved) && core.matchObserved < 100) {
			warnings.add(String.format(java.util.Locale.ROOT, "Pochi match attesi tra le coppie candidate (~%.0f)",
					core.matchObserved));
		}
		if (core.model == null) {
			warnings.add("Modello di mistura non stimabile: nessuna stima di precision/recall");
		}

		return new ThresholdEstimate(mode, epsilon, core.knee, threshold, Math.min(ciLow, core.knee),
				Math.max(ciHigh, core.knee), reliability, core.regime, components, core.estimators, precision, recall, f1,
				core.matchObserved, lost, histogram.getTotal(), warnings);
	}

	/**
	 * Applica epsilon al ginocchio e limita il risultato a
	 * {@code [max(lowerBound, MIN_THRESHOLD), 1.0]}: una soglia non puo' superare
	 * 1 (la Jaccard e' al massimo 1) ne' scendere sotto il picco dei non-match
	 * (o sotto {@value #MIN_THRESHOLD}), quindi ne' un epsilon grande sul
	 * ginocchio alto (0.9 + 0.2) ne' uno sul ginocchio basso (0.15 - 0.2) danno
	 * una soglia fuori dominio. Ogni limite raggiunto e' segnalato in
	 * {@code warnings}: l'epsilon richiesto non e' stato applicato per intero.
	 */
	static double boundedThreshold(double knee, ThresholdMode mode, double epsilon, double lowerBound,
			List<String> warnings) {
		final double requested = knee + mode.shift(epsilon);
		final double low = Math.max(lowerBound, MIN_THRESHOLD);
		final double bounded = Math.min(1d, Math.max(low, requested));
		if (requested > 1d) {
			warnings.add(String.format(java.util.Locale.ROOT,
					"Epsilon non applicato per intero: ginocchio %.3f + %.3f = %.3f, soglia limitata a 1.000", knee,
					epsilon, requested));
		}
		else if (requested < low) {
			warnings.add(String.format(java.util.Locale.ROOT,
					"Epsilon non applicato per intero: ginocchio %.3f - %.3f = %.3f, soglia limitata a %.3f "
							+ "(picco dei non-match)", knee, epsilon, requested, low));
		}
		if (bounded >= DEGENERATE_THRESHOLD) {
			warnings.add(String.format(java.util.Locale.ROOT,
					"Soglia %.3f: solo le coppie praticamente identiche possono superarla", bounded));
		}
		return bounded;
	}

	private static double[] centers(int bins) {
		final double[] centers = new double[bins];
		for (int i = 0; i < bins; i++) {
			centers[i] = (i + 0.5) / bins;
		}
		return centers;
	}

	/**
	 * Esegue l'ensemble su un vettore di conteggi.
	 *
	 * @param warm         modello di partenza per un bootstrap ({@code null} = adattamento completo con piu' strutture)
	 * @param bimodalHint  per un replicato di bootstrap, se il run principale era bimodale ({@code null} = calcolato dal BIC)
	 */
	private Core computeCore(double[] h, double[] centers, double width, double[] pass, BetaMixtureModel warm,
			Boolean bimodalHint) {
		final boolean full = warm == null && bimodalHint == null;
		final int bins = h.length;
		double total = 0;
		for (final double c : h) {
			total += c;
		}
		final Core core = new Core();
		final double minRightMass = Math.max(30d, 1e-4 * total);
		final int minGapBins = Math.max(3, (int) Math.round(0.02 * bins));

		core.gap = KneeDetectors.gap(h, minGapBins, minRightMass);
		core.corner = KneeDetectors.triangle(h);
		final double stick = KneeDetectors.brokenStick(h);
		final double logOtsu = KneeDetectors.logOtsu(h);
		final long[] rounded = new long[bins];
		for (int i = 0; i < bins; i++) {
			rounded[i] = Math.round(h[i]);
		}
		final double valley = SimilarityHistogram.fromCounts(rounded).valleyThreshold();
		final double corner = core.corner.depth >= MIN_CORNER_DEPTH ? core.corner.position : Double.NaN;

		core.estimators.put("mix", Double.NaN);
		core.estimators.put("triangolo", corner);
		core.estimators.put("cambio", stick);
		core.estimators.put("valle", valley);
		core.estimators.put("otsu-log", logOtsu);
		core.estimators.put("vuoto", core.gap != null ? core.gap.middle() : Double.NaN);

		final List<Double> others = new ArrayList<>();
		for (final String key : new String[] { "triangolo", "cambio", "valle", "otsu-log" }) {
			if (!Double.isNaN(core.estimators.get(key))) {
				others.add(core.estimators.get(key));
			}
		}
		final boolean nonparametricEvidence = !Double.isNaN(valley) || !Double.isNaN(corner) || !Double.isNaN(stick);

		// Modello di mistura (E1). Un replicato di bootstrap riparte dal modello principale.
		double modelKnee = Double.NaN;
		if (full) {
			final List<Double> splits = splitCandidates(others, core.gap);
			fitAll(core, centers, width, h, pass, splits, total);
		}
		else if (warm != null) {
			core.model = BetaMixtureModel.fit(centers, width, h, pass, warm.nonMatchComponents, warm.matchComponents, 0d,
					warm, WARM_FIT_ITERATIONS);
		}
		if (core.model != null) {
			final Quality quality = new Quality(core.model, h);
			core.matchObserved = quality.matchObserved;
			core.fitExcess = fitExcess(core.model, h, total);
			final double[] moments = {core.model.groupMoments(true)[0], core.model.groupMoments(false)[0]};
			final boolean separated = !Double.isNaN(moments[0]) && !Double.isNaN(moments[1])
					&& moments[0] - moments[1] >= MIN_MODE_SEPARATION;
			if (bimodalHint != null) {
				core.modelBimodal = bimodalHint && separated;
			}
			else {
				core.modelBimodal = core.bicGain >= MIN_BIC_GAIN && quality.matchObserved >= Math.max(30d, 1e-4 * total)
						&& separated;
			}
			modelKnee = quality.bestThreshold();
			core.estimators.put("mix", core.modelBimodal ? modelKnee : Double.NaN);
		}

		if (core.gap != null) {
			core.regime = Regime.SEPARATI;
			core.knee = core.gap.middle();
		}
		else if (core.modelBimodal) {
			// Un modello che descrive bene i dati (anche con un altopiano o picchi di duplicati esatti che
			// ingannano valle/triangolo) decide da solo; gli altri stimatori restano diagnostica di accordo.
			if (core.fitExcess <= TRUSTED_FIT_EXCESS || others.isEmpty()
					|| Math.abs(modelKnee - median(others)) <= CONSENSUS_TOLERANCE || !nonparametricEvidence) {
				core.knee = modelKnee;
			}
			else {
				final List<Double> all = new ArrayList<>(others);
				all.add(modelKnee);
				core.knee = median(all);
			}
			core.regime = Double.isNaN(valley) ? Regime.ALTOPIANO : Regime.VALLE;
		}
		else if (nonparametricEvidence && !others.isEmpty()) {
			core.knee = median(others);
			core.regime = Double.isNaN(valley) ? Regime.ALTOPIANO : Regime.VALLE;
		}
		else {
			core.regime = Regime.INDETERMINATO;
			core.knee = !Double.isNaN(core.corner.position) ? core.corner.position
					: !Double.isNaN(logOtsu) ? logOtsu : FALLBACK_KNEE;
		}
		core.knee = Math.min(1d, Math.max(1e-3, core.knee));
		return core;
	}

	/** Punti di partenza deterministici per l'EM: i ginocchi degli altri stimatori, o una griglia di ripiego. */
	private static List<Double> splitCandidates(List<Double> others, KneeDetectors.Gap gap) {
		final TreeSet<Long> unique = new TreeSet<>();
		final List<Double> values = new ArrayList<>(others);
		if (gap != null) {
			values.add(gap.middle());
		}
		for (final double v : values) {
			if (v > 0.05 && v < 0.98) {
				unique.add(Math.round(v / 0.02));
			}
		}
		final List<Double> splits = new ArrayList<>();
		for (final long u : unique) {
			splits.add(u * 0.02);
		}
		if (splits.isEmpty()) {
			splits.addAll(Arrays.asList(0.5, 0.65, 0.8));
		}
		while (splits.size() > 5) {
			splits.remove(splits.size() / 2);
		}
		return splits;
	}

	private void fitAll(Core core, double[] centers, double width, double[] h, double[] pass, List<Double> splits,
			double total) {
		final int[][] structures = { { 1, 1 }, { 1, 2 }, { 2, 1 } };
		BetaMixtureModel best = null;
		for (final int[] structure : structures) {
			BetaMixtureModel bestOfStructure = null;
			for (final double split : splits) {
				final BetaMixtureModel m = BetaMixtureModel.fit(centers, width, h, pass, structure[0], structure[1], split,
						null, FULL_FIT_ITERATIONS);
				if (!Double.isFinite(m.logLikelihood)) {
					continue;
				}
				if (bestOfStructure == null || m.logLikelihood > bestOfStructure.logLikelihood) {
					bestOfStructure = m;
				}
			}
			if (bestOfStructure != null && (best == null || bestOfStructure.bic(total) < best.bic(total))) {
				best = bestOfStructure;
			}
		}
		if (best == null) {
			return;
		}
		final BetaMixtureModel unimodal = BetaMixtureModel.fit(centers, width, h, pass, 1, 0, splits.get(0), null,
				FULL_FIT_ITERATIONS);
		core.model = best;
		core.bicGain = Double.isFinite(unimodal.logLikelihood) ? unimodal.bic(total) - best.bic(total) : Double.POSITIVE_INFINITY;
	}

	private double[] bootstrapKnees(double[] h, double[] centers, double width, double[] pass, Core main) {
		final Random random = new Random(BOOTSTRAP_SEED);
		final List<Double> knees = new ArrayList<>();
		final double[] replicate = new double[h.length];
		for (int b = 0; b < BOOTSTRAP_REPLICATES; b++) {
			for (int i = 0; i < h.length; i++) {
				replicate[i] = poisson(random, h[i]);
			}
			final Core core = computeCore(replicate.clone(), centers, width, pass, main.model, main.modelBimodal);
			if (Double.isFinite(core.knee)) {
				knees.add(core.knee);
			}
		}
		final double[] result = new double[knees.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = knees.get(i);
		}
		return result;
	}

	private static double poisson(Random random, double lambda) {
		if (lambda <= 0) {
			return 0;
		}
		if (lambda < 30) {
			final double limit = Math.exp(-lambda);
			long k = 0;
			double p = 1;
			do {
				k++;
				p *= random.nextDouble();
			}
			while (p > limit);
			return k - 1;
		}
		return Math.max(0, Math.round(lambda + Math.sqrt(lambda) * random.nextGaussian()));
	}

	/** Stime di TP/FP/FN in funzione della soglia dal modello di mistura. */
	private static final class Quality {
		private final double[] obsMatch;
		private final double[] obsNonMatch;
		private final double width;
		final double lost;
		final double matchObserved;

		Quality(BetaMixtureModel model, double[] h) {
			final double[] posterior = model.posteriorMatch();
			obsMatch = new double[h.length];
			obsNonMatch = new double[h.length];
			double matches = 0;
			for (int i = 0; i < h.length; i++) {
				obsMatch[i] = h[i] * posterior[i];
				obsNonMatch[i] = h[i] * (1d - posterior[i]);
				matches += obsMatch[i];
			}
			matchObserved = matches;
			lost = model.lostMatches();
			width = model.binWidth();
		}

		/** @return {@code {TP, FP, FN}} (FN include i match persi dal blocking). */
		double[] at(double threshold) {
			return counts(threshold, true);
		}

		private double[] counts(double threshold, boolean includeLost) {
			double tp = 0;
			double fp = 0;
			double fn = 0;
			for (int i = 0; i < obsMatch.length; i++) {
				final double high = (i + 1) * width;
				final double above = Math.min(1d, Math.max(0d, (high - threshold) / width));
				tp += above * obsMatch[i];
				fp += above * obsNonMatch[i];
				fn += (1d - above) * obsMatch[i];
			}
			return new double[] { tp, fp, fn + (includeLost ? lost : 0d) };
		}

		double f1(double[] tpFpFn) {
			final double denominator = 2 * tpFpFn[0] + tpFpFn[1] + tpFpFn[2];
			return denominator > 0 ? 2 * tpFpFn[0] / denominator : 0d;
		}

		/** F1 tra le sole coppie candidate (senza i match persi dal blocking) alla soglia. */
		double observedF1(double threshold) {
			return f1(counts(threshold, false));
		}

		/** Soglia che massimizza la F1 stimata (punto medio di un eventuale plateau). */
		double bestThreshold() {
			double best = -1;
			final List<Double> grid = new ArrayList<>();
			final List<Double> scores = new ArrayList<>();
			for (double t = GRID_STEP; t <= 1d + 1e-12; t += GRID_STEP) {
				final double score = f1(at(t));
				grid.add(t);
				scores.add(score);
				best = Math.max(best, score);
			}
			double first = Double.NaN;
			double last = Double.NaN;
			for (int i = 0; i < grid.size(); i++) {
				if (scores.get(i) >= best - 1e-9) {
					if (Double.isNaN(first)) {
						first = grid.get(i);
					}
					last = grid.get(i);
				}
			}
			return (first + last) / 2d;
		}
	}

	private double reliability(Core core, double[] h, double total, double sigma, Map<String, Double> components) {
		// Separazione: 1 - errore stimato al ginocchio (F1 tra le coppie osservate), 1 se c'e' un vuoto
		double separation;
		if (core.gap != null) {
			separation = 1d;
		}
		else if (core.model != null) {
			separation = clamp01(1d - (1d - new Quality(core.model, h).observedF1(core.knee)) / 0.2d);
		}
		else {
			separation = clamp01(core.corner.depth / 0.5d);
		}

		// Bonta' del fit: distanza di variazione totale corretta per il rumore di Poisson + evidenza BIC
		double fit = 0d;
		if (core.model != null) {
			final double fitTerm = clamp01(1d - core.fitExcess / 0.25d);
			final double gainTerm = clamp01((Double.isNaN(core.bicGain) ? 0d : core.bicGain) / 20d);
			fit = 0.6 * fitTerm + 0.4 * gainTerm;
		}

		// Accordo tra stimatori (scarti dentro un eventuale vuoto non contano)
		double agreement = 0.3;
		final List<Double> values = new ArrayList<>();
		for (final double v : core.estimators.values()) {
			if (!Double.isNaN(v)) {
				values.add(v);
			}
		}
		if (values.size() >= 2) {
			final double halfGap = core.gap != null ? (core.gap.to - core.gap.from) / 2d : 0d;
			double sum = 0;
			for (final double v : values) {
				final double deviation = Math.max(0d, Math.abs(v - core.knee) - halfGap);
				sum += clamp01(1d - deviation / 0.10d);
			}
			agreement = sum / values.size();
		}

		final double stability = Double.isNaN(sigma) ? 0d : clamp01(1d - sigma / 0.05d);

		double matches = core.matchObserved;
		if (Double.isNaN(matches)) {
			matches = 0;
			for (int i = 0; i < h.length; i++) {
				if ((i + 0.5) / h.length >= core.knee) {
					matches += h[i];
				}
			}
		}
		final double support = Math.min(clamp01(Math.log1p(matches) / Math.log(201d)),
				clamp01(Math.log1p(total) / Math.log(5001d)));

		components.put("separazione", separation);
		components.put("fit", fit);
		components.put("accordo", agreement);
		components.put("stabilita'", stability);
		components.put("supporto", support);

		final double[] weights = { 0.35, 0.15, 0.20, 0.20, 0.10 };
		final double[] scores = { separation, fit, agreement, stability, support };
		double logSum = 0;
		for (int i = 0; i < scores.length; i++) {
			logSum += weights[i] * Math.log(Math.max(scores[i], 0.02));
		}
		double reliability = Math.exp(logSum);
		if (core.regime == Regime.INDETERMINATO) {
			reliability = Math.min(reliability, 0.25);
		}
		return clamp01(reliability);
	}

	/**
	 * Distanza di variazione totale tra conteggi osservati e attesi dal modello,
	 * oltre il rumore di Poisson atteso ({@code sum|h-lambda|/N - sum sqrt(2 lambda/pi)/sum lambda}).
	 */
	private static double fitExcess(BetaMixtureModel model, double[] h, double total) {
		final double[] lambda = model.expectedObserved();
		double distance = 0;
		double noise = 0;
		double sumLambda = 0;
		for (int i = 0; i < h.length; i++) {
			distance += Math.abs(h[i] - lambda[i]);
			noise += Math.sqrt(2d * Math.max(0d, lambda[i]) / Math.PI);
			sumLambda += lambda[i];
		}
		return Math.max(0d, distance / total - noise / Math.max(1d, sumLambda));
	}

	private static double clamp01(double value) {
		return Math.min(1d, Math.max(0d, value));
	}

	private static double median(List<Double> values) {
		final double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
		final int n = sorted.length;
		return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2d;
	}

	private static double standardDeviation(double[] values) {
		if (values.length < 20) {
			return Double.NaN;
		}
		double mean = 0;
		for (final double v : values) {
			mean += v;
		}
		mean /= values.length;
		double sum = 0;
		for (final double v : values) {
			sum += (v - mean) * (v - mean);
		}
		return Math.sqrt(sum / (values.length - 1));
	}

	private static double percentile(double[] values, double fraction) {
		final double[] sorted = values.clone();
		Arrays.sort(sorted);
		final double position = fraction * (sorted.length - 1);
		final int low = (int) Math.floor(position);
		final int high = (int) Math.ceil(position);
		return sorted[low] + (sorted[high] - sorted[low]) * (position - low);
	}
}
