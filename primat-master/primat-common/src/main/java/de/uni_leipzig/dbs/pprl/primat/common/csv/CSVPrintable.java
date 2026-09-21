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

package de.uni_leipzig.dbs.pprl.primat.common.csv;

/**
 * By implementing this interface it is specified how a 
 * class is printed as csv record.
 * 
 * @author mfranke
 *
 */
public interface CSVPrintable {

	/**
	 * Return the values to be printed as {@link Iterable}. For each object the
	 * <code>toString()</code> method will be called for printing.
	 * 
	 * @return {@link Iterable} object.
	 */
	public Iterable<?> getValuesToPrint();

}