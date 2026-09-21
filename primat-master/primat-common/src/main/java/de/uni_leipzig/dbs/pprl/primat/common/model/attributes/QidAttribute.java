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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * 
 * @author     mfranke
 *
 * @param  <T>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class QidAttribute<T> extends Attribute<T> {

	@Id
	@GeneratedValue
	private int id;

	@ManyToOne
	@JoinColumn(name = "record_id")
	private Record record;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Record getRecord() {
		return this.record;
	}

	public void setRecord(Record record) {
		this.record = record;
	}

	@Override
	public abstract QidAttribute<T> newInstance();

	@Override
	public void accept(AttributeVisitor visitor) {
		visitor.visit(this);
	}

	public abstract void accept(QidAttributeVisitor visitor);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QidAttribute<?> other = (QidAttribute<?>) obj;
		if (id != other.id)
			return false;
		return true;
	}
}