/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold;

/**
 * Come usare il ginocchio stimato dalla distribuzione delle similarita':
 * esattamente ({@link #AUTO}) oppure spostato di {@code epsilon} per premiare
 * la precision ({@link #AUTO_PRECISION}, soglia piu' alta) o la recall
 * ({@link #AUTO_RECALL}, soglia piu' bassa).
 */
public enum ThresholdMode {

	AUTO,
	AUTO_PRECISION,
	AUTO_RECALL;

	/** @return lo spostamento con segno da applicare al ginocchio. */
	public double shift(double epsilon) {
		switch (this) {
			case AUTO_PRECISION:
				return epsilon;
			case AUTO_RECALL:
				return -epsilon;
			default:
				return 0d;
		}
	}
}
