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

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.AttributeVisitor;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.PartyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * Class for appropriate handling of the different attribute
 * types during parsing of the record's data from an external
 * source.
 * 
 * @author mfranke
 *
 */
public class AttributeHandler implements AttributeVisitor {

	private Record record;
	
	public AttributeHandler() {
		this.record = null;
	}

	public AttributeHandler(Record record) {
		this.record = record;
	}
	
	public void setRecord(Record record) {
		this.record = record;
	}

	@Override
	public void visit(IdAttribute attr) {
		this.record.setIdAttribute(attr);
	}
	
	@Override
	public void visit(GlobalIdAttribute attr) {
		this.record.setGlobalIdAttribute(attr);
		
	}

	@Override
	public void visit(PartyAttribute attr) {
		this.record.setPartyAttribute(attr);
	}

	@Override
	public void visit(QidAttribute<?> attr) {
		this.record.addQidAttribute(attr);
	}

	@Override
	public void visit(BlockingKeyAttribute attr) {
		this.record.addBlockingKey(attr);
	}
}