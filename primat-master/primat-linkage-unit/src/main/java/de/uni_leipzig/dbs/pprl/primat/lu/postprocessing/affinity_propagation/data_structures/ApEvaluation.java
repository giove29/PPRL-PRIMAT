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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures;

/**
 * Data structure to store statistical information and evaluation results of one single (MSCD-) Affinity
 * Propagation clustering result on a dataset.
 */
public class ApEvaluation {

  /**
   * Value of the precision metric. Defines how many of the clustered data point pairs are correct.
   */
  private double precision;

  /**
   * Value of the recall metric. Defines how many of the correct data point pairs were found.
   */
  private double recall;

  /**
   * Value of the fMeasure metric. Harmonic mean of precision and recall.
   */
  private double fMeasure;

  /**
   * Number of all clustered data point pairs.
   */
  private long allPositives;

  /**
   * Number of all correct clustered data point pairs.
   */
  private long truePositives;

  /**
   * Number of all incorrect clustered data point pairs.
   */
  private long falsePositives;

  /**
   * Number of all missed data point pairs, that should have been clustered together.
   */
  private long falseNegatives;

  /**
   * Number of all vertices in the dataSet.
   */
  private long nrVertices;

  /**
   * Number of connected components in the dataSet.
   */
  private int nrGroups;

  /**
   * Number of connected components, that needed parameter adaption to find a converging solution.
   */
  private int nrGroupsWithAdaptions;

  /**
   * Number of connected components, where all similarities are equal.
   */
  private int nrGroupsAllEqual;

  /**
   * Number of iterations over all connected components, that were needed in the converging AP configuration.
   */
  private int nrTotalConvergenceIterations;

  /**
   * Number of iterations over all connected components and all parameter adaption steps.
   */
  private int nrTotalSumIterations;

  /**
   * Number of parameter adaptions over all connected components.
   */
  private int nrTotalAdaptions;

  /**
   * Maximum convergence iterations of the connected components.
   */
  private int componentMaxConvergenceIterations;

  /**
   * Maximum sum of iterations, including adaptions, of the connected components.
   */
  private int componentMaxSumIterations;

  /**
   * Maximum number of parameter adaptions of the connected components.
   */
  private int componentMaxAdaptions;

  /**
   * Minimum convergence iterations of the connected components.
   */
  private int componentMinConvergenceIterations;

  /**
   * Minimum sum of iterations, including adaptions, of the connected components.
   */
  private int componentMinSumIterations;

  /**
   * Minimum number of parameter adaptions of the connected components.
   */
  private int componentMinAdaptions;

  /**
   * Number of data source in the data set.
   */
  private int nrSources;

  /**
   * Defines, whether the (MSCD-) AP process converged for all components.
   */
  private boolean converged;

  /**
   * Average hierarchy depth of (MSCD-) HAP over all HAP components.
   */
  private double avgHierarchyDepth;

  /**
   * Maximum hierarchy depth of (MSCD-) HAP over all HAP components.
   */
  private int maxHierarchyDepth;

  /**
   * Number of vertices of components, that needed to be partitioned.
   */
  private long nrHapVertices;

  /**
   * Number of HAP vertices, that could be assigned to exemplars.
   */
  private int nrAssignedVertices;

  public double getPrecision() {
    return precision;
  }

  public void setPrecision(double precision) {
    this.precision = precision;
  }

  public double getRecall() {
    return recall;
  }

  public void setRecall(double recall) {
    this.recall = recall;
  }

  public double getFMeasure() {
    return fMeasure;
  }

  public void setFMeasure(double fMeasure) {
    this.fMeasure = fMeasure;
  }

  public long getAllPositives() {
    return allPositives;
  }

  public void setAllPositives(long allPositives) {
    this.allPositives = allPositives;
  }

  public long getTruePositives() {
    return truePositives;
  }

  public void setTruePositives(long truePositives) {
    this.truePositives = truePositives;
  }

  public long getFalsePositives() {
    return falsePositives;
  }

  public void setFalsePositives(long falsePositives) {
    this.falsePositives = falsePositives;
  }

  public long getFalseNegatives() {
    return falseNegatives;
  }

  public void setFalseNegatives(long falseNegatives) {
    this.falseNegatives = falseNegatives;
  }

