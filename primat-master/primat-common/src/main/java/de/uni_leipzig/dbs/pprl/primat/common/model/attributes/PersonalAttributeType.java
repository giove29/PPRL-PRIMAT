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

/**
 * 
 * @author mfranke
 *
 */
public enum PersonalAttributeType {
	ID("ID"), 
	GLOBAL_ID("GID"),
	
	PARTY("PARTY"),
	
	FIRST_NAME("FN"), 
	MIDDLE_NAME("MN"),
	LAST_NAME("LN"), 
	LAST_NAME_AT_BIRTH("LNAB"),
	
	DAY_OF_BIRTH("DOB"),
	MONTH_OF_BIRTH("MOB"),
	YEAR_OF_BIRTH("YOB"),
	DATE_OF_BIRTH("BD"),
	
	COUNTRY("COUNTRY"),
	CITY("CITY"),
	STREET("STREET"),
	ZIP("ZIP"), 
	
	SEX("SEX"),
	ETHNICITY("ETHNICITY");

	private final String name;

	private PersonalAttributeType(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public static PersonalAttributeType from(String s) {
		for (final PersonalAttributeType at : PersonalAttributeType.values()) {
			if (at.getName().equalsIgnoreCase(s)) {
				return at;
			}
		}
		return null;
	}
}