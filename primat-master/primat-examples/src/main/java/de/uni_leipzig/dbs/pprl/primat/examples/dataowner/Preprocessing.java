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
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.FieldNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.FieldSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.NormalizeDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.PartySupplier;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.SplitDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.AccentRemover;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.LetterLowerCaseToNumberNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.LetterUpperCaseToNumberNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.LowerCaseNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.NormalizerChain;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.SpecialCharacterRemover;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.SubstringNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.TrimNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.UmlautNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.BlankSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.DotSplitter;


/**
 * 
 * @author mfranke
 *
 */
public class Preprocessing {
	
	
	public static void main(String[] args) throws IOException {

		final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
				.add(0, NonQidAttributeType.ID)
				.add(1, QidAttributeType.STRING, "FN")
				.add(2, QidAttributeType.STRING, "LN")
				.add(3, QidAttributeType.STRING, "MN")
				.add(4, QidAttributeType.STRING, "DOB")
				.build();	
		
		final String inputPath = args[0];
		final String outputPath = args[1];
		
		final DatasetReader reader = new DatasetReader(inputPath, rsc);
		final List<Record> records = reader.read();

		final PartySupplier partySupp = new PartySupplier();
		partySupp.preprocess(records);

		final SplitDefinition splitDef = new SplitDefinition();
		splitDef.setSplitter("CITY_ZIP", new BlankSplitter(2));
		splitDef.setSplitter("DOB", new DotSplitter(3));

		final FieldSplitter fs = new FieldSplitter(splitDef);
		fs.preprocess(records);

		final NormalizerChain normChain = new NormalizerChain(
			List.of(new UmlautNormalizer(), new TrimNormalizer(), new LowerCaseNormalizer(), new AccentRemover(),
				new SpecialCharacterRemover(), new SubstringNormalizer(0, 12)));

		final NormalizerChain numberNorm = new NormalizerChain(new LetterLowerCaseToNumberNormalizer(),
			new LetterUpperCaseToNumberNormalizer());

		final NormalizeDefinition normDef = new NormalizeDefinition();
		normDef.setNormalizer(0, normChain);
		normDef.setNormalizer(1, normChain);
		normDef.setNormalizer(2, numberNorm);
		normDef.setNormalizer(3, normChain);
		normDef.setNormalizer(4, numberNorm);
		normDef.setNormalizer(5, numberNorm);
		normDef.setNormalizer(6, numberNorm);

		final FieldNormalizer fn = new FieldNormalizer(normDef);
		fn.preprocess(records);

		final CSVWriter csvWriter = new CSVWriter(outputPath);
		csvWriter.writeRecords(records);
	}
}
