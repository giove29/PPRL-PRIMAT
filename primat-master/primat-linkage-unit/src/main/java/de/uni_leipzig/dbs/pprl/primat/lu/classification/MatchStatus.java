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
package de.uni_leipzig.dbs.pprl.primat.lu.classification;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;


/**
 * 
 * @author mfranke
 *
 */
public enum MatchStatus {

	DEFINITE_MATCH {
		@Override
		public <T extends Linkable> void handle(LinkageResultPartition<T> part, LinkedPair<T> pair) {
			part.addMatch(pair);
		}
	},

	POSSIBLE_MATCH {
		@Override
		public <T extends Linkable> void handle(LinkageResultPartition<T> part, LinkedPair<T> pair) {
			part.addMatch(pair);
		}
	},

	POSSIBLE_NON_MATCH {
		@Override
		public <T extends Linkable> void handle(LinkageResultPartition<T> part, LinkedPair<T> pair) {
			part.addNonMatch(pair);
		}
	},

	NON_MATCH {
		@Override
		public <T extends Linkable> void handle(LinkageResultPartition<T> part, LinkedPair<T> pair) {
			part.addNonMatch(pair);
		}
	},

	UNKNOWN {
		@Override
		public <T extends Linkable> void handle(LinkageResultPartition<T> part, LinkedPair<T> pair) {
			part.addNonMatch(pair);
		}
	};

	public boolean isAtLeast(MatchStatus reference) {
		return this.ordinal() <= reference.ordinal();
	}

	public abstract <T extends Linkable> void handle(LinkageResultPartition<T> part, LinkedPair<T> pair);
}
