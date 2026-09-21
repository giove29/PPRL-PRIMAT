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
package de.uni_leipzig.dbs.pprl.primat.analysis.results;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.analysis.utils.CommandLineTable;

/**
 * 
 * @author frohde
 *
 */
public class ResultSet {
	private String name;
	private String description;
	private List<Result> results;
	private Map<String, Table> additionalResults;

	public ResultSet(String name) {
		this.name = name;
		this.description = "";
		this.results = new ArrayList<>();
		this.additionalResults = new LinkedHashMap<>();
	}

	public void addResult(Result result) {
		results.add(result);
	}

	public List<Result> getResults() {
		return results;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void addAdditionalResult(Table result) {
		additionalResults.put(result.name(), result);
	}

	public Map<String, Table> getAdditionalResults() {
		return additionalResults;
	}

	public Table getAsTable() {
		return getAsTable(false);
	}

	public Table getAsTable(boolean roundMetrics) {
		List<String> paramNames = getParameterNames();
		List<String> metricNames = getMetricNames();
		Table table = Table.create(name);
		paramNames.forEach(s -> table.addColumns(StringColumn.create(s)));
		metricNames.forEach(s -> table.addColumns(DoubleColumn.create(s)));

		for (Result result : results) {
			paramNames.forEach(n -> table.stringColumn(n).append(result.getParams().get(n)));
			metricNames.forEach(n -> {
				BigDecimal value = result.getMetrics().get(n);
				value = value.setScale(4, RoundingMode.HALF_UP);
				table.doubleColumn(n).append(value.doubleValue());
			});
		}
		return table;
	}

	public void store(String path) throws IOException {

		if (results.isEmpty()) {
			System.out.println("Skip storing results for " + name + " as there are none.");
			return;
		}
		getAsTable().write().toFile(path);
	}

	public String getResultSummary() {
		final StringBuilder sb = new StringBuilder();
		sb.append("# ").append(name).append("\n");

		if (!description.isEmpty()) {
			sb.append(description).append("\n");
		}
		CommandLineTable st = new CommandLineTable();
		st.setShowVerticalLines(true);
		st.setRightAlign(true);
		st.setHeaders(buildHeader());

		for (Result result : results) {
			st.addRow(buildResultRow(result, 4));
		}
		sb.append(st.build());
		return sb.toString();
	}

	private List<String> buildHeader() {
		final List<String> header = new ArrayList<>();
		header.addAll(getParameterNames());
		header.addAll(getMetricNames());
		return header;
	}

	private List<String> buildResultRow(Result result, int scale) {
		List<String> row = new ArrayList<>();

		for (String parameterName : getParameterNames()) {
			row.add(result.getParams().get(parameterName));
		}

		for (String metricName : getMetricNames()) {
			BigDecimal metricValue = result.getMetrics().get(metricName);

			if (metricValue.scale() > 0) {
				metricValue = metricValue.setScale(scale, RoundingMode.HALF_UP);
			}
			row.add(metricValue.toString());
		}
		return row;
	}

	private List<String> getParameterNames() {
		return results.stream().flatMap(r -> r.getParams().keySet().stream()).distinct().collect(Collectors.toList());
	}

	private List<String> getMetricNames() {
		return results.stream().flatMap(r -> r.getMetrics().keySet().stream()).distinct().collect(Collectors.toList());
	}

}