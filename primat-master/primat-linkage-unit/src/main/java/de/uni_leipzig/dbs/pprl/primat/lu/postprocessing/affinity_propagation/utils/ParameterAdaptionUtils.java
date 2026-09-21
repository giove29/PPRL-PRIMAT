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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils;

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.exception.ConvergenceException;



/**
 * Utility class to provide functions for the handling of the parameter adaption in the (MSCD-) Affinity
 * Propagation clustering algorithm.
 */
public class ParameterAdaptionUtils {

  /**
   * New calculated value for the preference of dirty source dataPoints
   */
  private double newPreferenceDirtySrc;

  /**
   * New calculated value for the preference of clean source dataPoints
   */
  private double newPreferenceCleanSrc;

  /**
   * New calculated damping factor
   */
  private double newDampingFactor;

  /**
   * Adapt the values for the parameters preference (for clean and dirt source dataPoints) and the damping
   * factor.
   *
   * @param origPrefDirtySrc original preference value for dirty source dataPoints from the algorithm start
   * @param origPrefCleanSrc original preference value for clean source dataPoints from the algorithm start
   * @param currentPrefDirtySrc current preference value for dirty source dataPoints
   * @param currentPrefCleanSrc current preference value for clean source dataPoints
   * @param currentDamping current damping factor
   * @param numberOfAdaptions current number of adaptions, that were already made
   * @param apConfig Configuration of the (MSCD-) Affinity Propagation clustering
   *
   * @return ParameterAdaptionUtils instance with the new calculated parameter values
   *
   * @throws ConvergenceException when all possible parameter values were tried
   */
  public static ParameterAdaptionUtils getNewParameterValues(
    double origPrefDirtySrc, double origPrefCleanSrc, double currentPrefDirtySrc,
    double currentPrefCleanSrc, double currentDamping, int numberOfAdaptions, ApConfig apConfig) throws
          ConvergenceException {

    /* try all preference values with the current damping:
       first try to decrease the preference, because AP tends to find too many clusters
       try values > 0 and < 1   */
    ParameterAdaptionUtils newParams = new ParameterAdaptionUtils();
    double prefCleanDirtyDistance = origPrefCleanSrc - origPrefDirtySrc;

    /* Preference of dirtySrc and cleanSrc should always be adapted together, to keep the distance.
       We adapt the lower one first, because it switches from decrease to increase first. */
    if (prefCleanDirtyDistance >= 0) {  // this should be the normal case
      newParams.newPreferenceDirtySrc = ParameterAdaptionUtils.adaptParameterValue(
        origPrefDirtySrc, currentPrefDirtySrc,
        apConfig.getPreferenceConfig().getPreferenceAdaptionStep(),
        1.0001, -0.0001, true);

      newParams.newPreferenceCleanSrc = newParams.newPreferenceDirtySrc + prefCleanDirtyDistance;
    } else {
      newParams.newPreferenceCleanSrc = ParameterAdaptionUtils.adaptParameterValue(
        origPrefCleanSrc, currentPrefCleanSrc,
        apConfig.getPreferenceConfig().getPreferenceAdaptionStep(),
        1.0001, -0.0001, true);

      newParams.newPreferenceDirtySrc = newParams.newPreferenceCleanSrc - prefCleanDirtyDistance;
    }

    newParams.newDampingFactor = currentDamping;

    if ((newParams.newPreferenceDirtySrc <= -0.0001) || (newParams.newPreferenceCleanSrc <= -0.0001) ||
      (newParams.newPreferenceDirtySrc >= 1.0001) || (newParams.newPreferenceCleanSrc >= 1.0001)) {
      // all possible preference values tried => adapt damping
      newParams.newDampingFactor = ParameterAdaptionUtils.adaptParameterValue(
        apConfig.getDampingFactor(),
        currentDamping,
        apConfig.getDampingAdaptionStep(),
        0.96, ApConfig.DAMPING_MIN_VALUE - 0.01, false);

      // reset preference
      newParams.newPreferenceDirtySrc = origPrefDirtySrc;
      newParams.newPreferenceCleanSrc = origPrefCleanSrc;
    }

    if (newParams.newDampingFactor < ApConfig.DAMPING_MIN_VALUE) {
      throw new ConvergenceException(
        String.format("The algorithm could not converge for a connected component. It tried all " +
          "possible parameter settings. (%d tries)", numberOfAdaptions));
    }

    return newParams;
  }

