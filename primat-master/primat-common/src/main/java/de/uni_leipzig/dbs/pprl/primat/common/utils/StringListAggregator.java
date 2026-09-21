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

package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.Collections;
import java.util.List;


/**
 * Common functions for aggregating a list of string values.
 * 
 * @author mfranke
 *
 */
public enum StringListAggregator implements ListAggregator<String> {

	FIRST, MIN, MAX, LAST, CONCAT;

	public String aggregate(List<String> values) {
		if (this == FIRST) {
			return values.get(0);
		}
		else if (this == LAST) {
			return values.get(values.size());
		}
		else if (this == MIN) {
			return Collections.min(values);
		}
		else if (this == MAX) {
			return Collections.max(values);
		}
		else if (this == CONCAT) {
			return String.join("", values);
		}
		else {
			return null;
		}
	}
}