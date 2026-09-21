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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;


/**
 * 
 * @author mfranke
 *
 */
@Embeddable
public class BlockingKeyId implements Serializable {

	private static final long serialVersionUID = 5564635834668624841L;

	@Column
	private int blockingKeyId;

	@Column
	private String blockingKeyValue;

	public BlockingKeyId() {
	}

	public BlockingKeyId(int blockingKeyId, String blockingKeyValue) {
		this.blockingKeyId = blockingKeyId;
		this.blockingKeyValue = blockingKeyValue;
	}

	public int getBlockingKeyId() {
		return blockingKeyId;
	}

	public void setBlockingKeyId(int blockingKeyId) {
		this.blockingKeyId = blockingKeyId;
	}

	public String getBlockingKeyValue() {
		return blockingKeyValue;
	}

	public void setBlockingKeyValue(String blockingKeyValue) {
		this.blockingKeyValue = blockingKeyValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (blockingKeyId ^ (blockingKeyId >>> 32));
		result = prime * result + ((blockingKeyValue == null) ? 0 : blockingKeyValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof BlockingKeyId)) {
			return false;
		}
		BlockingKeyId other = (BlockingKeyId) obj;

		if (blockingKeyId != other.blockingKeyId) {
			return false;
		}

		if (blockingKeyValue == null) {

			if (other.blockingKeyValue != null) {
				return false;
			}
		}
		else if (!blockingKeyValue.equals(other.blockingKeyValue)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(blockingKeyId);
		builder.append("_");
		builder.append(blockingKeyValue);
		return builder.toString();
	}
}