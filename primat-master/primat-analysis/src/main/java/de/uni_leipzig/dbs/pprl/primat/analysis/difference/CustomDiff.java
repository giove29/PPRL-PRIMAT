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
package de.uni_leipzig.dbs.pprl.primat.analysis.difference;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author mfranke
 *
 */
public class CustomDiff {
	private CustomDiffOperation operation;
	private String s0;
	private String s1;

	public CustomDiff(CustomDiffOperation operation, String s0, String s1) {
		this.operation = operation;
		this.s0 = s0;
		this.s1 = s1;
	}
	
	

	public CustomDiffOperation getOperation() {
		return operation;
	}
	
	public void setOperation(CustomDiffOperation operation) {
		this.operation = operation;
	}

	public String getS0() {
		return s0;
	}

	public void setS0(String s0) {
		this.s0 = s0;
	}

	public String getS1() {
		return s1;
	}

	public void setS1(String s1) {
		this.s1 = s1;
	}

	public String toString() {
		switch (operation) {
			case EQUAL:
				return StringUtils.repeat("?", s0.length());
			case INSERT:
				return wrap(s0);
			case SWAP:
				return wrap(s0 + "<>" + s1);
			case REPLACEMENT:
				return wrap(s0 + "/" + s1);
			default:
				return "INVALIDOP";
		}
	}

	private String wrap(String in) {
		return "[" + in + "]";
	}
}