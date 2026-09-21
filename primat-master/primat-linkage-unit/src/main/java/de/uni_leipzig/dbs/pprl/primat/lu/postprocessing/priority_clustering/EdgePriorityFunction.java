/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.priority_clustering;

/**
 * Funzione di priorità di un arco {@code (i,j)} della matrice di similarità
 * densa di un componente connesso, usata da {@link PriorityClusteringEngine}
 * per decidere in che ordine processare gli archi prima del passo di
 * union-find. {@code rowMax}/{@code vertexDegree} sono precalcolati una sola
 * volta dal motore (non per ogni arco) e passati qui per evitare di
 * ricalcolarli: {@code rowMax[v]} è la similarità massima uscente dal
 * vertice {@code v}, {@code vertexDegree[v]} il suo grado (numero di archi
 * non nulli) nel componente.
 *
 * <p>Ritornare {@link Double#NEGATIVE_INFINITY} esclude l'arco a priori dal
 * clustering, indipendentemente dalla sua similarità (usato da CLIP per
 * scartare i link "weak").
 */
@FunctionalInterface
public interface EdgePriorityFunction {

	double priority(double[][] similarityMatrix, double[] rowMax, int[] vertexDegree, int i, int j);

	/**
	 * Priorità = similarità grezza dell'arco. Implementazione usata dal
	 * Global Greedy "classico" (generalizzazione multipartita di
	 * {@code GreedyPostprocessor}): ignora {@code rowMax}/{@code vertexDegree}.
	 */
	EdgePriorityFunction RAW_SIMILARITY = (matrix, rowMax, vertexDegree, i, j) -> matrix[i][j];
}
