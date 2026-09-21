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

package de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.commons.math3.fraction.Fraction;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * 
 * @author mfranke
 *
 */
public class RestrictedHammingLshKeyGenerator extends HammingLshKeyGenerator {

	private List<Record> records;
	private Fraction pruningProportion;

	public RestrictedHammingLshKeyGenerator(Integer keySize, Integer keys, Integer valueRange, Long seed,
		List<Record> records, Fraction pruningProportion) {
		super(keySize, keys, valueRange, seed);
		this.records = records;
		this.pruningProportion = pruningProportion;

		if (this.pruningProportion.multiply(valueRange).intValue() < (this.keys * this.keySize)) {
			throw new RuntimeException("To many or to large keys for pruning proportion.");
		}
	}

	@Override
	protected Integer[] getPositions() {
		final Integer[] nonFrequentPositions = this.determineNonFrequentBitPositions();
		final Integer[] positions = new Integer[this.keys * this.keySize];
		final Random rnd = new Random(this.seed);

		for (int i = 0; i < positions.length; i++) {
			final int nextIndex = rnd.nextInt(nonFrequentPositions.length);
			positions[i] = nonFrequentPositions[nextIndex];
		}

		return positions;
	}

	private Integer[] determineNonFrequentBitPositions() {
		final Map<Integer, Long> bitPositionsMap = this.getBitPositionFrequency();

		final Integer[] sortedBitPositions = this.getBitPositionsSortedByFrequency(bitPositionsMap);

		final Integer[] nonFrequentBitPositions = this.getNonFrequentBitPositions(sortedBitPositions);

		return nonFrequentBitPositions;
	}

	private Map<Integer, Long> getBitPositionFrequency() {
		final Map<Integer, Long> bitpositions = new HashMap<Integer, Long>(this.valueRange);

		for (final Record rec : this.records) {
			updateBitPositionsFrequency(bitpositions, rec);
		}

		return bitpositions;
	}

	private void updateBitPositionsFrequency(Map<Integer, Long> bitPositions, Record record) {
		// TODO: not only the first one
		final BitSet bitVector = record.getBitSetAttributes().get(0).getValue();

		for (int i = bitVector.nextSetBit(0); i >= 0; i = bitVector.nextSetBit(i + 1)) {
			bitPositions.merge(i, 1L, Long::sum);

			if (i == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
			}
		}
	}

	private Integer[] getBitPositionsSortedByFrequency(Map<Integer, Long> bitPositionsMap) {
		return bitPositionsMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Entry::getKey)
			.toArray(Integer[]::new);
	}

	private Integer[] getNonFrequentBitPositions(Integer[] sortedBitPositions) {
		final int startIndex = this.pruningProportion.multiply(this.valueRange).intValue();
		final int endIndex = this.valueRange - startIndex;

		return Arrays.copyOfRange(sortedBitPositions, startIndex, endIndex);
	}

	@Override
	public String toString() {
		return super.toString() + " (Restricted)";
	}

}