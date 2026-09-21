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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.BlankSplitter;

/**
 * 
 * @author mfranke
 *
 */
class FieldSplitterTest {

	@Test
	void test() {
		Record record = new Record();
		record.setIdAttribute(new IdAttribute("111"));
		record.setParty(new Party("A"));
		record.addQidAttribute(new StringAttribute("Peter"));
		record.addQidAttribute(new StringAttribute("Schmidt"));
		record.addQidAttribute(new StringAttribute("20"));
		record.addQidAttribute(new StringAttribute("07"));
		record.addQidAttribute(new StringAttribute("1975"));
		record.addQidAttribute(new StringAttribute("04109 Leipzig"));
		record.addQidAttribute(new StringAttribute("M"));

		assertEquals(7, record.getAttributes().size());
	
		RecordSchema.INSTANCE.put("FN", QidAttributeType.STRING, 0);
		RecordSchema.INSTANCE.put("LN", QidAttributeType.STRING, 1);
		RecordSchema.INSTANCE.put("DOB", QidAttributeType.STRING, 2);
		RecordSchema.INSTANCE.put("MOB", QidAttributeType.STRING, 3);
		RecordSchema.INSTANCE.put("YOB", QidAttributeType.STRING, 4);
		RecordSchema.INSTANCE.put("CITY", QidAttributeType.STRING, 5);
		RecordSchema.INSTANCE.put("GENDER", QidAttributeType.STRING, 6);
		
		assertEquals(7, RecordSchema.INSTANCE.getNameColumnMapping().size());
		assertEquals(7, RecordSchema.INSTANCE.getNameAttributeTypeMapping().size());
		
		SplitDefinition splitDef = new SplitDefinition();
		splitDef.setSplitter("CITY", new BlankSplitter(2));
		FieldSplitter splitter = new FieldSplitter(splitDef);
		splitter.preprocess(record);
	
		assertEquals(8, record.getAttributes().size());
		assertEquals("Peter", record.getQidAttribute(0).getStringValue());
		assertEquals("Schmidt", record.getQidAttribute(1).getStringValue());
		assertEquals("20", record.getQidAttribute(2).getStringValue());
		assertEquals("07", record.getQidAttribute(3).getStringValue());
		assertEquals("1975", record.getQidAttribute(4).getStringValue());
		assertEquals("04109", record.getQidAttribute(5).getStringValue());
		assertEquals("Leipzig", record.getQidAttribute(6).getStringValue());	
		assertEquals("M", record.getQidAttribute(7).getStringValue());	
		
		splitter.updateSchema();
		
		assertEquals("Peter", record.getQidAttribute("FN").getStringValue());
		assertEquals("Schmidt", record.getQidAttribute("LN").getStringValue());
		assertEquals("20", record.getQidAttribute("DOB").getStringValue());
		assertEquals("07", record.getQidAttribute("MOB").getStringValue());
		assertEquals("1975", record.getQidAttribute("YOB").getStringValue());
		assertEquals("04109", record.getQidAttribute("CITY_0").getStringValue());
		assertEquals("Leipzig", record.getQidAttribute("CITY_1").getStringValue());
		assertEquals("M", record.getQidAttribute("GENDER").getStringValue());	
		
		assertEquals(8, RecordSchema.INSTANCE.getNameColumnMapping().size());
		assertEquals(8, RecordSchema.INSTANCE.getNameAttributeTypeMapping().size());	
		
	}
}