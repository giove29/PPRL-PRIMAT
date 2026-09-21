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
package de.uni_leipzig.dbs.pprl.primat.lu.blocking.standard;

import java.util.Collection;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.StreamingMode;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.BlockSpace;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;


/**
 * List of blocking keys is used. Logical OR (at least one common BK).
 * 
 * @author mfranke
 *
 */
public class StandardBlocker implements Blocker {

	private StreamingMode mode;

	public StandardBlocker() {
		this(StreamingMode.SEQUENTIAL);
	}

	public StandardBlocker(StreamingMode mode) {
		this.mode = mode;
	}

	@Override
	public Collection<Block> getBlocks(Map<Party, Collection<Record>> records) {
		final BlockSpace blocks = new BlockSpace();

		this.mode.set(records.entrySet().stream()).forEach(entry -> {
			final Party party = entry.getKey();
			final Collection<Record> dataset = entry.getValue();
			dataset.parallelStream().forEach(rec -> {
				rec.getBlockingKeys().parallelStream().forEach(bk -> {
					blocks.add(bk, party, rec);
				});
			});
		});

		return blocks.getBlocks();
	}

	@Override
	public String toString() {
		return "Standard Blocking";
	}
}