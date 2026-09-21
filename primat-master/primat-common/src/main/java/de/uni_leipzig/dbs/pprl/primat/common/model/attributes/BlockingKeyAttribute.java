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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class BlockingKeyAttribute {

	@EmbeddedId
	private BlockingKeyId blockingKeyId;

	public BlockingKeyAttribute() {
	}

	public BlockingKeyAttribute(int blockingKeyId, String blockingKeyValue) {
		this.blockingKeyId = new BlockingKeyId(blockingKeyId, blockingKeyValue);
	}

	public BlockingKeyId getBlockingKeyId() {
		return blockingKeyId;
	}

	public void setBlockingKeyId(BlockingKeyId blockingKeyId) {
		this.blockingKeyId = blockingKeyId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blockingKeyId == null) ? 0 : blockingKeyId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof BlockingKeyAttribute)) {
			return false;
		}
		BlockingKeyAttribute other = (BlockingKeyAttribute) obj;

		if (blockingKeyId == null) {

			if (other.blockingKeyId != null) {
				return false;
			}
		}
		else if (!blockingKeyId.equals(other.blockingKeyId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return blockingKeyId.toString();
	}

	public BlockingKeyAttribute copy() {
		return new BlockingKeyAttribute();
	}
}