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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.Timed;


/**
 * Wrapper class for blockers to measure the execution time of the blocking
 * process.
 * 
 * @author mfranke
 *
 */
public class TimedBlocker implements Blocker, Timed {

	private Blocker blocker;

	private long executionTime;

	public TimedBlocker(Blocker blocker) {
		this.blocker = blocker;
	}

	@Override
	public long getExecutionTime() {
		return this.executionTime;
	}

	@Override
	public Collection<Block> getBlocks(Map<Party, Collection<Record>> records) {
		final StopWatch stopWatch = StopWatch.createStarted();
		final Collection<Block> blocks = blocker.getBlocks(records);
		this.executionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		return blocks;
	}
}