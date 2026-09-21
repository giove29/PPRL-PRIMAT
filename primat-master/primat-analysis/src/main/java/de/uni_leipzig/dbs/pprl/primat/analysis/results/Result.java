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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 
 * @author frohde
 *
 */
public class Result {
	protected Map<String, String> params;
	protected Map<String, BigDecimal> metrics;

	public Result() {
		params = new LinkedHashMap<>();
		metrics = new LinkedHashMap<>();
	}

	public void setParam(String key, String val) {
		params.put(key, val);
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Map<String, BigDecimal> getMetrics() {
		return metrics;
	}

	public void setMetrics(Map<String, BigDecimal> metrics) {
		this.metrics = metrics;
	}

	public void addMetric(String name, BigDecimal value) {
		this.metrics.put(name, value);
	}

	protected String round(Double d) {
		return String.format(Locale.ENGLISH, "%.4f", d);
	}

	@Override
	public String toString() {
		return "Result{" + "params=" + params + ", metrics=" + metrics + '}';
	}
}
