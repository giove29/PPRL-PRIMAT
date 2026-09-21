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

import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.PreferenceConfig;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;


import java.util.Arrays;

/**
 * Utility class that provides functions for the handling of the preference-parameter of the (MSCD-)
 * Affinity Propagation algorithm.
 */
public class PreferenceUtils {

  /**
   * Retrieve the preference value for clean or dirty source data points, dependant on the preference
   * config and, if configured, on the similarities. The preference can be a simple fix value, the minimum
   * of the similarities or a defined percentile of the similarities.
   *
   * @param preferenceConfig Configuration of the preference parameter for (MSCD-) Affinity Propagation.
   * @param similarities array of similarity values to retrieve a dependant preference value from.
   * @param dirtySrc <b>true:</b> get dirty source preference.
   *                 <b>false:</b> get clean source preference.
   * @return by the preferenceConfig and the similarities desired preference value
   */
  public static double getPreferenceValue(PreferenceConfig preferenceConfig, double[] similarities,
                                          boolean dirtySrc) {

    double preferenceValue;

    if (dirtySrc) {
      if (preferenceConfig.isPreferenceUseMinSimilarityDirtySrc()) {
        // use minimal value of the similarities for the preference
        preferenceValue = Arrays.stream(similarities).min().orElse(0d);
      } else {
        if (preferenceConfig.getPreferenceFixValueDirtySrc() > 0) {
          preferenceValue = preferenceConfig.getPreferenceFixValueDirtySrc();
        } else {
          // get the preference value as percentil (e.g. median) of the similarity values
          preferenceValue = new Percentile().evaluate(similarities,
            preferenceConfig.getPreferencePercentileDirtySrc());
        }
      }
    } else {
      if (preferenceConfig.isPreferenceUseMinSimilarityCleanSrc()) {
        preferenceValue = Arrays.stream(similarities).min().orElse(0d);
      } else {
        if (preferenceConfig.getPreferenceFixValueCleanSrc() > 0) {
          preferenceValue = preferenceConfig.getPreferenceFixValueCleanSrc();
        } else {
          preferenceValue = new Percentile().evaluate(similarities,
            preferenceConfig.getPreferencePercentileCleanSrc());
        }
      }
    }

    return preferenceValue;
  }

  /**
   * Retrieve the preference value for precalculated percentile values. Distinguish clean or dirty source
   * data points. The preference can be a simple fix value, the precalculated minimum of the similarities
   * or a precalculated percentile of the similarities.
   *
   * @param preferenceConfig Configuration of the preference parameter for (MSCD-) Affinity Propagation.
   * @param percentileValue precalculated percentile value
   * @param minValue precalculated minimum value
   * @param dirtySrc <b>true:</b> get dirty source preference.
   *                 <b>false:</b> get clean source preference.
   * @return by the preferenceConfig desired preference value
   */
  public static double getPreferenceValue(PreferenceConfig preferenceConfig,
    double percentileValue, double minValue, boolean dirtySrc) {

    double preferenceValue;

    if (dirtySrc) {
      if (preferenceConfig.isPreferenceUseMinSimilarityDirtySrc()) {
        preferenceValue = minValue;
      } else {
        if (preferenceConfig.getPreferenceFixValueDirtySrc() > 0) {
          preferenceValue = preferenceConfig.getPreferenceFixValueDirtySrc();
        } else {
          preferenceValue = percentileValue;
        }
      }

    } else {
      if (preferenceConfig.isPreferenceUseMinSimilarityCleanSrc()) {
        preferenceValue = minValue;
      } else {
        if (preferenceConfig.getPreferenceFixValueCleanSrc() > 0) {
          preferenceValue = preferenceConfig.getPreferenceFixValueCleanSrc();
        } else {
          preferenceValue = percentileValue;
        }
      }
    }

    return preferenceValue;
  }
}
