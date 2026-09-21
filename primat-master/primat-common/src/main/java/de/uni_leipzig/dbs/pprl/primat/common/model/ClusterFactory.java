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

package de.uni_leipzig.dbs.pprl.primat.common.model;

/**
 * 
 * @author mfranke
 *
 */
public class ClusterFactory {

	private final ClusterBlockingKeyStrategy bkStrategy;
	private final ClusterRepresentantStrategy repStrategy;

	public ClusterFactory() {
		this(ClusterBlockingKeyStrategy.UNION, ClusterRepresentantStrategy.RETAIN_FIRST);
	}

	public ClusterFactory(ClusterBlockingKeyStrategy bkStrategy, ClusterRepresentantStrategy repStrategy) {
		this.bkStrategy = bkStrategy;
		this.repStrategy = repStrategy;
	}

	public Cluster build() {
		return new Cluster(this.bkStrategy, this.repStrategy);
	}

	public Cluster build(Record record) {
		final Cluster cluster = this.build();
		cluster.addRecord(record);
		return cluster;
	}

	public ClusterBlockingKeyStrategy getClusterBlockingKeyStrategy() {
		return this.bkStrategy;
	}

	public ClusterRepresentantStrategy getClusterRepresentantStrategy() {
		return this.repStrategy;
	}
}