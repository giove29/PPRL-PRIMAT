/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;

/**
 * Valore di {@code similarityThreshold} nel JSON della Linkage Unit: una soglia
 * fissa (numero in (0,1]), la richiesta di stimarla dalla distribuzione delle
 * similarita' ({@code "auto"}, {@code "auto_precision"}, {@code "auto_recall"},
 * con l'{@code epsilon} di {@code autoThreshold}), oppure {@code "range"}
 * (solo testing: esegue classificazione+clustering una volta per ogni soglia
 * nell'intervallo {@code range.{from,to,step}}, mai persistente).
 */
public final class SimilarityThresholdSpec {

	private final double fixedValue;
	private final ThresholdMode mode;
	private final double epsilon;
	private final boolean range;
	private final double rangeFrom;
	private final double rangeTo;
	private final double rangeStep;

	private SimilarityThresholdSpec(double fixedValue, ThresholdMode mode, double epsilon, boolean range,
			double rangeFrom, double rangeTo, double rangeStep) {
		this.fixedValue = fixedValue;
		this.mode = mode;
		this.epsilon = epsilon;
		this.range = range;
		this.rangeFrom = rangeFrom;
		this.rangeTo = rangeTo;
		this.rangeStep = rangeStep;
	}

	public static SimilarityThresholdSpec fixed(double value) {
		return new SimilarityThresholdSpec(value, null, 0d, false, 0d, 0d, 0d);
	}

	public static SimilarityThresholdSpec auto(ThresholdMode mode, double epsilon) {
		return new SimilarityThresholdSpec(Double.NaN, mode, epsilon, false, 0d, 0d, 0d);
	}

	/** @return una soglia di tipo {@code "range"}: mai persistente, solo testing (vedi {@link #isRange()}). */
	public static SimilarityThresholdSpec range(double from, double to, double step) {
		return new SimilarityThresholdSpec(Double.NaN, null, 0d, true, from, to, step);
	}

	/** @return {@code null} se {@code text} non e' una modalita' automatica riconosciuta. */
	public static ThresholdMode parseMode(String text) {
		if (text == null) {
			return null;
		}
		switch (text.trim().toLowerCase(Locale.ROOT)) {
			case "auto":
				return ThresholdMode.AUTO;
			case "auto_precision":
				return ThresholdMode.AUTO_PRECISION;
			case "auto_recall":
				return ThresholdMode.AUTO_RECALL;
			default:
				return null;
		}
	}

	public boolean isAuto() {
		return mode != null;
	}

	/** @return {@code true} se e' la modalita' di testing {@code "range"} (vedi {@link #rangeValues()}). */
	public boolean isRange() {
		return range;
	}

	/** @return la soglia fissa, {@code NaN} in modalita' automatica o range. */
	public double getFixedValue() {
		return fixedValue;
	}

	/** @return la modalita' automatica, {@code null} per una soglia fissa o range. */
	public ThresholdMode getMode() {
		return mode;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public double getRangeFrom() {
		return rangeFrom;
	}

	public double getRangeTo() {
		return rangeTo;
	}

	public double getRangeStep() {
		return rangeStep;
	}

	/**
	 * @return i valori di soglia da testare, da {@code rangeFrom} a
	 *         {@code rangeTo} inclusi con passo {@code rangeStep}. Ogni valore
	 *         e' calcolato come {@code rangeFrom + i * rangeStep} con
	 *         {@link BigDecimal} (moltiplicazione per indice, non addizione
	 *         ripetuta) per non accumulare drift in virgola mobile.
	 * @throws IllegalStateException se {@code !isRange()}
	 */
	public List<Double> rangeValues() {
		if (!range) {
			throw new IllegalStateException("rangeValues() richiede una soglia \"range\", questa e' " + this);
		}
		final List<Double> values = new ArrayList<>();
		final BigDecimal from = BigDecimal.valueOf(rangeFrom);
		final BigDecimal to = BigDecimal.valueOf(rangeTo);
		final BigDecimal step = BigDecimal.valueOf(rangeStep);
		for (int i = 0;; i++) {
			final BigDecimal value = from.add(step.multiply(BigDecimal.valueOf(i)));
			if (value.compareTo(to) > 0) {
				break;
			}
			values.add(value.doubleValue());
		}
		return values;
	}

	/** @return il valore come scritto nel JSON ({@code 0.75}, {@code auto_precision}, {@code range}, ...). */
	public String label() {
		if (isRange()) {
			return String.format(Locale.ROOT, "range [%.2f..%.2f step %.2f]", rangeFrom, rangeTo, rangeStep);
		}
		return isAuto() ? mode.name().toLowerCase(Locale.ROOT) : String.valueOf(fixedValue);
	}

	@Override
	public String toString() {
		if (isRange()) {
			return label();
		}
		return !isAuto() ? label() : mode == ThresholdMode.AUTO ? label() : label() + " (epsilon " + epsilon + ")";
	}
}
