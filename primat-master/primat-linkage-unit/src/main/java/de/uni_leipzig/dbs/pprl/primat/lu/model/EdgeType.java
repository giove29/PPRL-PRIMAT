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
package de.uni_leipzig.dbs.pprl.primat.lu.model;

/**
 * 
 * @author mfranke
 *
 */
public enum EdgeType {

	ONE_TO_ONE_LINK,
	ONE_TO_MANY_LINK,
	MANY_TO_ONE_LINK,
	MANY_TO_MANY_LINK;
	
	
	public static EdgeType getType(int source, int target) {
		if (source <= 0 || target <= 0) {
			throw new IllegalArgumentException();
		}
		else if (source == 1 && target == 1) {
			return ONE_TO_ONE_LINK;
		}
		else if (source == 1 && target > 1) {
			return ONE_TO_MANY_LINK;
		}
		else if (source > 1 && target == 1) {
			return MANY_TO_ONE_LINK;
		}
		else {
			return MANY_TO_MANY_LINK;
		}
	}	
}