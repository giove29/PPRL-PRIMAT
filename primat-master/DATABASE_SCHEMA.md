# Schema del database Postgres

Questo documento descrive lo schema Postgres usato da `primat-linkage-unit-service` per la persistenza cross-run della Link Table (vedi `ARCHITECTURE_FLOW.md` sezione B per il flusso applicativo, `TESTING.md` per come verificarlo, `CLAUDE.md` per il changelog). Qui invece ci si concentra solo sulla **struttura del DB**: tabelle, colonne, e cosa significa una riga.

**4 database distinti dal 2026-09-21 (erano 5 dal 2026-09-17, 3 dal 2026-09-16), stesso schema ripetuto identico in ognuno** — uno per ciascuna strategia di clustering persistente: `primat_center_clustering`, `primat_mscd_ap`, `primat_global_greedy`, `primat_clip` (nomi di default, sovrascrivibili da `database.url` nella config JSON della Linkage Unit). Le tabelle di ciascuno sono generate automaticamente da Hibernate a partire dalle stesse entity JPA (`hibernate.hbm2ddl.auto=update`, 4 persistence-unit — `PrimatCenterClustering`/`PrimatMscdAp`/`PrimatGlobalGreedy`/`PrimatClip` — in `primat-linkage-unit/src/main/resources/META-INF/persistence.xml`) — non esiste uno script SQL scritto a mano, questo documento è la trascrizione leggibile di quello schema (valido per tutti e 4 i DB, cambia solo quale processo scrive su quale). Per svuotare uno o tutti questi database senza smontare/ricreare le tabelle a mano, vedi `db_reset_scripts/reset_db.py` (esegue lo stesso `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` descritto più sotto).

