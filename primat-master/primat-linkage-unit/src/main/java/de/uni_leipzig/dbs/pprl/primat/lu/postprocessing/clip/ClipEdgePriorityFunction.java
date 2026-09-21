/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip;

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.LinkStrength;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clip.data_structures.ClipConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.priority_clustering.EdgePriorityFunction;

/**
 * Funzione di priorità CLIP (letteratura FAMER/Leipzig DBS): combina
 * similarità, "link degree" (quanto sono ambigui/connessi i due estremi) e
 * "link strength" (classificazione mutual-best-match, lo stesso criterio di
 * Symmetric Best Match, riusato da {@link LinkStrength#getLinkStrength}).
 * Porta a un motore di clustering N-ario (via {@code PriorityClusteringEngine})
 * la logica di scoring già presente ma incompleta nella classe legacy
 * {@code postprocessing.CLIP} (la cui esecuzione finale era un matching
 * bipartito rotto, mai completato).
 *
 * <p><b>Differenza intenzionale rispetto al codice legacy</b>: qui
 * {@code leftMax}/{@code rightMax} usano l'uguaglianza di valore sul double
 * ({@code similarity == rowMax[i]}) invece dell'uguaglianza di oggetto
 * {@code SimilarityVector} (che con {@code Collections.max} su archi pari
 * merito ne marcava arbitrariamente uno solo come massimo). Con l'uguaglianza
 * di valore, tutti gli archi pari merito al massimo di una riga sono
 * considerati "max" — comportamento più corretto e deterministico.
 */
public class ClipEdgePriorityFunction implements EdgePriorityFunction {

	private final ClipConfig config;

	public ClipEdgePriorityFunction(ClipConfig config) {
		this.config = config;
	}

	@Override
	public double priority(double[][] similarityMatrix, double[] rowMax, int[] vertexDegree, int i, int j) {
		final double similarity = similarityMatrix[i][j];
		final int linkDegree = Math.min(vertexDegree[i], vertexDegree[j]);

		final boolean leftMax = similarity == rowMax[i];
		final boolean rightMax = similarity == rowMax[j];
		final LinkStrength strength = LinkStrength.getLinkStrength(leftMax, rightMax);

		if (config.isIgnoreWeakLinks() && strength == LinkStrength.WEAK) {
			return Double.NEGATIVE_INFINITY;
		}

		final double strengthValue;
		switch (strength) {
			case STRONG:
				strengthValue = config.getValueStrong();
				break;
			case NORMAL:
				strengthValue = config.getValueNormal();
				break;
			default:
				strengthValue = config.getValueWeak();
		}

		return config.getWeightSimilarity() * similarity + config.getWeightLinkDegree() * linkDegree
				+ config.getWeightLinkStrength() * strengthValue;
	}
}
