/*
 * Copyright © 2016 - 2020 Leipzig University (Database Research Group)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApExemplarAssignmentType;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils.MatrixUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils.VectorUtils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * The abstract class all sequential Affinity Propagation implementations inherit from. Provides input and
 * output parameters and methods to check the consistency constraints.
 */
public abstract class AbstractMscdAffinityPropagationSeq {

  /* **********************************************************
                         Input Parameters
   ***********************************************************/

  /**
   * NxN similarity matrix for N dataPoints. Cell ij represents the similarity between dataPoint i and
   * dataPoint j.
   */
  protected final double[][] similarityMatrix;

  /**
   * The self-similarity of dirty source dataPoints. The higher the preference, the more likely a dataPoint
   * becomes an exemplar.
   */
  protected double preferenceDirtySrc;

  /**
   * The self-similarity of clean source dataPoints. The higher the preference, the more likely a dataPoint
   * becomes an exemplar.
   */
  protected double preferenceCleanSrc;

  /**
   * Damping factor for the message values of ALPHA and RHO, so they can be damped by a portion of their
   * old value from the last iteration.
   */
  protected double damping;

  /**
   * Configuration for the generic AP parameters.
   */
  protected ApConfig apConfig;

  /**
   * Random number generator to create gaussian noise
   */
  protected Random random;

  /**
   * Maps each clean source (by its name) to a list of integer IDs of its elements in the similarity-matrix.
   */
  protected Multimap<String, Integer> cleanSourceElementIndices;

  /* **********************************************************
                        WORKING PARAMETERS
   ***********************************************************/

  /**
   * Dimensions of the input matrix
   */
  protected int m;

  /* **********************************************************
                        OUTPUT PARAMETERS
   ***********************************************************/

  /**
   * Defines, whether MSCD-AP found a solution during the iterative process and stopped it.
   */
  protected boolean converged;

  /**
   * Defines, whether all similarity values in {@link #similarityMatrix} are equal.
   */
  protected boolean allEqualComponent;

  /**
   * Number of iterations which MSCD-AP took to find a solution
   */
  protected int iterCount;

  /**
   * Row/column index of an exemplar dataPoint in the message-matrices.
   */
  protected ArrayList<Integer> exemplarIndices;

  /**
   * The clustering solution. This is a vector of size N for N dataPoints. Position i contains the
   * clusterId of dataPoint i.
   */
  protected int[] labels;

  /**
   * The clustering solution. This is a vector of size N for N dataPoints. Position i contains the
   * index of the exemplar to which dataPoint i is assigned.
   */
  protected int[] exemplarAssignments;

  /**
   * Defines, whether the 1-of-N constraint of Affinity Propagation is satisfied.
   */
  protected boolean isOneOfNConstraintSatisfied;

  /**
   * Constructs AbstractMscdAffinityPropagationSeq
   *
   * @param similarityMatrix NxN similarity matrix for N dataPoints. Cell ij represents the similarity
   *                         between dataPoint i and dataPoint j.
   * @param preferenceDirtySrc The self-similarity of dirty source dataPoints
   * @param preferenceCleanSrc The self-similarity of clean source dataPoints
   * @param damping Damping factor for the message values of ALPHA and RHO, so they can be damped by a
   *                portion of their old value from the last iteration
   * @param apConfig Configuration for the generic AP parameters
   * @param cleanSourceElementIndices Maps each clean source (by its name) to a list of integer IDs of its
   *                                 elements in the similarity-matrix.
   */
  public AbstractMscdAffinityPropagationSeq(double[][] similarityMatrix, double preferenceDirtySrc,
    double preferenceCleanSrc, double damping, ApConfig apConfig,
    Multimap<String, Integer> cleanSourceElementIndices) {
    this.similarityMatrix = MatrixUtils.copy(similarityMatrix);
    this.preferenceDirtySrc = preferenceDirtySrc;
    this.preferenceCleanSrc = preferenceCleanSrc;
    this.apConfig = apConfig;
    this.damping = damping;
    this.cleanSourceElementIndices = cleanSourceElementIndices;
    this.random = new Random();
    initializeOutputParams();
  }

  /**
   * Constructs AbstractMscdAffinityPropagationSeq
   *
   * @param similarityMatrix NxN similarity matrix for N dataPoints. Cell ij represents the similarity
   *                         between dataPoint i and dataPoint j.
   * @param preference shared self-similarity for clean and dirty source dataPoints
   */
  public AbstractMscdAffinityPropagationSeq(double[][] similarityMatrix, double preference) {
    this.similarityMatrix = MatrixUtils.copy(similarityMatrix);
    this.preferenceDirtySrc = preference;
    this.preferenceCleanSrc = preference;
    this.apConfig = new ApConfig();
    apConfig.setAllSameSimClusteringThreshold(0);
    this.damping = 0.5;
    this.cleanSourceElementIndices = ArrayListMultimap.create();
    this.random = new Random();
    initializeOutputParams();
  }

