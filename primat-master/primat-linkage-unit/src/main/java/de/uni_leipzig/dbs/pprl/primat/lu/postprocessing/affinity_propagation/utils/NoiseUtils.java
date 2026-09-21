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

import java.util.Random;

/**
 * Utility class to provide functions for the handling of noise for the similarity values in the
 * (MSCD-) Affinity Propagation algorithm. Noise is important to avoid oscillations between multiple, equal
 * well suited solutions of the clustering procedure.
 */
public class NoiseUtils {

  /**
   * Get the decimal shift that is multiplied to a random noise value, which is configured at
   * {@link ApConfig#getNoiseDecimalPlace()}.
   *
   * @param apConfig Configuration of the (MSCD-) Affinity Propagation clustering algorithm.
   *
   * @return 10^(-1 * (noiseDecimalPlace -1))
   */
  public static double getNoiseDecimalShift(ApConfig apConfig) {
    return Math.pow(10, -1 * (apConfig.getNoiseDecimalPlace() - 1));
  }

  /**
   * Get the decimal shift that is multiplied to a random noise value.
   *
   * @param noiseDecimalPlace position after comma, where the main part of the noise is added.
   *
   * @return 10^(-1 * (noiseDecimalPlace -1))
   */
  public static double getNoiseDecimalShift(int noiseDecimalPlace) {
    return Math.pow(10, -1 * (noiseDecimalPlace - 1));
  }

  /**
   * Get new random gaussian noise, shifted by the desired decimal places in the configuration at
   * {@link ApConfig#getNoiseDecimalPlace()}.
   *
   * @param apConfig Configuration of the (MSCD-) Affinity Propagation clustering algorithm.
   * @param random Random number generator to get a gaussian random number
   *
   * @return shifted random gaussian noise
   */
  public static double getNoise(ApConfig apConfig, Random random) {
    return getNoiseDecimalShift(apConfig) * random.nextGaussian();
  }
}
