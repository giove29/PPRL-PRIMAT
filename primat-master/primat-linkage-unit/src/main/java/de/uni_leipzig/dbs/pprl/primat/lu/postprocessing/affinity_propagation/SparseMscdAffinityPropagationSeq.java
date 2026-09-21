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

/*
 * NOTICE: THIS FILE HAS BEEN MODIFIED BY Leipzig University (Database Research Group) UNDER COMPLIANCE WITH
 * THE APACHE 2.0 LICENCE FROM THE ORIGINAL WORK OF Taylor G Smith. THE FOLLOWING IS THE COPYRIGHT OF THE
 * ORIGINAL DOCUMENT:
 *
 *
 * Copyright 2015, 2016 Taylor G Smith
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils.MatrixUtils;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils.NoiseUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * Sequential implementation of the Sparse Multi-Source Clean-Dirty Affinity Propagation clustering algorithm.
 * Based on Affinity Propagation by Frey and Dueck, this extension provides the possibility to cluster
 * dataPoints of multiple duplicate containing (dirty) and duplicate free (clean) sources, while consistently
 * separating all dataPoints of clean sources into different clusters. The sparse version of the algorithm
 * computes only necessary message values.
 * <p>
 * This implementation is based on the Affinity Propagation implementation in the clust4j project by Taylor G
 * Smith (<a href="https://github.com/tgsmith61591/clust4j">clust4j github project</a>).
 * <p>
 * <b>original paper of AP:</b> FREY, Brendan J.; DUECK, Delbert. Clustering by passing messages between
 * data points. science, 2007, 315. Jg., Nr. 5814, S. 972-976.
 */
public class SparseMscdAffinityPropagationSeq extends AbstractMscdAffinityPropagationSeq {
  /*
   * This class is based on the AffinityPropagation class of the clust4j project by Taylor G Smith.
   * The first license is for the modifications in this project. The second license in this file is the
   * original one. The original source code can be found on:
   *
   *    https://github.com/tgsmith61591/clust4j
   *
   * Compared to the original class, the following modifications were made:
   * - the major workflow and the convergence check was taken from clust4j
   * - most of the other code was changed and adapted to the MSCD extension of Affinity Propagation
   * - additional features were added: constraints check, handling of all-equal-components
   * - instead of dense matrices, this implementation uses sparse tables and calculates only the necessary
   *   message values
   */

  /**
   * Sparse table representation of the similarity matrix. It contains only the cells ij, if a similarity
   * value between dataPoint i and j is available.
   */
  private Table<Integer, Integer, Double> sTable;

  /**
   * Constructs SparseMscdAffinityPropagationSeq
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
   * @param nullValue value used in the sparse similarity matrix to mark not existing similarities
   */
  public SparseMscdAffinityPropagationSeq(double[][] similarityMatrix, double preferenceDirtySrc,
    double preferenceCleanSrc, double damping, ApConfig apConfig,
    Multimap<String, Integer> cleanSourceElementIndices, double nullValue) {
    super(similarityMatrix, preferenceDirtySrc, preferenceCleanSrc, damping, apConfig,
      cleanSourceElementIndices);
    initializeSTable(similarityMatrix, nullValue);
  }

  /**
   * Constructs SparseMscdAffinityPropagationSeq
   *
   * @param similarityMatrix NxN similarity matrix for N dataPoints. Cell ij represents the similarity
   *                         between dataPoint i and dataPoint j.
   * @param preference shared self-similarity for clean and dirty source dataPoints
   * @param nullValue value used in the sparse similarity matrix to mark not existing similarities
   */
  public SparseMscdAffinityPropagationSeq(double[][] similarityMatrix, double preference, double nullValue) {
    super(similarityMatrix, preference);
    initializeSTable(similarityMatrix, nullValue);
  }

