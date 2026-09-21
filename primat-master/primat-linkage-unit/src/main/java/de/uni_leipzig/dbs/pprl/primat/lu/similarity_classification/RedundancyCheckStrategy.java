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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategy;

/**
 * 
 * @author mfranke
 *
 */
public enum RedundancyCheckStrategy{
	
	MATCH_TWICE {
		@Override
		public <T extends Linkable> boolean isChecked(LinkageResultPartition<T> linkageResult, Record left, T right) {
			return false;
		}
	},
	DONT_MATCH_TWICE {
		@Override
		public <T extends Linkable> boolean isChecked(LinkageResultPartition<T> linkageResult, Record left, T right) {
			final MatchStrategy<T> matches = linkageResult.getMatchStrategy();

			if (matches.isContained(left, right)) {
				return true;
			}
			else {
				final NonMatchStrategy<T> nonMatches = linkageResult.getNonMatchStrategy();

				if (nonMatches.isContained(left, right)) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		
	};

	public abstract <T extends Linkable> boolean isChecked(LinkageResultPartition<T> linkageResult, Record left, T right);

}