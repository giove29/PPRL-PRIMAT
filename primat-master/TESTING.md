# Testing indipendente della pipeline PPRL Data Owner / Linkage Unit

Tutorial per costruire ed eseguire la pipeline (`primat-mqtt-common`, `primat-data-owner-service`, `primat-linkage-unit-service`) senza dover rileggere il codice sorgente.

## 1. Prerequisiti

- Java 11+
- Maven. Se non è nel PATH, usa quello bundlato con IntelliJ IDEA:
  ```
  MVN="C:/Program Files/JetBrains/IntelliJ IDEA <versione>/plugins/maven-plugin/lib/maven3/bin/mvn"
  JAVA_HOME="C:/Program Files/JetBrains/IntelliJ IDEA <versione>/jbr"
  ```
- Broker MQTT: nessuna installazione esterna, ma è un **processo indipendente** (Moquette, `EmbeddedBrokerLauncher.main`, modulo `primat-mqtt-common`), da avviare per primo — vedi sezione 2bis. Dal 2026-09-21 non è più embedded nella Linkage Unit.
- **PostgreSQL**, per 4 delle 5 strategie di clustering (Center Clustering, MSCD-AP, Global Greedy, CLIP — ciascuna con il proprio database dedicato dal 2026-09-16/2026-09-17, vedi `ARCHITECTURE_FLOW.md`/`DATABASE_SCHEMA.md`): **1 solo container**, **4 database** al suo interno:
  ```bash
  docker run -d --name primat-postgres -e POSTGRES_USER=primat -e POSTGRES_PASSWORD=primat -p 5432:5432 postgres:16
  docker exec primat-postgres psql -U primat -c "CREATE DATABASE primat_center_clustering"
  docker exec primat-postgres psql -U primat -c "CREATE DATABASE primat_mscd_ap"
  docker exec primat-postgres psql -U primat -c "CREATE DATABASE primat_global_greedy"
  docker exec primat-postgres psql -U primat -c "CREATE DATABASE primat_clip"
  ```
  Le tabelle di ciascun DB vengono create/aggiornate da sole al primo avvio (`hibernate.hbm2ddl.auto=update`). Senza Postgres raggiungibile, la strategia **MCL** funziona comunque (non persiste su DB per design); le altre 4 no.
  Per svuotare uno o più di questi database senza cancellare il container (es. per ripetere un test end-to-end da zero), vedi `db_reset_scripts/reset_db.py` (sezione 9bis).

## 2. Build

Dalla root del reactor (`primat-master/primat-master`, quella con il `pom.xml` principale):

```bash
"$MVN" -pl primat-mqtt-common,primat-data-owner-service,primat-linkage-unit-service -am install -DskipTests
```

`-am` costruisce anche i moduli da cui dipendono (`primat-common`, `primat-data-owner`, `primat-linkage-unit`).

## 2bis. Avvio del broker MQTT (per primo)

Un terminale dedicato, dalla root del reactor; resta in ascolto fino a Ctrl+C (porta opzionale, default 1883):

```bash
"$MVN" -pl primat-mqtt-common exec:java -Dexec.args="1883"
```

Output atteso: `Broker MQTT in ascolto su tcp://0.0.0.0:1883 (Ctrl+C per fermarlo)`. Il broker è non persistente (i messaggi non sopravvivono a un suo riavvio). Data Owner e Linkage Unit lo raggiungono tramite `mqttBrokerUrl` nel proprio JSON (deve puntare a host/porta del broker; se cambi la porta qui, cambiala in tutti i JSON).

**Ordine di avvio**: il broker deve partire prima di tutto il resto, ma solo per la Linkage Unit — se manca, la LU fallisce dopo `mqtt.brokerConnectTimeoutSeconds` (default 30s) con `Broker MQTT non raggiungibile su ...`. I Data Owner invece ritentano all'infinito (ogni 2s) e si connettono appena il broker sale. Una volta up il broker, Data Owner e Linkage Unit possono partire in qualsiasi ordine: la LU si sottoscrive agli RBF prima di pubblicare e ripubblica il comando a ogni `rbfRepublishIntervalSeconds` finché tutti i Data Owner non rispondono.

## 3. Avvio dei Data Owner

