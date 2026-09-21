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

import java.math.BigInteger;


/**
 * 
 * @author mfranke
 *
 */
public class BigBitPairCounts {

	private final BitPairCounts bpc;

	public BigBitPairCounts(BitPairCounts bpc) {
		this.bpc = bpc;
	}

	public BigInteger getC1x() {
		return BigInteger.valueOf(this.bpc.getC1x());
	}

	public BigInteger getCx1() {
		return BigInteger.valueOf(this.bpc.getCx1());
	}

	public BigInteger getC0x() {
		return BigInteger.valueOf(this.bpc.getC0x());
	}

	public BigInteger getCx0() {
		return BigInteger.valueOf(this.bpc.getCx0());
	}

	public BigInteger getC() {
		return BigInteger.valueOf(this.bpc.getC());
	}

	public BigInteger getC00() {
		return BigInteger.valueOf(this.bpc.getC00());
	}

	public BigInteger getC01() {
		return BigInteger.valueOf(this.bpc.getC01());
	}

	public BigInteger getC10() {
		return BigInteger.valueOf(this.bpc.getC10());
	}

	public BigInteger getC11() {
		return BigInteger.valueOf(this.bpc.getC11());
	}
}