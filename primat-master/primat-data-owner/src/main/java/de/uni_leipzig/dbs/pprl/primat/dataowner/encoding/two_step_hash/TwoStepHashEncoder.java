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
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.two_step_hash;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtraction;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IntegerSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.Encoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.HashingMethod;


/**
 * 
 * @author mfranke
 *
 */
public class TwoStepHashEncoder implements Encoder {

	private final List<TwoStepHashDefinition> tshDefs;

	public TwoStepHashEncoder(List<TwoStepHashDefinition> tshDefs) {
		this.tshDefs = tshDefs;
	}

	@Override
	public Record encode(Record record) {
		final Record encodedRec = new Record();
		encodedRec.setIdAttribute(record.getIdAttribute());
		encodedRec.setPartyAttribute(record.getPartyAttribute());

		for (final TwoStepHashDefinition tshDef : tshDefs) {
			final Set<Integer> tsh = this.constructTSH(tshDef, record);
			System.out.println("SetSize: " + tsh.size());

			final int finalBfLength = tshDef.getFinalBfLength();

			if (finalBfLength == -1) {
				encodedRec.addQidAttribute(new IntegerSetAttribute(tsh));
			}
			else {
				// Convert to Bloom Filter
				final BloomFilter bf = this.toBloomFilter(finalBfLength, tsh);
				encodedRec.addQidAttribute(new BitSetAttribute(bf.getBitVector()));
			}
		}

		return encodedRec;
	}

	private Set<Integer> constructTSH(TwoStepHashDefinition tshDef, Record record) {
		final int bfLength = tshDef.getBfLength();
		final List<ExtractorDefinition> exDefs = tshDef.getFeatureExtractors();
		final HashingMethod hashingMethod = tshDef.getHashingMethod();
		final int hashFunctions = tshDef.getHashFunctions();

		final Set<String> features = this.getFeatureSet(exDefs, record);

		final List<BloomFilter> bfList = this.constructBloomFilters(hashingMethod, hashFunctions, bfLength, features);

		return this.hashBloomFilterColumnsToIntegerArray(hashFunctions, bfLength, bfList);
	}

	private Set<String> getFeatureSet(List<ExtractorDefinition> exDefs, Record record) {
		final Set<String> allFeatures = new HashSet<String>();

		for (final ExtractorDefinition exDef : exDefs) {
			final FeatureExtraction featEx = new FeatureExtraction(exDef);
			final Set<String> features = new HashSet<String>(featEx.getFeatures(record));
			allFeatures.addAll(features);
		}

		return allFeatures;
	}

	private List<BloomFilter> constructBloomFilters(HashingMethod hashingMethod, int hashFunctions, int bfLength,
		Set<String> features) {
		final List<BloomFilter> bfList = new ArrayList<>(hashFunctions);

		for (int i = 0; i < hashFunctions; i++) {
			hashingMethod.setSalt(i + "");
			final Set<Integer> positions = hashingMethod.hash(features, 1);
			final BloomFilter bf = new BloomFilter(bfLength);
			bf.setPositions(positions);
			bfList.add(bf);
		}

		return bfList;
	}

	private Set<Integer> hashBloomFilterColumnsToIntegerArray(int hashFunctions, int bfLength,
		List<BloomFilter> bfList) {
		final Set<Integer> integerSet = new TreeSet<>();

		for (int columnIndex = 0; columnIndex < bfLength; columnIndex++) {
			final BloomFilter columnBf = new BloomFilter(64);

			for (int rowIndex = 0; rowIndex < hashFunctions; rowIndex++) {
				final boolean colValue = bfList.get(rowIndex).get(columnIndex);
				columnBf.setPosition(rowIndex, colValue);
			}

			if (!columnBf.isEmpty()) {
				// TODO: Random Number Generator in the original paper? Which
				// method?

				final long firstLong = columnBf.getBitVector().toLongArray()[0];
				final Random rnd = new Random(firstLong * (columnIndex + 1));
				final int hash = rnd.nextInt(Integer.MAX_VALUE);

				/*
				 * final byte[] hashValue = HashUtils.getDigest(
				 * MessageDigestAlgorithm.SHA_512,
				 * columnBf.getBitVector().toByteArray() ); final int hash =
				 * ByteBuffer.wrap(hashValue).getInt();
				 */

				integerSet.add(hash);
			}
			else {
				// Don't do anything here.
			}
		}
		return integerSet;
	}

	private BloomFilter toBloomFilter(int finalBfLength, Set<Integer> integerSet) {
		final BloomFilter bf = new BloomFilter(finalBfLength);

		for (final Integer value : integerSet) {
			bf.setPosition(Math.abs(value) % finalBfLength);
		}

		return bf;
	}

	@Override
	public List<String> getSchema() {
		final List<String> schema = new ArrayList<>();
		schema.add("ID");
		schema.add("PARTY");

		for (final TwoStepHashDefinition def : this.tshDefs) {
			final String name = def.getName();
			schema.add(name);
		}

		return schema;
	}
}