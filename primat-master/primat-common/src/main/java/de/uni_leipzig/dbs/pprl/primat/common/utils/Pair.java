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

package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;


/**
 * 
 * @author     mfranke
 *
 * @param  <S>
 * @param  <T>
 */
@Embeddable
public class Pair<S, T> {

	@Column(name = "left_value")
	private S left;

	@Column(name = "right_value")
	private T right;

	public Pair() {
	}

	public Pair(S left, T right) {
		this.left = left;
		this.right = right;
	}

	public S getLeft() {
		return left;
	}

	public T getRight() {
		return right;
	}

	public void setLeft(S left) {
		this.left = left;
	}

	public void setRight(T right) {
		this.right = right;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Pair && Objects.equals(left, ((Pair<?, ?>) o).left)
			&& Objects.equals(right, ((Pair<?, ?>) o).right);
	}

	@Override
	public int hashCode() {
		return 31 * Objects.hashCode(left) + Objects.hashCode(right);
	}

	@Override
	public String toString() {
		return "(" + left + ", " + right + ")";
	}
}