  /**
   * Adapt the parameter value in a range between the {@code lowerBoundExclusive} and the
   * {@code upperBoundExclusive} by the defined {@code step}.
   *
   * @param startValue value of the parameter, that was used when AP started, before any adaption was made
   * @param currentValue the current value of the parameter, after the last adaption
   * @param step the step by which the parameter is increased or decreased
   * @param upperBoundExclusive the exclusive upper limit. The parameter value will not reach or exceed
   *                            this limit.
   * @param lowerBoundExclusive the exclusive lower limit. The parameter value will not reach or undercut
   *                            this limit.
   * @param startWithDecrease if true, first decrease the parameter value until the lowerBound is reached.
   *                          Then increase it. Otherwise first increase, then decrease.
   *
   * @return the adapted parameter value, or -2 when all values were tried
   */
  public static double adaptParameterValue(double startValue, double currentValue, double step,
    double upperBoundExclusive, double lowerBoundExclusive, boolean startWithDecrease) {

    double nextValue = -1;
    double lowerBoundCompare = lowerBoundExclusive + step;
    double upperBoundCompare = upperBoundExclusive - step;

    if (startValue - step >= upperBoundCompare) {
      startWithDecrease = true;
    }
    if (startValue + step <= lowerBoundCompare) {
      startWithDecrease = false;
    }

    if (startWithDecrease) {
      if (currentValue <= startValue && currentValue > lowerBoundCompare) {
        // #1 decrease until <= lowerBound + step
        nextValue = currentValue - step;
      } else if (currentValue <= lowerBoundCompare && startValue <= upperBoundCompare) {
        // #2 start increasing from the original value
        nextValue = startValue + step;
      } else if (currentValue > startValue && currentValue < upperBoundCompare) {
        // #3 increase until >= upperBound - step
        nextValue = currentValue + step;
      } else {
        nextValue = -2;
      }
    } else {
      if (currentValue >= startValue && currentValue < upperBoundCompare) {
        // #1 increase until >= upperBound - step
        nextValue = currentValue + step;
      } else if (currentValue >= upperBoundCompare && startValue >= lowerBoundCompare) {
        // #2 start decreasing from the original value
        nextValue = startValue - step;
      } else if (currentValue <= startValue && currentValue > lowerBoundCompare) {
        // #3 decrease until <= lowerBound + step
        nextValue = currentValue - step;
      } else {
        nextValue = -2;
      }
    }

    if (nextValue == -1) {
      throw new IllegalStateException("The parameter adaption resulted into the illegal value of -1. " +
        "This value is used as start value for the adaption and could indicate, that no adaption was done.");
    }

    return nextValue;
  }

  public double getNewPreferenceDirtySrc() {
    return newPreferenceDirtySrc;
  }

  public void setNewPreferenceDirtySrc(double newPreferenceDirtySrc) {
    this.newPreferenceDirtySrc = newPreferenceDirtySrc;
  }

  public double getNewPreferenceCleanSrc() {
    return newPreferenceCleanSrc;
  }

  public void setNewPreferenceCleanSrc(double newPreferenceCleanSrc) {
    this.newPreferenceCleanSrc = newPreferenceCleanSrc;
  }

  public double getNewDampingFactor() {
    return newDampingFactor;
  }

  public void setNewDampingFactor(double newDampingFactor) {
    this.newDampingFactor = newDampingFactor;
  }
}
