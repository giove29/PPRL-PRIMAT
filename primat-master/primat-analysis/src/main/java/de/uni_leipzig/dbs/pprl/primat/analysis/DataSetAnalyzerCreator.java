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

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeAvailability;
import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeLength;
import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeMostFrequent;
import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeMostFrequentNGrams;
import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributePatternFrequency;
import de.uni_leipzig.dbs.pprl.primat.analysis.cluster.ClusterPairwiseDiff;
import de.uni_leipzig.dbs.pprl.primat.analysis.cluster.ClusterPairwiseEqual;
import de.uni_leipzig.dbs.pprl.primat.analysis.cluster.ClusterSize;
import de.uni_leipzig.dbs.pprl.primat.analysis.record.RecordCounter;
import de.uni_leipzig.dbs.pprl.primat.analysis.record.RecordOverlap;


public class DataSetAnalyzerCreator {

	public static DataSetAnalyzer create() {
		DataSetAnalyzer dsa = new DataSetAnalyzer();

		// RecordAnalyzer
		dsa.addAnalyzer(new RecordCounter());
		dsa.addAnalyzer(new RecordOverlap());
		// dsa.addAnalyzer(new RecordOverlapEstimate());
		// TODO Find clusters according to equal attributes (e.g. same last name
		// and address -> family)

		// ClusterAnalyzer
		dsa.addAnalyzer(new ClusterSize());
		dsa.addAnalyzer(new ClusterPairwiseDiff());
		dsa.addAnalyzer(new ClusterPairwiseEqual());
		// TODO Share of completely different attributes of matching records
		// (e.g. family name)
		// TODO Share of completely equal records within a cluster

		// AttributeAnalyzer
		dsa.addAnalyzer(new AttributeAvailability());
		dsa.addAnalyzer(new AttributeLength());
		// dsa.addAnalyzer(new AttributeBitPositionFrequency());
		dsa.addAnalyzer(new AttributeMostFrequent());
		// dsa.addAnalyzer(new AttributeMostFrequentNGrams(1));
		dsa.addAnalyzer(new AttributeMostFrequentNGrams(2));
		dsa.addAnalyzer(new AttributeMostFrequentNGrams(3));

		AttributePatternFrequency apf = new AttributePatternFrequency();
		apf.addPatterns(
//			"FIRSTNAME",
			0,
			List.of(".*-.*", ".\\.", ".*\\s.*")
		);
		apf.addPatterns(
//			"LASTNAME",
			2,
			List.of(".*-.*", ".\\.", ".*\\s.*")
		);
		dsa.addAnalyzer(apf);

		// TODO Share of types per attribute (may be mixed...) including share
		// of list attributes
		// TODO correlations of certain attribute types (e.g. first name and
		// date of birth)

		return dsa;
	}
}
