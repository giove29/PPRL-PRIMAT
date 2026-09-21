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

package de.uni_leipzig.dbs.pprl.primat.common.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class Party implements Comparable<Party> {

	@Id
	private String name;

	@Column
	private boolean duplicateFree;

	@OneToMany(mappedBy = "party")
	private Set<Record> records;

	public Party() {
		this(null, true);
	}

	public Party(String name) {
		this(name, true);
	}

	public Party(String name, boolean duplicateFree) {
		this.name = name;
		this.duplicateFree = duplicateFree;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isDuplicateFree() {
		return duplicateFree;
	}

	public void setDuplicateFree(boolean duplicateFree) {
		this.duplicateFree = duplicateFree;
	}

	public boolean removeRecord(Record rec) {
		rec.getPartyAttribute().setValue(null);
		return this.records.remove(rec);
	}

	public boolean addRecord(Record rec) {
		rec.getPartyAttribute().setValue(this);
		return this.records.add(rec);
	}

	public Set<Record> getRecords() {
		return records;
	}

	public void setRecords(Set<Record> records) {
		this.records = records;
	}

	@Override
	public int compareTo(Party o) {
		return this.getName().compareTo(o.getName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Party)) {
			return false;
		}
		Party other = (Party) obj;

		if (name == null) {

			if (other.name != null) {
				return false;
			}
		}
		else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return name;
	}
	
	
}
