package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/**
 * Algoritmo ungherese (Kuhn-Munkres) a peso massimo usato da
 * {@link ClusterAssignmentPlanner} per sostituire l'assegnazione greedy.
 */
class HungarianAssignmentTest {

	private static final double U = HungarianAssignment.UNASSIGNED_WEIGHT;

	@Test
	void squareMatrixPrefersGlobalTotalOverLocalBestMatch() {
		// riga0 preferirebbe col0 (0.90) individualmente, ma la scelta
		// globalmente migliore e' riga0->col1, riga1->col0 (1.73 > 1.30).
		final double[][] weights = { { 0.90, 0.85 }, { 0.88, 0.40 } };

		final int[] assignment = HungarianAssignment.solveMaximize(weights);

		assertArrayEquals(new int[] { 1, 0 }, assignment);
	}

	@Test
	void moreRowsThanColumnsLeavesTheWorseRowUnassigned() {
		final double[][] weights = { { 0.9 }, { 0.5 } };

		final int[] assignment = HungarianAssignment.solveMaximize(weights);

		assertArrayEquals(new int[] { 0, -1 }, assignment);
	}

	@Test
	void moreColumnsThanRowsPicksTheBestColumn() {
		final double[][] weights = { { 0.2, 0.9, 0.1 } };

		final int[] assignment = HungarianAssignment.solveMaximize(weights);

		assertArrayEquals(new int[] { 1 }, assignment);
	}

	@Test
	void unassignedCellIsNeverChosenEvenIfItWouldHaveBeenTheGreedyPick() {
		final double[][] weights = { { 0.9, 0.8 }, { 0.85, U } };

		final int[] assignment = HungarianAssignment.solveMaximize(weights);

		assertArrayEquals(new int[] { 1, 0 }, assignment);
	}

	@Test
	void rowWithNoAdmissibleColumnStaysUnassigned() {
		final double[][] weights = { { 0.5, U }, { U, U } };

		final int[] assignment = HungarianAssignment.solveMaximize(weights);

		assertArrayEquals(new int[] { 0, -1 }, assignment);
	}

	@Test
	void emptyInputReturnsEmptyAssignment() {
		final int[] assignment = HungarianAssignment.solveMaximize(new double[0][0]);

		assertArrayEquals(new int[0], assignment);
	}
}
