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
package de.uni_leipzig.dbs.pprl.primat.examples.dataowner;

import java.io.IOException;
import java.util.List;
import de.uni_leipzig.dbs.pprl.primat.common.csv.CSVWriter;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram.BigramExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.common.utils.RandomFactory;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.Encoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterEncoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.NoHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.HashingMethod;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.RandomHashing;


/**
 * 
 * @author mfranke
 *
 */
public class Encoding {

	public static void main(String[] args) throws IOException {
		final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
				.add(0, NonQidAttributeType.ID)
				.add(1, NonQidAttributeType.PARTY)
				.add(2, QidAttributeType.STRING, "FN")
				.add(3, QidAttributeType.STRING, "LN")
				.add(4, QidAttributeType.STRING, "ZIP")
				.add(5, QidAttributeType.STRING, "DOB")
				.add(6, QidAttributeType.STRING, "MOB")
				.add(7, QidAttributeType.STRING, "YOB")
				.build();	
		
		final String inputPath = args[0];
		final String outputPath = args[1];
		
		final DatasetReader reader = new DatasetReader(inputPath, rsc);
		final List<Record> records = reader.read();

		final FeatureExtractor featEx = new BigramExtractor(true);
		final int k = 10;

		final BloomFilterExtractorDefinition exDef = new BloomFilterExtractorDefinition();
		exDef.setColumnsByName("FN", "LN", "ZIP", "DOB", "MOB", "YOB");
		exDef.setExtractors(featEx);
		exDef.setNumberOfHashFunctions(k);

		final HashingMethod hashing = new RandomHashing(1024, RandomFactory.SECURE_RANDOM);

		final BloomFilterDefinition def1 = new BloomFilterDefinition();
		def1.setName("RBF");
		def1.setBfLength(1024);
		def1.setHashingMethod(hashing);
		def1.setFeatureExtractors(List.of(exDef));
		def1.setHardener(new NoHardener());

		final Encoder encoder = new BloomFilterEncoder(List.of(def1));

		final List<Record> encodedRecords = encoder.encode(records);

		final CSVWriter csvWriter = new CSVWriter(outputPath);
		csvWriter.writeRecords(encodedRecords, encoder.getSchema());
	}
}