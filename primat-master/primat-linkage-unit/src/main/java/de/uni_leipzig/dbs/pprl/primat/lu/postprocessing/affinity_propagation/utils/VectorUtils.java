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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.utils;

/**
 * Utility class for the handling of vector operations. This class is based on the VecUtils class of the
 * clust4j project by Taylor G Smith
 * (<a href="https://github.com/tgsmith61591/clust4j">clust4j github project</a>).
 */
public abstract class VectorUtils {
  /*
   * This class is based on the VecUtils class of the clust4j project by Taylor G Smith. The first license
   * is for the modifications in this project. The second license in this file is the original one.
   * The original source code can be found on:
   *
   *    https://github.com/tgsmith61591/clust4j
   *
   * Compared to the original class, the following modifications were made:
   * - removed subClasses, which are not needed in this project: VecSeries, DoubleSeries, IntSeries
   * - removed dimension checks from each method
   * - removed methods, which are not needed in this project:
   *   dimAssess, checkDims, dimAssessPermitEmpty, checkDimsPermitEmpty, dimAssess, checkDims,
   *   dimAssessPermitEmpty, checkDimsPermitEmpty abs, add, check_arange_return_len, arange, argSort,
   *   _arange, argsort, asArray, asDouble, cat, center, completeCases, containsNaN, copy(for int, boolean,
   *   string, ArrayList), cosSim, cumsum, divide, equalsExactly, equalsWithTolerance, exp, floor,
   *   innerProduct, iqr, isOrthogonalTo, l1Norm, l2Norm, log, lpNorm, magnitude, max, min, multiply,nanCount,
   *   nanMax, nanMin, nanMean, nanMedian, nanStdDev, nanSum, nanVar, negative, normalize, outerProduct,
   *   partition, permutation, pmax, pmin, pow, prod, randomGaussian, reorder, rep, repInt, repBool,
   *   reverseSeries, scalarAdd, scalarDivide, scalarMultiply, scalarSubtract, slice, sortAsc, sqrt, sum,
   *   stdDev, subtract, sum(for boolean), unique, var, vstack, where, mean
   * - removed constants which are not needed in this project: VEC_LEN_ERR, MIN_ACCEPTABLE_VEC_LEN,
   *   DEF_SUBTRACT_ONE_VAR
   * - added javaDocs for methods and parameters were it was missing
   * - removed final modifiers of methods
   */

  /**
   * Return the index of the max element in the vector. In the case
   * of a tie, the first "max" element ordinally will be returned.
   *
   * @param v the vector
   * @throws IllegalArgumentException if the vector is empty
   * @return the idx of the max element
   */
  public static int argMax(final double[] v) {
//    checkDims(v);                           // removed compared to original code

    double max = Double.NEGATIVE_INFINITY;    // changed compared to original code
//    int maxIdx = -1;                       // changed compared to original code
    int maxIdx = 0;

    for (int i = 0; i < v.length; i++) {
      double val = v[i];
      if (val > max) {
        max = val;
        maxIdx = i;
      }
    }

    return maxIdx;
  }

  /**
   * Return the index of the min element in the vector. In the case
   * of a tie, the first "min" element ordinally will be returned.
   *
   * @param v the vector
   * @throws IllegalArgumentException if the vector is empty
   * @return the idx of the min element
   */
  public static int argMin(final double[] v) {
//    checkDims(v);                             // removed compared to original code

    double min = Double.POSITIVE_INFINITY;      // changed compared to original code
    int minIdx = -1;

    for (int i = 0; i < v.length; i++) {
      double val = v[i];
      if (val < min) {
        min = val;
        minIdx = i;
      }
    }

    return minIdx;
  }

  /**
   * Return a copy of a double array
   *
   * @param d double vector
   * @return the copy
   */
  public static double[] copy(final double[] d) {
    if (null == d) {
      return null;
    }

    final double[] copy = new double[d.length];
    System.arraycopy(d, 0, copy, 0, d.length);
    return copy;
  }

  /**
   * Return a copy of an int array
   *
   * @param i integer vector
   * @return the copy
   */
  public static int[] copy(final int[] i) {
    if (null == i) {
      return null;
    }

    final int[] copy = new int[i.length];
    System.arraycopy(i, 0, copy, 0, i.length);
    return copy;
  }
}
