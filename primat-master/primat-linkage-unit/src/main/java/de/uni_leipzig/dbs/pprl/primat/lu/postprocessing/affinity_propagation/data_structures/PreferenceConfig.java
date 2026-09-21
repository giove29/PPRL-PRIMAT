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

import java.io.Serializable;

/**
 * Special configuration class for the preference parameter of AP.
 * The preference is the most important parameter. There are many different parameter tuning possibilities.
 * That's why there is a special configuration class for this parameter. The parameters are checked in the
 * following order:
 * <ol>
 *   <li>use min similarity</li>
 *   <li>fix value</li>
 *   <li>percentile</li>
 * </ol>
 * Different parameters can be used for clean and dirty source data points.
 */
public class PreferenceConfig implements Serializable {

  /**
   * If true, dirty source Data points of use the minimum of all similarities of their connected component
   * as their preference.
   */
  private boolean preferenceUseMinSimilarityDirtySrc;

  /**
   * If true, clean source Data points use the minimum of all similarities of their connected component as
   * their preference.
   */
  private boolean preferenceUseMinSimilarityCleanSrc;

  /**
   * If > 0, dirty source data points use this fix value as their preference.
   */
  private double preferenceFixValueDirtySrc;

  /**
   * If > 0, clean source data points use this fix value as their preference.
   */
  private double preferenceFixValueCleanSrc;

  /**
   * If > 0 and =< 100, dirty source data points use the percentile of the similarities of their connected
   * component for their preference. A 25-percentile for example is a number, where 25% of the similarities
   * are lower and 75% are larger.
   */
  private int preferencePercentileDirtySrc;

  /**
   * If > 0 and =< 100, clean source data points use the percentile of the similarities of their connected
   * component for their preference. A 25-percentile for example is a number, where 25% of the similarities
   * are lower and 75% are larger.
   */
  private int preferencePercentileCleanSrc;

  /**
   * When the preference of a connected component must be adapted, because AP could not converge for a
   * solution, it gets lowered or raised by this step.
   */
  private double preferenceAdaptionStep;

  /**
   * Creates an instance of PreferenceConfig with default values.
   * For dirty source nodes: use the minimum of the similarities.
   * For clean source nodes: use the 30-percentile.
   * This results in a small number of clusters with clean source nodes being preferred to form exemplars.
   */
  public PreferenceConfig() {
    this.preferenceUseMinSimilarityDirtySrc = true;
    this.preferenceUseMinSimilarityCleanSrc = false;
    this.preferenceFixValueDirtySrc = -1d;
    this.preferenceFixValueCleanSrc = -1d;
    this.preferencePercentileDirtySrc = -1;
    this.preferencePercentileCleanSrc = 30;
    this.preferenceAdaptionStep = 0.05;
  }

  /**
   * Creates an instance of PreferenceConfig with handed values.
   *
   * @param preferenceUseMinSimilarityDirtySrc If true, dirty source Data points of use the minimum of all
   *                                           similarities of their connected component as their preference.
   * @param preferenceUseMinSimilarityCleanSrc If true, clean source Data points use the minimum of all
   *                                           similarities of their connected component as their preference.
   * @param preferenceFixValueDirtySrc If > 0, dirty source data points use this fix value as their
   *                                   preference.
   * @param preferenceFixValueCleanSrc If > 0, clean source data points use this fix value as their
   *                                   preference.
   * @param preferencePercentileDirtySrc If > 0 and =< 100, dirty source data points use the percentile of
   *                                     the similarities of their connected component for their preference.
   *                                     A 25-percentile for example is a number, where 25% of the
   *                                     similarities are lower and 75% are larger.
   * @param preferencePercentileCleanSrc If > 0 and =< 100, clean source data points use the percentile of
   *                                     the similarities of their connected component for their preference
   *                                     A 25-percentile for example is a number, where 25% of the
   *                                     similarities are lower and 75% are larger.
   * @param preferenceAdaptionStep When the preference of a connected component must be adapted, because AP
   *                              could not converge for a solution, it gets lowered or raised by this step.
   */
  public PreferenceConfig(boolean preferenceUseMinSimilarityDirtySrc,
    boolean preferenceUseMinSimilarityCleanSrc, double preferenceFixValueDirtySrc,
    double preferenceFixValueCleanSrc, int preferencePercentileDirtySrc, int preferencePercentileCleanSrc,
    double preferenceAdaptionStep) {
    this.preferenceUseMinSimilarityDirtySrc = preferenceUseMinSimilarityDirtySrc;
    this.preferenceUseMinSimilarityCleanSrc = preferenceUseMinSimilarityCleanSrc;
    this.preferenceFixValueDirtySrc = preferenceFixValueDirtySrc;
    this.preferenceFixValueCleanSrc = preferenceFixValueCleanSrc;
    this.preferencePercentileDirtySrc = preferencePercentileDirtySrc;
    this.preferencePercentileCleanSrc = preferencePercentileCleanSrc;
    this.preferenceAdaptionStep = preferenceAdaptionStep;
  }

