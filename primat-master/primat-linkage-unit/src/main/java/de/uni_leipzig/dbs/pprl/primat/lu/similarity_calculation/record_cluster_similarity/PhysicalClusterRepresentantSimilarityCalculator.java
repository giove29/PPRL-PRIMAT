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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_cluster_similarity;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_calculation.record_similarity.RecordSimilarityCalculator;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public class PhysicalClusterRepresentantSimilarityCalculator implements RecordClusterSimilarityCalculator {

	private final RecordSimilarityCalculator simCalc;

	public PhysicalClusterRepresentantSimilarityCalculator(RecordSimilarityCalculator simCalc) {
		this.simCalc = simCalc;
	}

	@Override
	public SimilarityVector calculateSimilarity(Record left, Cluster right) {
		return simCalc.calculateSimilarity(left, right.getPhysicalRepresentant());
	}
}