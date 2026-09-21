package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.data_structures.ClipConfig;

class ClipEdgePriorityFunctionTest {

	private static final double DELTA = 1e-9;

	@Test
	void strongLinkUsesMutualBestMatchAndFullStrengthValue() {
		final double[][] matrix = { { 0.0, 0.9 }, { 0.9, 0.0 } };
		final double[] rowMax = { 0.9, 0.9 }; // 0.9 e' il massimo per entrambe le righe -> STRONG
		final int[] vertexDegree = { 3, 5 };

		final ClipConfig config = new ClipConfig(); // default: 0.5/0.1/0.4, 1/0.5/0
		final ClipEdgePriorityFunction priorityFunction = new ClipEdgePriorityFunction(config);

		final double priority = priorityFunction.priority(matrix, rowMax, vertexDegree, 0, 1);

		final double expected = 0.5 * 0.9 + 0.1 * Math.min(3, 5) + 0.4 * 1.0;
		assertEquals(expected, priority, DELTA);
	}

	@Test
	void normalLinkBestForOnlyOneSide() {
		final double[][] matrix = { { 0.0, 0.0, 0.0 }, { 0.0, 0.0, 0.8 }, { 0.0, 0.8, 0.0 } };
		final double[] rowMax = { 0.0, 0.9, 0.8 }; // riga 1 ha massimo 0.9 altrove -> non max; riga 2 ha massimo 0.8 -> max
		final int[] vertexDegree = { 0, 2, 4 };

		final ClipConfig config = new ClipConfig();
		final ClipEdgePriorityFunction priorityFunction = new ClipEdgePriorityFunction(config);

		final double priority = priorityFunction.priority(matrix, rowMax, vertexDegree, 1, 2);

		final double expected = 0.5 * 0.8 + 0.1 * Math.min(2, 4) + 0.4 * 0.5;
		assertEquals(expected, priority, DELTA);
	}

	@Test
	void weakLinkIsExcludedWhenIgnoreWeakLinksEnabled() {
		final double[][] matrix = { { 0.0, 0.3 }, { 0.3, 0.0 } };
		final double[] rowMax = { 0.9, 0.8 }; // 0.3 non e' massimo per nessuna delle due righe -> WEAK
		final int[] vertexDegree = { 2, 2 };

		final ClipConfig config = new ClipConfig();
		config.setIgnoreWeakLinks(true);
		final ClipEdgePriorityFunction priorityFunction = new ClipEdgePriorityFunction(config);

		assertEquals(Double.NEGATIVE_INFINITY, priorityFunction.priority(matrix, rowMax, vertexDegree, 0, 1));
	}

	@Test
	void weakLinkIsScoredNormallyWhenIgnoreWeakLinksDisabled() {
		final double[][] matrix = { { 0.0, 0.3 }, { 0.3, 0.0 } };
		final double[] rowMax = { 0.9, 0.8 };
		final int[] vertexDegree = { 2, 4 };

		final ClipConfig config = new ClipConfig();
		config.setIgnoreWeakLinks(false);
		final ClipEdgePriorityFunction priorityFunction = new ClipEdgePriorityFunction(config);

		final double priority = priorityFunction.priority(matrix, rowMax, vertexDegree, 0, 1);

		final double expected = 0.5 * 0.3 + 0.1 * Math.min(2, 4) + 0.4 * config.getValueWeak();
		assertEquals(expected, priority, DELTA);
	}

	@Test
	void linkDegreeIsMinimumOfBothVertexDegrees() {
		final double[][] matrix = { { 0.0, 0.9 }, { 0.9, 0.0 } };
		final double[] rowMax = { 0.9, 0.9 };
		final int[] vertexDegree = { 7, 2 };

		final ClipConfig config = new ClipConfig();
		final ClipEdgePriorityFunction priorityFunction = new ClipEdgePriorityFunction(config);

		final double priority = priorityFunction.priority(matrix, rowMax, vertexDegree, 0, 1);

		final double expected = 0.5 * 0.9 + 0.1 * 2 + 0.4 * 1.0;
		assertEquals(expected, priority, DELTA);
	}
}
