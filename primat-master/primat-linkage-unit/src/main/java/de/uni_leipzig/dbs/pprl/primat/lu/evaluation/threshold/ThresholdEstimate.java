/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Esito di {@link ThresholdEstimator}: la soglia da applicare, il ginocchio da
 * cui deriva, quanto ci si puo' fidare e le stime di qualita' senza ground
 * truth. Tutto ricavato dai soli punteggi di similarita'.
 */
public final class ThresholdEstimate {

	/** Forma della distribuzione riconosciuta. */
	public enum Regime {
		/** Vuoto di bin tra i due modi (perfettamente separati). */
		SEPARATI,
		/** Minimo netto tra i due modi. */
		VALLE,
		/** Nessun minimo: i match formano una spalla/altopiano nella coda dei non-match. */
		ALTOPIANO,
		/** Nessuna evidenza di due popolazioni: la soglia e' solo un'ipotesi. */
		INDETERMINATO
	}

	/** Livello di affidabilita' (ALTA >= 0.75, MEDIA >= 0.50, BASSA altrimenti). */
	public enum Reliability {
		ALTA, MEDIA, BASSA;

		static Reliability of(double score) {
			return score >= 0.75 ? ALTA : score >= 0.5 ? MEDIA : BASSA;
		}
	}

	private final ThresholdMode mode;
	private final double epsilon;
	private final double knee;
	private final double threshold;
	private final double ciLow;
	private final double ciHigh;
	private final double reliability;
	private final Regime regime;
	private final Map<String, Double> components;
	private final Map<String, Double> estimators;
	private final double estimatedPrecision;
	private final double estimatedRecall;
	private final double estimatedF1;
	private final double estimatedMatches;
	private final double estimatedLostMatches;
	private final long pairs;
	private final List<String> warnings;

	ThresholdEstimate(ThresholdMode mode, double epsilon, double knee, double threshold, double ciLow, double ciHigh,
			double reliability, Regime regime, Map<String, Double> components, Map<String, Double> estimators,
			double estimatedPrecision, double estimatedRecall, double estimatedF1, double estimatedMatches,
			double estimatedLostMatches, long pairs, List<String> warnings) {
		this.mode = mode;
		this.epsilon = epsilon;
		this.knee = knee;
		this.threshold = threshold;
		this.ciLow = ciLow;
		this.ciHigh = ciHigh;
		this.reliability = reliability;
		this.regime = regime;
		this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
		this.estimators = Collections.unmodifiableMap(new LinkedHashMap<>(estimators));
		this.estimatedPrecision = estimatedPrecision;
		this.estimatedRecall = estimatedRecall;
		this.estimatedF1 = estimatedF1;
		this.estimatedMatches = estimatedMatches;
		this.estimatedLostMatches = estimatedLostMatches;
		this.pairs = pairs;
		this.warnings = Collections.unmodifiableList(warnings);
	}

	public ThresholdMode getMode() {
		return mode;
	}

	public double getEpsilon() {
		return epsilon;
	}

	/** @return il ginocchio stimato, prima dello spostamento di epsilon. */
	public double getKnee() {
		return knee;
	}

	/** @return la soglia da applicare: ginocchio spostato di epsilon secondo la modalita'. */
	public double getThreshold() {
		return threshold;
	}

	/** @return estremo basso dell'intervallo di confidenza al 95% del ginocchio (bootstrap). */
	public double getCiLow() {
		return ciLow;
	}

	public double getCiHigh() {
		return ciHigh;
	}

	/** @return affidabilita' in [0,1]. */
	public double getReliability() {
		return reliability;
	}

	public Reliability getReliabilityLevel() {
		return Reliability.of(reliability);
	}

	public Regime getRegime() {
		return regime;
	}

	/** @return le componenti dell'affidabilita' (separazione, fit, accordo, stabilita', supporto), ciascuna in [0,1]. */
	public Map<String, Double> getComponents() {
		return components;
	}

	/** @return il ginocchio proposto da ciascuno stimatore disponibile (mix, triangolo, cambio, valle, otsu-log, vuoto). */
	public Map<String, Double> getEstimators() {
		return estimators;
	}

	/** @return precision stimata alla soglia applicata (tra le coppie candidate), {@code NaN} senza modello. */
	public double getEstimatedPrecision() {
		return estimatedPrecision;
	}

	/** @return recall stimata alla soglia applicata, includendo i match persi dal blocking; {@code NaN} senza modello. */
	public double getEstimatedRecall() {
		return estimatedRecall;
	}

	public double getEstimatedF1() {
		return estimatedF1;
	}

	/** @return match attesi tra le coppie candidate, {@code NaN} senza modello. */
	public double getEstimatedMatches() {
		return estimatedMatches;
	}

	/** @return match della popolazione che il blocking ha scartato, {@code NaN} senza modello. */
	public double getEstimatedLostMatches() {
		return estimatedLostMatches;
	}

	/** @return numero di coppie candidate su cui e' stata fatta la stima. */
	public long getPairs() {
		return pairs;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	/** Riga di riepilogo per la sezione risultati della LU. */
	public String describe() {
		final StringBuilder sb = new StringBuilder();
		sb.append(String.format(Locale.ROOT, "%s", mode.name().toLowerCase(Locale.ROOT)));
		if (mode != ThresholdMode.AUTO) {
			sb.append(String.format(Locale.ROOT, " eps=%.3f", epsilon));
		}
		sb.append(String.format(Locale.ROOT, " -> %.3f | ginocchio %.3f [IC95 %.3f-%.3f] | regime %s"
				+ " | affidabilita' %.2f %s", threshold, knee, ciLow, ciHigh, regime, reliability,
				getReliabilityLevel()));
		if (!components.isEmpty()) {
			sb.append(" (");
			boolean first = true;
			for (final Map.Entry<String, Double> e : components.entrySet()) {
				sb.append(first ? "" : ", ").append(e.getKey()).append(String.format(Locale.ROOT, " %.2f", e.getValue()));
				first = false;
			}
			sb.append(')');
		}
		if (!Double.isNaN(estimatedF1)) {
			sb.append(String.format(Locale.ROOT, " | stima P~%.3f R~%.3f F1~%.3f", estimatedPrecision,
					estimatedRecall, estimatedF1));
		}
		return sb.toString();
	}

	/** Riga con i ginocchi proposti dai singoli stimatori. */
	public String describeEstimators() {
		final StringBuilder sb = new StringBuilder();
		for (final Map.Entry<String, Double> e : estimators.entrySet()) {
			sb.append(sb.length() > 0 ? ", " : "").append(e.getKey()).append(' ')
					.append(Double.isNaN(e.getValue()) ? "n/d" : String.format(Locale.ROOT, "%.3f", e.getValue()));
		}
		return sb.toString();
	}
}
