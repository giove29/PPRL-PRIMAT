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

import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.SetUtils;


/**
 * 
 * @author mfranke
 *
 */
public enum ClusterBlockingKeyStrategy {

	RETAIN_FIRST {
		@Override
		void update(Record record, Cluster cluster) {

			if (cluster.getBlockingKeys().isEmpty()) {
				cluster.setBlockingKeys(record.getBlockingKeys());
			}
			else {
				// nothing to do here
			}
		}
	},

	UNION {
		@Override
		void update(Record record, Cluster cluster) {
			final Set<BlockingKeyAttribute> recordBks = record.getBlockingKeys();
			final Set<BlockingKeyAttribute> clusterBks = cluster.getBlockingKeys();
			final Set<BlockingKeyAttribute> mergedBks = SetUtils.union(recordBks, clusterBks);
			cluster.setBlockingKeys(mergedBks);
		}
	},

	INTERSECTION {
		@Override
		void update(Record record, Cluster cluster) {
			final Set<BlockingKeyAttribute> recordBks = record.getBlockingKeys();
			final Set<BlockingKeyAttribute> clusterBks = cluster.getBlockingKeys();
			final Set<BlockingKeyAttribute> mergedBks = SetUtils.intersection(recordBks, clusterBks);
			cluster.setBlockingKeys(mergedBks);
		}
	},

	REPRESENTANT {
		@Override
		void update(Record record, Cluster cluster) {
			final Set<BlockingKeyAttribute> bks = cluster.getPhysicalRepresentant().getBlockingKeys();
			cluster.setBlockingKeys(bks);
		}
	};

	abstract void update(Record record, Cluster cluster);

}