  /**
   * Checks if all parameter values are in an allowed value range.
   * @throws IllegalArgumentException if a parameter value is outside of the allowed range.
   */
  public void checkConfigCorrectness() throws IllegalArgumentException {

    if (preferencePercentileDirtySrc != -1 && (preferencePercentileDirtySrc <= 0 ||
      preferencePercentileDirtySrc > 100)) {
      throw new IllegalArgumentException("preferencePercentile must be either -1 to disable percentile " +
        "usage, or in the interval (0,100]");
    }

    if (preferencePercentileCleanSrc != -1 &&
      (preferencePercentileCleanSrc <= 0 || preferencePercentileCleanSrc > 100)) {
      throw new IllegalArgumentException("preferencePercentileCleanSrc must be either -1 to disable " +
        "percentile usage, or in the interval (0,100]");
    }

    if (preferenceFixValueDirtySrc > 1) {
      throw new IllegalArgumentException("preferenceFixValue must be a value between 0 and 1 to be used. " +
        "A value < 0 is also correct, but disables the usage of the fix value parameter.");
    }

    if (preferenceFixValueCleanSrc > 1) {
      throw new IllegalArgumentException("preferenceFixValueCleanSrc must be a value between 0 and 1 to be " +
        "used. A value < 0 is also correct, but disables the usage of the fix value parameter.");
    }

    if (preferenceAdaptionStep < 0 || preferenceAdaptionStep >= 1) {
      throw new IllegalArgumentException("preferenceAdaptionStep must be a value between 0 and 1");
    }

    if (preferenceUseMinSimilarityDirtySrc && preferenceFixValueDirtySrc >= 0) {
      throw new IllegalArgumentException("preferenceUseMinSimilarityDirtySrc can't be true, when a " +
        "preferenceFixValueDirtySrc is set");
    }

    if (preferenceUseMinSimilarityDirtySrc && preferencePercentileDirtySrc > 0) {
      throw new IllegalArgumentException("preferenceUseMinSimilarityDirtySrc can't be true, when a " +
        "preferencePercentileDirtySrc is set");
    }

    if (preferenceFixValueDirtySrc >= 0 && preferencePercentileDirtySrc > 0) {
      throw new IllegalArgumentException("preferenceFixValueDirtySrc and preferencePercentileDirtySrc can't" +
        " be set simultaneously");
    }

    if (preferenceUseMinSimilarityCleanSrc && preferenceFixValueCleanSrc >= 0) {
      throw new IllegalArgumentException("preferenceUseMinSimilarityCleanSrc can't be true, when a " +
        "preferenceFixValueCleanSrc is set");
    }

    if (preferenceUseMinSimilarityCleanSrc && preferencePercentileCleanSrc > 0) {
      throw new IllegalArgumentException("preferenceUseMinSimilarityCleanSrc can't be true, when a " +
        "preferencePercentileCleanSrc is set");
    }

    if (preferenceFixValueCleanSrc >= 0 && preferencePercentileCleanSrc > 0) {
      throw new IllegalArgumentException("preferenceFixValueCleanSrc and preferencePercentileCleanSrc can't" +
        " be set simultaneously");
    }
  }

  /**
   * Return info, whether the configuration only requires fixed preference values. So no computations of
   * percentiles or min values are necessary.
   *
   * @return true, if only fixed values needed. False otherwise.
   */
  public boolean useOnlyFixedPreferenceValues() {

    if (preferenceUseMinSimilarityDirtySrc || preferenceUseMinSimilarityCleanSrc) {
      return false;
    }

    return preferenceFixValueDirtySrc >= 0 && preferenceFixValueCleanSrc >= 0;
  }

