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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing;


import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * 
 * @author mfranke
 *
 */
public class PartySupplier implements Preprocessor {

	@Override
	public void preprocess(Record record) {
		final Party partyA = new Party("A");
		final Party partyB = new Party("B");

		final String id = record.getId();

		if (id.contains("org")) {
			record.setParty(partyA);
		}
		else {
			record.setParty(partyB);
		}
	}

	@Override
	public void updateSchema() {}
}