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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.vladmihalcea.hibernate.type.array.IntArrayType;


/**
 * 
 * @author mfranke
 *
 */
@Entity
@TypeDefs({
	@TypeDef(name = "int-array", typeClass = IntArrayType.class)
})
public class IntegerSetAttribute extends QidAttribute<Set<Integer>> {

	@Transient
	private Set<Integer> value;

	public IntegerSetAttribute() {
	}

	public IntegerSetAttribute(Set<Integer> set) {
		this.value = set;
	}

	@Access(AccessType.PROPERTY)
	@Type(type = "int-array")
	@Column(name = "isv", columnDefinition = "INTEGER[]")
	public Integer[] getValues() {
		return this.value.toArray(new Integer[0]);
	}

	public void setValues(Integer[] values) {
		this.value = new HashSet<>(Arrays.asList(values));
	}

	@Override
	public IntegerSetAttribute newInstance() {
		return new IntegerSetAttribute(this.value);
	}

	@Override
	public void setValueFromString(String value) {

		if (value == null || value.isEmpty()) {
			this.value = null;
		}
		else {

			try {
				this.value = new HashSet<>();
				final String[] strInts = value.substring(1, value.length() - 1).replaceAll("\\s+", "").split(",");

				for (String strInt : strInts) {
					this.value.add(Integer.valueOf(strInt));
				}

			}
			catch (NumberFormatException e) {
				throw new AttributeParseException();
			}
		}
	}

	@Override
	public AttributeType getType() {
		return QidAttributeType.INTEGER_SET;
	}

	@Override
	public void accept(QidAttributeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public Set<Integer> getValue() {
		return this.value;
	}

	@Override
	public String getStringValue() {
		return this.value.toString();
	}

	@Override
	public void setValue(Set<Integer> value) {
		this.value = value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}
}
