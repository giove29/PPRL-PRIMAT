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

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * 
 * @author mfranke
 *
 */
public class IdMiddlePartHyphenTrueMatchChecker implements TrueMatchChecker {

	@Override
	public boolean isTrueMatch(Record left, Linkable right) {
		final String idLeft = left.getUniqueIdentifier();
		final String idRight = right.getUniqueIdentifier();

		try {
			final String partLeft = idLeft.split("-")[1];
			final String partRight = idRight.split("-")[1];
			return partLeft.equals(partRight);
		}
		catch (IndexOutOfBoundsException e) {
		}

		return false;
	}

}