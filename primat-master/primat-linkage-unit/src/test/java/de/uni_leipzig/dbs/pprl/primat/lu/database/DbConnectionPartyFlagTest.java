package de.uni_leipzig.dbs.pprl.primat.lu.database;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;

class DbConnectionPartyFlagTest {

	@Test
	void sameFlagIsAccepted() {
		DbConnection.checkPartyFlag(new Party("A", false), new Party("A", false));
		DbConnection.checkPartyFlag(new Party("A", true), new Party("A", true));
	}

	@Test
	void changedFlagAbortsWithResetHint() {
		final IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> DbConnection.checkPartyFlag(new Party("A", true), new Party("A", false)));
		assertTrue(e.getMessage().contains("'A'"));
		assertTrue(e.getMessage().contains("reset_db.py"));
	}
}
