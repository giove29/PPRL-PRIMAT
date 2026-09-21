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

import java.util.BitSet;

import javax.persistence.Embedded;
import javax.persistence.Entity;

import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class XorBitSetAttribute extends QidAttribute<XorBitSet> {

	@Embedded
	private XorBitSet value;

	public XorBitSetAttribute() {
	}

	public XorBitSetAttribute(XorBitSet value) {
		this.value = value;
	}

	public XorBitSetAttribute(Integer cardOrg, BitSet bitset) {
		this(new XorBitSet(cardOrg, bitset));
	}

	@Override
	public QidAttribute<XorBitSet> newInstance() {
		return new XorBitSetAttribute(this.value);
	}

	@Override
	public void setValueFromString(String value) {

		if (value == null || value.isEmpty()) {
			this.value = null;
		}
		else {

			try {
				final String[] parts = value.split(",");
				final Integer cardinality = Integer.parseInt(parts[0]);

				final BitSet bs = BitSetUtils.fromBase64LittleEndian(parts[1]);

				this.value = new XorBitSet(cardinality, bs);

			}
			catch (IllegalArgumentException e) {
				throw new AttributeParseException();
			}
		}
	}

	@Override
	public String getStringValue() {
		return this.value == null ? ""
			: this.value.getCounts() + "," + BitSetUtils.toBase64LittleEndian(this.value.getBitSet());
	}

	@Override
	public AttributeType getType() {
		return QidAttributeType.XOR_BITSET;
	}

	@Override
	public void accept(QidAttributeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public XorBitSet getValue() {
		return this.value;
	}

	@Override
	public void setValue(XorBitSet value) {
		this.value = value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}
}