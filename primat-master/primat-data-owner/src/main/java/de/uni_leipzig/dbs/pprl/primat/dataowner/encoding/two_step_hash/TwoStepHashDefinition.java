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

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.HashingMethod;


/**
 * 
 * 
 * @author mfranke
 *
 */
public class TwoStepHashDefinition {

	private String name;
	private int finalBfLength;
	private int bfLength;
	private int hashFunctions;
	private List<ExtractorDefinition> featureExtractors;
	private HashingMethod hashingMethod;

	public TwoStepHashDefinition() {
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public int getBfLength() {
		return bfLength;
	}

	public void setBfLength(int bfLength) {
		this.bfLength = bfLength;
	}

	public List<ExtractorDefinition> getFeatureExtractors() {
		return this.featureExtractors;
	}

	public void setFeatureExtractors(List<ExtractorDefinition> featEx) {
		this.featureExtractors = featEx;
	}

	public int getHashFunctions() {
		return hashFunctions;
	}

	public void setHashFunctions(int hashFunctions) {
		this.hashFunctions = hashFunctions;
	}

	public HashingMethod getHashingMethod() {
		return hashingMethod;
	}

	public void setHashingMethod(HashingMethod hashingMethod) {
		this.hashingMethod = hashingMethod;
	}

	public int getFinalBfLength() {
		return finalBfLength;
	}

	public void setFinalBfLength(int finalBfLength) {
		this.finalBfLength = finalBfLength;
	}
}