  /**
   * Create a sparse similarity table out of the similarity matrix.
   *
   * @param s similarity matrix
   * @param nullValue defines not existing edges
   */
  private void initializeSTable(double[][] s, double nullValue) {
    this.sTable = HashBasedTable.create();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < m; j++) {
        if (s[i][j] != nullValue) {
          this.sTable.put(i, j, s[i][j]);
        }
      }
    }
  }

  /**
   * Check whether all similarity values in {@link #sTable} are equal.
   *
   * @return true if all equal.
   */
  protected boolean isSimilarityAllEqual() {
    boolean allEqual = true;
    Iterator<Double> sValueIterator = this.sTable.values().iterator();
    double firstValue = sValueIterator.next();

    while (sValueIterator.hasNext()) {
      double currentValue = sValueIterator.next();
      if (Math.abs(firstValue - currentValue) > 0.0001) {
        allEqual = false;
        break;
      }
    }

    return  allEqual;
  }

  /**
   * Execute the clustering for the case, when all similarity values are equal. If the
   * {@link ApConfig#getAllSameSimClusteringThreshold} is reached, put all dirty source dataPoints in one
   * cluster and separate all clean source dataPoints, but create as few clusters as possible. When the
   * threshold is not reached, each dataPoint becomes a singleton.
   */
  private void clusterAllEqualComponent() {
    // algorithm converged immediately due to all elements being equal in input matrix
    this.converged = true;
    this.allEqualComponent = true;
    this.exemplarIndices.clear();

    double firstValue = sTable.values().iterator().next();
    if (firstValue < this.apConfig.getAllSameSimClusteringThreshold()) {
      // put each element in its own cluster
      for (int i = 0; i < m; i++) {
        this.labels[i] = i;
        this.exemplarIndices.add(i);
      }
    } else {
      if (this.cleanSourceElementIndices.keySet().size() == 0) {
        // Else: all are in the same cluster
        // take just the first element as exemplar
        this.exemplarIndices.add(0);
      } else {
          /* MSCD:
           one entity of each clean source can be together with one entity of each other clean source
           - all dirtySource entities are assigned to cluster 0
           - each cleanSource source entity is assigned to a separated cluster 0,1,2,...
           - centroid of a cluster becomes the entity that is first assigned to it */
        this.exemplarIndices.add(0);

        for (String source : this.cleanSourceElementIndices.keySet()) {
          List<Integer> sourdeDataPoints = new ArrayList<>(this.cleanSourceElementIndices.get(source));
          Collections.sort(sourdeDataPoints);   // list conversion and sorting not necessary.
          // just to get reproducible results.
          int currentClusterId = 0;
          for (int dataPointId : sourdeDataPoints) {
            this.labels[dataPointId] = currentClusterId;
            // set centroid if first of cluster
            if (currentClusterId >= this.exemplarIndices.size()) {
              this.exemplarIndices.add(dataPointId);
            }
            currentClusterId++;
          }
        }
      }
    }
  }

  /**
   * Execute the MSCD Affinity Propagation clustering algorithm.
   */
  public void fit() {

    resetResults();

    // check if the dataPoints form an all-equal-component
    if (this.isSimilarityAllEqual()) {
      clusterAllEqualComponent();
      return;
    }

    // set the dirty source preference
    for (int i = 0; i < m; i++) {
      this.sTable.put(i, i, this.preferenceDirtySrc);
    }
    // set clean source preference
    for (String src : this.cleanSourceElementIndices.keySet()) {
      for (int i : this.cleanSourceElementIndices.get(src)) {
        this.sTable.put(i, i, this.preferenceCleanSrc);
      }
    }

    // add noise
    final double noiseDecimalShift = NoiseUtils.getNoiseDecimalShift(this.apConfig.getNoiseDecimalPlace());
    if (this.apConfig.getNoiseDecimalPlace() > 0) {
      addNoise(this.sTable, this.random, noiseDecimalShift);
    }

    // initialize the message-matrices, which are not temporary inside of an iteration
    Table<Integer, Integer, Double> aTable = HashBasedTable.create();
    Table<Integer, Integer, Double> rTable = HashBasedTable.create();
    Table<Integer, Integer, Double> thetaTable = HashBasedTable.create();
    for (int i : this.sTable.rowKeySet()) {
      for (int j: this.sTable.columnKeySet()) {
        if (this.sTable.contains(i, j)) {
          aTable.put(i, j, 0d);
          rTable.put(i, j, 0d);
          thetaTable.put(i, j, 0d);
        }
      }
    }

    double[][] e = new double[m][this.apConfig.getConvergenceIter()];
    double[] sumE;

    for (this.iterCount = 0; this.iterCount < this.apConfig.getMaxAdaptionIteration(); this.iterCount++) {

      Table<Integer, Integer, Double> etaTable = initializeTable();
      // calculate the first piece of AP - compute ETA
      affinityPiece1getEta(aTable, this.sTable, thetaTable, etaTable);

      thetaTable = initializeTable();
      // calculate the second piece of AP - compute THETA (containing the main MSCD extension)
      affinityPiece2getTheta(aTable, this.sTable, thetaTable, etaTable, this.cleanSourceElementIndices);

      // calculate the third piece of AP - compute R and A
      affinityPiece3getA(aTable, this.sTable, thetaTable, etaTable, rTable, this.damping);

      // Set the mask in `e`
      final double[] mask = new double[m];
      int idx = this.iterCount % this.apConfig.getConvergenceIter();
      for (int i : this.sTable.rowKeySet()) {
        mask[i] = aTable.get(i, i) + rTable.get(i, i) > 0.0 ? 1.0 : 0.0;
        e[i][idx] = mask[i];
      }

      // get the number of clusters
      int numClusters = 0;
      for (double d : mask) {
        numClusters += (int) d;
      }

      if (this.iterCount >= this.apConfig.getConvergenceIter()) { // Time to check convergence criteria...
        // calculate the rowSums of e
        sumE = new double[m];
        for (int i = 0; i < m; i++) {
          double rowSum = 0.0;
          for (double d : e[i]) {
            rowSum += d;
          }
          sumE[i] = rowSum;
        }

        // masking
        int maskCt = 0;
        for (int i = 0; i < sumE.length; i++) {
          maskCt += sumE[i] == 0 || sumE[i] == this.apConfig.getConvergenceIter() ? 1 : 0;
        }

        this.converged = maskCt == m;

        if (this.converged || (this.iterCount == this.apConfig.getMaxAdaptionIteration())) {
          if (numClusters < 1) {
            this.converged = false;
          }
          break;
        }
      }
    } // end iteration

    double[][] criterionMatrix = MatrixUtils.add(tableToMatrix(aTable), tableToMatrix(rTable));

    calculateResultLabeling(criterionMatrix, tableToMatrix(this.sTable));
    calculateOneOfNConstraint(criterionMatrix);
  }

  /**
   * Create a new table, using the dimensions and sparsity of the {@link #sTable}. All non-empty fields
   * are initialized with a value of 0.
   *
   * @return new table
   */
  private Table<Integer, Integer, Double> initializeTable() {
    Table<Integer, Integer, Double> newTable = HashBasedTable.create();
    for (int i : this.sTable.rowKeySet()) {
      for (int j: this.sTable.columnKeySet()) {
        if (this.sTable.contains(i, j)) {
          newTable.put(i, j, 0d);
        }
      }
    }
    return newTable;
  }

  /**
   * Computes the first portion of the AffinityPropagation iteration sequence. Calculate the
   * message values for the BETA and ETA matrices. Separating this piece from the {@link #fit()} method
   * itself allows easier testing.
   *
   * @param aTable Sparse matrix of the ALPHA messages
   * @param sTable Sparse similarity matrix
   * @param thetaTable Sparse matrix of the THETA messages
   * @param etaTable Sparse matrix of the ETA messages
   */
  public static void affinityPiece1getEta(Table<Integer, Integer, Double> aTable,
    Table<Integer, Integer, Double> sTable,
    Table<Integer, Integer, Double> thetaTable,
    Table<Integer, Integer, Double> etaTable) {

    for (int i : sTable.rowKeySet()) {
      // Compute row maxes
      double runningMax = Double.NEGATIVE_INFINITY;
      double secondMax  = Double.NEGATIVE_INFINITY;
      int runningMaxIdx = 0; // Idx of max row element -- start at 0 in case metric produces -Infs

      for (int j : sTable.row(i).keySet()) {  // all columnKeys of that sparse row
        double betaIJ = aTable.get(i, j) + sTable.get(i, j) + thetaTable.get(i, j);

        if (betaIJ > runningMax) {
          secondMax = runningMax;
          runningMax = betaIJ;
          runningMaxIdx = j;
        } else if (betaIJ > secondMax) {
          secondMax = betaIJ;
        }
      }

      for (int j : sTable.row(i).keySet()) {  // all columnKeys of that sparse row
        etaTable.put(i, j, runningMax * -1);
      }
      etaTable.put(i, runningMaxIdx, secondMax * -1);
    }
  }

  /**
   * Computes the second portion of the AffinityPropagation iteration sequence. Calculate the
   * message values for the GAMMA and THETA matrices.
   *
   * @param aTable Sparse matrix of the ALPHA messages
   * @param sTable Sparse similarity matrix
   * @param thetaTable Sparse matrix of the THETA messages
   * @param etaTable Sparse matrix of the ETA messages
   * @param cleanSourceElementIndices Maps each clean source (by its name) to a list of integer IDs of its
   *                                 elements in the similarity-matrix.
   */
  public static void affinityPiece2getTheta(
    Table<Integer, Integer, Double> aTable,
    Table<Integer, Integer, Double> sTable,
    Table<Integer, Integer, Double> thetaTable,
    Table<Integer, Integer, Double> etaTable,
    Multimap<String, Integer> cleanSourceElementIndices) {

    for (String src : cleanSourceElementIndices.keySet()) {
      Collection<Integer> srcPoints = cleanSourceElementIndices.get(src);

      if (srcPoints.size() > 1) {                         // - there must be at least 2 points to prevent them
                                                         //   from being in the same cluster
        for (int j : sTable.columnKeySet()) {
          // Compute col maxes
          double runningMax = Double.NEGATIVE_INFINITY;
          double secondMax  = Double.NEGATIVE_INFINITY;
          int runningMaxIdx = 0;

          // get all i from this column
          List<Integer> thisColumnsSrcPoints = new ArrayList<>();
          for (Integer i : sTable.column(j).keySet()) {
            if (srcPoints.contains(i)) {
              thisColumnsSrcPoints.add(i);
            }
          }

          if (thisColumnsSrcPoints.size() > 1) {
            for (Integer i : thisColumnsSrcPoints) {                   // - i are only rows from the
              // current clean source
              // GAMMA = A + S + ETA
              double gammaIJ = aTable.get(i, j) + sTable.get(i, j) + etaTable.get(i, j);

              if (gammaIJ > runningMax) {
                secondMax = runningMax;
                runningMax = gammaIJ;
                runningMaxIdx = i;
              } else if (gammaIJ > secondMax) {
                secondMax = gammaIJ;
              }
            }

            // Theta = -1 * maxValue of all other rows of that column, excluding self
            // Theta = 0 if > 0
            for (Integer i : thisColumnsSrcPoints) {
              if (i == runningMaxIdx) {
                thetaTable.put(i, j, secondMax * -1);
              } else {
                thetaTable.put(i, j, runningMax * -1);
              }
              if (thetaTable.get(i, j) > 0) {
                thetaTable.put(i, j, 0d);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Computes the third portion of the AffinityPropagation iteration sequence. Calculate the
   * message values for the RHO and ALPHA matrices.
   *
   * @param aTable Sparse matrix of the ALPHA messages
   * @param sTable Sparse similarity matrix
   * @param thetaTable Sparse matrix of the THETA messages
   * @param etaTable Sparse matrix of the ETA messages
   * @param rTable Sparse matrix of the RHO messages
   * @param damping damping factor, to damp RHO and ALPHA by a portion of their value from the last iteration
   */
  public static void affinityPiece3getA(
    Table<Integer, Integer, Double> aTable,
    Table<Integer, Integer, Double> sTable,
    Table<Integer, Integer, Double> thetaTable,
    Table<Integer, Integer, Double> etaTable,
    Table<Integer, Integer, Double> rTable,
    double damping) {

    final double omd = 1.0 - damping;

    for (int j : sTable.columnKeySet()) {

      double columnSum = 0;

      for (int i : sTable.column(j).keySet()) {     // only the row keys of this column
        // R = S + ETA + THETA
        double newValueForR = sTable.get(i, j) + etaTable.get(i, j) + thetaTable.get(i, j);
        double oldValueDamped = rTable.get(i, j) * damping;
        rTable.put(i, j, oldValueDamped + (newValueForR * omd));

        if (i != j) {                  // don't use the diagonal value for the columnSums
          if (rTable.get(i, j) > 0) {           // only positive values for the columnSums
            columnSum += rTable.get(i, j);
          }
        }
      }

      // A [diagonal] = sum of positive values of the column of R, excluding diagonalValue
      // A [others] = diagonalValue + ( sum of positive values of the column of R, excluding diagonalValue
      //                                and self)   => values > 0  => 0
      for (int i : sTable.column(j).keySet()) {    // only the row keys of this column
        double newValueForA;
        if (i == j) {                  // on the diagonal: take the columnSum
          newValueForA = columnSum;
        } else {                      // rest: remove own value from columnSum and add diagonalValue
          newValueForA = columnSum - Math.max(0, rTable.get(i, j)) + rTable.get(j, j);
          newValueForA = Math.min(0, newValueForA);
        }
        double oldValueDamped = aTable.get(i, j) * damping;
        aTable.put(i, j, oldValueDamped + (newValueForA * omd));
      }
    }
  }

  /**
   * Convert a table to a matrix, for debugging purposes.
   *
   * @param table the table to convert
   * @return matrix representation of the table
   */
  private double[][] tableToMatrix(Table<Integer, Integer, Double> table) {

    double[][] matrix = new double[m][m];

    for (int i : table.rowKeySet()) {
      for (int j : table.row(i).keySet()) {
        matrix[i][j] = table.get(i, j);
      }
    }
    return matrix;
  }

  /**
   * Add random gaussian noise to the matrix.
   *
   * @param table the table to be noised
   * @param random random number generator
   * @param noiseDecimalShift decimal shift that is multiplied to a random noise value, to add the main
   *                          part of the noise to a certain position behind the comma
   */
  private void addNoise(Table<Integer, Integer, Double> table, Random random, double noiseDecimalShift) {
    double oldVal;
    double newVal;

    for (int i : table.rowKeySet()) {
      for (int j : table.row(i).keySet()) {
        oldVal = table.get(i, j);
        newVal = oldVal + (noiseDecimalShift * random.nextGaussian());
        table.put(i, j, newVal);
      }
    }
  }
}
