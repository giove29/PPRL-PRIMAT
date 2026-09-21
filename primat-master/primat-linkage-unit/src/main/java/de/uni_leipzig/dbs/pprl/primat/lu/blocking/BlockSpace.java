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
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;


/**
 * The block space is a collections of blocks that are build from a blocking
 * approach.
 * 
 * @author mfranke
 *
 */
public class BlockSpace {

	private Map<BlockingKeyAttribute, Block> blocks;

	public BlockSpace() {
		this.blocks = new HashMap<>();
	}

	// TODO: really synchronized?
	public synchronized void add(BlockingKeyAttribute bk, Party party, Record record) {
		Block block = this.blocks.get(bk);

		if (block == null) {
			block = new Block();
			block.add(party, record);
			this.blocks.put(bk, block);
		}
		else {
			block.add(party, record);
		}
	}

	public Set<BlockingKeyAttribute> getBlockingKeys() {
		return this.blocks.keySet();
	}

	public int getNumberOfBlockingKeys() {
		return this.blocks.keySet().size();
	}

	public Collection<Block> getBlocks() {
		return this.blocks.values();
	}
}