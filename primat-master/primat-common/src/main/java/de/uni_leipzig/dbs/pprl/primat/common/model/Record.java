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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;

import de.uni_leipzig.dbs.pprl.primat.common.csv.CSVPrintable;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttributeCollector;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttributeRetriever;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IntegerSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IntegerSetAttributeCollector;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.PartyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class Record implements CSVPrintable, Linkable, Serializable {

	@Id
	private IdAttribute id;
	
	private GlobalIdAttribute gid;

	@Transient
	private PartyAttribute party;

	@OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
	@OrderColumn(name = "position")
	private List<QidAttribute<?>> attributes;

	@ManyToMany(cascade = {
		CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
	})
	@JoinTable(name = "block", joinColumns = @JoinColumn(name = "recordid"), inverseJoinColumns = {
		@JoinColumn(name = "blockingkeyid", referencedColumnName = "blockingkeyid"),
		@JoinColumn(name = "blockingKeyValue", referencedColumnName = "blockingKeyValue")
	}, indexes = {
		@Index(name = "idx_blocks_record_id", columnList = "recordid"),
	})
	private Set<BlockingKeyAttribute> blockingKeys;

	@ManyToOne
	private Cluster cluster;

	public Record() {
		this(
			new IdAttribute(), 
			new GlobalIdAttribute(), 
			new PartyAttribute(), 
			new ArrayList<QidAttribute<?>>(),
			new HashSet<BlockingKeyAttribute>()
		);
	}

	public Record(IdAttribute id, GlobalIdAttribute gid, PartyAttribute party, 
			List<QidAttribute<?>> attributes) {
		this(id, gid, party, attributes, new HashSet<BlockingKeyAttribute>());
	}

	public Record(IdAttribute id, GlobalIdAttribute gid, PartyAttribute party, List<QidAttribute<?>> attributes,
		Set<BlockingKeyAttribute> blockingKeys) {
		this.id = id;
		this.gid = gid;
		this.party = party;
		this.attributes = attributes;
		this.blockingKeys = blockingKeys;
	}

	public List<BitSetAttribute> getBitSetAttributes() {
		final BitSetAttributeCollector bsCollector = new BitSetAttributeCollector();

		for (final QidAttribute<?> attr : this.attributes) {
			attr.accept(bsCollector);
		}
		return bsCollector.getBitSetAttributes();
	}

	public List<IntegerSetAttribute> getIntegerSetAttributes() {
		final IntegerSetAttributeCollector isCollector = new IntegerSetAttributeCollector();

		for (final QidAttribute<?> attr : this.attributes) {
			attr.accept(isCollector);
		}
		return isCollector.getIntegerSetAttributes();
	}

	public Record copy() {
		final Record copy = new Record();
		copy.setIdAttribute(this.getIdAttribute().newInstance());

		for (final QidAttribute<?> attr : this.getAttributes()) {
			final QidAttribute<?> attrCopy = attr.newInstance();
			copy.addQidAttribute(attrCopy);
		}

		for (final BlockingKeyAttribute bk : this.getBlockingKeys()) {
			final BlockingKeyAttribute bkCopy = bk.copy();
			copy.addBlockingKey(bkCopy);
		}

		return copy;
	}

	public String getId() {
		return this.id.getValue();
	}

	public String getIdWithParty() {
		return this.id.getValue() + "_" + this.party.getStringValue();
	}

	public IdAttribute getIdAttribute() {
		return this.id;
	}

	public void setIdAttribute(IdAttribute id) {
		this.id = id;
	}
	
	public String getGlobalId() {
		return this.gid.getValue();
	}

	public GlobalIdAttribute getGlobalIdAttribute() {
		return this.gid;
	}

	public void setGlobalIdAttribute(GlobalIdAttribute gid) {
		this.gid = gid;
	}

	@Access(AccessType.PROPERTY)
	@ManyToOne
	public Party getParty() {
		return this.getPartyAttribute().getValue();
	}

	public void setParty(Party party) {
		this.party = new PartyAttribute(party);
	}

	public PartyAttribute getPartyAttribute() {
		return this.party;
	}

	public void setPartyAttribute(PartyAttribute party) {
		this.party = party;
	}

	public void addBlockingKey(BlockingKeyAttribute bk) {
		this.blockingKeys.add(bk);
	}

	public void clearBlockingKeys() {
		this.blockingKeys.clear();
	}

	public void setBlockingKeys(Set<BlockingKeyAttribute> blockingKeys) {
		this.blockingKeys = blockingKeys;
	}

	public Set<BlockingKeyAttribute> getBlockingKeys() {
		return this.blockingKeys;
	}

	public Cluster getCluster() {
		return this.cluster;
	}

	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}

	/**
	 * Add attribute to the end;
	 */
	public void addQidAttribute(QidAttribute<?> attribute) {
		attribute.setRecord(this);
		this.attributes.add(attribute);
	}

	public void addQidAttribute(int index, QidAttribute<?> attribute) {
		attribute.setRecord(this);
		this.attributes.add(index, attribute);
	}

	public void removeQidAttribute(int index) {
		final QidAttribute<?> attr = this.attributes.remove(index);
		attr.setRecord(null);
	}

	// TODO: ID Column problem, see MenuBarController and DataPaneController,
	// when importing a csv and use column semantics
	public QidAttribute<?> getQidAttribute(int column) {
		// return this.attributes.get(column - 1);
		return this.attributes.get(column);
	}

	public QidAttribute<?> getQidAttribute(String name){
		final Integer column = RecordSchema.INSTANCE.getAttributeColumn(name);
		if (column == null) {
			return null;
		}
		else {
			return getQidAttribute(column);
		}
	}
	
	
	// TODO: ???
	// public <E extends QidAttribute<?>> Optional<E> getSpecificAttribute(int
	// column, E e){
	// return null;
	// }

	public Optional<BitSetAttribute> getBitSetAttribute(int column) {
		final BitSetAttributeRetriever bsRet = new BitSetAttributeRetriever();
		this.attributes.get(column).accept(bsRet);
		return bsRet.getBitSetAttribute();
	}

	public List<QidAttribute<?>> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<QidAttribute<?>> attributes) {
		this.attributes = attributes;
	}

	public void addAttribute(QidAttribute<?> attribute) {
		attribute.setRecord(this);
		this.attributes.add(attribute);
	}

	public void removeAwesome(QidAttribute<?> attribute) {
		attribute.setRecord(null);
		this.attributes.remove(attribute);
	}




	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}
		Record other = (Record) obj;

		if (id == null) {

			if (other.id != null) {
				return false;
			}
		}
		else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringJoiner result = new StringJoiner(",");
		result.add(this.getId());
		result.add(this.getGlobalId());
		result.add(this.getPartyAttribute().getStringValue());
		// for (final Attribute<?> att : this.attributes) {
		// result.add(att.toString());
		// }IdMiddlePartHyphenTrueMatchChecker
		return result.toString();
	}

	@Override
	public Iterable<?> getValuesToPrint() {
		final List<Object> values = new ArrayList<>();
		values.add(this.getId());
		values.add(this.getGlobalId());

		if (this.party != null && this.party.getStringValue() != null) {
			values.add(this.party.getStringValue());
		}

		// values.add(this.getAttributes());
		for (final QidAttribute<?> a : this.getAttributes()) {
			values.add(a);
		}

		for (final BlockingKeyAttribute b : this.getBlockingKeys()) {
			values.add(b.getBlockingKeyId());
		}
		return values;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(this.id);
		out.writeObject(this.gid);
		//out.writeObject(this.attributes);
		out.writeObject(this.getParty().getName());
	}

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {

		//this.attributes = (List<QidAttribute<?>>) ois.readObject();
		this.id = (IdAttribute) ois.readObject();
		this.gid = (GlobalIdAttribute) ois.readObject();
		String partyName = (String)ois.readObject();
		this.party = new PartyAttribute(new Party(partyName));


	}

	@Override
	public void accept(LinkableVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String getUniqueIdentifier() {
		return this.getId();
	}
	
	@Override
	public String getGlobalIdentifier() {
		return this.getGlobalId();
	}

	@Override
	public int compareTo(Linkable o) {
		return this.getUniqueIdentifier().compareTo(o.getUniqueIdentifier());
	}
}