  /**
   * Initialize the Affinity Propagation output values and working parameters
   */
  private void initializeOutputParams() {
    this.converged = false;
    this.allEqualComponent = false;
    this.iterCount = 0;
    this.m = this.similarityMatrix.length;
    this.labels = new int[m];
    this.exemplarAssignments = new int[m];
    this.exemplarIndices = new ArrayList<>();
  }

  /**
   * Reset the Affinity Propagation output values
   */
  protected void resetResults() {
    this.converged = false;
    this.allEqualComponent = false;
    this.iterCount = 0;
    Arrays.fill(labels, 0);
    Arrays.fill(exemplarAssignments, 0);
    this.exemplarIndices.clear();
  }

  /**
   * Check whether all similarity values in the {@link #similarityMatrix} are equal.
   *
   * @return true if all equal.
   */
  protected abstract boolean isSimilarityAllEqual();

  /**
   * Execute the MSCD Affinity Propagation clustering algorithm.
   */
  public abstract void fit();

  /**
   * Calculate the final result labeling.
   *
   * @param criterionMatrix matrix with the final iteration result (alpha + rho). Might be used for
   *                        exemplar assignment, if configured.
   * @param simMatrix noised similarity matrix, used in the iterative procedure. Might be used for exemplar
   *                 assignment, if configured.
   */
  protected void calculateResultLabeling(double[][] criterionMatrix, double[][] simMatrix) {
    // labeling clusters from availability and responsibility matrices
    // sklearn line: I = np.where(np.diag(A + R) > 0)[0]
    final ArrayList<Integer> indicesCriterionOverZero = new ArrayList<>();

    // Get diagonal of criterionMatrix and add to indicesCriterionOverZero if > 0
    // Could do this: MatUtils.diagFromSquare(MatUtils.add(A, R)); but takes 3M time... this takes M
    for (int i = 0; i < m; i++) {
      if (criterionMatrix[i][i] > 0) {
        indicesCriterionOverZero.add(i);
      }
    }

    // Reassign list to array, so whole thing takes 1M + K rather than 3M + K
    int[] iVector = new int[indicesCriterionOverZero.size()];
    for (int j = 0; j < iVector.length; j++) {
      iVector[j] = indicesCriterionOverZero.get(j);
    }

    if (iVector.length > 0) {
      // if exemplars were found, assign the labels for MSCD-AP by the criterion matrix and for traditional
      // AP by the configured matrix (similarity or criterion)
      if ((cleanSourceElementIndices.size() > 0) ||
        this.apConfig.getApExemplarAssignmentType().equals(ApExemplarAssignmentType.CRITERION)) {
        calculateClusteringResultLabeling(iVector, criterionMatrix);
      } else {
        calculateClusteringResultLabeling(iVector, simMatrix);
      }
    } else {
      exemplarIndices.clear(); // Empty
      for (int i = 0; i < m; i++) {
        labels[i] = -1; // Missing
      }
    }
  }

  /**
   * Calculate the final clustering result labeling.
   *
   * @param iVector indices of diagonal cells with criterion greater zero
   * @param assignmentMatrix matrix for the final cluster assignment (similarityMatrix or criterionMatrix)
   */
  private void calculateClusteringResultLabeling(int[] iVector, double[][] assignmentMatrix) {
    /* changes to original code:
    - removed the refinement of the final set of exemplars and clusters here
    - so we keep the result of the criterion matrix instead, which is necessary for MSCD-AP */

    // sklearn line: c = np.argmax(S[:, I], axis=1)
    double[][] colCube = MatrixUtils.getColumns(assignmentMatrix, iVector);
    int[] c = MatrixUtils.argMax(colCube, MatrixUtils.Axis.ROW);

    exemplarIndices.clear();
    // sklearn line: c[I] = np.arange(K)
    for (int j = 0; j < iVector.length; j++) { // I.length == K, == numClusters
      c[iVector[j]] = j;  // exemplars choose themselfes
      exemplarIndices.add(iVector[j]);
    }

    // sklearn line: labels = I[c]
    for (int j = 0; j < m; j++) {
      exemplarAssignments[j] = iVector[c[j]];
    }

    /*
     * final label assignment...
     * sklearn line: labels = np.searchsorted(cluster_centers_indices, labels)
     */
    for (int i = 0; i < labels.length; i++) {
      labels[i] = exemplarIndices.indexOf(exemplarAssignments[i]);
    }

    /* changes to original code:
     * => removed centroid assignment */
  }

