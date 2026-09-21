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

package de.uni_leipzig.dbs.pprl.primat.common.model;

import java.util.BitSet;
import java.util.Collection;
import java.util.stream.IntStream;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.vladmihalcea.hibernate.type.array.IntArrayType;

import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;


/**
 * 
 * @author mfranke
 *
 */
@Embeddable
@TypeDefs({
	@TypeDef(name = "int-array", typeClass = IntArrayType.class)
})
public class CountingBloomFilter {

	@Access(AccessType.PROPERTY)
	@Type(type = "int-array")
	@Column(name = "virtrep", columnDefinition = "INTEGER[]")
	private int[] data;

	@Column
	private int elements;

	public CountingBloomFilter() {
	}

	public CountingBloomFilter(int size) {
		this.data = new int[size];
		this.elements = 0;
	}

	public CountingBloomFilter(int[] data, int elements) {
		this.data = data;
		this.elements = elements;
	}

	public static CountingBloomFilter from(int length, Collection<BitSet> bitsets) {
		final int[] data = BitSetUtils.getCounts(length, bitsets);
		final CountingBloomFilter cbf = new CountingBloomFilter(data, bitsets.size());
		return cbf;
	}

	public void add(BitSet bitset) {
		final int[] increments = BitSetUtils.toIntArray(this.data.length, bitset);

		for (int i = 0; i < this.data.length; i++) {
			this.data[i] += increments[i];
		}

		this.elements++;
	}

	public void increment(int pos) {
		this.data[pos]++;
	}

	public int cardinality() {
		return IntStream.of(this.data).sum();
	}

	public int[] getData() {
		return data;
	}

	public void setData(int[] data) {
		this.data = data;
	}

	public int getElements() {
		return elements;
	}

	public void setElements(int elements) {
		this.elements = elements;
	}
}