  public long getNrVertices() {
    return nrVertices;
  }

  public void setNrVertices(long nrVertices) {
    this.nrVertices = nrVertices;
  }

  public int getNrGroups() {
    return nrGroups;
  }

  public void setNrGroups(int nrGroups) {
    this.nrGroups = nrGroups;
  }

  public int getNrGroupsWithAdaptions() {
    return nrGroupsWithAdaptions;
  }

  public void setNrGroupsWithAdaptions(int nrGroupsWithAdaptions) {
    this.nrGroupsWithAdaptions = nrGroupsWithAdaptions;
  }

  public int getNrGroupsAllEqual() {
    return nrGroupsAllEqual;
  }

  public void setNrGroupsAllEqual(int nrGroupsAllEqual) {
    this.nrGroupsAllEqual = nrGroupsAllEqual;
  }

  public int getNrTotalConvergenceIterations() {
    return nrTotalConvergenceIterations;
  }

  public void setNrTotalConvergenceIterations(int nrTotalConvergenceIterations) {
    this.nrTotalConvergenceIterations = nrTotalConvergenceIterations;
  }

  public int getNrTotalAdaptions() {
    return nrTotalAdaptions;
  }

  public void setNrTotalAdaptions(int nrTotalAdaptions) {
    this.nrTotalAdaptions = nrTotalAdaptions;
  }

  public int getComponentMaxConvergenceIterations() {
    return componentMaxConvergenceIterations;
  }

  public void setComponentMaxConvergenceIterations(int componentMaxConvergenceIterations) {
    this.componentMaxConvergenceIterations = componentMaxConvergenceIterations;
  }

  public int getComponentMaxAdaptions() {
    return componentMaxAdaptions;
  }

  public void setComponentMaxAdaptions(int componentMaxAdaptions) {
    this.componentMaxAdaptions = componentMaxAdaptions;
  }

  public int getComponentMinConvergenceIterations() {
    return componentMinConvergenceIterations;
  }

  public void setComponentMinConvergenceIterations(int componentMinConvergenceIterations) {
    this.componentMinConvergenceIterations = componentMinConvergenceIterations;
  }

  public int getComponentMinAdaptions() {
    return componentMinAdaptions;
  }

  public void setComponentMinAdaptions(int componentMinAdaptions) {
    this.componentMinAdaptions = componentMinAdaptions;
  }

  public int getNrSources() {
    return nrSources;
  }

  public void setNrSources(int nrSources) {
    this.nrSources = nrSources;
  }

  public boolean isConverged() {
    return converged;
  }

  public void setConverged(boolean converged) {
    this.converged = converged;
  }

  public double getAvgHierarchyDepth() {
    return avgHierarchyDepth;
  }

  public void setAvgHierarchyDepth(double avgHierarchyDepth) {
    this.avgHierarchyDepth = avgHierarchyDepth;
  }

  public int getMaxHierarchyDepth() {
    return maxHierarchyDepth;
  }

  public void setMaxHierarchyDepth(int maxHierarchyDepth) {
    this.maxHierarchyDepth = maxHierarchyDepth;
  }

  public int getNrAssignedVertices() {
    return nrAssignedVertices;
  }

  public void setNrAssignedVertices(int nrAssignedVertices) {
    this.nrAssignedVertices = nrAssignedVertices;
  }

  public long getNrHapVertices() {
    return nrHapVertices;
  }

  public void setNrHapVertices(long nrHapVertices) {
    this.nrHapVertices = nrHapVertices;
  }

  public int getNrTotalSumIterations() {
    return nrTotalSumIterations;
  }

  public void setNrTotalSumIterations(int nrTotalSumIterations) {
    this.nrTotalSumIterations = nrTotalSumIterations;
  }

  public int getComponentMaxSumIterations() {
    return componentMaxSumIterations;
  }

  public void setComponentMaxSumIterations(int componentMaxSumIterations) {
    this.componentMaxSumIterations = componentMaxSumIterations;
  }

  public int getComponentMinSumIterations() {
    return componentMinSumIterations;
  }

  public void setComponentMinSumIterations(int componentMinSumIterations) {
    this.componentMinSumIterations = componentMinSumIterations;
  }
}
