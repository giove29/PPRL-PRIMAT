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

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;

/**
 * Class representing a attribute of a {@link Record}.
 * 
 * @author mfranke
 *
 * @param  <T> Attribute type.
 */
public abstract class Attribute<T> {

	public abstract Attribute<T> newInstance();

	public abstract T getValue();

	public abstract String getStringValue();

	public abstract void setValue(T value);

	public abstract void setValueFromString(String value);

	public abstract void accept(AttributeVisitor visitor);

	public abstract boolean isNull();

	public abstract AttributeType getType();

	@Override
	public String toString() {
		return this.getStringValue();
	}
}