**Chi scrive sul DB**: le 4 strategie persistenti (Center Clustering, MSCD-AP, Global Greedy, CLIP), ciascuna esclusivamente sul proprio database, tramite `DbConnection` (`primat-linkage-unit`, dal 2026-09-16 una classe normale con `EntityManagerFactory` per persistence-unit, non più un singleton enum). MCL (l'unica strategia non persistente) non è persistente per design e non tocca mai queste tabelle — vedi `ARCHITECTURE_FLOW.md`.

## Le tabelle, in breve

```
party (1) ──< record (N) >── (1) cluster
                │ │                 │
                │ └──< block >──┐   ├──< clusterBlock >──┐
                │                │   │                    │
                └──< qidattribute│   └── physrep ──> record│
                                 │                          │
                                 └──> blockingkeyattribute <┘

partyencodingstate  (tabella indipendente, PK = party, nessuna FK)
```

- Un `party` ha molti `record`.
- Un `record` appartiene (al più) a un `cluster`; un `cluster` ha molti `record`.
- Un `record` ha molti `qidattribute` (i suoi campi anagrafici codificati/originali) e molti `blockingkeyattribute` (tramite la tabella ponte `block`).
- Un `cluster` ha anche le proprie `blockingkeyattribute` (tramite `clusterBlock`) e un `record` "rappresentante" (`physrep`).

## `party`

Entity: `Party`. Una riga = un Data Owner conosciuto dalla Linkage Unit.

| Colonna | Tipo | Significato |
|---|---|---|
| `name` (**PK**) | varchar | identificativo del party, es. `A`, `B`, `C` — lo stesso dichiarato nel campo `"party"` del JSON di configurazione passato a `DataOwnerService` |
| `duplicatefree` | boolean | se `true`, il party è considerato una "clean source" (nessun duplicato interno) — usato da MSCD-AP/`ApConfig.addCleanSource(...)`; per Global Greedy/CLIP deve essere `true` per **tutte** le party, per Center Clustering/MCL `false` per tutte (validato da `LinkageUnitConfigLoader` e da guardia runtime in `MultiSourceLinkage`). Il flag e' scritto solo alla prima persistenza e non piu' modificabile: se la config lo cambia, `DbConnection.addParties` abortisce (`checkPartyFlag`) e il DB va resettato; `python_evaluation` lo legge da qui per la regola within-party |

## `record`

Entity: `Record`. Una riga = un'impronta (RBF) ricevuta da un Data Owner in un qualunque run passato o presente.

| Colonna | Tipo | Significato |
|---|---|---|
| `id` (**PK**) | varchar | id locale del record, assegnato dal Data Owner (in chiaro, non è dato sensibile — non è nome/cognome, solo un identificativo di riga) |
| `gid` | varchar | ground truth (`GLOBAL_ID`) — valorizzato solo nei CSV sintetici di test, **mai** trasmesso dal Data Owner reale via MQTT |
| `cluster_id` (**FK** → `cluster.id`) | int, nullable | se valorizzato: questo record è già stato riconciliato con un'entità nota. Se `null`: il record esiste (perché arrivato in un run) ma non è ancora stato assegnato a nessun cluster — condizione transitoria durante l'elaborazione, non dovrebbe restare `null` a fine run per record passati a MSCD-AP |
| — | — | il `party` di appartenenza **non** è una colonna qui: è derivato dalla relazione inversa `party.records`, il campo Java è `@Transient` |

**Riga interessante da controllare**: `SELECT id, cluster_id FROM record WHERE cluster_id IS NULL` — record MSCD-AP mai riconciliati (se il flusso ha girato correttamente, non dovrebbe restituire righe per i record già processati).

## `cluster`

Entity: `Cluster`. Id da sequence dedicata `cluster_seq` (allocationSize=50, dal 2026-09-21; prima `hibernate_sequence` condivisa → conflitto `Found [1] and [50]`). DB creati prima di questa modifica vanno resettati (`db_reset_scripts/reset_db.py --yes`). Una riga = un'**entità reale riconosciuta nel tempo** (una persona, secondo la Linkage Unit). Questo è l'oggetto che rimane stabile tra run: lo stesso `id` deve ripresentarsi ai run successivi finché quell'entità non viene fusa con un'altra.

| Colonna | Tipo | Significato |
|---|---|---|
| `id` (**PK**, auto) | int | identificativo stabile dell'entità — è il numero che vedi ripetersi identico tra un run e l'altro nei log dell'orchestratore |
| `physrep` (**FK** → `record.id`) | varchar, nullable | il "record fisico rappresentante" del cluster (tipicamente il primo record che ha originato il cluster) |
| colonne di `cbf` (embedded `CountingBloomFilter`) | `INTEGER[]` (`virtrep`) + numerico | un Bloom filter di conteggio aggregato del cluster, aggiornato quando il cluster cresce — non usato dal path di persistenza attuale (`DbConnection`/`PersistentLinkTableBuilder` non lo popolano), resta a `null`/default |

**Una riga in questa tabella non viene mai cancellata per "smontaggio"**: può solo apparire (nuova entità) o essere estesa con record nuovi — mai divisa né fusa da un run (dal 2026-09-24 `ClusterAssignmentPlanner` assegna ogni record fresco a un solo cluster e rispetta il vincolo clean-source; `DbConnection.mergeClusters` resta nel codice ma non è più chiamato). Questa è la garanzia di stabilità descritta in `CONCEPTUAL_FLOW.md`.

## `qidattribute`

Entity: `QidAttribute` (astratta) + sottotipi concreti `BitSetAttribute`, `StringAttribute`, `NumericAttribute`, `DateAttribute`, `IntegerSetAttribute`, `XorBitSetAttribute`. Mappata con **single-table inheritance**: un'unica tabella fisica per tutti i sottotipi, con colonna discriminante automatica di Hibernate (`DTYPE`) e colonne diverse valorizzate a seconda del tipo reale della riga. Una riga = **un attributo codificato di un record** (nella pipeline attuale, l'unico sottotipo realmente popolato è `BitSetAttribute`, cioè l'RBF).

| Colonna | Tipo | Significato |
|---|---|---|
| `id` (**PK**, auto) | int | id tecnico dell'attributo |
| `record_id` (**FK** → `record.id`) | varchar | a quale record appartiene |
| `position` | int | posizione nella lista attributi del record (ordine di inserimento in schema) |
| `bsv` | `BIGINT[]` | **il bitset dell'RBF** (`BitSetAttribute`) — questa è la colonna che conta nella pipeline reale: rappresenta l'impronta Bloom-filter offuscata del record, mai il dato in chiaro |
| `sv` | varchar | valore stringa (`StringAttribute`, non usato dal path RBF) |
| `nv` | numeric | valore numerico (`NumericAttribute`, non usato) |
| `dv` | date | valore data (`DateAttribute`, non usato) |
| `isv` | `INTEGER[]` | set di interi (`IntegerSetAttribute`, non usato) |
| colonne di `XorBitSetAttribute`/`XorBitSet` | `BIGINT[]` (`xbsv`) + altro | variante XOR del bitset, non usata dal path attuale |

Per un record RBF tipico, solo `bsv` è non-`null`; tutte le altre colonne tipo-specifiche restano `null` sulla stessa riga.

## `blockingkeyattribute`

Entity: `BlockingKeyAttribute`, chiave composta embedded `BlockingKeyId`. Una riga = **un valore di chiave di blocking** generato dal blocker (JaccardLSH) per almeno un record o cluster.

| Colonna | Tipo | Significato |
|---|---|---|
| `blockingkeyid` (**PK composta**) | int | indice della funzione hash/permutazione LSH che ha generato questo valore (0..`keys-1`) |
| `blockingKeyValue` (**PK composta**) | varchar | il valore della chiave calcolato per quella permutazione — due record con lo stesso `(blockingkeyid, blockingKeyValue)` cadono nello stesso bucket di blocking |

Non contiene mai dato in chiaro: è un valore derivato dall'RBF tramite l'hashing LSH, non dal testo originale.

## `block`

Tabella ponte (many-to-many `record` ↔ `blockingkeyattribute`, nessuna entity Java dedicata). Una riga = "questo record cade in questo bucket di blocking".

| Colonna | Tipo | Significato |
|---|---|---|
| `recordid` | varchar (**FK** → `record.id`) | il record |
| `blockingkeyid`, `blockingKeyValue` | (**FK** → `blockingkeyattribute`) | il bucket |

## `clusterBlock`

Stessa struttura di `block`, ma per `cluster` invece di `record`. Una riga = "questo cluster (entità nota) cade in questo bucket di blocking" — usata da `DbConnection.getCandidateClusters(...)` (join nativo `clusterBlock ↔ BlockStaging`) per bloccare i cluster storici contro i soli record nuovi di un run, invece di ricaricare e riblockare l'intero storico (vedi `ARCHITECTURE_FLOW.md`).

Popolata esplicitamente da `DbConnection.syncClusterBlockingKeys(cluster)` (metodo privato, chiamato prima del `persist`/`merge` finale in `persistNewCluster`/`extendCluster`/`mergeClusters`), che ricalcola l'unione delle blocking key di tutti i record del cluster — **non** da `Cluster.addRecord(Record)` (codice framework non toccato): quel metodo popola `cluster.blockingKeys` solo quando il record passato era già presente nell'insieme, condizione che nel flusso normale (record nuovo e distinto) non si verifica mai. Conseguenza pratica: i cluster persistiti prima di questa modifica hanno `clusterBlock` vuota e non emergeranno come candidati finché non vengono ri-persistiti da zero (nessuna migrazione prevista, vedi `TESTING.md`).

## `partyencodingstate` (2026-09-23)

Entity `PartyEncodingState` (`primat-common/.../model/`). Nessuna relazione con le altre tabelle (nessuna FK dichiarata, anche se `party` coincide logicamente con `party.name`). Una riga = "con quale configurazione di encoding questo party ha persistito i suoi dati su **questo specifico DB**" — una riga per party, non per run.

| Colonna | Tipo | Significato |
|---|---|---|
| `party` (**PK**) | varchar | nome del party, stesso valore di `party.name` |
| `bitlength` | int | lunghezza effettiva dell'RBF dopo l'hardening, calcolata con certezza da `DataOwnerConfig.computeEffectiveRbfBitLength()` (es. dimezzata da un `XorFolder`). Solo informativo/diagnostico qui — non è la base del confronto, vedi sotto |
| `confighash` | varchar(100) | digest **non reversibile** (HMAC-SHA384 in Base64, `DeterministicHashing.digestBase64(...)`) dell'intera configurazione di encoding del party — lunghezza RBF, hardening, missing-value handling, e per ogni colonna QID dataType/hashFunctions/salt/CWE/missingValueTokenCount. Il testo che genera il digest non lascia mai il Data Owner: solo il digest viaggia fino a qui |

**Perché un digest e non una descrizione o solo la lunghezza**: la Linkage Unit non deve mai ricevere salt/hashFunctions/CWE o altri dettagli implementativi della codifica del Data Owner (principio di minimizzazione dei dati in un sistema PPRL — un tentativo precedente che trasmetteva una stringa descrittiva è stato scartato per questo). Ma confrontare solo la lunghezza dell'RBF (un tentativo intermedio, anch'esso scartato) non rileva un cambio di salt/hashFunctions/CWE che lasci invariata la lunghezza — le blocking key cambierebbero comunque, con lo stesso rischio di corruzione. Un digest cattura qualunque cambiamento nella configurazione, qualunque esso sia, senza mai rivelarne il contenuto.

