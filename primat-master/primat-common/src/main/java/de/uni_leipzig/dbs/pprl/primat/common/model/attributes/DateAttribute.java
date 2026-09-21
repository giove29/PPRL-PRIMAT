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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.persistence.Column;
import javax.persistence.Entity;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class DateAttribute extends QidAttribute<LocalDate> {

	@Column(name = "dv")
	private LocalDate value;

	public DateAttribute() {
	}

	public DateAttribute(LocalDate date) {
		this.value = date;
	}

	@Override
	public void setValueFromString(String value) throws AttributeParseException {

		if (value == null || value.isEmpty()) {
			this.value = null;
		}
		else {

			try {
				LocalDate date = LocalDate.parse(value);

				this.value = date;
			}
			catch (DateTimeParseException e) {
				throw new AttributeParseException();
			}
		}
	}

	@Override
	public DateAttribute newInstance() {
		return new DateAttribute(this.getValue());
	}

	@Override
	public AttributeType getType() {
		return QidAttributeType.DATE;
	}

	@Override
	public void accept(QidAttributeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public LocalDate getValue() {
		return this.value;
	}

	@Override
	public String getStringValue() {
		return this.value.toString();
	}

	@Override
	public void setValue(LocalDate value) {
		this.value = value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}
}