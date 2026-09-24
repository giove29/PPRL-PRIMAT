package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;

/**
 * MCL/Center Clustering: solo party dirty; Global Greedy/CLIP: solo party clean.
 */
class MultiSourceLinkageGuardTest {

	private static Map<Party, Collection<Record>> input(boolean... duplicateFree) {
		final Map<Party, Collection<Record>> input = new LinkedHashMap<>();
		for (int i = 0; i < duplicateFree.length; i++) {
			input.put(new Party("P" + i, duplicateFree[i]), List.of());
		}
		return input;
	}

	@Test
	void dirtyOnlyStrategiesAcceptAllDirtyAndRejectAnyClean() {
		MultiSourceLinkage.requireSourceDirtiness(input(false, false), "MCL", false);
		final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> MultiSourceLinkage.requireSourceDirtiness(input(false, true), "CENTER_CLUSTERING", false));
		assertTrue(e.getMessage().contains("CENTER_CLUSTERING"));
		assertTrue(e.getMessage().contains("dirty"));
	}

	@Test
	void cleanOnlyStrategiesAcceptAllCleanAndRejectAnyDirty() {
		MultiSourceLinkage.requireSourceDirtiness(input(true, true), "CLIP", true);
		final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> MultiSourceLinkage.requireSourceDirtiness(input(true, false), "GLOBAL_GREEDY", true));
		assertTrue(e.getMessage().contains("GLOBAL_GREEDY"));
		assertTrue(e.getMessage().contains("clean"));
	}
}
