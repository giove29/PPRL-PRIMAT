/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.XorBitSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.XorBitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;

/**
 * Copre {@link ChainedHardener}: gli step vengono applicati in ordine, e il
 * tipo di {@link QidAttribute} risultante da {@link ChainedHardener#harden}
 * e' deciso dall'ultimo step (non da {@link ChainedHardener} stesso) — caso
 * concreto: BLIP seguito da {@link XorFolder} deve produrre uno
 * {@link XorBitSetAttribute} la cui cardinalita' riflette il bitset *dopo*
 * BLIP, non l'RBF originale.
 */
class ChainedHardenerTest {

	private static final int SIZE = 256;

	@Test
	void appliesStepsInOrder() {
		final BitSet initial = new BitSet(SIZE);
		initial.set(0);

		// XorFolder(1) dimezza le dimensioni una volta: applicarlo due volte in
		// sequenza equivale a XorFolder(2) applicato una sola volta, utile per
		// verificare che ChainedHardener rispetti l'ordine e concateni davvero gli
		// step invece di applicarne solo uno.
		final ChainedHardener chained = new ChainedHardener(List.of(new XorFolder(1), new XorFolder(1)));
		final BloomFilter result = chained.hardenBloomFilter(new BloomFilter(SIZE, initial));

		final BloomFilter expected = new XorFolder(2).hardenBloomFilter(new BloomFilter(SIZE, (BitSet) initial.clone()));
		assertEquals(expected.getSize(), result.getSize());
		assertEquals(expected.getBitVector(), result.getBitVector());
	}

	@Test
	void lastStepDecidesResultingAttributeType() {
		final BitSet initial = new BitSet(SIZE);
		initial.set(3);
		initial.set(17);

		final double probability = 0.3;
		final long seed = 7L;
		final XorFolder fold = new XorFolder(1);

		// Due istanze separate con lo stesso seed: una alimenta la catena, l'altra
		// ricalcola indipendentemente il risultato atteso dopo il solo step BLIP —
		// riusare la stessa istanza (Random e' stateful) avanzerebbe il generatore e
		// produrrebbe un bitset diverso al secondo utilizzo.
		final ChainedHardener chained = new ChainedHardener(List.of(new RandomizedResponse(probability, seed), fold));
		final QidAttribute<?> result = chained.harden(new BloomFilter(SIZE, initial));

		assertTrue(result instanceof XorBitSetAttribute, "l'ultimo step (XorFolder) deve determinare il tipo di attributo");

		// La cardinalita' catturata deve essere quella del bitset DOPO BLIP (l'input
		// che viene effettivamente passato a XorFolder.harden), non quella dell'RBF
		// originale: la si ricalcola applicando solo il primo step e confrontando.
		final BloomFilter afterBlip = new RandomizedResponse(probability, seed)
				.hardenBloomFilter(new BloomFilter(SIZE, (BitSet) initial.clone()));
		final int expectedCardinality = afterBlip.cardinality();

		final XorBitSet value = ((XorBitSetAttribute) result).getValue();
		assertEquals(expectedCardinality, value.getCounts());
	}

	@Test
	void resultingLengthComposesEachStepInOrder() {
		// BLIP non cambia la lunghezza (identita', default dell'interfaccia);
		// solo i due XorFolder la dimezzano, in sequenza: 256 -> 128 -> 64.
		final ChainedHardener chained = new ChainedHardener(
				List.of(new RandomizedResponse(0.2, 3L), new XorFolder(1), new XorFolder(1)));

		assertEquals(64, chained.resultingLength(SIZE));
	}

	@Test
	void singleStepChainBehavesLikeThatStepAlone() {
		final BitSet initial = new BitSet(SIZE);
		initial.set(5);

		final RandomizedResponse blip = new RandomizedResponse(0.2, 3L);
		final ChainedHardener chained = new ChainedHardener(List.of(blip));

		final BloomFilter direct = new RandomizedResponse(0.2, 3L).hardenBloomFilter(new BloomFilter(SIZE,
				(BitSet) initial.clone()));
		final BloomFilter viaChain = chained.hardenBloomFilter(new BloomFilter(SIZE, (BitSet) initial.clone()));

		assertEquals(direct.getBitVector(), viaChain.getBitVector());
	}
}
