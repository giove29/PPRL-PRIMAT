package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;

/**
 * {@code anyPartyDuplicateFree} non instrada più alcun run (era il vecchio
 * {@code chooseStrategy}, rinominato): oggi è usato solo come regola di
 * validazione da {@code LinkageUnitConfigLoader} per la strategia MSCD_AP.
 * Mirror del precedente {@code LinkageUnitOrchestratorRoutingTest}, riscritto
 * su un booleano invece di {@code LinkStrategy}.
 */
class LinkageUnitOrchestratorValidationTest {

	@Test
	void allCleanReturnsTrue() {
		final List<Party> parties = List.of(new Party("A", true), new Party("B", true));
		assertTrue(LinkageUnitOrchestrator.anyPartyDuplicateFree(parties));
	}

	@Test
	void mixedCleanAndDirtyReturnsTrue() {
		final List<Party> parties = List.of(new Party("A", true), new Party("B", false), new Party("C", false));
		assertTrue(LinkageUnitOrchestrator.anyPartyDuplicateFree(parties));
	}

	@Test
	void allDirtyReturnsFalse() {
		final List<Party> parties = List.of(new Party("A", false), new Party("B", false), new Party("C", false));
		assertFalse(LinkageUnitOrchestrator.anyPartyDuplicateFree(parties));
	}

	@Test
	void allPartiesDuplicateFreeReturnsTrueWhenAllClean() {
		final List<Party> parties = List.of(new Party("A", true), new Party("B", true), new Party("C", true));
		assertTrue(LinkageUnitOrchestrator.allPartiesDuplicateFree(parties));
	}

	@Test
	void allPartiesDuplicateFreeReturnsFalseWhenOneDirty() {
		final List<Party> parties = List.of(new Party("A", true), new Party("B", true), new Party("C", false));
		assertFalse(LinkageUnitOrchestrator.allPartiesDuplicateFree(parties));
	}
}
