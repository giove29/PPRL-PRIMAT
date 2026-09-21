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
package de.uni_leipzig.dbs.pprl.primat.common.model.attributes;

import java.util.List;

import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchemaConfigurationVisitor;
import de.uni_leipzig.dbs.pprl.primat.common.model.UnnamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.utils.SetUtils;

/**
 * 
 * @author mfranke
 *
 */
public class ColumnNameHandler implements RecordSchemaConfigurationVisitor{

	private final List<String> headerNames;
	private final boolean usefulHeaderNames;
	
	private int column;
	private int qidColumn;
	private QidAttributeType attrType;
	
	public ColumnNameHandler(List<String> headerNames) {
		this.headerNames = headerNames;
		this.usefulHeaderNames = headerNames != null && !SetUtils.hasDuplicates(headerNames);
	}
	
	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public int getQidColumn() {
		return qidColumn;
	}

	public void setQidColumn(int qidColumn) {
		this.qidColumn = qidColumn;
	}

	public QidAttributeType getAttrType() {
		return attrType;
	}

	public void setAttrType(QidAttributeType attrType) {
		this.attrType = attrType;
	}

	public void visit(UnnamedRecordSchemaConfiguration rsc) {
		if (usefulHeaderNames) {
			final String name = this.headerNames.get(column);
			RecordSchema.INSTANCE.put(name, attrType, qidColumn);
		}
	}
	
	public void visit(NamedRecordSchemaConfiguration rsc) {		
		final String name = rsc.getQidAttributeNameMap().get(column);		
		RecordSchema.INSTANCE.put(name, attrType, qidColumn);
	}
}
