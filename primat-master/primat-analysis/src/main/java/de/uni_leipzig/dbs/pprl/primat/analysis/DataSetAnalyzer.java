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
package de.uni_leipzig.dbs.pprl.primat.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeAnalyzer;
import de.uni_leipzig.dbs.pprl.primat.analysis.cluster.ClusterAnalyzer;
import de.uni_leipzig.dbs.pprl.primat.analysis.record.RecordAnalyzer;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.RecordUtils;


public class DataSetAnalyzer {
	public static final String RECORD_GROUP_ALL = "all";

	private static final boolean DEFAULT_RUN_PER_SOURCE = false;

	private Map<String, List<ResultSet>> results;
	private boolean runPerSource;

	private List<RecordAnalyzer> recordAnalyzers;
	private List<ClusterAnalyzer> clusterAnalyzers;
	private List<AttributeAnalyzer> attributeAnalyzers;

	public DataSetAnalyzer() {
		this.results = new HashMap<>();
		this.runPerSource = DEFAULT_RUN_PER_SOURCE;
		recordAnalyzers = new ArrayList<>();
		clusterAnalyzers = new ArrayList<>();
		attributeAnalyzers = new ArrayList<>();
	}

	public AnalysisResult run(List<Record> records) {
		results.clear();

		if (records.isEmpty()) {
			System.out.println("Abort Analyzer because the list of input records is empty");
			return new AnalysisResult();
		}

		final List<ResultSet> curResults = new ArrayList<>();

		if (!recordAnalyzers.isEmpty()) {
			curResults.addAll(runRecordAnalyzers(records));
		}

		if (!clusterAnalyzers.isEmpty()) {
			curResults.addAll(runClusterAnalyzers(records));
		}

		if (!attributeAnalyzers.isEmpty()) {
			curResults.addAll(runAttributeAnalyzers(records));
		}
		results.put(RECORD_GROUP_ALL, curResults);

		if (runPerSource && RecordUtils.numberOfSources(records) > 1) {
			for (Map.Entry<String, List<Record>> group : RecordUtils.groupByParty(records).entrySet()) {
				results.put(group.getKey(), runAttributeAnalyzers(group.getValue()));
			}
		}
		return new AnalysisResult(results);
	}

	public List<ResultSet> getResults(String group) {
		return results.get(group);
	}

	private List<ResultSet> runRecordAnalyzers(List<Record> records) {
		List<ResultSet> results = new ArrayList<>();

		for (RecordAnalyzer recordAnalyzer : recordAnalyzers) {
			results.add(recordAnalyzer.analyze(records));
		}
		return results;
	}

	private List<ResultSet> runClusterAnalyzers(List<Record> records) {
		List<ResultSet> results = new ArrayList<>();
		Map<String, List<Record>> clusters = RecordUtils.groupByGlobalId(records);

		if (clusters.size() == records.size()) {
			System.out.println("Skipping cluster analyzers because all clusters are singletons");
		}
		else {

			for (ClusterAnalyzer clusterAnalyzer : clusterAnalyzers) {
				System.out.println("Running analyzer: " + clusterAnalyzer.getName());
				results.add(clusterAnalyzer.analyze(clusters));
			}
		}
		return results;
	}

	private List<ResultSet> runAttributeAnalyzers(List<Record> records) {

		int recordCount = records.size();
		final List<ResultSet> results = new ArrayList<>();
		final Map<Integer, List<QidAttribute<?>>> attributes = RecordUtils.groupByAttribute(records);

		for (AttributeAnalyzer attributeAnalyzer : attributeAnalyzers) {

			attributeAnalyzer.setRecordCount(recordCount);
			results.add(attributeAnalyzer.analyze(attributes));
		}
		return results;
	}

	public void addAnalyzers(List<Analyzer> analyzers) {
		analyzers.forEach(this::addAnalyzer);
	}

	public void addAnalyzer(Analyzer analyzer) {

		if (analyzer instanceof RecordAnalyzer) {
			recordAnalyzers.add((RecordAnalyzer) analyzer);
		}
		else if (analyzer instanceof ClusterAnalyzer) {
			clusterAnalyzers.add((ClusterAnalyzer) analyzer);
		}
		else if (analyzer instanceof AttributeAnalyzer) {
			attributeAnalyzers.add((AttributeAnalyzer) analyzer);
		}
	}

	public void setRunPerSource(boolean runPerSource) {
		this.runPerSource = runPerSource;
	}
}
