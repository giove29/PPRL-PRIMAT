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
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.vladmihalcea.hibernate.type.array.LongArrayType;


/**
 * 
 * @author mfranke
 *
 */
@Embeddable
@TypeDefs({
	@TypeDef(name = "long-array", typeClass = LongArrayType.class)
})
public class XorBitSet {

	@Column
	private int counts;

	@Transient
	private BitSet bs;

	public XorBitSet() {
	}

	public XorBitSet(int counts, BitSet bs) {
		this.counts = counts;
		this.bs = bs;
	}

	@Access(AccessType.PROPERTY)
	@Type(type = "long-array")
	@Column(name = "xbsv", columnDefinition = "BIGINT[]")
	public long[] getWords() {
		return this.bs.toLongArray();
	}

	public void setWords(long[] data) {
		this.bs = BitSet.valueOf(data);
	}

	public int getCounts() {
		return counts;
	}

	public void setCounts(int counts) {
		this.counts = counts;
	}

	public BitSet getBitSet() {
		return bs;
	}

	public void setBitSet(BitSet bs) {
		this.bs = bs;
	}
}