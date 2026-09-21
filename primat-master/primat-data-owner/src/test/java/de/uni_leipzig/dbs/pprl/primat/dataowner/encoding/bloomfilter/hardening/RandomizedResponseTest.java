/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;

/**
 * Copre l'implementazione BLIP (BLoom-and-flIP, Alaggan et. al. 2012) di
 * {@link RandomizedResponse}: il bug corretto era un secondo sorteggio random
 * indipendente (invece di riusare lo stesso valore) per decidere il nuovo
 * valore del bit flippato, il che rompeva la simmetria 50/50 richiesta
 * dall'algoritmo. Questi test verificano probabilita' di flip zero/uno e la
 * simmetria del bit risultante quando si flippa.
 */
class RandomizedResponseTest {

	private static final int SIZE = 2048;

	private BloomFilter allZeroBloomFilter() {
		return new BloomFilter(SIZE, new BitSet(SIZE));
	}

	@Test
	void zeroProbabilityLeavesBitsUnchanged() {
		final BloomFilter bf = allZeroBloomFilter();
		bf.getBitVector().set(5);
		bf.getBitVector().set(42);

		final RandomizedResponse blip = new RandomizedResponse(0.0, 1L);
		final BloomFilter hardened = blip.hardenBloomFilter(bf);

		assertEquals(bf.getBitVector(), hardened.getBitVector());
	}

	@Test
	void oneProbabilityAlwaysFlipsEveryBit() {
		final BloomFilter bf = allZeroBloomFilter();

		final RandomizedResponse blip = new RandomizedResponse(1.0, 1L);
		final BloomFilter hardened = blip.hardenBloomFilter(bf);

		// truth = 0 quando probability = 1: ogni bit entra sempre nel ramo di
		// flip, quindi il risultato e' un mix di bit a 0 e 1 (mai identico
		// all'input tutto a zero) su un bitset abbastanza grande.
		assertTrue(hardened.getBitVector().cardinality() > 0);
		assertTrue(hardened.getBitVector().cardinality() < SIZE);
	}

	@Test
	void bitSymmetryMatchesTheoreticalProbability() {
		// Con input tutto a zero, un bit finisce a 1 nel risultato solo se e'
		// stato flippato E il nuovo valore sorteggiato e' 1: la probabilita'
		// teorica corretta e' probability/2 (simmetria 50/50 del flip).
		//
		// Con il bug originale (seconda estrazione random indipendente,
		// confrontata con la soglia "truth + yes" anziche' 0.5), la
		// probabilita' effettiva di ottenere 1 nel ramo di flip era
		// "truth + yes" = 1 - probability/2 anziche' 0.5, quindi la
		// probabilita' complessiva di bit=1 diventava
		// probability * (1 - probability/2) — per probability=0.1, 0.095
		// invece di 0.05: quasi il doppio del valore corretto.
		final double probability = 0.1;
		final double expectedP = probability / 2.0; // 0.05
		final double buggyP = probability * (1 - probability / 2.0); // 0.095

		final BloomFilter bf = allZeroBloomFilter();

		int onesTotal = 0;
		int bitsTotal = 0;
		for (long seed = 0; seed < 200; seed++) {
			final RandomizedResponse blip = new RandomizedResponse(probability, seed);
			final BitSet result = blip.hardenBloomFilter(bf).getBitVector();
			onesTotal += result.cardinality();
			bitsTotal += SIZE;
		}

		final double observedP = (double) onesTotal / bitsTotal;
		assertTrue(Math.abs(observedP - expectedP) < 0.01,
				"P(bit=1) dovrebbe essere vicina a probability/2=" + expectedP + ", trovato " + observedP);
		assertTrue(Math.abs(observedP - buggyP) > 0.02,
				"P(bit=1) osservata (" + observedP + ") e' troppo vicina al valore atteso dalla versione buggata ("
						+ buggyP + ")");
	}

	@Test
	void sameSeedAndInputProduceDeterministicOutput() {
		final BloomFilter bf = allZeroBloomFilter();
		bf.getBitVector().set(7);

		final BitSet first = new RandomizedResponse(0.3, 99L).hardenBloomFilter(bf).getBitVector();
		final BitSet second = new RandomizedResponse(0.3, 99L).hardenBloomFilter(bf).getBitVector();

		assertEquals(first, second);
	}
}
