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

import java.util.ArrayList;
import java.util.List;

/**
 * Data structure to store statistical information and evaluation results of multiple runs of (MSCD-)
 * Affinity Propagation clustering on a dataset.
 */
public class ApEvaluationStatistics {

  /**
   * Stores statistics of several metrics over multiple runs of the clustering algorithm.
   */
  private List<EvaluationStatistics> evaluationStatistics = new ArrayList<>();

  /**
   * Defines, how many percent of the runs of AP converged.
   */
  private double convergedPercentage;

  /**
   * Number of source in the evaluation dataSet.
   */
  private int nrSources;

  /**
   * Number of vertices in the evaluation dataSet.
   */
  private long nrVertices;

  /**
   * Number of vertices in connected components, that needed to be partitioned.
   */
  private long nrHapVertices;

  /**
   * Number of connected components.
   */
  private int nrGroups;

  /**
   * Number of connected components, where all similarities are equal.
   */
  private int nrGroupsAllEqual;

  /**
   * Configuration of the (MSCD-) Affinity Propagation clustering algorithm.
   */
  private ApConfig apConfig;

  /**
   * Add a new evaluation metric statistic to {@link #evaluationStatistics}.
   *
   * @param stats statistics for a metric over multiple runs of the clustering algorithm.
   */
  public void addEvaluationStatistics(EvaluationStatistics stats) {
    evaluationStatistics.add(stats);
  }

  public List<EvaluationStatistics> getEvaluationStatistics() {
    return evaluationStatistics;
  }

  public void setEvaluationStatistics(List<EvaluationStatistics> evaluationStatistics) {
    this.evaluationStatistics = evaluationStatistics;
  }

  public double getConvergedPercentage() {
    return convergedPercentage;
  }

  public void setConvergedPercentage(double convergedPercentage) {
    this.convergedPercentage = convergedPercentage;
  }

  public int getNrSources() {
    return nrSources;
  }

  public void setNrSources(int nrSources) {
    this.nrSources = nrSources;
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

  public int getNrGroupsAllEqual() {
    return nrGroupsAllEqual;
  }

  public void setNrGroupsAllEqual(int nrGroupsAllEqual) {
    this.nrGroupsAllEqual = nrGroupsAllEqual;
  }

  public long getNrHapVertices() {
    return nrHapVertices;
  }

  public void setNrHapVertices(long nrHapVertices) {
    this.nrHapVertices = nrHapVertices;
  }

  public ApConfig getApConfig() {
    return apConfig;
  }

  public void setApConfig(ApConfig apConfig) {
    this.apConfig = apConfig;
  }
}