Scritta/verificata da `DbConnection.checkEncodingState(Map<String, DbConnection.EncodingSnapshot>)`, chiamata da `LinkageUnitOrchestrator.collectRbf(...)` **prima** di decodificare gli RBF ricevuti in `Record` e prima di qualunque `persistNewClusters`/`getCandidateClusters`: se un party non ha ancora una riga qui, viene creata al volo (prima persistenza su questo DB); se ce l'ha già e `confighash` del run corrente è diverso, la transazione viene fatta rollback e il run abortisce con un `IllegalStateException` leggibile — arricchito con `bitlength` solo per dire se *anche* la dimensione dell'RBF è cambiata, o se è rimasta invariata (quindi il responsabile è un altro parametro) — **senza toccare `record`/`cluster`/`block`/`clusterBlock`**.

Serve a prevenire un problema reale: `record`/`cluster` sono riconciliati tra run in base all'overlap delle blocking key JaccardLSH derivate dall'RBF (vedi sopra); se la configurazione di encoding di un party cambia tra un run e l'altro (es. si attiva l'XOR-folding, oppure cambia solo un salt) **senza svuotare il DB**, le blocking key del record cambiano, la LU non riconosce più un record già noto come tale, e il tentativo di re-inserirlo con lo stesso `record.id` deterministico produce un `duplicate key value violates unique constraint "record_pkey"` — oppure, nei casi che non collidono su un id esistente, un secondo cluster duplicato silenzioso per la stessa persona reale, senza alcun errore. Questa tabella intercetta il cambio **prima** che uno dei due scenari possa verificarsi, qualunque sia il parametro cambiato.

