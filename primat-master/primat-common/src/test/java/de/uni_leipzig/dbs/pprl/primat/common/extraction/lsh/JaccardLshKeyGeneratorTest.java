package de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh;

import static org.junit.jupiter.api.Assertions.*;

import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class JaccardLshKeyGeneratorTest {

	@Test
	public void generatePermutationsDifferPerKeySize() {
		// keySize=4 -> 4 permutations within the single key, all bits set so
		// apply() returns permutation[0] for each: if the seed only depends on
		// this.seed/this.keys (bug), all 4 entries are identical.
		final JaccardLshKeyGenerator gen = new JaccardLshKeyGenerator(4, 1, 10, 1L);
		final List<LshBlockingFunction> functions = gen.generate();
		assertEquals(1, functions.size());

		final BitSet allSet = new BitSet(10);
		allSet.set(0, 10);

		// valueRange=10 -> positions 0-9, each a single digit, so the
		// no-separator bkv string can be split one character per permutation.
		final String bkv = functions.get(0).apply(allSet);
		assertEquals(4, bkv.length());

		final long distinct = bkv.chars().distinct().count();
		assertTrue(distinct > 1, "expected differing permutations across keySize, got all-identical: " + bkv);
	}

	@Test
	public void generatePermutationsDifferPerKey() {
		// keys=3 -> 3 independently generated keys; if seed ignores the loop
		// variable "keys" (bug), every key gets the same permutation set.
		final JaccardLshKeyGenerator gen = new JaccardLshKeyGenerator(1, 3, 10, 1L);
		final List<LshBlockingFunction> functions = gen.generate();
		assertEquals(3, functions.size());

		final BitSet allSet = new BitSet(10);
		allSet.set(0, 10);

		final String bkv0 = functions.get(0).apply(allSet);
		final String bkv1 = functions.get(1).apply(allSet);
		final String bkv2 = functions.get(2).apply(allSet);

		assertFalse(bkv0.equals(bkv1) && bkv1.equals(bkv2),
			"expected differing permutations across keys, got all-identical: " + bkv0);
	}

}
