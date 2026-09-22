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

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.BloomFilterHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.HashingMethod;
import java.util.Collections;


/**
 * 
 * 
 * @author mfranke
 *
 */
public class BloomFilterDefinition {

	private String name;
	private int bfLength;
	private List<BloomFilterExtractorDefinition> featureExtractors;
	private HashingMethod hashingMethod;
	private BloomFilterHardener hardener;
	private Integer recordSaltColumn;
	private boolean missingValueHandlingEnabled;
	private List<String> missingValueAnchorPriority = Collections.emptyList();

	public BloomFilterDefinition() {
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

	public List<BloomFilterExtractorDefinition> getFeatureExtractors() {
		return this.featureExtractors;
	}

	public void setFeatureExtractors(List<BloomFilterExtractorDefinition> featEx) {
		this.featureExtractors = featEx;
	}

	public HashingMethod getHashingMethod() {
		return hashingMethod;
	}

	public void setHashingMethod(HashingMethod hashingMethod) {
		this.hashingMethod = hashingMethod;
	}

	public void setHardener(BloomFilterHardener hardener) {
		this.hardener = hardener;
	}

	public BloomFilterHardener getHardener() {
		return this.hardener;
	}

	public boolean hasRecordSalt() {
		return this.recordSaltColumn != null;
	}

	public Integer getRecordSaltColumn() {
		return this.recordSaltColumn;
	}

	public void setRecordSaltColumn(Integer recordSaltColumn) {
		this.recordSaltColumn = recordSaltColumn;
	}

	public boolean isMissingValueHandlingEnabled() {
		return this.missingValueHandlingEnabled;
	}

	public void setMissingValueHandlingEnabled(boolean missingValueHandlingEnabled) {
		this.missingValueHandlingEnabled = missingValueHandlingEnabled;
	}

	public List<String> getMissingValueAnchorPriority() {
		return this.missingValueAnchorPriority;
	}

	public void setMissingValueAnchorPriority(List<String> missingValueAnchorPriority) {
		this.missingValueAnchorPriority = missingValueAnchorPriority;
	}
}