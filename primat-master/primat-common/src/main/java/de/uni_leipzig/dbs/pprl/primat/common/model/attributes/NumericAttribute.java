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

import java.text.NumberFormat;
import java.text.ParseException;

import javax.persistence.Column;
import javax.persistence.Entity;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class NumericAttribute extends QidAttribute<Number> {

	@Column(name = "nv")
	private Number value;

	public NumericAttribute() {
	}

	public NumericAttribute(Number value) {
		this.value = value;
	}

	@Override
	public void setValueFromString(String value) {

		if (value != null && !value.isEmpty()) {

			try {
				final Number number = NumberFormat.getInstance().parse(value);
				this.value = number;
			}
			catch (ParseException e) {
				throw new AttributeParseException();
			}
		}
		else {
			this.value = null;
		}
	}

	@Override
	public NumericAttribute newInstance() {
		return new NumericAttribute(this.getValue());
	}

	@Override
	public AttributeType getType() {
		return QidAttributeType.NUMERIC;
	}

	@Override
	public void accept(QidAttributeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public Number getValue() {
		return this.value;
	}

	@Override
	public String getStringValue() {
		return this.value.toString();
	}

	@Override
	public void setValue(Number value) {
		this.value = value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}
}