  /**
   * Calculate the binary matrix and check the 1-of-N-Constraint. The binary matrix cell ij is true for row i
   * and column j, if criterion_ij = alpha_ij + rho_ij is greater than zero. Because we chose the maximum
   * value of a row i as the exemplar for the row's dataPoint, it must have at least one criterion_ij greater
   * than zero. More than one criterion_ij greater than zero is tolerated for better convergence. It does
   * not lead to an inconsistent result. For more restrictive constraints check, this method could be
   * rewritten to guarantee there is exactly one criterion_ij of each row i greater than zero.
   *
   * @param criterionMatrix matrix for the final cluster assignment (alpha + rho)
   */
  protected void calculateOneOfNConstraint(double[][] criterionMatrix) {
    this.isOneOfNConstraintSatisfied = true;

    for (int i = 0; i < m; i++) {
      int nrSelectionsOfRow = 0;
      for (int j = 0; j < m; j++) {
        if (criterionMatrix[i][j] > 0) {
          nrSelectionsOfRow++;
        }
      }
      if (nrSelectionsOfRow == 0) {   // != 1  for more restrictive constraints check
        // (also passes the unit tests)
        this.isOneOfNConstraintSatisfied = false;
      }
    }
  }

  /**
   * Check, whether the Exemplar-Consistency-Constraint is satisfied. If any other dataPoint choose
   * another one as its exemplar, the other one must choose itself as its own exemplar too.
   *
   * @return true, if the constraint is satisfied
   */
  public boolean isExemplarConsistencyConstraintSatisfied() {
    boolean result = true;

    for (int centroidIndex : exemplarIndices) {
      if (exemplarAssignments[centroidIndex] != centroidIndex) {
        // the exemplar didn't choose itself
        result = false;
        break;
      }
    }
    return result;
  }

  /**
   * Check, whether the Clean-Source-Constraint is satisfied. Two dataPoints of the same clean source are
   * not allowed to be in the same cluster.
   *
   * @return true, if the constraint is satisfied
   */
  public boolean isCleanSourcesConstraintSatisfied() {
    boolean result = true;

    for (String src : cleanSourceElementIndices.keySet()) {
      Set<Integer> thisSourceCentroids = new HashSet<>();
      for (Integer dataPointId : cleanSourceElementIndices.get(src)) {
        thisSourceCentroids.add(exemplarAssignments[dataPointId]);
      }
      if (thisSourceCentroids.size() != cleanSourceElementIndices.get(src).size()) {
        result = false;
        break;
      }
    }
    return result;
  }

  /**
   * Check, whether all three consistency constraints of the clustering are satisfied, so that the solution
   * is valid.
   *
   * @return true, when all consistency constraints are satisfied.
   */
  public boolean areConstraintsSatisfied() {
    if (allEqualComponent) {
      return true;
    }

    return isExemplarConsistencyConstraintSatisfied() && isCleanSourcesConstraintSatisfied() &&
      isOneOfNConstraintSatisfied;
  }

  /**
   * Print a matrix to the standard output for debugging purposes.
   *
   * @param name name of the matrix
   * @param matrix the matrix to print
   */
  private void printMatrix(String name, double[][] matrix) {
    System.out.print("\n" + name);
    int n = matrix.length;
    for (int i = 0; i < n; i++) {
      System.out.print("\n");
      for (int j = 0; j < n; j++) {
        System.out.print(matrix[i][j] + "\t");
      }
    }
    System.out.println("\n");
  }

  public boolean isConverged() {
    return converged;
  }

  public ArrayList<Integer> getExemplarIndices() {
    return exemplarIndices;
  }

  public int[] getLabels() {
    return VectorUtils.copy(labels);
  }

  public int getIterCount() {
    return iterCount;
  }

  public double getPreferenceDirtySrc() {
    return preferenceDirtySrc;
  }

  public void setPreferenceDirtySrc(double preferenceDirtySrc) {
    this.preferenceDirtySrc = preferenceDirtySrc;
  }

  public double getPreferenceCleanSrc() {
    return preferenceCleanSrc;
  }

  public void setPreferenceCleanSrc(double preferenceCleanSrc) {
    this.preferenceCleanSrc = preferenceCleanSrc;
  }

  public double getDamping() {
    return damping;
  }

  public void setDamping(double damping) {
    this.damping = damping;
  }

  public Random getRandom() {
    return random;
  }

  public void setRandom(Random random) {
    this.random = random;
  }

  public Multimap<String, Integer> getCleanSourceElementIndices() {
    return cleanSourceElementIndices;
  }

  public void setCleanSourceElementIndices(Multimap<String, Integer> cleanSourceElementIndices) {
    this.cleanSourceElementIndices = cleanSourceElementIndices;
  }

  public ApConfig getApConfig() {
    return apConfig;
  }

  public void setApConfig(ApConfig apConfig) {
    this.apConfig = apConfig;
  }
}
