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
package de.uni_leipzig.dbs.pprl.primat.lu.blocking.lsh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.LshBlockingFunction;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.LshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.StreamingMode;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.standard.StandardBlocker;


/**
 * 
 * @author mfranke
 *
 */
public class LshBlocker extends StandardBlocker {

	private StreamingMode mode;
	private StandardBlocker blocker;
	private LshKeyGenerator keyGenerator;

	public LshBlocker(LshKeyGenerator keyGenerator) {
		this(new StandardBlocker(StreamingMode.SEQUENTIAL), keyGenerator, StreamingMode.SEQUENTIAL);
	}

	public LshBlocker(LshKeyGenerator keyGenerator, StreamingMode mode) {
		this(new StandardBlocker(mode), keyGenerator, mode);
	}

	private LshBlocker(StandardBlocker blocker, LshKeyGenerator keyGenerator, StreamingMode mode) {
		this.blocker = blocker;
		this.keyGenerator = keyGenerator;
		this.mode = mode;
	}

	private void calculateLshBlockingKeys(List<Record> records, List<LshBlockingFunction> lshKeys) {
		this.mode.set(records.stream()).forEach(record -> {
			// TODO: not only the first one
			final BitSetAttribute bsAttr = record.getBitSetAttributes().get(0);

			for (int i = 0; i < lshKeys.size(); i++) {
				final LshBlockingFunction key = lshKeys.get(i);
				final String bk = key.apply(bsAttr);
				final BlockingKeyAttribute bkAttr = new BlockingKeyAttribute(i, bk);
				record.addBlockingKey(bkAttr);
			}
		});
	}

	@Override
	public Collection<Block> getBlocks(Map<Party, Collection<Record>> records) {
		final List<LshBlockingFunction> lshKeys = this.keyGenerator.generate();
		final List<Record> allRecords = new ArrayList<>();

		for (final Collection<Record> dataset : records.values()) {
			allRecords.addAll(dataset);
		}

		this.calculateLshBlockingKeys(allRecords, lshKeys);

		return this.blocker.getBlocks(records);
	}

	@Override
	public String toString() {
		return "LSH " + keyGenerator.toString();
	}
}