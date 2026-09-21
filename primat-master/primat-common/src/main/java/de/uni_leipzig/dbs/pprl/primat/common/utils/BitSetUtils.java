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
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;


/**
 * Utility class providing common {@link BitSet}-related operations.
 * 
 * @author mfranke
 *
 */
public final class BitSetUtils {

	private BitSetUtils() {
		throw new RuntimeException();
	}

	/**
	 * Construct a new BitSet based on the position of 1-bits.
	 * 
	 * @param  setBits positions of 1-bits in the resulting BitSet.
	 * 
	 * @return         BitSet where the bits at the specified positions are set
	 *                 to one.
	 */
	public static BitSet from(int... setBits) {
		final BitSet bs = new BitSet();

		for (final int bitIndex : setBits) {
			bs.set(bitIndex);
		}
		return bs;
	}

	public static String toShortString(BitSet bs) {

		final StringBuilder b = new StringBuilder();

		int i = bs.nextSetBit(0);

		if (i != -1) {
			b.append(i);

			while (true) {

				if (++i < 0) {
					break;
				}

				if ((i = bs.nextSetBit(i)) < 0) {
					break;
				}
				int endOfRun = bs.nextClearBit(i);

				do {
					b.append(",").append(i);
				}
				while (++i != endOfRun);
			}
		}

		return b.toString();
	}

	/**
	 * Stores an integer number in a BitSet object.
	 *
	 * @param value integer number to store
	 * @param length number of bits to use.
	 * @return
	 */
	public static BitSet intToBitSet(int value, int length) {
		final BitSet bits = new BitSet(length);
		int index = 0;
		while (value != 0) {
			if (value % 2 != 0) {
				bits.set(index);
			}
			index++;
			value = value >>> 1;
		}

		return bits;
	}


	/**
     * Convert a BigInteger into a BitSet
     * 
     * @param i BigInteger to convert
     * @return 
     */
    public static BitSet bigIntToBitSet(BigInteger i) {
        return BitSet.valueOf(i.toByteArray());
    }
	
	/**
	 * Construct a BitSet based on a String encoded as Base64 little endian.
	 * 
	 * @param  string Base64 little endian representation of a bit array.
	 * @return        BitSet object.
	 */
	public static BitSet fromBase64LittleEndian(String string) {
		final byte[] bytes = Base64.getDecoder().decode(string);
		return BitSet.valueOf(bytes);
	}

	/**
	 * Encodes a BitSet into a Base64 little endian encoded string.
	 * 
	 * @param  bitset the Bitset object to encode.
	 * @return        String representation of the BitSet.
	 */
	public static String toBase64LittleEndian(BitSet bitset) {
		return Base64.getEncoder().encodeToString(bitset.toByteArray());
	}

	/**
	 * Construct a BitSet based on a String encoded as Base64 big endian.
	 * 
	 * @param  string Base64 big endian representation of a bit array.
	 * @return        BitSet object.
	 */
	public static BitSet fromBase64BigEndian(String string) {
		final byte[] bytes = Base64.getDecoder().decode(string);
		ArrayUtils.reverse(bytes);
		return BitSet.valueOf(bytes);
	}

	/**
	 * Encodes a BitSet into a Base64 big endian encoded string.
	 * 
	 * @param  bitset the Bitset object to encode.
	 * @return        String representation of the BitSet.
	 */
	public static String toBase64BigEndian(BitSet bitset) {
		final byte[] byteArray = bitset.toByteArray();
		ArrayUtils.reverse(byteArray);
		return Base64.getEncoder().encodeToString(byteArray);
	}

	public static BitSet fromBinaryLittleEndian(String binary) {
		final BitSet bitset = new BitSet(binary.length() - 1);

		for (int i = 0; i < binary.length(); i++) {

			if (binary.charAt(i) == '1') {
				bitset.set(i);
			}
		}
		return bitset;
	}

	public static BitSet fromBinaryBigEndian(String binary) {
		final BitSet bitset = new BitSet(binary.length() - 1);

		for (int i = 0; i < binary.length(); i++) {

			if (binary.charAt(i) == '1') {
				final int pos = binary.length() - (i + 1);
				bitset.set(pos);
			}
		}
		return bitset;
	}

