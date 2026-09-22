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

import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;


/**
 * 
 * @author mfranke
 *
 */
public class BloomFilterExtractorDefinition extends ExtractorDefinition {

	private int hashes;
	private String salt;
	private int missingValueTokenCount;

	public BloomFilterExtractorDefinition() {
		this("");
	}

	public BloomFilterExtractorDefinition(String name) {
		super(name);
		this.hashes = -1;
		this.salt = "";
		this.missingValueTokenCount = 0;
	}

	public void setNumberOfHashFunctions(int hashes) {
		this.hashes = hashes;
	}

	public int getNumberOfHashFunctions() {
		return this.hashes;
	}

	public String getSalt() {
		return this.salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	/** @return numero di token sintetici da generare via {@code MissingValueBucketing} se questo attributo risulta vuoto. */
	public int getMissingValueTokenCount() {
		return this.missingValueTokenCount;
	}

	public void setMissingValueTokenCount(int missingValueTokenCount) {
		this.missingValueTokenCount = missingValueTokenCount;
	}

}