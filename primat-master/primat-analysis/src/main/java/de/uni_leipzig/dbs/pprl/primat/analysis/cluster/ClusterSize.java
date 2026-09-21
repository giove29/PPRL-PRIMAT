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
package de.uni_leipzig.dbs.pprl.primat.analysis.cluster;


import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Analyze the cluster of records belonging to the same real world entity by
 * their size
 * 
 * @author frohde
 */
public class ClusterSize extends ClusterAnalyzer {

	@Override
	public ResultSet analyze(Map<String, List<Record>> clusters) {
		ResultSet resultSet = getResultSet();
		DescriptiveStatistics stats = new DescriptiveStatistics();

		for (Map.Entry<String, List<Record>> cluster : clusters.entrySet()) {
			stats.addValue(cluster.getValue().size());
		}
		Result result = new Result();
		addDescriptiveStatisticMetrics(result, stats, Arrays.asList("count", "median", "mean", "min", "max", "sd"));
		resultSet.addResult(result);
		return resultSet;
	}
}