	public static String toBinaryLittleEndian(BitSet bitset) {
		return toBinaryLittleEndian(bitset, bitset.size());
	}

	public static String toBinaryLittleEndian(BitSet bitset, int size) {
		final StringBuilder result = new StringBuilder();

		for (int bitIndex = 0; bitIndex < size; bitIndex++) {

			if (bitset.get(bitIndex)) {
				result.append("1");
			}
			else {
				result.append("0");
			}
		}
		return result.toString();
	}

	public static String toBinaryBigEndian(BitSet bitset) {
		return toBinaryBigEndian(bitset, bitset.size());
	}

	public static String toBinaryBigEndian(BitSet bitset, int size) {
		final StringBuilder result = new StringBuilder();

		for (int bitIndex = size - 1; bitIndex >= 0; bitIndex--) {

			if (bitset.get(bitIndex)) {
				result.append("1");
			}
			else {
				result.append("0");
			}
		}
		return result.toString();
	}

	public static BitSet generateRandomBitSet(int length, int oneBits, long seed) {
		final int setBits = oneBits > length ? length : oneBits;

		final BitSet bs = new BitSet(length);
		final Random rnd = new Random(seed);

		final Set<Integer> positions = rnd.ints(0, length).distinct().limit(setBits).boxed()
			.collect(Collectors.toSet());

		positions.forEach(p -> bs.set(p));

		return bs;
	}

	public static int[] getCounts(int length, Collection<BitSet> bitsets) {
		final int[] counts = new int[length];

		for (final BitSet bs : bitsets) {

			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
				counts[i]++;

				if (i == Integer.MAX_VALUE) {
					break; // or (i+1) would overflow
				}
			}
		}

