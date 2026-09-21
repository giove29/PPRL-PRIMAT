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

import javax.persistence.Column;
import javax.persistence.Entity;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class StringAttribute extends QidAttribute<String> {

	@Column(name = "sv")
	private String value;

	public StringAttribute() {
	}

	public StringAttribute(String value) {
		this.value = value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public StringAttribute newInstance() {
		return new StringAttribute(this.getValue());
	}

	@Override
	public AttributeType getType() {
		return QidAttributeType.STRING;
	}

	@Override
	public void accept(QidAttributeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void setValueFromString(String value) {
		this.setValue(value);
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public String getStringValue() {
		return this.value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}
}