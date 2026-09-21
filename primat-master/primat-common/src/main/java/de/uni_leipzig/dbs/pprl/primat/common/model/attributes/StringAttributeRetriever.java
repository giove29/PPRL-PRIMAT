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

import java.util.Optional;


/**
 * 
 * @author mfranke
 *
 */
public class StringAttributeRetriever implements QidAttributeVisitor {

	private StringAttribute stringAttribute;

	public StringAttributeRetriever() {
		this.stringAttribute = null;
	}

	@Override
	public void visit(StringAttribute attr) {
		this.stringAttribute = attr;
	}

	public Optional<StringAttribute> getStringAttribute() {
		return Optional.ofNullable(this.stringAttribute);
	}

	public void clear() {
		this.stringAttribute = null;
	}

	@Override
	public void visit(DateAttribute attr) {
	}

	@Override
	public void visit(NumericAttribute attr) {
	}

	@Override
	public void visit(IntegerSetAttribute attr) {
	}

	@Override
	public void visit(BitSetAttribute attr) {
	}

	@Override
	public void visit(XorBitSetAttribute attr) {
	}

}