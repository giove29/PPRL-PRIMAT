/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

/**
 * Probabilita' che una coppia con similarita' Jaccard {@code s} co-occorra in
 * almeno un blocco del JaccardLSH: {@code P(s) = 1 - (1 - s^r)^b}, con
 * {@code r = keySize} permutazioni MinHash per chiave e {@code b = keys}
 * chiavi. L'istogramma osservato delle similarita' e' la popolazione delle
 * coppie moltiplicata per {@code P(s)}: {@link ThresholdEstimator} ne tiene
 * conto per non scambiare il troncamento del blocking per la forma dei modi.
 */
public final class LshPassProbability {

	/** Nessun blocking (probabilita' 1 ovunque): test e istogrammi non filtrati. */
	public static final LshPassProbability NONE = new LshPassProbability(0, 0);

	private final int keySize;
	private final int keys;

	public LshPassProbability(int keySize, int keys) {
		this.keySize = keySize;
		this.keys = keys;
	}

	public double at(double similarity) {
		if (keySize <= 0 || keys <= 0) {
			return 1d;
		}
		final double s = Math.min(1d, Math.max(0d, similarity));
		return 1d - Math.pow(1d - Math.pow(s, keySize), keys);
	}

	public int getKeySize() {
		return keySize;
	}

	public int getKeys() {
		return keys;
	}
}
