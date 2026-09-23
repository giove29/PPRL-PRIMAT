/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Copre {@link XorFolder#resultingLength(int)}: deve dimezzare la lunghezza
 * dichiarata tante volte quanto {@code foldCount}, senza bisogno di un
 * {@code BloomFilter} reale — e' il valore che la Linkage Unit riceve per
 * verificare la coerenza col proprio {@code rbfSize}/{@code valueRange}
 * (vedi {@code DataOwnerConfig#computeEffectiveRbfBitLength()}).
 */
class XorFolderTest {

	@Test
	void halvesLengthOncePerFold() {
		assertEquals(512, new XorFolder(1).resultingLength(1024));
		assertEquals(256, new XorFolder(2).resultingLength(1024));
		assertEquals(128, new XorFolder(3).resultingLength(1024));
	}

	@Test
	void zeroFoldsLeavesLengthUnchanged() {
		assertEquals(1024, new XorFolder(0).resultingLength(1024));
	}
}
