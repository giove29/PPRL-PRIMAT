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

package de.uni_leipzig.dbs.pprl.primat.common.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.AttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.ColumnNameHandler;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.Attribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.AttributeParseException;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;

/**
 * Class for converting (wrapping) {@link CSVRecord} into {@link Record} objects.
 * 
 * @author mfranke
 */
public class CSVRecordWrapper {

	private final RecordSchemaConfiguration rsc;
	private final AttributeHandler attrHandler;
	private final ColumnNameHandler colNameHandler;
	
	public CSVRecordWrapper(RecordSchemaConfiguration rsc) {
		this(rsc, null);
	}
	
	public CSVRecordWrapper(RecordSchemaConfiguration rsc, List<String> headerNames) {
		this.rsc = rsc;
		this.attrHandler = new AttributeHandler();
		this.colNameHandler = new ColumnNameHandler(headerNames);
	}

	public List<Record> from(List<CSVRecord> csvRecords) throws AttributeParseException {
		final List<Record> result = new ArrayList<Record>(csvRecords.size());
 
		for (final CSVRecord csvRecord : csvRecords) {
			final Record attRecord = this.from(csvRecord);
			result.add(attRecord);
		}

		return result;
	}

	public Record from(CSVRecord csvRecord) {
		final Record record = new Record();
		this.attrHandler.setRecord(record);
		
		// Handle non QID attributes
		for (final Entry<Integer, NonQidAttributeType> entry : this.rsc.getNonQidAttributeMap().entrySet()) {
			final Integer column = entry.getKey();
			final NonQidAttributeType attrType = entry.getValue();
			this.handleAttribute(column, attrType, csvRecord);
		}
		
		// Handle QID attributes
		RecordSchema.INSTANCE.clear();
		int qidColumn = 0;
		
		for (final Entry<Integer, QidAttributeType> entry : this.rsc.getQidAttributeMap().entrySet()) {
			final Integer column = entry.getKey();
			final QidAttributeType attrType = entry.getValue();
			this.handleAttribute(column, attrType, csvRecord);
			
			this.colNameHandler.setAttrType(attrType);
			this.colNameHandler.setQidColumn(qidColumn);
			this.colNameHandler.setColumn(column);
			this.rsc.accept(colNameHandler);
						
			qidColumn++;
		}
		
		return record;
	}
	
	

	
	public void handleAttribute(Integer column, AttributeType attrType, CSVRecord csvRecord) {
		final String attributeValue = csvRecord.get(column);
		final Attribute<?> attr = attrType.constructAttribute();
		attr.setValueFromString(attributeValue);
		attr.accept(this.attrHandler);
	}
}