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

public enum QidAttributeType implements AttributeType{

	
	/**
	 * Attribute containing a string value.
	 */
	STRING{

		@Override
		public Attribute<?> constructAttribute() {
			return new StringAttribute();
		}
		
	}, 
	
	/**
	 * Attribute containing a data value.
	 */
	DATE{

		@Override
		public Attribute<?> constructAttribute() {
			return new DateAttribute();
		}
		
	}, 
	
	/**
	 * Attribute containing a numerical value.
	 */
	NUMERIC{

		@Override
		public Attribute<?> constructAttribute() {
			return new NumericAttribute();
		}
		
	},
	
	/**
	 * Attribute containing a bitset value.
	 * This attribute typically represents
	 * a Bloom filter.
	 */
	BITSET{

		@Override
		public Attribute<?> constructAttribute() {
			return new BitSetAttribute();
		}
		
	},
	
	/**
	 * Attribute containing bitset value.
	 * The bitset was folded (aggregated)
	 * with the xor-operation in order to
	 * make the Bloom filter encoding
	 * more robust against cryptanalysis.
	 */
	XOR_BITSET{

		@Override
		public Attribute<?> constructAttribute() {
			return new XorBitSetAttribute();
		}
		
	},
	
	/**
	 * Attribute containing a integer set
	 * (or array) as value.
	 * This attribute typically represents
	 * a encoding that is produced by the
	 * Two-Step Hash Encoding.
	 */
	INTEGER_SET{

		@Override
		public Attribute<?> constructAttribute() {
			return new IntegerSetAttribute();
		}
		
	};

}
