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

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;


/**
 * 
 * @author mfranke
 *
 */
@Embeddable
public final class IdAttribute extends Attribute<String> implements Serializable {

	private static final long serialVersionUID = 1275196624862512488L;

	@Column(name = "id")
	private String value;

	public IdAttribute() {
	}

	public IdAttribute(String id) {
		this.value = id;
	}

	public static String getRandomId() {
		return UUID.randomUUID().toString();
	}

	@Override
	public AttributeType getType() {
		return NonQidAttributeType.ID;
	}

	@Override
	public IdAttribute newInstance() {
		return new IdAttribute();
	}

	@Override
	public void setValueFromString(String value) {
		this.setValue(value);
	}

	@Override
	public void accept(AttributeVisitor visitor) {
		visitor.visit(this);
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
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		IdAttribute other = (IdAttribute) obj;

		if (value == null) {
			if (other.value != null)
				return false;
		}
		else if (!value.equals(other.value))
			return false;
		return true;
	}
}
