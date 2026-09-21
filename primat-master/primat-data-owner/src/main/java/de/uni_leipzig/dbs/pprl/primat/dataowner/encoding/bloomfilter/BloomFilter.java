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

import java.util.BitSet;
import java.util.Set;


/**
 * A Bloom filter is a bit array or bit set of fixed size where initially all
 * bits are set to zero. By using a specific hashing scheme elements of an
 * arbitrary set are hash-mapped into the Bloom filter. As a consequence, a
 * Bloom filter is a space-efficient way to represent a set of elements and is
 * frequently used for testing set membership.
 * 
 * The purpose of this class is to enforce the size constraint on Java
 * {@link BitSet}.
 * 
 * 
 * @author mfranke
 *
 */
public class BloomFilter {

	private static final int WORD_SIZE = 64;

	private final int size;

	private final BitSet bitset;

	/**
	 * Copy constructor.
	 * 
	 * @param another the Bloom filter to copy.
	 */
	public BloomFilter(BloomFilter another) {
		this.size = another.size;
		this.bitset = (BitSet) another.bitset.clone();
	}

	/**
	 * Constructs a new Bloom filter by initializing the underlying
	 * {@link BitSet} of the specified size.
	 * 
	 * @param size Bloom filter size constraint.
	 */
	public BloomFilter(int size) {
		this(size, new BitSet(size));
	}

	/**
	 * Constructs a new Bloom filter by using a given {@link BitSet}.
	 * 
	 * @param size   Bloom filter size constraint.
	 * @param bitset the underlying {@link BitSet} object to use.
	 */
	public BloomFilter(int size, BitSet bitset) {
		if (size % WORD_SIZE != 0)
			throw new IllegalArgumentException("Size must be a multiple of " + WORD_SIZE);

		this.size = size;
		this.bitset = bitset;
	}

	/**
	 * Sets the bits at the specified positions to one.
	 * 
	 * @param positions a set of positions.
	 */
	public void setPositions(Set<Integer> positions) {

		for (final Integer pos : positions) {
			this.setPosition(pos);
		}
	}

	/**
	 * Sets the bit at the specified position to one. If the position is out of
	 * the Bloom filter range a {@link IndexOutOfBoundsException} is thrown. If
	 * the bit at the position is already set to one nothing is done.
	 * 
	 * @param pos
	 */
	public void setPosition(Integer pos) {
		checkRangeConstraint(pos);
		this.bitset.set(pos);
	}

	private void checkRangeConstraint(Integer pos) {

		if (pos >= this.size) {
			throw new IndexOutOfBoundsException("size < " + pos);
		}
	}

	/**
	 * Sets the bit at the specified position to the specified value. If the
	 * position is out of the Bloom filter range a
	 * {@link IndexOutOfBoundsException} is thrown. If the bit at the position
	 * is already set to one nothing is done.
	 * 
	 * @param pos
	 */
	public void setPosition(Integer pos, boolean value) {
		checkRangeConstraint(pos);
		this.bitset.set(pos, value);
	}

	public boolean get(Integer pos) {
		checkRangeConstraint(pos);
		return this.bitset.get(pos);
	}

	/**
	 * @return size of the Bloom filter.
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * 
	 * @return underlying {@link BitSet} object.
	 */
	public BitSet getBitVector() {
		return this.bitset;
	}

	/**
	 * Returns the number of bits set to {@code true}.
	 *
	 * @return the number of bits set to {@code true}.
	 */
	public int cardinality() {
		return this.bitset.cardinality();
	}

	/**
	 * A Bloom filter is empty if all bits are set to zero.
	 * 
	 * @return boolean value indication if the Bloom filter is empty.
	 */
	public boolean isEmpty() {
		return this.cardinality() == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(bitset);
		builder.append("]");
		return builder.toString();
	}
}