/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import java.util.Locale;

import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;

/**
 * Valore di {@code similarityThreshold} nel JSON della Linkage Unit: una soglia
 * fissa (numero in (0,1]) oppure la richiesta di stimarla dalla distribuzione
 * delle similarita' ({@code "auto"}, {@code "auto_precision"},
 * {@code "auto_recall"}, con l'{@code epsilon} di {@code autoThreshold}).
 */
public final class SimilarityThresholdSpec {

	private final double fixedValue;
	private final ThresholdMode mode;
	private final double epsilon;

	private SimilarityThresholdSpec(double fixedValue, ThresholdMode mode, double epsilon) {
		this.fixedValue = fixedValue;
		this.mode = mode;
		this.epsilon = epsilon;
	}

	public static SimilarityThresholdSpec fixed(double value) {
		return new SimilarityThresholdSpec(value, null, 0d);
	}

	public static SimilarityThresholdSpec auto(ThresholdMode mode, double epsilon) {
		return new SimilarityThresholdSpec(Double.NaN, mode, epsilon);
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

	/** @return la soglia fissa, {@code NaN} in modalita' automatica. */
	public double getFixedValue() {
		return fixedValue;
	}

	/** @return la modalita' automatica, {@code null} per una soglia fissa. */
	public ThresholdMode getMode() {
		return mode;
	}

	public double getEpsilon() {
		return epsilon;
	}

	/** @return il valore come scritto nel JSON ({@code 0.75}, {@code auto_precision}, ...). */
	public String label() {
		return isAuto() ? mode.name().toLowerCase(Locale.ROOT) : String.valueOf(fixedValue);
	}

	@Override
	public String toString() {
		return !isAuto() ? label() : mode == ThresholdMode.AUTO ? label() : label() + " (epsilon " + epsilon + ")";
	}
}
