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
package de.uni_leipzig.dbs.pprl.primat.lu.blocking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * A block is a collection of records that agree on a specific blocking key.
 * 
 * @author mfranke
 *
 */
public class Block {

	private final Map<Party, SubBlock> blocksByParty;

	public Block() {
		this.blocksByParty = new HashMap<Party, SubBlock>();
	}

	private void addSubBlockIfNotPresent(Party party) {

		if (!this.blocksByParty.containsKey(party)) {
			this.blocksByParty.put(party, new SubBlock());
		}
	}

	public void add(Party party, Record record) {
		this.addSubBlockIfNotPresent(party);
		final SubBlock partySubBlock = this.blocksByParty.get(party);
		partySubBlock.add(record);
	}

	public void addAll(Party party, Collection<Record> record) {
		this.addSubBlockIfNotPresent(party);
		final SubBlock partySubBlock = this.blocksByParty.get(party);
		partySubBlock.addAll(record);
	}

	public Set<Party> getParties() {
		return this.blocksByParty.keySet();
	}

	public SubBlock getPartySubBlock(Party party) {
		return this.blocksByParty.get(party);
	}

	public Collection<SubBlock> getSubBlocks() {
		return this.blocksByParty.values();
	}

	public int getSize() {
		return this.blocksByParty.values().stream().mapToInt(i -> i.getSize()).sum();
	}
}