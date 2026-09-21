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

package de.uni_leipzig.dbs.pprl.primat.common.model;

import de.uni_leipzig.dbs.pprl.primat.common.utils.HomogenPair;


/**
 * 
 * @author mfranke
 *
 */
public class PartyPair {

	private Party leftParty;
	private Party rightParty;

	public PartyPair(HomogenPair<Party> partyPair) {
		this(partyPair.getLeft(), partyPair.getRight());
	}

	public PartyPair(Party leftParty, Party rightParty) {
		this.leftParty = leftParty;
		this.rightParty = rightParty;
	}

	public boolean isIdentityPair() {
		return this.leftParty.equals(this.rightParty);
	}

	public LinkageConstraint getLinkageConstraint() {

		if (leftParty.isDuplicateFree() && rightParty.isDuplicateFree()) {
			return LinkageConstraint.ONE_TO_ONE;
		}
		else if (leftParty.isDuplicateFree() && !rightParty.isDuplicateFree()) {
			return LinkageConstraint.ONE_TO_MANY;
		}
		else if (!leftParty.isDuplicateFree() && rightParty.isDuplicateFree()) {
			return LinkageConstraint.MANY_TO_ONE;
		}
		else {
			return LinkageConstraint.MANY_TO_MANY;
		}
	}

	public Party getLeftParty() {
		return leftParty;
	}

	public void setLeftParty(Party leftParty) {
		this.leftParty = leftParty;
	}

	public Party getRightParty() {
		return rightParty;
	}

	public void setRightParty(Party rightParty) {
		this.rightParty = rightParty;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((leftParty == null) ? 0 : leftParty.hashCode());
		result = prime * result + ((rightParty == null) ? 0 : rightParty.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PartyPair other = (PartyPair) obj;

		if (leftParty == null) {
			if (other.leftParty != null)
				return false;
		}
		else if (!leftParty.equals(other.leftParty))
			return false;

		if (rightParty == null) {
			if (other.rightParty != null)
				return false;
		}
		else if (!rightParty.equals(other.rightParty))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[ ");
		builder.append(leftParty);
		builder.append(", ");
		builder.append(rightParty);
		builder.append("]");
		return builder.toString();
	}
}