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
package de.uni_leipzig.dbs.pprl.primat.lu.database;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterFactory;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HomogenPair;
import de.uni_leipzig.dbs.pprl.primat.common.utils.Pair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;


/**
 * Wrapper su un {@link EntityManagerFactory} per una singola persistence-unit
 * Postgres. Non più un singleton: ogni strategia di clustering persistente
 * (Center Clustering, MSCD-AP, Global Greedy, CLIP) ha il proprio DB dedicato,
 * quindi il proprio {@code DbConnection}, costruito dalla config JSON della
 * Linkage Unit. Un solo {@code DbConnection} vivo per processo (una config
 * seleziona esattamente una strategia).
 *
 * @author mfranke
 */
public class DbConnection {

	/**
	 * Se {@code true}, Hibernate stampa ogni statement SQL (formattato) su
	 * stdout. Tenerlo spento di default: riattivare solo per debugging.
	 */
	private static final boolean SQL_DEBUG_LOGGING = false;

	private final EntityManagerFactory entityManagerFactory;

	public DbConnection(String persistenceUnitName, String jdbcUrl, String jdbcUser, String jdbcPassword) {
		final Map<String, Object> overrides = new HashMap<>();
		overrides.put("hibernate.show_sql", String.valueOf(SQL_DEBUG_LOGGING));
		overrides.put("hibernate.format_sql", String.valueOf(SQL_DEBUG_LOGGING));
		overrides.put("javax.persistence.jdbc.url", jdbcUrl);
		overrides.put("javax.persistence.jdbc.user", jdbcUser);
		overrides.put("javax.persistence.jdbc.password", jdbcPassword);
		this.entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, overrides);
	}

	public EntityManager openEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	public List<Party> getParties() {
		final EntityManager entityManager = openEntityManager();

		entityManager.getTransaction().begin();

		final List<Party> parties = entityManager.createQuery("FROM Party", Party.class).getResultList();

		entityManager.getTransaction().commit();

		return parties;
	}

	public void addParties(Set<Party> parties) {
		final EntityManager entityManager = openEntityManager();

		entityManager.getTransaction().begin();

		for (final Party party : parties) {
			// TODO: easier way?
			Party p1 = entityManager.find(Party.class, party.getName());

			if (p1 == null) {
				entityManager.persist(party);
			}
		}

		entityManager.getTransaction().commit();
	}

	public void addBlockingKeysStaging(Collection<Record> data) {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		final Query dropQuery = entityManager.createNativeQuery("DROP TABLE IF EXISTS BlockStaging;");
		dropQuery.executeUpdate();

		final Query query1 = entityManager.createNativeQuery(
			  "CREATE TEMPORARY TABLE BlockStaging( "
			+ "	blockingkeyid INTEGER NOT NULL, " 
			+ "	blockingkeyvalue VARCHAR NOT NULL, "
			+ "	recordId VARCHAR NOT NULL, " 
			+ "	party_name VARCHAR NOT NULL, "
			+ " duplicatefree BOOLEAN NOT NULL, " 
			+ "	PRIMARY KEY (blockingkeyid, blockingkeyvalue, recordId) " 
			+ ");");

		query1.executeUpdate();

		final Query query2 = entityManager.createNativeQuery(
			"INSERT INTO BlockStaging (blockingkeyid, blockingkeyvalue, recordid, party_name, duplicatefree) "
				+ "VALUES (?, ?, ?, ?, ?)");

		for (final Record rec : data) {
			final Set<BlockingKeyAttribute> bks = rec.getBlockingKeys();

			for (final BlockingKeyAttribute bka : bks) {
				query2.setParameter(1, bka.getBlockingKeyId().getBlockingKeyId());
				query2.setParameter(2, bka.getBlockingKeyId().getBlockingKeyValue());
				query2.setParameter(3, rec.getId());
				query2.setParameter(4, rec.getParty().getName());
				query2.setParameter(5, rec.getParty().isDuplicateFree());
				query2.executeUpdate();
			}
		}

		entityManager.getTransaction().commit();
	}

	@SuppressWarnings("unchecked")
	public List<HomogenPair<Record>> getCandidatesAllRecords(Map<String, Record> newRecords) {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		final Query test1 = entityManager.createNativeQuery(
			"SELECT b1.blockingkeyid, b1.blockingkeyvalue, b1.recordid, b1.party_name, b1.duplicateFree FROM blockstaging b1");

		System.out.println("Test1: " + test1.getResultList().size());

		
		final Query test2 = entityManager.createNativeQuery(
				  "SELECT DISTINCT t1.recordid AS l, t2.recordid AS r, t1.party_name, t1.duplicateFree "
				+ "FROM " 
				+ "("
				+ "SELECT b1.blockingkeyid, b1.blockingkeyvalue, b1.recordid, b1.party_name, b1.duplicateFree "
				+ "FROM blockstaging b1 "
				+ ") AS t1, " 
				+ "(" 
				+ "SELECT b2.blockingkeyid, b2.blockingkeyvalue, b2.recordid " + "FROM block b2 "
				+ ") AS t2 " 
				+ "WHERE t1.blockingkeyid = t2.blockingkeyid "
				+ "AND t1.blockingkeyvalue = t2.blockingkeyvalue "
				);
		
		System.out.println("Test2: " + (test2.getResultList().size() > 0 ? Arrays.toString((Object[]) test2.getResultList().get(0)) : ""));
		
		final Query query3 = entityManager.createNativeQuery(
			  "SELECT DISTINCT t1.recordid AS l, t2.recordid AS r "
			+ "FROM " 
			+ "("
			+ "SELECT b1.blockingkeyid, b1.blockingkeyvalue, b1.recordid, b1.party_name, b1.duplicateFree "
			+ "FROM blockstaging b1 "
			+ ") AS t1, " 
			+ "(" 
			+ "SELECT b2.blockingkeyid, b2.blockingkeyvalue, b2.recordid " + "FROM block b2 "
			+ ") AS t2 " 
			+ "WHERE t1.blockingkeyid = t2.blockingkeyid "
			+ "AND t1.blockingkeyvalue = t2.blockingkeyvalue " 
			+ "AND (" 
			+ "		(t1.duplicateFree = false) "
			+ "		OR " 
			+ "		(" 
			+ "			t1.duplicateFree = true " 
			+ "			AND t1.party_name NOT IN ("
			+ "             SELECT DISTINCT party_name " 
			+ "				FROM record "
			+ "             WHERE cluster_id = (" 
			+ "					SELECT cluster_id "
			+ "             	FROM record " 
			+ "             	WHERE id = t2.recordid " 
			+ "				)"
			+ "			)" 
			+ "		)" 
			+ ")");

		final List<Object[]> res = query3.getResultList();

		System.out.println("ResultSize: " +  res.size());
		
		final List<HomogenPair<String>> pairs = res.stream()
			.map(r -> new HomogenPair<String>((String) r[0], (String) r[1])).collect(Collectors.toList());

		final Set<IdAttribute> recordIds = new HashSet<>();

		for (final HomogenPair<String> pair : pairs) {
			recordIds.add(new IdAttribute(pair.getRight()));
		}

		final TypedQuery<Record> q4 = entityManager.createQuery("FROM Record WHERE id IN ?1", Record.class);
		q4.setParameter(1, recordIds);

		final List<Record> records = q4.getResultList();
		final Map<String, Record> recordMap = records.stream()
			.collect(Collectors.toMap(Record::getId, Function.identity()));

		final List<HomogenPair<Record>> candidates = pairs.stream().map(p -> {
			final String leftRecordId = p.getLeft();
			final Record leftRecord = newRecords.get(leftRecordId);
			final String rightRecordId = p.getRight();
			final Record rightRecord = recordMap.get(rightRecordId);
			return new HomogenPair<Record>(leftRecord, rightRecord);
		}).collect(Collectors.toList());

		entityManager.getTransaction().commit();

		return candidates;
	}

	@SuppressWarnings("unchecked")
	public List<Pair<Record, Cluster>> getCandidates(Map<String, Record> newRecords) {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		final Query test1 = entityManager.createNativeQuery(
			"SELECT b1.blockingkeyid, b1.blockingkeyvalue, b1.recordid, b1.party_name, b1.duplicateFree FROM blockstaging b1");

		System.out.println("Test1: " + test1.getResultList().size());

		final Query query3 = entityManager.createNativeQuery("SELECT DISTINCT t1.recordid, t2.clusterid " + "FROM "
			+ "(" + "SELECT b1.blockingkeyid, b1.blockingkeyvalue, b1.recordid, b1.party_name, b1.duplicateFree "
			+ "FROM blockstaging b1 " + ") AS t1, " + "("
			+ "SELECT b2.blockingkeyid, b2.blockingkeyvalue, b2.clusterid " + "FROM clusterblock b2 " + ") AS t2 "
			+ "WHERE t1.blockingkeyid = t2.blockingkeyid " + "AND t1.blockingkeyvalue = t2.blockingkeyvalue " + "AND ("
			+ "		(t1.duplicateFree = false) " + "		OR " + "		(" + "			t1.duplicateFree = true "
			+ "			AND t1.party_name NOT IN (" + "				SELECT DISTINCT party_name "
			+ "				FROM record " + "				WHERE cluster_id = t2.clusterid " + "			)"
			+ "		)" + ")");

		final List<Object[]> res = query3.getResultList();

		final List<Pair<String, Integer>> pairs = res.stream()
			.map(r -> new Pair<String, Integer>((String) r[0], (Integer) r[1])).collect(Collectors.toList());

		final Set<Integer> clusterIds = pairs.stream().map(p -> p.getRight()).collect(Collectors.toSet());

		final TypedQuery<Cluster> q4 = entityManager.createQuery("FROM Cluster WHERE id IN ?1", Cluster.class);
		q4.setParameter(1, clusterIds);

		final List<Cluster> clusters = q4.getResultList();
		final Map<Integer, Cluster> clusterMap = clusters.stream()
			.collect(Collectors.toMap(Cluster::getId, Function.identity()));

		final List<Pair<Record, Cluster>> candidates = pairs.stream().map(p -> {
			final String recordId = p.getLeft();
			final Record record = newRecords.get(recordId);
			final Integer clusterId = p.getRight();
			final Cluster cluster = clusterMap.get(clusterId);
			return new Pair<Record, Cluster>(record, cluster);
		}).collect(Collectors.toList());

		entityManager.getTransaction().commit();

		return candidates;
	}

	public void insertSingletonsNewCluster(ClusterFactory clusterFactory, Map<String, Record> unmappedRecords) {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		for (final Record rec : unmappedRecords.values()) {
			System.out.println("Singleton: " + rec);

			final Set<BlockingKeyAttribute> bks = rec.getBlockingKeys().stream().map(bk -> entityManager.merge(bk))
				.collect(Collectors.toSet());
			rec.setBlockingKeys(bks);

			final Cluster cluster = clusterFactory.build(rec);
			entityManager.persist(cluster);
		}

		entityManager.getTransaction().commit();
	}

	public void updateAffectedCluster(List<LinkedPair<Cluster>> matches) {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		for (final LinkedPair<Cluster> match : matches) {
			final Record rec = match.getLeftRecord();
			final Cluster cluster = match.getRight();
			System.out.println("Match: " + rec + " --> " + cluster);
			cluster.addRecord(rec);
			entityManager.merge(cluster);
		}

		entityManager.getTransaction().commit();
	}

	/**
	 * Storico completo dei record con il rispettivo {@link Cluster} (se
	 * presente) già inizializzato, oltre alle blocking keys del cluster e agli
	 * attributi QID del record: il chiamante userà questi record oltre la
	 * durata di questo {@link EntityManager} (per rieseguire blocking e
	 * classificazione), quindi ogni collezione lazy toccata più tardi deve
	 * essere già stata caricata qui per evitare una
	 * {@code LazyInitializationException}.
	 */
	public List<Record> getAllRecordsWithClusters() {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		final List<Record> records = entityManager.createQuery(
			"SELECT DISTINCT r FROM Record r "
				+ "LEFT JOIN FETCH r.cluster c "
				+ "LEFT JOIN FETCH c.blockingKeys "
				+ "LEFT JOIN FETCH r.attributes",
			Record.class).getResultList();

		entityManager.getTransaction().commit();

		return records;
	}

	/**
	 * Trova i {@link Cluster} storici candidati per {@code newRecords}: quelli
	 * che condividono almeno una blocking key con uno dei record nuovi,
	 * tramite join nativo {@code BlockStaging ↔ clusterBlock}. Sostituisce il
	 * reload completo di {@link #getAllRecordsWithClusters()} nel path
	 * persistente: il costo dipende dal numero di candidati, non dalla
	 * dimensione totale dello storico.
	 */
	@SuppressWarnings("unchecked")
	public Set<Cluster> getCandidateClusters(Collection<Record> newRecords) {
		addBlockingKeysStaging(newRecords);

		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		final Query query = entityManager.createNativeQuery(
			"SELECT DISTINCT cb.clusterid FROM clusterblock cb "
				+ "JOIN blockstaging bs ON cb.blockingkeyid = bs.blockingkeyid "
				+ "AND cb.blockingkeyvalue = bs.blockingkeyvalue");

		final List<Object> res = query.getResultList();
		final Set<Integer> ids = res.stream().map(o -> (Integer) o).collect(Collectors.toSet());

		if (ids.isEmpty()) {
			entityManager.getTransaction().commit();
			return Set.of();
		}

		final TypedQuery<Cluster> q = entityManager.createQuery(
			"SELECT DISTINCT c FROM Cluster c "
				+ "LEFT JOIN FETCH c.records r "
				+ "LEFT JOIN FETCH r.attributes "
				+ "LEFT JOIN FETCH c.blockingKeys "
				+ "WHERE c.id IN :ids",
			Cluster.class);
		q.setParameter("ids", ids);

		final Set<Cluster> clusters = new HashSet<>(q.getResultList());

		entityManager.getTransaction().commit();

		return clusters;
	}

	/**
	 * Persiste un nuovo {@link Cluster} contenente tutti i {@code records}
	 * passati (una componente connessa senza alcun cluster persistito
	 * preesistente).
	 */
	public Cluster persistNewCluster(ClusterFactory clusterFactory, Collection<Record> records) {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		Cluster cluster = null;
		for (final Record rec : records) {
			mergeBlockingKeys(entityManager, rec);
			if (cluster == null) {
				cluster = clusterFactory.build(rec);
			}
			else {
				cluster.addRecord(rec);
			}
		}
		syncClusterBlockingKeys(cluster);
		entityManager.persist(cluster);

		entityManager.getTransaction().commit();

		return cluster;
	}

	/**
	 * Estende un {@link Cluster} già persistito con {@code newRecords} (record
	 * mai visti prima, appartenenti alla stessa componente connessa del
	 * cluster). No-op se {@code newRecords} è vuoto (run idempotente sullo
	 * stesso dataset).
	 */
	public void extendCluster(Cluster cluster, Collection<Record> newRecords) {
		if (newRecords.isEmpty()) {
			return;
		}

		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		final Cluster managedCluster = entityManager.find(Cluster.class, cluster.getId());
		for (final Record rec : newRecords) {
			mergeBlockingKeys(entityManager, rec);
			managedCluster.addRecord(rec);
		}
		syncClusterBlockingKeys(managedCluster);
		entityManager.merge(managedCluster);

		entityManager.getTransaction().commit();
	}

	/**
	 * Fonde {@code losers} in {@code target}: ogni record dei cluster
	 * perdenti viene ripuntato a {@code target}, poi i perdenti (ormai senza
	 * record propri) vengono eliminati. Aggiunge infine
	 * {@code newUnclusteredRecords} (record della stessa componente connessa
	 * mai visti prima) a {@code target}.
	 */
	public Cluster mergeClusters(Cluster target, Collection<Cluster> losers, Collection<Record> newUnclusteredRecords) {
		final EntityManager entityManager = openEntityManager();
		entityManager.getTransaction().begin();

		final Cluster managedTarget = entityManager.find(Cluster.class, target.getId());
		for (final Cluster loser : losers) {
			final Cluster managedLoser = entityManager.find(Cluster.class, loser.getId());

			// Copia perché addRecord/remove modificano gli insiemi sottostanti
			// durante l'iterazione.
			final Set<Record> loserRecords = new HashSet<>(managedLoser.getRecords());
			for (final Record rec : loserRecords) {
				managedTarget.addRecord(rec);
				managedLoser.getRecords().remove(rec);
			}
			// Forza gli UPDATE su record.cluster_id PRIMA della remove: a
			// questo punto il loser non ha più record figli in memoria, cosi'
			// il cascade/orphanRemoval su Cluster.records non cancella
			// accidentalmente i record appena migrati al target.
			entityManager.flush();
			entityManager.remove(managedLoser);
		}

		for (final Record rec : newUnclusteredRecords) {
			mergeBlockingKeys(entityManager, rec);
			managedTarget.addRecord(rec);
		}
		syncClusterBlockingKeys(managedTarget);
		entityManager.merge(managedTarget);

		entityManager.getTransaction().commit();

		return managedTarget;
	}

	private static void mergeBlockingKeys(EntityManager entityManager, Record rec) {
		final Set<BlockingKeyAttribute> bks = rec.getBlockingKeys().stream().map(bk -> entityManager.merge(bk))
			.collect(Collectors.toSet());
		rec.setBlockingKeys(bks);
	}

	/**
	 * {@link Cluster#addRecord(Record)} aggiorna {@code cluster.blockingKeys}
	 * (che sostiene la tabella {@code clusterBlock}) solo quando il record
	 * passato era GIA' presente nell'insieme (bug nel codice framework non
	 * toccato, vedi {@code CLAUDE.md}) — nel flusso normale di aggiunta di un
	 * record nuovo questo non scatta mai, quindi {@code clusterBlock} non
	 * verrebbe mai popolata. Questo metodo lo compensa esplicitamente,
	 * ricalcolando l'unione delle blocking key di tutti i record del cluster.
	 */
	private static void syncClusterBlockingKeys(Cluster cluster) {
		final Set<BlockingKeyAttribute> union = cluster.getRecords().stream()
			.flatMap(r -> r.getBlockingKeys().stream())
			.collect(Collectors.toSet());
		cluster.setBlockingKeys(union);
	}
}

/*
 * StandardServiceRegistry ssr = new
 * StandardServiceRegistryBuilder().configure("hibernate.cfg.xml").build();
 * Metadata meta = new MetadataSources(ssr).getMetadataBuilder().build();
 * 
 * SessionFactory factory = meta.getSessionFactoryBuilder().build(); Session
 * session = factory.openSession();
 * 
 * Transaction t = session.beginTransaction();
 * 
 * t.commit();
 * 
 * factory.close(); session.close();
 */