  public boolean isPreferenceUseMinSimilarityDirtySrc() {
    return preferenceUseMinSimilarityDirtySrc;
  }

  /**
   * Set the preference as similarities minimum and reset the other settings for dirty source data points.
   *
   * @param preferenceUseMinSimilarityDirtySrc If true, dirty source Data points of use the minimum of all
   *                                           similarities of their connected component as their preference.
   */
  public void setPreferenceUseMinSimilarityDirtySrc(boolean preferenceUseMinSimilarityDirtySrc) {
    this.preferenceUseMinSimilarityDirtySrc = preferenceUseMinSimilarityDirtySrc;
    this.preferencePercentileDirtySrc = -1;
    this.preferenceFixValueDirtySrc = -1d;
  }

  public boolean isPreferenceUseMinSimilarityCleanSrc() {
    return preferenceUseMinSimilarityCleanSrc;
  }

  /**
   * Set the preference as similarities minimum and reset the other settings for clean source data points.
   *
   * @param preferenceUseMinSimilarityCleanSrc If true, clean source Data points of use the minimum of all
   *                                           similarities of their connected component as their preference.
   */
  public void setPreferenceUseMinSimilarityCleanSrc(boolean preferenceUseMinSimilarityCleanSrc) {
    this.preferenceUseMinSimilarityCleanSrc = preferenceUseMinSimilarityCleanSrc;
    this.preferencePercentileCleanSrc = -1;
    this.preferenceFixValueCleanSrc = -1d;
  }

  public int getPreferencePercentileDirtySrc() {
    return preferencePercentileDirtySrc;
  }

  /**
   * Set the preference percentile and reset the other settings for dirty source data points.
   *
   * @param preferencePercentileDirtySrc dirty source data points use the percentile of the similarities of
   *                                    their connected component for their preference
   */
  public void setPreferencePercentileDirtySrc(int preferencePercentileDirtySrc) {
    this.preferencePercentileDirtySrc = preferencePercentileDirtySrc;
    this.preferenceFixValueDirtySrc = -1d;
    this.preferenceUseMinSimilarityDirtySrc = false;
  }

  public int getPreferencePercentileCleanSrc() {
    return preferencePercentileCleanSrc;
  }

  /**
   * Set the preference percentile and reset the other settings for clean source data points.
   *
   * @param preferencePercentileCleanSrc clean source data points use the percentile of the similarities of
   *                                    their connected component for their preference
   */
  public void setPreferencePercentileCleanSrc(int preferencePercentileCleanSrc) {
    this.preferencePercentileCleanSrc = preferencePercentileCleanSrc;
    this.preferenceFixValueCleanSrc = -1d;
    this.preferenceUseMinSimilarityCleanSrc = false;
  }

  public double getPreferenceFixValueDirtySrc() {
    return preferenceFixValueDirtySrc;
  }

  /**
   * Set the preference fix value and reset the other settings for dirty source data points.
   *
   * @param preferenceFixValueDirtySrc dirty source data points use this fix value as their preference.
   */
  public void setPreferenceFixValueDirtySrc(double preferenceFixValueDirtySrc) {
    this.preferenceFixValueDirtySrc = preferenceFixValueDirtySrc;
    this.preferencePercentileDirtySrc = -1;
    this.preferenceUseMinSimilarityDirtySrc = false;
  }

  public double getPreferenceFixValueCleanSrc() {
    return preferenceFixValueCleanSrc;
  }

  /**
   * Set the preference fix value and reset the other settings for dirty source data points.
   *
   * @param preferenceFixValueCleanSrc clean source data points use this fix value as their preference.
   */
  public void setPreferenceFixValueCleanSrc(double preferenceFixValueCleanSrc) {
    this.preferenceFixValueCleanSrc = preferenceFixValueCleanSrc;
    this.preferencePercentileCleanSrc = -1;
    this.preferenceUseMinSimilarityCleanSrc = false;
  }

  public double getPreferenceAdaptionStep() {
    return preferenceAdaptionStep;
  }

  public void setPreferenceAdaptionStep(double preferenceAdaptionStep) {
    this.preferenceAdaptionStep = preferenceAdaptionStep;
  }
}
