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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.vladmihalcea.hibernate.type.array.LongArrayType;

import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;


/**
 * 
 * @author mfranke
 *
 */
@Entity
@TypeDefs({
	@TypeDef(name = "long-array", typeClass = LongArrayType.class)
})
public class BitSetAttribute extends QidAttribute<BitSet> {

	@Transient
	private BitSet value;

	public BitSetAttribute() {
	}

	public BitSetAttribute(BitSet s) {
		this.value = s;
	}

	@Access(AccessType.PROPERTY)
	@Type(type = "long-array")
	@Column(name = "bsv", columnDefinition = "BIGINT[]")
	public long[] getWords() {

		if (value != null) {
			return this.value.toLongArray();
		}
		else {
			return null;
		}
	}

	public void setWords(long[] data) {
		this.value = BitSet.valueOf(data);
	}

	@Override
	public BitSetAttribute newInstance() {
		return new BitSetAttribute(this.value);
	}

	@Override
	public void setValueFromString(String value) {

		if (value == null || value.isEmpty()) {
			this.value = null;
		}
		else {

			try {
				this.value = BitSetUtils.fromBase64LittleEndian(value
				// value.substring(1, value.length()-1)
				);
			}
			catch (IllegalArgumentException e) {
				throw new AttributeParseException("Error parsing BitSet");
			}
		}
	}

	@Override
	public String getStringValue() {
		return this.value == null ? "" : BitSetUtils.toBase64LittleEndian(this.value);
	}

	@Override
	public AttributeType getType() {
		return QidAttributeType.BITSET;
	}

	@Override
	public void accept(QidAttributeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public BitSet getValue() {
		return this.value;
	}

	@Override
	public void setValue(BitSet value) {
		this.value = value;
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}
}