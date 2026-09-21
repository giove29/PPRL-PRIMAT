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

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.BitSetUtils;


/**
 * 
 * @author mfranke
 *
 */
@Entity
public class Cluster implements Linkable {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cluster_seq")
	@SequenceGenerator(name = "cluster_seq", sequenceName = "cluster_seq", allocationSize = 50)
	private int id;

	@OneToOne
	@JoinColumn(name = "physrep")
	private Record physicalRepresentant;

	@Embedded
	private CountingBloomFilter cbf;

	@OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Record> records;

	@ManyToMany(cascade = {
		CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH
	})
	@JoinTable(name = "clusterBlock", joinColumns = @JoinColumn(name = "clusterid"), inverseJoinColumns = {
		@JoinColumn(name = "blockingkeyid", referencedColumnName = "blockingkeyid"),
		@JoinColumn(name = "blockingKeyValue", referencedColumnName = "blockingKeyValue")
	}, indexes = {
		@Index(name = "idx_blocks_cluster_id", columnList = "clusterid"),
	})
	private Set<BlockingKeyAttribute> blockingKeys;

	@Enumerated(EnumType.STRING)
	private ClusterBlockingKeyStrategy blockingKeyStrategy;

	@Enumerated(EnumType.STRING)
	private ClusterRepresentantStrategy representantStrategy;

	public Cluster() {
		this.records = new HashSet<>();
		this.representantStrategy = ClusterRepresentantStrategy.RETAIN_FIRST;
		this.blockingKeyStrategy = ClusterBlockingKeyStrategy.UNION;
	}

	public Cluster(ClusterBlockingKeyStrategy blockingKeyStrategy, ClusterRepresentantStrategy representantStrategy) {
		this.records = new HashSet<>();
		this.representantStrategy = representantStrategy;
		this.blockingKeyStrategy = blockingKeyStrategy;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Record getPhysicalRepresentant() {
		return physicalRepresentant;
	}

	public void setPhysicalRepresentant(Record physicalRepresentant) {
		this.physicalRepresentant = physicalRepresentant;
	}

	public CountingBloomFilter getVirtualRepresentant() {
		return cbf;
	}

	public void setVirtualRepresentant(CountingBloomFilter virtualRepresentant) {
		this.cbf = virtualRepresentant;
	}

	/*
	 * public void removeRecord(Record rec) { rec.setCluster(null);
	 * this.records.remove(rec); }
	 */

	public void addRecord(Record rec) {
		rec.setCluster(this);
		final boolean contained = this.records.add(rec);

		if (!contained) {
			this.updateVirtualRepresentant();
			this.representantStrategy.update(rec, this);
			this.blockingKeyStrategy.update(rec, this);
		}
	}

	private void updateVirtualRepresentant() {
		// TODO: BitSet Count??
		// TODO: BitSet attribute first?
		// TODO: Optimize by adding only last one
		final List<BitSet> bitsets = this.records.stream().map(r -> r.getBitSetAttributes().get(0).getValue())
			.collect(Collectors.toList());

		final int[] cbfData = BitSetUtils.getCounts(1024, bitsets);
		this.cbf = new CountingBloomFilter(cbfData, bitsets.size());
	}

	public Set<Record> getRecords() {
		return records;
	}

	public void addRecords(Set<Record> records) {

		for (final Record rec : records) {
			this.addRecord(rec);
		}
	}

	public void setRecords(Set<Record> records) {
		this.records = new HashSet<Record>(records.size());

		for (final Record rec : records) {
			this.addRecord(rec);
		}
	}

	public void setBlockingKeys(Set<BlockingKeyAttribute> blockingKeys) {
		this.blockingKeys = blockingKeys;
	}

	public Set<BlockingKeyAttribute> getBlockingKeys() {
		return this.blockingKeys;
	}

	public ClusterBlockingKeyStrategy getBlockingKeyStrategy() {
		return blockingKeyStrategy;
	}

	public void setBlockingKeyStrategy(ClusterBlockingKeyStrategy blockingKeyStrategy) {
		this.blockingKeyStrategy = blockingKeyStrategy;
	}

	public ClusterRepresentantStrategy getRepresentantStrategy() {
		return representantStrategy;
	}

	public void setRepresentantStrategy(ClusterRepresentantStrategy representantStrategy) {
		this.representantStrategy = representantStrategy;
	}

	public Set<Party> getMappedParties() {
		final Set<Party> mappedParties = new HashSet<>();

		for (final Record rec : records) {
			final Party party = rec.getPartyAttribute().getValue();
			mappedParties.add(party);
		}
		return mappedParties;
	}

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
		Cluster other = (Cluster) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Cluster [id=");
		builder.append(id);
		builder.append(", physicalRepresentant=");
		builder.append(physicalRepresentant);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public void accept(LinkableVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String getUniqueIdentifier() {
		return String.valueOf(this.id);
	}

	@Override
	public String getGlobalIdentifier() {
		return this.getUniqueIdentifier();
	}
	
	public int getId() {
		return this.id;
	}
	
	@Override
	public int compareTo(Linkable o) {
		return this.getUniqueIdentifier().compareTo(o.getUniqueIdentifier());
	}

}