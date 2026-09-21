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

/**
 * The functions of this class are adapted from
 * 
 * Diff Match and Patch https://github.com/google/diff-match-patch By Neil
 * Fraser
 * 
 */
public class Diff {

	private DiffOperation operation;
	private String text;

	public Diff(DiffOperation operation, String text) {
		this.operation = operation;
		this.text = text;
	}

	public DiffOperation getOperation() {
		return operation;
	}

	public void setOperation(DiffOperation operation) {
		this.operation = operation;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String toString() {
		return "Diff(" + this.operation + ",\"" + this.text + "\")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = (operation == null) ? 0 : operation.hashCode();
		result += prime * ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}
		Diff other = (Diff) obj;

		if (operation != other.operation) {
			return false;
		}

		if (text == null) {

			if (other.text != null) {
				return false;
			}
		}
		else if (!text.equals(other.text)) {
			return false;
		}
		return true;
	}
}