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

import java.util.List;


/**
 * This interface provides functionality for aggregating a
 * {@link List} of values into a single value.
 * 
 * 
 * @author mfranke
 *
 * @param <T> Type of the objects in the {@link List}
 */
public interface ListAggregator<T> {

	/**
	 * Aggregates the values of the list into a single value
	 * of the same type.
	 * 
	 * @param values a {@link List} of values
	 * @return aggregated value
	 */
	public T aggregate(List<T> values);
	
}