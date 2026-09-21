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
public enum ClusterRepresentantStrategy {

	RETAIN_FIRST {
		@Override
		void update(Record record, Cluster cluster) {

			if (cluster.getPhysicalRepresentant() == null) {
				cluster.setPhysicalRepresentant(record);
			}
			else {
				// nothing to do here
			}
		}
	},

	REPLACE {
		@Override
		void update(Record record, Cluster cluster) {
			cluster.setPhysicalRepresentant(record);
		}
	};

	// TODO: Use record with highest average similarity to other cluster members

	abstract void update(Record record, Cluster cluster);
}