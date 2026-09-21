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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;

/**
 * 
 * @author mfranke
 *
 */
public class SimpleClusterBuilder implements ClusterBuilder {

	int currentClusterId;

	public SimpleClusterBuilder() {
		this.currentClusterId = 0;
	}

	@Override
	public Set<Cluster> getCluster(LinkageResult<Record> result) {
		final Set<Cluster> clusterSet = new HashSet<Cluster>();

		final Map<Record, Cluster> recClusterMap = new HashMap<>();

		for (final LinkedPair<Record> match : result.getMatches()) {
			final Record left = match.getLeftRecord();
			final Record right = match.getRight();

			final Cluster clusterLeft = recClusterMap.get(left);
			final Cluster clusterRight = recClusterMap.get(right);

			final Cluster clusterAssign;

			if (clusterLeft == null && clusterRight == null) {
				clusterAssign = new Cluster();
				clusterAssign.setId(currentClusterId);
				currentClusterId++;
				clusterAssign.addRecord(left);
				clusterAssign.addRecord(right);
			}
			else if (clusterLeft != null && clusterRight == null) {
				clusterAssign = clusterLeft;
				clusterAssign.addRecord(right);
			}
			else if (clusterLeft == null && clusterRight != null) {
				clusterAssign = clusterRight;
				clusterAssign.addRecord(left);
			}
			else {
				if (clusterLeft.equals(clusterRight)) {
					continue;
				}
				else {
					clusterAssign = clusterLeft;
					final Set<Record> recordsToMove = clusterRight.getRecords();
					clusterAssign.addRecords(recordsToMove);

					for (final Record rec : recordsToMove) {
						recClusterMap.put(rec, clusterAssign);
					}
				}
			}

			recClusterMap.put(left, clusterAssign);
			recClusterMap.put(right, clusterAssign);
		}

		return clusterSet;
	}
}