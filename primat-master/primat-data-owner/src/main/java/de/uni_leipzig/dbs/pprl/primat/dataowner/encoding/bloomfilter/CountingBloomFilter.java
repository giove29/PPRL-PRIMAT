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
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter;

import java.util.Arrays;


/**
 * 
 * @author mfranke
 *
 */
public class CountingBloomFilter {

	private final int size;
	private final int[] cbf;

	public CountingBloomFilter(int size) {
		this.size = size;
		this.cbf = new int[size];
	}

	public int increment(int position) {
		return ++this.cbf[position];
	}

	public int decrement(int position) {

		if (this.cbf[position] > 0) {
			return --this.cbf[position];
		}
		else {
			return 0;
		}
	}

	public int cardinality() {
		return Arrays.stream(this.cbf).sum();
	}

	public int size() {
		return this.size;
	}

	public int[] intArray() {
		return this.cbf;
	}

}