## Query di verifica utili

Conteggio record per cluster (quanti record compongono ogni entità nota):
```sql
SELECT cluster_id, count(*) FROM record WHERE cluster_id IS NOT NULL GROUP BY cluster_id ORDER BY cluster_id;
```

Cluster totali e record totali (da confrontare tra due run consecutivi sugli stessi dati: devono coincidere, vedi `TESTING.md` punto 6):
```sql
SELECT (SELECT count(*) FROM cluster) AS n_cluster, (SELECT count(*) FROM record) AS n_record;
```

Record non ancora riconciliati (dovrebbe essere vuoto a fine run per i record passati a MSCD-AP):
```sql
SELECT id FROM record WHERE cluster_id IS NULL;
```

Conteggio righe in `clusterBlock` (deve essere > 0 dopo il primo run MSCD-AP su un DB pulito, prova che `syncClusterBlockingKeys` sta popolando la tabella):
```sql
SELECT count(*) FROM clusterblock;
```

Contenuto di un'entità specifica (tutti i record, con party, che compongono il cluster 3):
```sql
SELECT r.id, p.name AS party FROM record r JOIN party p ON r.id LIKE p.name || '%' WHERE r.cluster_id = 3;
```
*(nota: non esiste una colonna `party` diretta su `record` — se serve il party per riga, va risalito dal contesto applicativo/dal CSV originale, non dal DB da solo)*

Reset completo per ripartire da un DB pulito (usato durante il testing end-to-end):
```sql
DROP SCHEMA public CASCADE; CREATE SCHEMA public;
```
