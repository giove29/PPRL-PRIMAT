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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging;

import java.util.StringJoiner;


/**
 * {@link Merger} that uses a separator string for the merging process.
 * 
 * @author mfranke
 *
 */
public class SimpleMerger implements Merger {

	private final String separator;

	/**
	 * Creates a new simple merger.
	 * 
	 * @param separator the separator to use for the merging process.
	 */
	public SimpleMerger(String separator) {
		this.separator = separator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String merge(String[] string) {
		final StringJoiner joiner = new StringJoiner(separator);

		for (final String s : string) {
			joiner.add(s);
		}
		return joiner.toString();
	}
}