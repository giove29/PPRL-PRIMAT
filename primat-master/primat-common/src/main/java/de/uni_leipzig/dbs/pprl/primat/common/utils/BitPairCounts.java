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

/**
 * 
 * @author mfranke
 *
 */
public class BitPairCounts {

	private int c00;
	private int c01;
	private int c10;
	private int c11;

	public BitPairCounts() {
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BitPairCounts [c00=");
		builder.append(c00);
		builder.append(", c01=");
		builder.append(c01);
		builder.append(", c10=");
		builder.append(c10);
		builder.append(", c11=");
		builder.append(c11);
		builder.append("]");
		return builder.toString();
	}

	public int getC1x() {
		return c11 + c10;
	}

	public int getCx1() {
		return c11 + c01;
	}

	public int getC0x() {
		return c00 + c01;
	}

	public int getCx0() {
		return c00 + c10;
	}

	public int getC() {
		return this.c00 + this.c01 + this.c10 + this.c11;
	}

	public int getC00() {
		return c00;
	}

	public void setC00(int c00) {
		this.c00 = c00;
	}

	public int getC01() {
		return c01;
	}

	public void setC01(int c01) {
		this.c01 = c01;
	}

	public int getC10() {
		return c10;
	}

	public void setC10(int c10) {
		this.c10 = c10;
	}

	public int getC11() {
		return c11;
	}

	public void setC11(int c11) {
		this.c11 = c11;
	}
}