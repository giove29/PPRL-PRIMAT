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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing;

/**
 * Extract a substring.
 * 
 * @author mfranke
 *
 */
public class SubstringNormalizer implements Normalizer {

	private int start;
	private int end;

	public SubstringNormalizer(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	@Override
	public String normalize(String string) {
		int end = this.end;

		if (end >= string.length()) {
			end = string.length();
		}

		if (end == 0) {
			return "";
		}

		return string.substring(start, end);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.start + "," + this.end + ")";
	}
}