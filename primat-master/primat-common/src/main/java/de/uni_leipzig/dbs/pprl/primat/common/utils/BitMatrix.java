/*******************************************************************************
 *  Copyright © 2017 - 2022 Leipzig University (Database Research Group)
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under 
 * the License.
 *******************************************************************************/

package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.BitSet;


/**
 * 
 * @author mfranke
 *
 */
public class BitMatrix {

	private final int rows;
	private final int cols;
	private BitSet[] columnArray;
	private BitSet[] rowArray;

	public BitMatrix(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		this.initBitMatrix();
	}

	public void clear() {
		this.initBitMatrix();
	}

	private void initBitMatrix() {
		this.columnArray = new BitSet[this.cols];
		this.rowArray = new BitSet[this.rows];

		for (int i = 0; i < this.rows; i++) {
			this.rowArray[i] = new BitSet(cols);
		}

		for (int i = 0; i < this.cols; i++) {
			this.columnArray[i] = new BitSet(rows);
		}
	}

	public int[] getColumnCounts() {
		final int[] colCount = new int[this.cols];

		for (int i = 0; i < this.cols; i++) {
			final BitSet col = this.columnArray[i];
			colCount[i] = col.cardinality();
		}

		return colCount;
	}

	public int[] getRowCounts() {
		final int[] rowCount = new int[this.rows];

		for (int i = 0; i < this.rows; i++) {
			final BitSet row = this.rowArray[i];
			rowCount[i] = row.cardinality();
		}

		return rowCount;
	}

	public int rows() {
		return rows;
	}

	public int cols() {
		return cols;
	}

	public BitSet getRow(int row) {
		return this.rowArray[row];
	}

	public BitSet getColumn(int col) {
		return this.columnArray[col];
	}

	public boolean get(int row, int col) {
		return this.rowArray[row].get(col);
	}

	public void set(int row, int col) {
		this.rowArray[row].set(col);
		this.columnArray[col].set(row);
	}

	public void setRow(int row, BitSet val) {
		this.rowArray[row] = val;

		for (int i = 0; i < this.cols; i++) {
			this.columnArray[i].set(row, val.get(i));
		}
	}

	public void setColumn(int col, BitSet val) {
		this.columnArray[col] = val;

		for (int i = 0; i < this.rows; i++) {
			this.rowArray[i].set(col, val.get(i));
		}
	}

	public void print() {

		for (final BitSet bs : this.rowArray) {
			final String val = BitSetUtils.toBinaryLittleEndian(bs, rows);
			System.out.println(val);
		}

		System.out.println();

		for (final BitSet bs : this.columnArray) {
			final String val = BitSetUtils.toBinaryLittleEndian(bs, cols);
			System.out.println(val);
		}

		System.out.println();
	}
}