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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.Timed;


/**
 * 
 * @author mfranke
 *
 */
public class TimedPostprocessor<T extends Linkable> implements Postprocessor<T>, Timed {

	private Postprocessor<T> postprocessor;
	private long executionTime;

	public TimedPostprocessor(Postprocessor<T> postprocessor) {
		this.postprocessor = postprocessor;
	}

	@Override
	public long getExecutionTime() {
		return this.executionTime;
	}

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> linkageResult) {
		final StopWatch stopWatch = StopWatch.createStarted();
		final MatchStrategy<T> result = this.postprocessor.clean(linkageResult);
		this.executionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
		return result;
	}
}
