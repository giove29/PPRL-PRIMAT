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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.clustering;

import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.PostprocessingStrategy;

/**
 * 
 * @author mfranke
 *
 */
public class DefaultLocalViewClustering extends LocalViewClustering{
	
	final ClusterBuilder clusterBuilder;
	final PostprocessingStrategy<Record> postprocessor;

	public DefaultLocalViewClustering(PostprocessingStrategy<Record> postprocessor, ClusterBuilder clusterBuilder) {
		this.clusterBuilder = clusterBuilder;
		this.postprocessor = postprocessor;
	}

	@Override
	public Set<Cluster> cluster(LinkageResult<Record> linkageResult) {

		for (LinkageResultPartition<Record> partition : linkageResult.getPartitions()) {
			postprocessor.clean(partition);
		}
		
		// TODO: Transitivity check?
		/*
		 * Currently only each partition is post-processed independently.
		 * As a consequence, the output are record pairs that might
		 * break transitivity, i.e., (a1,b1), (b1,c2), (a1,c1) where 
		 * tupel (b1,c2) should be deleted.
		 *  
		 */
		
		return clusterBuilder.getCluster(linkageResult);
	}
}