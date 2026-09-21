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
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * The hashing method defines how features are hashed into a Bloom filter, i.e.,
 * how a feature is mapped to an integer that is interpreted as position within
 * the Bloom filter where the bit at this position should be set to one.
 * 
 * @author mfranke
 *
 */
public abstract class HashingMethod {

	protected final int bfSize;
	protected String salt;
	protected final Map<Integer, Set<String>> featurePositionMapping;

	protected HashingMethod(int bfSize) {
		this(bfSize, "");
	}

	protected HashingMethod(int bfSize, String salt) {
		this.bfSize = bfSize;
		this.salt = salt;
		this.featurePositionMapping = new TreeMap<>();
		this.initFeaturePositionMapping();
	}

	protected void initFeaturePositionMapping() {

		for (int i = 0; i < this.bfSize; i++) {
			this.featurePositionMapping.put(i, new TreeSet<>());
		}

	}

	public abstract List<Integer> hashToInts(String element, int hashFunctions);

	/**
	 * Hashes the specified feature, i.e., computes a mapping from the feature
	 * to a set of positions used for setting bits in a Bloom filter.
	 * 
	 * @param  element       feature to be hashed in a Bloom filter.
	 * @param  hashFunctions number of hash functions that should be used or
	 *                       simulated.
	 * @return               set of integers (positions).
	 */
	public Set<Integer> hash(String element, int hashFunctions) {
		final List<Integer> ints = this.hashToInts(element, hashFunctions);
		// TODO: HashSet; TreeSet only for easier testing
		return new TreeSet<Integer>(ints);
	}

	/**
	 * Hashes the specified set of features, i.e., computes a mapping from the
	 * set of features to a set of positions used for setting bits in a Bloom
	 * filter.
	 * 
	 * @param  elements      features to be hashed in a Bloom filter.
	 * @param  hashFunctions number of hash functions that should be used or
	 *                       simulated.
	 * @return               set of integers (positions).
	 */
	public Set<Integer> hash(Collection<String> elements, int hashFunctions) {
		final List<Integer> ints = this.hashToInts(elements, hashFunctions);
		// TODO: HashSet; TreeSet only for easier testing
		return new TreeSet<Integer>(ints);
	}

	public List<Integer> hashToInts(Collection<String> elements, int hashFunctions) {
		final List<Integer> positions = new ArrayList<>(hashFunctions * elements.size());

		for (final String e : elements) {
			final List<Integer> positionsForElement = this.hashToInts(e, hashFunctions);
			positions.addAll(positionsForElement);
			this.updateFeaturePositionMapping(e, positionsForElement);
		}
		return positions;
	}

	protected void updateFeaturePositionMapping(String feature, Collection<Integer> positions) {

		for (final Integer pos : positions) {
			final Set<String> featuresAtPosition = this.featurePositionMapping.get(pos);
			featuresAtPosition.add(feature);
		}
	}

	public Map<Integer, Set<String>> getFeaturePositionMapping() {
		return this.featurePositionMapping;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public String getSalt() {
		return this.salt;
	}

	public int getBloomFilterSize() {
		return this.bfSize;
	}
}