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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Data structure to store statistical information of a metric for the evaluation of multiple executions of a
 * clustering algorithm for a dataSet. Used for automated evaluation result persistence.
 */
public class EvaluationStatistics {

  /**
   * Name of the metric that was evaluated.
   */
  private String metric;

  /**
   * Average of the evaluation result that was reached.
   */
  private double average;

  /**
   * Minimum value of the evaluation result over all runs for that metric.
   */
  private double max;

  /**
   * Maximum value of the evaluation result over all runs for that metric.
   */
  private double min;

  /**
   * Standard deviation of the evaluation result.
   */
  private double standardDeviation;

  /**
   * Constructor that fills the evaluation statistics attributes by a {@link DescriptiveStatistics} object.
   * @param metric name of the metric, that was evaluated.
   * @param stats results of the evaluation.
   */
  public EvaluationStatistics(String metric, DescriptiveStatistics stats) {
    this.metric = metric;
    average = stats.getMean();
    max = stats.getMax();
    min = stats.getMin();
    standardDeviation = stats.getStandardDeviation();
  }

  public String getMetric() {
    return metric;
  }

  public void setMetric(String metric) {
    this.metric = metric;
  }

  public double getAverage() {
    return average;
  }

  public void setAverage(double average) {
    this.average = average;
  }

  public double getMax() {
    return max;
  }

  public void setMax(double max) {
    this.max = max;
  }

  public double getMin() {
    return min;
  }

  public void setMin(double min) {
    this.min = min;
  }

  public double getStandardDeviation() {
    return standardDeviation;
  }

  public void setStandardDeviation(double standardDeviation) {
    this.standardDeviation = standardDeviation;
  }
}
