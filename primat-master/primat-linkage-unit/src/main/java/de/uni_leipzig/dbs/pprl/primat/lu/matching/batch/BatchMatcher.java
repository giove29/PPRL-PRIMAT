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

package de.uni_leipzig.dbs.pprl.primat.lu.matching.batch;

import java.util.Collection;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.Matcher;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.SimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ThresholdClassificationRefinement;


/**
 * 
 * @author mfranke
 *
 */
public class BatchMatcher implements Matcher<Record> {

	private final Blocker blocker;
	private final SimilarityClassification simClass;
	private final ThresholdClassificationRefinement threshRef;
	private final PostprocessingStrategy<Record> postprocessor;

	public BatchMatcher(Blocker blocker, SimilarityClassification simClass, ThresholdClassificationRefinement threshRef,
		PostprocessingStrategy<Record> postprocessor) {
		this.blocker = blocker;
		this.simClass = simClass;
		this.threshRef = threshRef;
		this.postprocessor = postprocessor;
	}

	@Override
	public LinkageResult<Record> match(Map<Party, Collection<Record>> input) {
		final Collection<Block> blocks = this.blocker.getBlocks(input);

		final LinkageResult<Record> linkageResult = this.simClass.classifyRecords(input.keySet(), blocks);
		
		this.threshRef.refine(linkageResult);

		for (final LinkageResultPartition<Record> part : linkageResult.getPartitions()) {
			this.postprocessor.clean(part);
		}

		return linkageResult;
	}
}