Un processo per party, un solo argomento: il path del JSON di configurazione (party, broker MQTT, sorgente dati, schema colonne, tuning RBF — vedi `primat-data-owner-service/.../service/config/`). `exec-maven-plugin` è configurato nel `pom.xml` del modulo (2026-09-16), quindi non serve costruire il classpath a mano:

```bash
"$MVN" -pl primat-data-owner-service exec:java -Dexec.args="src/main/resources/config/examples/example_clean/party_A_clean.json"
```

Ripetere per `party_B_clean.json` e `party_C_clean.json` (o le varianti `example_dirty/*_dirty.json`; per FEBRL vedi `config/febrl/<scenario>/party_org*.json`, party `org`/`org1`, es. `config/febrl/febrl4_1_mixed/party_org_clean.json` + `party_org1_dirty.json`) (party diversi = client MQTT diversi, vedi `DataOwnerConfig.mqttClientId = "data-owner-" + party`), ciascuno in un terminale separato (il processo resta in ascolto all'infinito). I tre JSON d'esempio replicano lo schema/tuning NCVR usato finora; per puntare a dati propri basta un nuovo JSON (vedi sezione 7) senza toccare codice.

Esempio di contenuto (`party_A_clean.json`, abbreviato — vedi il file per lo schema completo a 9 colonne):
```json
{
  "party": "A",
  "mqttBrokerUrl": "tcp://localhost:1883",
  "dataSource": { "type": "CSV", "csv": { "filePath": "primat-examples/src/main/resources/synthetic_ncvr/party_A.csv" } },
  "bloomFilter": { "length": 1024, "hardening": { "type": "NONE" } },
  "columns": [
    { "index": 0, "name": "PARTY", "role": "PARTY" },
    { "index": 3, "name": "FN", "role": "QID", "dataType": "TEXT", "hashFunctions": 12, "salt": "FN_" }
  ]
}
```

Un JSON non valido (file assente, sintassi errata, colonne inconsistenti, ...) termina il processo con `Errore di configurazione: ...` ed exit code 1, senza avviare la connessione MQTT.

**Nota importante**: i Data Owner possono essere avviati anche PRIMA del broker — `MqttClientWrapper.connect()` ritenta la connessione all'infinito (ogni 2s) finché non è raggiungibile. Il broker va comunque avviato (sezione 2bis) prima di far partire la Linkage Unit.

Output atteso per ciascun Data Owner all'avvio: `[A] in ascolto su primat/do/A/cmd`.

## 4. Avvio dell'orchestratore (Linkage Unit)

Dal 2026-09-16 `LinkageUnitOrchestrator` è configurato via JSON (mirror del Data Owner): un solo argomento, il path del file di configurazione, che dichiara — tra le altre cose — **quale delle 5 strategie** girare (`clusteringMethod`: `CENTER_CLUSTERING`, `MSCD_AP`, `MCL`, `GLOBAL_GREEDY`, `CLIP` — le ultime due dal 2026-09-17; `MSCD_AP_NO_CLEAN` rimosso il 2026-09-21). Non c'è più auto-routing basato su dirty/clean, e un processo esegue **un solo run**. 5 file di esempio in `primat-linkage-unit-service/src/main/resources/config/`, uno per strategia (party NCVR `A`/`B`/`C`). Per FEBRL i JSON della LU stanno in `config/febrl/{febrl2_dirty,febrl3_dirty,febrl4_clean,febrl4_1_mixed}/<strategia>.json`, con party `org`/`org1` (stessi nomi dei Data Owner in `primat-data-owner-service/.../config/febrl/`):

```bash
"$MVN" -pl primat-linkage-unit-service exec:java -Dexec.args="src/main/resources/config/mscd_ap.json"
```

Il JSON ha il campo obbligatorio top-level `"mqttBrokerUrl": "tcp://localhost:1883"` (identico ai Data Owner, endpoint del broker avviato nella sezione 2bis); la sezione opzionale `mqtt` contiene solo i tuning `brokerConnectTimeoutSeconds` (default 30), `rbfCollectionTimeoutSeconds` (30), `rbfRepublishIntervalSeconds` (3).

Sostituire `mscd_ap.json` con `center_clustering.json` / `mcl.json` / `global_greedy.json` / `clip.json` per le altre 4 strategie (ciascuno un run a sé, ripetibile con gli stessi Data Owner senza riavviarli — basta rilanciare `exec:java` con un JSON diverso). `global_greedy.json`/`clip.json` richiedono party tutte `duplicateFree: true` (vincolo più stretto di MSCD-AP, che ne richiede solo una): un JSON con anche una sola party dirty viene rifiutato al caricamento con `LinkageUnitConfigException`.

Output atteso, in ordine:
1. `comando pubblicato su primat/do/<party>/cmd` (ripetuto ogni `rbfRepublishIntervalSeconds` finché non arrivano tutti gli RBF)
2. `record ricevuti per party` — conteggio record per A/B/C
3. `Blocking (JaccardLSH) - blocchi: N` — un solo blocker (JaccardLSH), nessun confronto con HammingLSH
4. `Strategia scelta: <CENTER_CLUSTERING|MSCD_AP|MCL|GLOBAL_GREEDY|CLIP>`: TP/FP/recall/precision/F-measure + i cluster della Link Table. Su dataset sintetico (3 party, 29 record) MSCD-AP arriva a recall/precision/F-measure 1,000 (verificato con un run end-to-end reale). Se `persistence.enabled` è (effettivamente) `false` — sempre il caso per MCL, opzionale per gli altri 4 — viene stampata anche la riga `Link Table scritta su CSV (persistence disabled): <path assoluto>` — vedi sezione 9 per come valutare offline quel file
5. Per rieseguire con gli stessi Data Owner (senza riavviarli) e verificare la stabilità dei `Cluster.id` tra run, rilanciare lo stesso comando con lo stesso JSON: per le 4 strategie persistenti i `Cluster.id` stampati nel secondo run devono coincidere con quelli del primo (stessi record → nessun nuovo cluster, vedi sezione 6); per MCL i `Cluster.id` **non** sono stabili tra run (nessuna persistenza, per design)

## 5. Come verificare la correttezza

Il ground truth è la colonna `GLOBAL_ID` nei CSV sintetici (`primat-examples/.../synthetic_ncvr/party_{A,B,C}.csv`): due record con lo stesso `GLOBAL_ID`, anche su party diversi, rappresentano la stessa entità reale. `IdEqualityTrueMatchChecker` confronta esattamente questo campo per contare TP/FP, ma solo sulle coppie **cross-party** (`MultiSourceLinkage.buildOutcome` filtra `aggregatedMatches` prima di passarle all'evaluator, fix 2026-09-15 — le coppie within-party, es. duplicati della stessa sorgente dirty, non sono ground truth cross-party e gonfiavano la recall oltre 1 se contate); `MultiSourceLinkage.countGroundTruthMatches()` conta tutte le coppie di ground truth attese tra ogni combinazione di party, usato come denominatore della recall.

Per verificare a mano: aprire i 3 CSV, individuare le entità con `GLOBAL_ID` ripetuto su più party, e controllare che compaiano nello stesso cluster della Link Table stampata.

## 6. Test del comportamento long-running e della persistenza

Con i Data Owner rimasti attivi tra un run e l'altro, rilanciare `exec:java` con lo stesso JSON di strategia è già il test: secondo comando pubblicato con un nuovo `runId` (topic RBF diversi per run, vedi `MqttTopics.rbfTopic`), stessi processi Data Owner, nessun riavvio.

Per verificare specificamente la **persistenza** di una delle 4 strategie persistenti tra run (vedi `ARCHITECTURE_FLOW.md`):
- con il database dedicato pulito, al primo run ogni entità genera un nuovo cluster;
- al secondo run (stessi dati) nessuna riga nuova in `record`/`cluster` e gli stessi `Cluster.id` stampati, perché ogni record viene riconosciuto come già mappato (query diretta sul DB della strategia: `SELECT id, count(*) FROM cluster JOIN record ON record.cluster_id=cluster.id GROUP BY id;`);
- modificando/aggiungendo record a un Data Owner tra un run e l'altro, i match con entità esistenti devono estendere lo stesso `cluster_id`, i non-match devono crearne uno nuovo.

**Nota — obbligo del DB pulito per questo test**: `buildPersistentInput` non ricarica tutto lo storico, ma interroga `clusterBlock` per i soli cluster candidati (`DbConnection.getCandidateClusters`, vedi `ARCHITECTURE_FLOW.md`). `clusterBlock` viene popolata esplicitamente da `syncClusterBlockingKeys` (bug pre-esistente in `Cluster.addRecord`, mai corretto — vedi `CLAUDE.md`): un DB con cluster persistiti **prima** di questo fix ha `clusterBlock` vuota per quei cluster, che quindi non verranno mai trovati come candidati e porteranno a duplicati silenziosi. Il test end-to-end va quindi rifatto su un DB pulito per la strategia in esame (`DROP SCHEMA public CASCADE; CREATE SCHEMA public;` sul database dedicato, vedi `DATABASE_SCHEMA.md`) cosi' tutti i cluster nascono già con `clusterBlock` popolata. Verifica aggiuntiva utile: `SELECT count(*) FROM clusterblock;` deve essere > 0 dopo il primo run.

## 7. Test con dati propri

Il CSV puo' avere uno schema di colonne qualunque (delimitatore `;`, nessun header, come letto da `DatasetReader`): non serve più rispettare le 9 colonne NCVR-style, basta dichiarare lo schema voluto nel JSON di configurazione del Data Owner (`columns`, vedi sezione 3):

- una colonna con `"role": "PARTY"` (obbligatoria, esattamente una) — dovrebbe combaciare con il campo `"party"` del JSON
- una colonna con `"role": "ID"` (obbligatoria, esattamente una) — id locale del record
- al più una colonna con `"role": "GLOBAL_ID"` (opzionale — ground truth, non trasmesso via MQTT, solo l'RBF lo è)
- una o più colonne con `"role": "QID"` (obbligatorio almeno una), ciascuna con `"dataType": "TEXT"` o `"NUMERIC"` (sceglie la `NormalizerChain`) e opzionalmente `"hashFunctions"`/`"salt"` per il tuning dell'RBF (default se omessi: `ColumnConfig.DEFAULT_HASH_FUNCTIONS` e `name + "_"`)

Non serve nessuna modifica di codice per cambiare i dati: basta un nuovo file JSON (`"dataSource.csv.filePath"` diverso e `columns` adattate, oppure `"type": "DB"` con `dataSource.db.{tableName,jdbcUrl,username,password}`) passato come unico argomento a `DataOwnerService`. Entrambe le sorgenti sono effettivamente lette (`CsvRecordSource`/`JdbcRecordSource`); per `DB` la tabella deve avere le colonne nello stesso ordine posizionale di `columns[].index`.

Per cambiare invece la composizione delle party o la strategia della Linkage Unit basta un nuovo JSON di config Linkage Unit (vedi sezione 4, schema completo nel bullet 2026-09-16 di `CLAUDE.md`): `parties[].duplicateFree`, `similarityThreshold`, parametri LSH/MCL/AP/Center Clustering hanno tutti un default che riproduce il comportamento storico se omessi.

## 8. Troubleshooting rapido

- **`Broker MQTT non raggiungibile su tcp://...`** (Linkage Unit, esce dopo `mqtt.brokerConnectTimeoutSeconds`): il broker non è stato avviato o `mqttBrokerUrl` non combacia con host/porta del broker. Avviarlo (sezione 2bis) e riprovare.
- **Timeout in `collectRbf`** (`Timeout in attesa degli RBF per il run ...: ricevuti da [...]`): uno dei Data Owner non è stato avviato/è ancora in retry di connessione, o il suo `mqttBrokerUrl` punta a un broker diverso da quello della LU. Verificare che tutti i processi Data Owner attesi (uno per party dichiarata nel JSON della Linkage Unit) risultino "in ascolto" prima di avviare l'orchestratore. Se il broker parte con `Address already in use`, la porta è occupata da un'altra istanza: fermarla o usare un'altra porta.
- **Errore di connessione Postgres per Center Clustering/MSCD-AP/Global Greedy/CLIP** (`PersistenceException`/`Connection refused`): il ramo MCL non dipende da Postgres, quindi resta disponibile anche senza DB; le altre 4 strategie lo richiedono, ciascuna sul proprio database (vedi sezione 1). Verificare `docker ps` (container `primat-postgres` in esecuzione) e che le credenziali/il nome DB nel JSON (`database.{url,user,password}`) combacino con quelli creati nel container.
- **`Campo obbligatorio 'database' mancante`**: il JSON usa `clusteringMethod` diverso da `MCL` ma non ha una sezione `database` — obbligatoria per le 4 strategie persistenti, vedi sezione 4.

## 9. Valutare offline le performance dei 5 algoritmi di clustering

Script Python indipendenti, tutti in `python_evaluation/`, pensati per essere eseguiti **dopo** un run (non richiedono Java/Maven, solo `pip install -r python_evaluation/requirements.txt` per il driver Postgres). Vanno lanciati dalla root del progetto (i default relativi, es. `mcl_debug_output.csv`, sono risolti rispetto alla directory di lavoro corrente, non alla posizione dello script):

- **MCL** (`python_evaluation/evaluate_mcl.py`): legge `mcl_debug_output.csv` (nella root, sovrascritto ad ogni run MCL — `persistence.csvOutputPath`, default `mcl_debug_output.csv`, dato che MCL scrive sempre su CSV) — nessuna dipendenza da Postgres, il file contiene già tutto il necessario (cluster assegnato + `GLOBAL_ID` di ground truth per ogni record).
  ```bash
  python python_evaluation/evaluate_mcl.py mcl_debug_output.csv
  ```
- **Center Clustering / MSCD-AP / Global Greedy / CLIP** (`python_evaluation/evaluate_mscd_ap.py --strategy ...`): si connette direttamente al Postgres dedicato alla strategia scelta (stesse credenziali di `persistence.xml`, richiede `psycopg2`) e ricava la Link Table dalle tabelle `record`/`cluster` (vedi `DATABASE_SCHEMA.md`) — nessun file intermedio, legge sempre lo stato più recente del DB.
  ```bash
  python python_evaluation/evaluate_mscd_ap.py --strategy mscd-ap
  python python_evaluation/evaluate_mscd_ap.py --strategy center-clustering
  python python_evaluation/evaluate_mscd_ap.py --strategy global-greedy
  python python_evaluation/evaluate_mscd_ap.py --strategy clip
  ```
- **Tutte e 5 insieme, un confronto in un colpo solo** (`python_evaluation/evaluate_all.py`): valuta MCL (CSV) + le 4 strategie Postgres e stampa una tabella comparativa; una fonte non ancora eseguita/raggiungibile (schema/tabelle assenti, o container irraggiungibile) appare come riga `N/A` invece di interrompere lo script.
  ```bash
  python python_evaluation/evaluate_all.py
  ```
- **GUI desktop** (`python_evaluation/evaluation_app.py`, Tkinter): stessa logica di `eval_core.py`, combobox "Sorgente" con le 5 opzioni (CSV MCL + 4 database, ciascuno con il proprio dbname pre-compilato).
  ```bash
  python python_evaluation/evaluation_app.py
  ```

Tutti calcolano recall/precision/F-measure rispetto al `GLOBAL_ID` (i due script di dettaglio, non `evaluate_all.py`, stampano anche):
- le entità (gruppi di `GLOBAL_ID` presenti su più party) i cui record sono finiti in cluster diversi (link mancati);
- i cluster che mescolano `GLOBAL_ID` diversi (link scorretti);
- i record rimasti senza cluster (singleton non intenzionali o, per le strategie persistenti, `cluster_id IS NULL`).

## 9bis. Svuotare i database tra un test e l'altro

`db_reset_scripts/reset_db.py` (root del progetto) svuota uno o tutti i 4 database persistenti senza dover ricreare il container/le tabelle a mano: esegue `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` (stesso reset di `DATABASE_SCHEMA.md`/sezione 6), poi Hibernate ricrea le tabelle al run successivo (`hibernate.hbm2ddl.auto=update`).

```bash
python db_reset_scripts/reset_db.py --strategy global-greedy
python db_reset_scripts/reset_db.py --strategy clip
python db_reset_scripts/reset_db.py --strategy mscd-ap --host localhost --port 5432 --user primat --password primat
python db_reset_scripts/reset_db.py --all               # tutti e 4, con conferma
python db_reset_scripts/reset_db.py --all --yes          # tutti e 4, senza conferma (es. in CI/script)
```

Richiede `psycopg2` (`pip install psycopg2-binary`, già presente in `venv`). Chiede conferma interattiva prima di procedere, salvo `--yes`.
