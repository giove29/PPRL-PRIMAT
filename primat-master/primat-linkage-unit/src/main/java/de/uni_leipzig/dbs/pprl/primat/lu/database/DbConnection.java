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
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyEncodingState;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BlockingKeyAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.HomogenPair;
import de.uni_leipzig.dbs.pprl.primat.common.utils.Pair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.utils.ProgressListener;


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

	private static final int FLUSH_INTERVAL = 500;

	private static final int IN_CLAUSE_CHUNK = 1000;

	private final EntityManagerFactory entityManagerFactory;

	public DbConnection(String persistenceUnitName, String jdbcUrl, String jdbcUser, String jdbcPassword) {
		final Map<String, Object> overrides = new HashMap<>();
		overrides.put("hibernate.show_sql", String.valueOf(SQL_DEBUG_LOGGING));
		overrides.put("hibernate.format_sql", String.valueOf(SQL_DEBUG_LOGGING));
		overrides.put("hibernate.jdbc.batch_size", "50");
		overrides.put("hibernate.order_inserts", "true");
		overrides.put("hibernate.order_updates", "true");
		overrides.put("hibernate.jdbc.batch_versioned_data", "true");
		overrides.put("javax.persistence.jdbc.url", withRewriteBatchedInserts(jdbcUrl));
		overrides.put("javax.persistence.jdbc.user", jdbcUser);
		overrides.put("javax.persistence.jdbc.password", jdbcPassword);
		this.entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, overrides);
	}

	private static String withRewriteBatchedInserts(String jdbcUrl) {
		if (jdbcUrl == null || jdbcUrl.contains("reWriteBatchedInserts")) {
			return jdbcUrl;
		}
		return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "reWriteBatchedInserts=true";
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

	/**
	 * Istantanea dell'encoding di un party per questo run, cosi' come
	 * ricevuta dal suo Data Owner: la lunghezza in chiaro dell'RBF (non
	 * sensibile) e il digest non reversibile dell'intera configurazione
	 * (vedi {@code DataOwnerConfig#computeConfigHash()}).
	 */
	public static final class EncodingSnapshot {

		private final int bitLength;
		private final String configHash;

		public EncodingSnapshot(int bitLength, String configHash) {
			this.bitLength = bitLength;
			this.configHash = configHash;
		}

		public int getBitLength() {
			return bitLength;
		}

		public String getConfigHash() {
			return configHash;
		}
	}

	/**
	 * Verifica, per ogni party, che la configurazione di encoding di questo
	 * run coincida con quella salvata da un run precedente su questo stesso DB
	 * (prima riga vista per quel party -> viene semplicemente registrata). Va
	 * chiamata prima di decodificare gli RBF in {@code Record} e prima di
	 * qualunque {@code persistNewClusters}/{@code getCandidateClusters}: una
	 * configurazione cambiata altera le blocking key JaccardLSH derivate
	 * dall'RBF, rompendo silenziosamente il riconoscimento "stesso record
	 * fisico gia' visto" su cui si basa la persistenza incrementale (un
	 * record noto verrebbe trattato come nuovo, con conseguente duplicate-key
	 * sul suo id deterministico oppure, se l'id non collide, un cluster
	 * duplicato silenzioso per la stessa persona reale).
	 *
	 * <p>Confronto decisivo su {@code configHash} (cattura qualunque cambio,
	 * inclusi salt/hashFunctions/CWE che lascino invariata la lunghezza
	 * dell'RBF — limite che il solo confronto sulla lunghezza non copriva):
	 * il digest e' un valore non reversibile, mai la configurazione in
	 * chiaro, coerente con la minimizzazione dei dati verso la LU in un
	 * sistema PPRL. {@code bitLength} resta comunque salvato e confrontato
	 * solo per arricchire il messaggio d'errore (permette di dire se e' anche
	 * la dimensione dell'RBF a essere cambiata, senza rivelare altro).
	 *
	 * @throws IllegalStateException se la configurazione di encoding di un
	 *                                party e' cambiata rispetto a quanto
	 *                                salvato in precedenza
	 */
	public void checkEncodingState(Map<String, EncodingSnapshot> snapshotsByParty) {
		final EntityManager entityManager = openEntityManager();
		try {
			entityManager.getTransaction().begin();

			for (final Map.Entry<String, EncodingSnapshot> entry : snapshotsByParty.entrySet()) {
				final String party = entry.getKey();
				final EncodingSnapshot current = entry.getValue();
				final PartyEncodingState existing = entityManager.find(PartyEncodingState.class, party);

				if (existing == null) {
					entityManager.persist(
							new PartyEncodingState(party, current.getBitLength(), current.getConfigHash()));
				}
				else if (!existing.getConfigHash().equals(current.getConfigHash())) {
					entityManager.getTransaction().rollback();
					final String dimensionNote = existing.getBitLength() != current.getBitLength()
							? "anche la dimensione dell'RBF e' cambiata: " + existing.getBitLength() + " -> "
									+ current.getBitLength() + " bit."
							: "la dimensione dell'RBF e' invariata (" + current.getBitLength()
									+ " bit): controlla salt/hashFunctions/CWE lato Data Owner.";
					throw new IllegalStateException("La configurazione di encoding del party '" + party
							+ "' e' cambiata rispetto a un run precedente su questo DB: le blocking key derivate "
							+ "non sarebbero piu' comparabili con lo storico persistito. " + dimensionNote
							+ "\nSvuota il DB (db_reset_scripts/reset_db.py) oppure ripristina la configurazione "
							+ "originale di questo party prima di continuare.");
				}
			}

			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}
	}

	public void addParties(Set<Party> parties) {
		final EntityManager entityManager = openEntityManager();
		try {
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
		finally {
			entityManager.close();
		}
	}

	public void addBlockingKeysStaging(Collection<Record> data) {
		final EntityManager entityManager = openEntityManager();
		try {
			entityManager.getTransaction().begin();
			stageBlockingKeys(entityManager, data);
			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}
	}

	/**
	 * Ricrea la tabella temporanea {@code BlockStaging} e la popola con le
	 * blocking key di {@code data} tramite batch JDBC (una INSERT per riga
	 * costerebbe un round-trip ciascuna: centinaia di migliaia per run).
	 * Deve girare sulla stessa connessione/transazione della query che la usa.
	 */
	private static void stageBlockingKeys(EntityManager entityManager, Collection<Record> data) {
		entityManager.unwrap(org.hibernate.Session.class).doWork(connection -> {
			try (java.sql.Statement st = connection.createStatement()) {
				st.execute("DROP TABLE IF EXISTS BlockStaging");
				st.execute("CREATE TEMPORARY TABLE BlockStaging( "
					+ "blockingkeyid INTEGER NOT NULL, blockingkeyvalue VARCHAR NOT NULL, "
					+ "recordId VARCHAR NOT NULL, party_name VARCHAR NOT NULL, duplicatefree BOOLEAN NOT NULL, "
					+ "PRIMARY KEY (blockingkeyid, blockingkeyvalue, recordId))");
			}
			try (java.sql.PreparedStatement ps = connection.prepareStatement(
				"INSERT INTO BlockStaging (blockingkeyid, blockingkeyvalue, recordid, party_name, duplicatefree) "
					+ "VALUES (?, ?, ?, ?, ?)")) {
				int pending = 0;
				for (final Record rec : data) {
					for (final BlockingKeyAttribute bka : rec.getBlockingKeys()) {
						ps.setInt(1, bka.getBlockingKeyId().getBlockingKeyId());
						ps.setString(2, bka.getBlockingKeyId().getBlockingKeyValue());
						ps.setString(3, rec.getId());
						ps.setString(4, rec.getParty().getName());
						ps.setBoolean(5, rec.getParty().isDuplicateFree());
						ps.addBatch();
						if (++pending >= 5000) {
							ps.executeBatch();
							pending = 0;
						}
					}
				}
				if (pending > 0) {
					ps.executeBatch();
				}
			}
		});
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

		// Query separata (stessa Session, righe sotto): vedi il commento
		// equivalente in getCandidateClusters, stesso motivo — fetchare anche
		// r.blockingKeys nella query sopra moltiplicherebbe ulteriormente un
		// fan-out già a 2 collezioni per record.
		if (!records.isEmpty()) {
			entityManager.createQuery(
				"SELECT r FROM Record r LEFT JOIN FETCH r.blockingKeys WHERE r IN :records",
				Record.class)
				.setParameter("records", records)
				.getResultList();
		}

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
		final EntityManager entityManager = openEntityManager();
		try {
			entityManager.getTransaction().begin();

			// DB senza cluster storici (es. primo run): nessun candidato possibile,
			// inutile popolare lo staging.
			if (entityManager.createNativeQuery("SELECT 1 FROM clusterblock LIMIT 1").getResultList().isEmpty()) {
				entityManager.getTransaction().commit();
				return Set.of();
			}

			stageBlockingKeys(entityManager, newRecords);

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

			// Query separata, stessa EntityManager/Session: inizializza
			// record.blockingKeys sulle stesse istanze già managed (Hibernate le
			// riconosce per id, non ne crea copie) senza aggiungere una quarta
			// collezione alla query sopra. Fetchare 3 collezioni insieme
			// (records/attributes/clusterBlockingKeys) produce già un fan-out
			// combinatorio sulle righe SQL restituite; una quarta (le blocking
			// key per-record, tante quante le chiavi LSH configurate) moltiplica
			// ulteriormente quel fan-out fino a esaurire lo heap con migliaia di
			// cluster storici (osservato: OutOfMemoryError con 7188 cluster).
			final List<Record> allRecords = clusters.stream()
				.flatMap(c -> c.getRecords().stream())
				.collect(Collectors.toList());
			if (!allRecords.isEmpty()) {
				entityManager.createQuery(
					"SELECT r FROM Record r LEFT JOIN FETCH r.blockingKeys WHERE r IN :records",
					Record.class)
					.setParameter("records", allRecords)
					.getResultList();
			}

			entityManager.getTransaction().commit();

			return clusters;
		}
		finally {
			entityManager.close();
		}
	}

	/**
	 * Persiste un nuovo {@link Cluster} contenente tutti i {@code records}
	 * passati (una componente connessa senza alcun cluster persistito
	 * preesistente).
	 */
	public Cluster persistNewCluster(ClusterFactory clusterFactory, Collection<Record> records) {
		return persistNewClusters(clusterFactory, java.util.Collections.singletonList(records), ProgressListener.NOOP)
			.get(0);
	}

	/**
	 * Persiste in blocco un nuovo {@link Cluster} per ciascuna componente
	 * passata: un solo {@link EntityManager}, una sola transazione, blocking
	 * key pre-caricate con poche query invece di un {@code merge} (SELECT) per
	 * ciascuna, flush periodico per attivare il batching JDBC.
	 *
	 * @return i cluster creati, nello stesso ordine di {@code components}
	 */
	public List<Cluster> persistNewClusters(ClusterFactory clusterFactory, List<? extends Collection<Record>> components,
			ProgressListener progress) {
		final List<Cluster> clusters = new java.util.ArrayList<>(components.size());
		if (components.isEmpty()) {
			return clusters;
		}

		final EntityManager entityManager = openEntityManager();
		try {
			entityManager.getTransaction().begin();

			final Map<BlockingKeyAttribute, BlockingKeyAttribute> managedKeys = loadManagedBlockingKeys(entityManager,
				components);

			int sinceFlush = 0;
			for (final Collection<Record> records : components) {
				Cluster cluster = null;
				for (final Record rec : records) {
					rec.setBlockingKeys(rec.getBlockingKeys().stream().map(managedKeys::get).collect(Collectors.toSet()));
					if (cluster == null) {
						cluster = clusterFactory.build(rec);
					}
					else {
						cluster.addRecord(rec);
					}
				}
				syncClusterBlockingKeys(cluster);
				entityManager.persist(cluster);
				clusters.add(cluster);

				if (++sinceFlush >= FLUSH_INTERVAL) {
					entityManager.flush();
					sinceFlush = 0;
					progress.update(clusters.size(), components.size());
				}
			}
			entityManager.flush();
			progress.update(components.size(), components.size());

			entityManager.getTransaction().commit();
		}
		catch (RuntimeException e) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
		return clusters;
	}

	/**
	 * Restituisce, per ogni blocking key usata dai record delle componenti,
	 * l'istanza gestita dall'{@code entityManager}: quelle già a DB vengono
	 * caricate con una query per ({@code blockingKeyId}, blocco di valori), le
	 * mancanti vengono persistite una sola volta.
	 */
	private static Map<BlockingKeyAttribute, BlockingKeyAttribute> loadManagedBlockingKeys(EntityManager entityManager,
			List<? extends Collection<Record>> components) {
		final Map<Integer, Set<String>> valuesByKeyId = new HashMap<>();
		for (final Collection<Record> records : components) {
			for (final Record rec : records) {
				for (final BlockingKeyAttribute bk : rec.getBlockingKeys()) {
					valuesByKeyId.computeIfAbsent(bk.getBlockingKeyId().getBlockingKeyId(), k -> new HashSet<>())
						.add(bk.getBlockingKeyId().getBlockingKeyValue());
				}
			}
		}

		final Map<BlockingKeyAttribute, BlockingKeyAttribute> managed = new HashMap<>();
		for (final Map.Entry<Integer, Set<String>> entry : valuesByKeyId.entrySet()) {
			final List<String> values = new java.util.ArrayList<>(entry.getValue());
			for (int from = 0; from < values.size(); from += IN_CLAUSE_CHUNK) {
				final List<String> chunk = values.subList(from, Math.min(from + IN_CLAUSE_CHUNK, values.size()));
				final List<BlockingKeyAttribute> found = entityManager.createQuery(
					"SELECT b FROM BlockingKeyAttribute b WHERE b.blockingKeyId.blockingKeyId = :id "
						+ "AND b.blockingKeyId.blockingKeyValue IN :vals",
					BlockingKeyAttribute.class).setParameter("id", entry.getKey()).setParameter("vals", chunk)
					.getResultList();
				for (final BlockingKeyAttribute bk : found) {
					managed.put(bk, bk);
				}
			}
			for (final String value : entry.getValue()) {
				final BlockingKeyAttribute probe = new BlockingKeyAttribute(entry.getKey(), value);
				if (!managed.containsKey(probe)) {
					entityManager.persist(probe);
					managed.put(probe, probe);
				}
			}
		}
		return managed;
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
		try {
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
		finally {
			entityManager.close();
		}
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
		try {
			return doMergeClusters(entityManager, target, losers, newUnclusteredRecords);
		}
		finally {
			entityManager.close();
		}
	}

	private static Cluster doMergeClusters(EntityManager entityManager, Cluster target, Collection<Cluster> losers,
			Collection<Record> newUnclusteredRecords) {
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
