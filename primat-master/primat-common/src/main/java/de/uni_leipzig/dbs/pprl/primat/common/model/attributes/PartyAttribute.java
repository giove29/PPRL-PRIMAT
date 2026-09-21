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

package de.uni_leipzig.dbs.pprl.primat.common.model.attributes;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;


/**
 * 
 * @author mfranke
 *
 */
public class PartyAttribute extends Attribute<Party> {

	private Party value;

	public PartyAttribute() {
	}

	public PartyAttribute(Party party) {
		this.value = party;
	}

	@Override
	public Attribute<Party> newInstance() {
		return new PartyAttribute();
	}

	@Override
	public AttributeType getType() {
		return NonQidAttributeType.PARTY;
	}

	@Override
	public void setValueFromString(String value) {
		this.setValue(new Party(value));
	}

	@Override
	public void accept(AttributeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public Party getValue() {
		return this.value;
	}

	@Override
	public String getStringValue() {

		if (this.value != null) {
			return this.value.getName();
		}
		else {
			return null;
		}
	}

	@Override
	public void setValue(Party value) {
		this.value = value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}
}