		return counts;
	}

	public static int[] toIntArray(int length, BitSet bitset) {
		final int[] arr = new int[length];

		for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i + 1)) {
			arr[i]++;

			if (i == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
			}
		}

		return arr;
	}

	public static BitSet concatenateByteWords(BitSet first, BitSet second) {
		final byte[] firstBytes = first.toByteArray();
		final byte[] secondBytes = second.toByteArray();

		final byte[] both = ArrayUtils.addAll(firstBytes, secondBytes);
		return BitSet.valueOf(both);
	}

	public static BitSet concatenateLongWords(BitSet first, BitSet second) {
		final long[] firstBytes = first.toLongArray();
		final long[] secondBytes = second.toLongArray();

		final long[] both = ArrayUtils.addAll(firstBytes, secondBytes);
		return BitSet.valueOf(both);
	}

	public static BitSet permuteBytes(BitSet bs, long seed) {
		final byte[] bytes = bs.toByteArray();
		final List<Byte> byteList = Arrays.asList(ArrayUtils.toObject(bytes));
		Collections.shuffle(byteList, new Random(seed));
		final Byte[] permBytes = byteList.toArray(new Byte[byteList.size()]);
		return BitSet.valueOf(ArrayUtils.toPrimitive(permBytes));
	}

	/**
	 * Complement of the <b>whole</b> {@link BitSet}, that is, all bits of the
	 * physical length (not logical!) are flipped.
	 * 
	 * @param  bs
	 * @return
	 */
	public static BitSet not(BitSet bs) {
		final long[] longs = bs.toLongArray();

		for (int i = 0; i < longs.length; i++) {
			longs[i] = ~longs[i];
		}
		return BitSet.valueOf(longs);
	}

	public static BitSet complement(BitSet bs, int size) {
		final BitSet complement = (BitSet) bs.clone();
		complement.flip(size, size);
		return complement;
	}

	public static BitSet and(BitSet first, BitSet second) {
		if (first != null && second != null) {
			final BitSet bitset = (BitSet) first.clone();
			bitset.and(second);
			return bitset;
		}
		else {
			return null;
		}
	}

	public static BitSet and(BitSet first, BitSet second, BitSet andSet) {
		andSet.clear();
		if (first != null && second != null && first.size() == second.size()) {
			andSet.or(first);
			andSet.and(second);
			return andSet;
		} 
		else {
			return andSet;
		}
	}

	public static BitSet andNot(BitSet first, BitSet second) {

		if (first != null && second != null) {
			final BitSet bitset = (BitSet) first.clone();
			bitset.andNot(second);
			return bitset;
		}
		else {
			return null;
		}
	}

	public static BitSet nand(BitSet first, BitSet second) {

		if (first != null && second != null) {
			final BitSet bitset = (BitSet) first.clone();
			bitset.and(second);
			bitset.clear(Math.min(first.size(), second.size()), bitset.size());
			return bitset;
		}
		else {
			return null;
		}
	}

	public static BitSet or(BitSet first, BitSet second) {

		if (first != null && second != null) {
			final BitSet bitset = (BitSet) first.clone();
			bitset.or(second);
			return bitset;
		}
		else {
			return null;
		}
	}

	public static BitSet nor(BitSet first, BitSet second) {

		if (first != null && second != null) {
			final BitSet bitset = (BitSet) first.clone();
			bitset.or(second);
			// TODO: or not from above
			bitset.flip(0, bitset.size());
			bitset.clear(Math.min(first.size(), second.size()), bitset.size());
			return bitset;
		}
		else {
			return null;
		}
	}

	public static BitSet xor(BitSet first, BitSet second) {

		if (first != null && second != null) {
			BitSet bitset = (BitSet) first.clone();
			bitset.xor(second);
			return bitset;
		}
		else {
			return null;
		}
	}

	/**
	 * Shifts bits of a BitSet object one place to the right.
	 *
	 * @param bs BitSet object
	 * @param length of binary number
	 * @param logical true for logical right shift; false for arithmetic right shift
	 * @return BitSet after shifting
	 */
	public static BitSet shiftRight(BitSet bs, int length, boolean logical) {
		for (int i = 0; i < length-1; i++) {
			final boolean val = bs.get(i+1);
			bs.set(i, val);
		}

		if (logical) {
			bs.set(length-1, false);
		}

		return bs;
	}

	/**
	 * Shifts bits of a BitSet object one place to the left.
	 *
	 * @param bs BitSet object
	 * @param length of binary number
	 * @return BitSet after shifting
	 */
	public static BitSet shiftLeft(BitSet bs, int length) {
		for (int i = length; i > 0; i--) {
			final boolean val = bs.get(i-1);
			bs.set(i, val);
		}

		bs.set(0, false);

		return bs;
	}

	public static BitSet allOne(int size) {
		final BitSet bs = new BitSet(size);
		bs.set(0, size);
		return bs;
	}

	public static BitSet clone(BitSet bs) {
		final BitSet bitset = new BitSet();
		bitset.or(bs);
		return bitset;
	}

	public static int cardinalityLimitTo(BitSet bs, int pos) {
		final BitSet bitset = (BitSet) bs.clone();
		bitset.clear(pos, bs.size());
		return bitset.cardinality();
	}

	public static BitPairCounts getBitPairCounts(BitSet one, BitSet two, int size) {
		final int c00 = cardinalityLimitTo(BitSetUtils.nor(one, two), size);
		final int c01 = cardinalityLimitTo(BitSetUtils.andNot(two, one), size);
		final int c10 = cardinalityLimitTo(BitSetUtils.andNot(one, two), size);
		final int c11 = cardinalityLimitTo(BitSetUtils.and(one, two), size);

		final BitPairCounts bpc = new BitPairCounts();
		bpc.setC00(c00);
		bpc.setC01(c01);
		bpc.setC10(c10);
		bpc.setC11(c11);

		return bpc;
	}

	public static BitPairCounts getBitPairCounts(BitSet one, BitSet two) {
		final int c00 = BitSetUtils.nor(one, two).cardinality();
		final int c01 = BitSetUtils.andNot(two, one).cardinality();
		final int c10 = BitSetUtils.andNot(one, two).cardinality();
		final int c11 = BitSetUtils.and(one, two).cardinality();

		final BitPairCounts bpc = new BitPairCounts();
		bpc.setC00(c00);
		bpc.setC01(c01);
		bpc.setC10(c10);
		bpc.setC11(c11);

		return bpc;
	}

}