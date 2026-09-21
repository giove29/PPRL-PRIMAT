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

import java.util.List;


/**
 * 
 * @author mfranke
 *
 */
public abstract class LshKeyGenerator {

	protected Integer keySize;
	protected Integer keys;
	protected Integer valueRange;
	protected Long seed;

	public LshKeyGenerator(Integer keySize, Integer keys, Integer valueRange, Long seed) {
		this.keySize = keySize;
		this.keys = keys;
		this.valueRange = valueRange;
		this.seed = seed;
	}

	public abstract List<LshBlockingFunction> generate();

	public Integer getKeySize() {
		return keySize;
	}

	public void setKeySize(Integer keySize) {
		this.keySize = keySize;
	}

	public Integer getKeys() {
		return keys;
	}

	public void setKeys(Integer keys) {
		this.keys = keys;
	}

	public Integer getValueRange() {
		return valueRange;
	}

	public void setValueRange(Integer valueRange) {
		this.valueRange = valueRange;
	}

	public Long getSeed() {
		return seed;
	}

	public void setSeed(Long seed) {
		this.seed = seed;
	}

}