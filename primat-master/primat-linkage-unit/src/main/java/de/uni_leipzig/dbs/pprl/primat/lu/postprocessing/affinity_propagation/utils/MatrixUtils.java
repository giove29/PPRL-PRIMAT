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
 * Utility class for the handling of matrix operations. This class is based on the MatUtils class of the
 * clust4j project by Taylor G Smith
 * (<a href="https://github.com/tgsmith61591/clust4j">clust4j github project</a>).
 */
public abstract class MatrixUtils {
  /*
   * This class is based on the MatUtils class of the clust4j project by Taylor G Smith. The first license
   * is for the modifications in this project. The second license in this file is the original one.
   * The original source code can be found on:
   *
   *    https://github.com/tgsmith61591/clust4j
   *
   * Compared to the original class, the following modifications were made:
   * - removed subClasses, which are not needed in this project: MatSeries
   * - removed enums, which are not needed in this project: Operator
   * - removed dimension checks from each method
   * - removed methods, which are not needed in this project:
   *   argMin, checkMultipliability, dimAssess, dimAssessPermitEmpty, throwDimException, checkDims,
   *   checkDimsForUniformity, checkDimsPermitEmpty, abs, colMeans, colSums, colMeansSums,
   *   completeCases, containsNaN, containsInf, copy, cumSum, diagFromSquare, equalsExactly,
   *   equalsWithTolerance, flatten, flattenUpperTriangularMatrix, floor, fromVector, fromList, getColumn
   *   (for int), getColumns(for Integer), getRows(for Integer), getRows, max, min, minMax, meanRecord,
   *   medianRecord, multiply, multiplyDistributed, negative, rbind, sortColsAsc, sortRowsAsc,
   *   randomGaussian, reorder, rep, reshape, rowMeans, rowSums, rowMeansSums, scalarAdd, scalarDivide,
   *   scalarMultiply, scalarOperate, scalarSubtract, slice, sortAscByCol, sortDescByCol, subtract, sum,
   *   toDouble, transpose, where, setColumnInPlace,setRowInPlace
   * - removed constants which are not needed in this project: MAT_DIM_ERR_MSG, MIN_ACCEPTABLE_MAT_LEN
   * - added javaDocs for methods and parameters were it was missing, corrected javaDocs @see links
   * - removed static modifiers at unnecessary places
   */


  /**
   * A number of axis-wise operations require an
   * axis argument. This set of enums indicates whether
   * to apply a function of the rows or columns of a matrix
   */
  public enum Axis {
    /**
     * Row axis of a matrix
     */
    ROW,
    /**
     * Column axis of a matrix
     */
    COL
  }

  /**
   * Computes the indices of the max along the provided axes.
   *
   * @param data the matrix
   * @param axis - row or column wise. For {@link Axis#ROW}, returns
   * the column index of the max for each row; for {@link Axis#COL}, returns
   * the row index of the max for each column.
   * @return an array of the indices of the arg max
   * @see VectorUtils#argMax(double[])
   */
  public static int[] argMax(final double[][] data, final Axis axis) {
    return argMaxMin(data, axis, true);
  }

  /**
   * Computes either the argMin or the argMax depending on the boolean parameter
   *
   * @param data the matrix
   * @param axis - row or column wise. For {@link Axis#ROW}, returns
   * @param max - whether to compute the min or max
   * @return the argMin or argMax vector
   */
  private static int[] argMaxMin(final double[][] data, final Axis axis, final boolean max) {
    if (data.length == 0) {
      return new int[0];
    }
//    checkDimsForUniformity(data);                           // removed compared to original code

    int[] out;
    final int m = data.length;
    final int n = data[0].length;

    if (axis.equals(Axis.COL)) {
      out = new int[n];
      double[] col;
      for (int i = 0; i < n; i++) {
        col = getColumn(data, i);
        out[i] = max ? VectorUtils.argMax(col) : VectorUtils.argMin(col);
      }
    } else {
      out = new int[m];
      for (int i = 0; i < m; i++) {
        out[i] = max ? VectorUtils.argMax(data[i]) : VectorUtils.argMin(data[i]);
      }
    }

    return out;
  }

  /**
   * Retrieve a column from a uniform matrix
   *
   * @param data the matrix
   * @param idx index of the column to retrieve
   * @throws IndexOutOfBoundsException if the idx is
   * less than 0 or >= the length of the matrix
   * @return the column at the idx
   */
  public static double[] getColumn(final double[][] data, final int idx) {
//    checkDimsForUniformity(data);                           // removed compared to original code

    final int m = data.length;
    final int n = data[0].length;

    if (idx >= n || idx < 0) {
      throw new IndexOutOfBoundsException(idx + "");
    }

    final double[] col = new double[m];
    for (int i = 0; i < m; i++) {
      col[i] = data[i][idx];
    }

    return col;
  }

  /**
   * Retrieve a set of columns from a uniform matrix
   *
   * @param data the matrix
   * @param idcs indices of the columns to retrieve
   * @throws IllegalArgumentException if the rows are empty
   * @throws IndexOutOfBoundsException if the idx is
   * less than 0 or >= the length of the matrix
   * @return the new matrix
   */
  public static double[][] getColumns(final double[][] data, final int[] idcs) {
//    checkDimsForUniformity(data);                           // removed compared to original code
    final double[][] out = new double[data.length][idcs.length];

    int idx = 0;
    for (int col: idcs) {
      if (col < 0 || col >= data[0].length) {
        throw new IndexOutOfBoundsException(col + "");
      }

      for (int i = 0; i < data.length; i++) {
        out[i][idx] = data[i][col];
      }

      idx++;
    }

    return out;
  }

  /**
   * Copy a 2d double array
   *
   * @param data input matrix
   * @return a copy of the input matrix
   */
  public static double[][] copy(final double[][] data) {
    if (null == data) {
      return null;
    }

    final double[][] copy = new double[data.length][];
    for (int i = 0; i < copy.length; i++) {
      copy[i] = VectorUtils.copy(data[i]);
    }

    return copy;
  }

  /**
   * Add two matrices. This operation demands uniformity of the input matrices, but permits
   * matrices with empty rows to be added as long as their dimensions match.
   *
   * @param a first matrix for addition
   * @param b second matrix for addition
   * @return the sum of the two matrices
   */
  public static double[][] add(final double[][] a, final double[][] b) {
    final int m = a.length;
    final int n = a[0].length;

    final double[][] c = new double[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        c[i][j] = a[i][j] + b[i][j];
      }
    }
    return c;
  }
}
