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

import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;


public abstract class Analyzer {

	public static final String PROP_BASE = "analyzer";
	public static final String HEADER_ATTRIBUTE = "attribute";
	public static final String HEADER_ABSOLUTE_FREQUENCY = "absFrequency";
	public static final String HEADER_RELATIVE_FREQUENCY = "relFrequency";

	protected ResultSet getResultSet() {
		return new ResultSet(getName());
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}

	protected void addDescriptiveStatisticMetrics(Result result, DescriptiveStatistics stats,
		List<String> metricNames) {

		for (String metricName : metricNames) {

			if (stats.getN() == 0) {
				result.addMetric(metricName, BigDecimal.ZERO);
				continue;
			}
			double value = 0;
			MathContext mc = MathContext.UNLIMITED;

			switch (metricName) {
				case "count":
					value = stats.getN();
					mc = new MathContext(0);
					break;
				case "median":
					try {
						value = stats.getPercentile(50);
					}
					catch (MathIllegalStateException e) {
						value = -1;
					}
					break;
				case "mean":
					value = stats.getMean();
					break;
				case "min":
					value = stats.getMin();
					break;
				case "max":
					value = stats.getMax();
					break;
				case "sd":
					value = stats.getStandardDeviation();
					break;
			}

			try {
				result.addMetric(metricName, new BigDecimal(value, mc));
			}
			catch (Exception e) {
				System.out.println("Could not parse as BigDecimal: " + stats.getPercentile(50));
				result.addMetric(metricName, BigDecimal.valueOf(-1));
			}
		}
	}
}
