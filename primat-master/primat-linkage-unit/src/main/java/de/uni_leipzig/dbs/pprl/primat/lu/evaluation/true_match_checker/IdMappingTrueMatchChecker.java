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
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker;

import java.util.Collection;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * 
 * @author mfranke
 *
 */
public class IdMappingTrueMatchChecker implements TrueMatchChecker {

	private final Map<String, Collection<String>> matchMapping;

	public IdMappingTrueMatchChecker(Map<String, Collection<String>> matchMapping) {
		this.matchMapping = matchMapping;
	}

	@Override
	public boolean isTrueMatch(Record left, Linkable right) {

		if (matchMapping.containsKey(left.getUniqueIdentifier())) {
			final Collection<String> matches = matchMapping.get(left.getId());
			return matches.contains(right.getUniqueIdentifier());
		}
		else {
			return false;
		}
	}
}