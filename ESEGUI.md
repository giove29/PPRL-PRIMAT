# Center Clustering + configurazione JSON per la Linkage Unit + persistenza multi-DB

> **Nota (2026-09-21)**: documento di design storico, relativo allo stato del 2026-09-16 (4 strategie, 3 DB). Superato da Global Greedy/CLIP (2026-09-17, non menzionati qui) e dalla successiva rimozione di `MSCD_AP_NO_CLEAN` (2026-09-21, vedi `primat-master/CLAUDE.md`). Lasciato intatto come cronologia delle decisioni prese; per lo stato architetturale corrente vedi `ARCHITECTURE_FLOW.md`.

## Contesto

Oggi `primat-linkage-unit-service` ha tutto hardcoded in Java (`LinkageUnitOrchestrator`): elenco party, parametri LSH, soglia Jaccard, e la scelta della strategia di clustering è **auto-derivata** da `chooseStrategy()` guardando se le party sono dirty/clean (MSCD-AP se almeno una è clean, altrimenti MCL). Solo MSCD-AP persiste su Postgres (un solo DB, PU `"Primat"` hardcoded in `DbConnection`); MCL non persiste per design.

Vogliamo:
1. Una terza strategia di clustering nativa nel nostro codice, **Center Clustering** (greedy, single-pass, deterministica — utile proprio perché deterministica e "leggera" si presta bene alla persistenza, a differenza di MCL).
2. Una **quarta modalità**, MSCD-AP-senza-clean-source (stesso algoritmo AP nativo di PRIMAT, ma senza popolare `cleanSources` — degrada ad AP tradizionale), per poter testare AP anche su scenari tutte-dirty.
3. Una **configurazione JSON** per la Linkage Unit, esattamente sullo stesso pattern già usato per il Data Owner Service (`DataOwnerConfigLoader`/`DataOwnerConfig`/Gson), che diventa la **sola fonte di verità** su quale delle 4 strategie girare — niente più auto-routing basato su dirty/clean.
4. **Persistenza per Center Clustering, MSCD-AP e MSCD-AP-no-clean, ciascuna su un Postgres separato** (3 DB distinti); MCL resta non persistente, ma guadagna un flag di config per il debug-export CSV (oggi hardcoded `true`).

Decisioni già confermate con l'utente:
- 3 DB Postgres distinti, uno per ciascuna strategia persistente (non 2, non 1 dedicato + condiviso).
- La config JSON è sempre l'unica fonte di verità sulla strategia: niente fallback automatico su composizione dirty/clean delle party.

## Approccio

### 1. Center Clustering (nuovo, in `primat-linkage-unit`)

Nuovo package `lu/postprocessing/center_clustering/`, stesso layout di `markov_clustering/` (il template più recente e più simile):

- **`data_structures/CenterClusteringConfig.java`** — POJO `Serializable`, un solo campo opzionale `Double centerAssignmentThreshold` (nullable; `null` = accetta ogni arco già presente nel grafo, dato che gli archi sono già filtrati dalla soglia di classificazione a monte — non duplichiamo quella soglia). `checkConfigCorrectness()` in stile `MclConfig`/`ApConfig`.
- **`CenterClusteringEngine.java`** — motore puro, nessuna dipendenza da `Record`/`Party`/grafo (come `MarkovClusteringEngine`): input `double[][] simMatrix`, output `int[] labels` via `fit()`/`getLabels()`. Algoritmo: raccogli tutti gli archi (`i<j`, peso ≠ 0), ordinali **in modo canonico e deterministico** — similarità decrescente, tie-break su `(i,j)` ascendente — poi singola passata greedy: primo arco non ancora "consumato" da nessuno dei due estremi crea un nuovo centro (l'indice più basso) e assegna l'altro come foglia; se un estremo è già centro e l'altro libero, lo assegna; altrimenti salta. I vertici mai toccati diventano centri singoletto. **Nessuna iterazione/convergenza** (a differenza di MCL): un solo passaggio deterministico, documentarlo esplicitamente nel javadoc per non far pensare che manchi `isConverged()`/`getIterCount()` per errore.
- **`CenterClusteringPostprocessor.java`** — `implements MultipartiteClusteringStrategy`, stesso pattern esatto di `MarkovClusteringPostprocessor.cluster()`: per ogni componente connessa (`graph.getConnectedComponentsSimGraph()`), costruisci la matrice di similarità, ma con una differenza cruciale rispetto a MCL: **ordina `vertexSet()` con una chiave stabile prima di assegnare gli indici** (`party.getName() + "#" + record.getId()`, non l'id da solo — possibili collisioni di id cross-party), perché l'ordine di iterazione di `vertexSet()`/`edgeSet()` di jgrapht non è garantito stabile tra run/JVM e l'algoritmo deve essere riproducibile per la persistenza. Poi esegui l'engine, raggruppa per label, ed emetti la **clique completa** di `LinkedPair<Record>` per ogni gruppo (stessa convenzione di output di AP/MCL — obbligatoria, `LinkTableBuilder`/`PersistentLinkTableBuilder` la presuppongono).

Riuso diretto, senza modifiche: `MultipartiteClusteringStrategy`, `MultiPartiteSimilarityGraph` (API già sufficiente), `SimilarityVector.getAggregatedValue()`, `PersistentLinkTableBuilder.build(...)` (già generico, non specifico ad AP).

Test nuovi (JUnit 5, niente mocking, fixture a mano — stile `MarkovClusteringEngineTest`/`MarkovClusteringPostprocessorTest`):
- `CenterClusteringEngineTest`: coppia semplice, stella (hub + 3 spoke), due archi disgiunti, **catena 0-1-2 senza arco 0-2** (verifica esplicita che il nodo 2 NON venga assorbito per transitività — è il comportamento "star, non transitive-closure" discusso con l'utente, va fissato con un test), arco sotto soglia ignorato, grafo vuoto, determinismo (due `fit()` su istanze fresh → stesso risultato), tie-break su pesi uguali.
- `CenterClusteringPostprocessorTest`: clique completa su 3 membri (non solo gli archi centro-foglia), componenti separate non si mescolano, stabilità del risultato indipendente dall'ordine di inserimento nel grafo (verifica la chiave di ordinamento stabile).

### 2. `MultiSourceLinkage` — un solo metodo nuovo

- **`runCenterClustering(input, blocker, CenterClusteringConfig, threshold, clusterFactory, dbConnection)`** — nuovo, identico a `runMscdAp` ma con `new CenterClusteringPostprocessor(config)` e chiusura via `PersistentLinkTableBuilder.build(matches, allRecords, clusterFactory, dbConnection)`.
- **Nessun metodo nuovo per MSCD-AP-no-clean**: è `runMscdAp(...)` riusato tale e quale — la differenza è solo in chi popola `ApConfig.cleanSources` a monte (l'orchestrator, non `MultiSourceLinkage`). Coerente col fatto che `runMscdAp` non legge mai `Party::isDuplicateFree` internamente.
- `runMscdAp`/`runCenterClustering` guadagnano un parametro esplicito `DbConnection dbConnection` (niente singleton statico); `runMcl` resta invariato (non persiste).
- `LinkStrategy` enum: resta un **tag di risultato**, non un parametro di dispatch (precedente già stabilito nel CLAUDE.md — non ripetere l'errore corretto nel 2026-09-15). Estenderlo a `{MSCD_AP, CENTER_CLUSTERING, MCL}` solo per l'etichetta di stampa; MSCD-AP-no-clean riusa il tag `MSCD_AP`.

### 3. Configurazione JSON — nuovo package `primat-linkage-unit-service/.../lu/service/config/`

Mirror esatto di `DataOwnerConfigLoader`/`DataOwnerConfig`/`DataOwnerConfigException` (Gson, già disponibile transitivamente via `primat-mqtt-common`, nessuna nuova dipendenza Maven):

- DTO grezzi Gson (`LinkageUnitJsonConfig` + nested: `PartyJsonConfig`, `BlockingJsonConfig`/`JaccardLshJsonConfig`, `MqttJsonConfig`, `ClusterJsonConfig`, `PersistenceJsonConfig`, `CenterClusteringJsonConfig`, `ApJsonConfig`/`PreferenceJsonConfig`, `MclJsonConfig`, `DatabaseJsonConfig`).
- Nuovo enum `ClusteringMethod { CENTER_CLUSTERING, MSCD_AP, MSCD_AP_NO_CLEAN, MCL }` — **distinto** da `MultiSourceLinkage.LinkStrategy** (quello resta un tag interno, questo è la chiave di dispatch pubblica della config).
- `LinkageUnitConfig` (finale, immutabile) + `LinkageUnitConfigException` (checked, stesso stile a due costruttori) + `LinkageUnitConfigLoader.load(Path)`.

**Schema JSON completo** (obbligatori solo `parties` e `clusteringMethod`, più `database.*` quando la strategia non è MCL — tutto il resto ha un default che riproduce esattamente il comportamento hardcoded odierno):

| Chiave | Tipo | Obbligatorio | Default |
|---|---|---|---|
| `parties[].name` / `.duplicateFree` | string / boolean | nome sì, flag no | `duplicateFree=false` se omesso |
| `clusteringMethod` | enum (4 valori) | **sì** | — |
| `similarityThreshold` | double (0,1] | no | `0.6` |
| `blocking.jaccardLsh.{keySize,keys,valueRange,seed}` | int/long | no | `4,30,1024,42` |
| `mqtt.{brokerUrl,rbfCollectionTimeoutSeconds,rbfRepublishIntervalSeconds}` | string/long | no | `"tcp://localhost:1883"`, `30`, `3` |
| `cluster.{blockingKeyStrategy,representantStrategy}` | enum | no | `UNION`, `RETAIN_FIRST` |
| `persistence.enabled` | boolean | no | `true` se metodo≠MCL; **errore** se `true` con MCL |
| `database.{url,user,password}` | string | **sì se metodo≠MCL** | — (nessun default: mai scrivere per sbaglio sul DB di qualcun altro) |
| `centerClustering.centerAssignmentThreshold` | double [0,1] | no | `null` |
| `mscdAp.*` (8 campi + `preference.*` 7 campi) | vari | no | valori attuali di `ApConfig()`/`PreferenceConfig()` |
| `mcl.*` (6 campi + `debugExport.{enabled,outputPath}`) | vari | no | valori attuali di `MclConfig()`; `debugExport.enabled=true`, path `"mcl_debug_output.csv"` |

Validazioni degne di nota (oltre ai tipi/range):
- Nomi party duplicati → errore.
- `MSCD_AP` scelto ma **nessuna** party `duplicateFree=true` → **errore di validazione a caricamento config**, non degradazione silenziosa a MSCD-AP-no-clean (chi scrive `MSCD_AP` intende attivare il vincolo; un fallback silenzioso mascherebbe una config sbagliata).
- `persistence.enabled=true` con `MCL` → errore esplicito (non ignorato in silenzio).
- `database` mancante quando richiesto → errore che nomina il campo mancante.
- Stringa enum in minuscolo (Gson è case-sensitive) → errore leggibile, non uno stack trace Gson grezzo — va testato esplicitamente.

File di esempio in `primat-linkage-unit-service/src/main/resources/config/` (uno per strategia, mirror di `party_A.json` ecc.).

Test `LinkageUnitConfigLoaderTest` (mirror di `DataOwnerConfigLoaderTest`: JSON inline + temp file, un test per regola di validazione, più happy-path completo e happy-path minimale che verifica tutti i default).

### 4. `LinkageUnitOrchestrator` — dispatch a 4 vie, niente più auto-routing

- Costruttore diventa `LinkageUnitOrchestrator(LinkageUnitConfig config)`; rimossi i 9 campi hardcoded (soglia, LSH, cluster factory, MCL debug) e `setParties(...)` (multi-run con party diverse = due file JSON + due processi, non più uno swap in-process).
- `chooseStrategy(List<Party>)` **ripensato, non cancellato**: diventa `static boolean anyPartyDuplicateFree(List<Party>)`, usato solo come **validazione** dentro `LinkageUnitConfigLoader` (regola sopra), non più per instradare il run. Il vecchio nome/tipo di ritorno (`LinkStrategy`) implicava auto-routing che non esiste più — va rinominato, non lasciato lì a confondere.
- `runOnce()`: uno switch a 4 rami su `config.getClusteringMethod()` (accettabile qui: è wiring che seleziona 4 chiamate/DB genuinamente diversi, `MultiSourceLinkage` continua ad esporre 4 metodi espliciti, non un `run(enum,...)` generico):
  - `CENTER_CLUSTERING` → `buildPersistentInput` + `runCenterClustering`, DB dedicato.
  - `MSCD_AP` → popola `cleanSources` da `parties` (comportamento odierno) + `runMscdAp`, DB dedicato.
  - `MSCD_AP_NO_CLEAN` → **non** popola `cleanSources` + `runMscdAp`, DB dedicato (diverso da quello di `MSCD_AP`).
  - `MCL` → `runMcl`, poi se `config.isMclDebugExportEnabled()` scrive il CSV al path da config.
- `buildPersistentInput` guadagna un parametro `DbConnection` esplicito.
- `main(String[])`: un solo argomento, path al JSON (mirror di `DataOwnerService.main`) — carica la config, cattura `LinkageUnitConfigException` con messaggio pulito + `exit(1)`, costruisce l'orchestrator, `start()` + `runOnce()`.

`LinkageUnitOrchestratorRoutingTest` → rinominato `LinkageUnitOrchestratorValidationTest`, 3 casi riscritti su `anyPartyDuplicateFree(...)` (boolean invece di `LinkStrategy`).

### 5. Persistenza multi-DB — `DbConnection` da enum a classe

`DbConnection.INSTACE` è oggi un **singleton enum**, PU `"Primat"` hardcoded, tutti i ~15 metodi pubblici aprono un `EntityManager` fresco da `this.entityManagerFactory` — nessuno di questi metodi referenzia l'enum-ness in sé. Per 3 DB serve:

- **`DbConnection` diventa una classe normale**, costruttore `DbConnection(String persistenceUnitName, String jdbcUrl, String jdbcUser, String jdbcPassword)` che passa url/user/password come **override runtime** nella stessa `Map<String,Object>` già usata oggi per `hibernate.show_sql`/`format_sql` — **zero modifiche al corpo dei ~15 metodi esistenti**, solo la dichiarazione di tipo e il costruttore cambiano.
- **Un solo `DbConnection` vivo per processo**: dato che un processo carica una config e ne esegue esattamente una (`ClusteringMethod`), non serve mai avere 3 `EntityManagerFactory` aperti insieme — `LinkageUnitConfig` espone un solo `getDbConnection()` (null se MCL), costruito da `LinkageUnitConfigLoader` solo per la strategia effettivamente scelta. Più semplice di un registry a 3 istanze e evita di pagare 3x il costo di startup/connection pool per niente.
- `persistence.xml`: da 1 a 3 `<persistence-unit>` (`PrimatCenterClustering`, `PrimatMscdAp`, `PrimatMscdApNoClean`), stessa lista di 11 `<class>` ripetuta identica in ognuna, URL di default diversi (`primat_center_clustering`, `primat_mscd_ap`, `primat_mscd_ap_no_clean` — sovrascritti a runtime dai valori di `database.*` della config, i default in XML contano solo per chi apre il modulo da IDE senza passare dalla config). Nessuna 4ª PU per MCL.
- **Fix del typo `INSTACE`→corretto** come parte di questo refactor: la conversione enum→classe è già una modifica breaking a ogni call-site (`DbConnection.INSTACE.foo()` → istanza), quindi non c'è più valore nel preservare l'errore di battitura — farlo nello stesso passaggio, documentato nel changelog.
- `PersistentLinkTableBuilder.build(...)` e `reconcile(...)` guadagnano un parametro `DbConnection`.

**Nota di rischio esplicita**: chi ha già dati nel vecchio DB unico `primat`/PU `"Primat"` li ritrova orfani — nessuna delle 3 nuove PU corrisponde. Nessuna migrazione automatica proposta (il progetto non ha tooling di migrazione schema, solo `hibernate.hbm2ddl.auto=update`); da segnalare come passo manuale nel changelog (probabile destinazione: `primat_mscd_ap`, essendo l'unica strategia persistente prima di questo cambiamento).

### 6. Documentazione (commenti "a regola d'arte" + `.md`)

- Codice nuovo: javadoc di classe in stile `PersistentLinkTableBuilder`/`MultiSourceLinkage` (blocchi descrittivi, **niente `@author`** — quello è lo stile del codice upstream Leipzig, non il nostro).
- **Root `CLAUDE.md`**: nuovo bullet datato (stile identico agli esistenti 2026-09-15/2026-09-16) che copre: JSON config della Linkage Unit, `ClusteringMethod` a 4 valori, `CENTER_CLUSTERING`/`MSCD_AP_NO_CLEAN` e come mappano sul codice esistente, `chooseStrategy`→`anyPartyDuplicateFree`, `DbConnection` enum→classe + rename `INSTACE`, 3 PU/3 DB, rimozione di `setParties`/demo a due run. Aggiornare anche i bullet esistenti su MSCD-AP/persistenza/LinkStrategy che diventano parzialmente obsoleti (cross-reference al nuovo bullet invece di lasciarli contraddittori).
- `primat-linkage-unit/.../lu/CLAUDE.md`: bullet `/postprocessing` (nuovo sotto-package `center_clustering`) e `/database` (multi-PU).
- `ARCHITECTURE_FLOW.md`: 4 rami invece di 2 dopo il blocking, gate da config non da composizione party; nota sul fan-out a 3 DB.
- `DATABASE_SCHEMA.md`: 3 PU documentate, stesso schema/11 entità ripetuto, MCL resta senza schema.
- `TESTING.md`: istruzioni Postgres per 3 DB throwaway invece di 1; riferimento ai nuovi test (`LinkageUnitConfigLoaderTest`, `CenterClusteringEngineTest`/`CenterClusteringPostprocessorTest`).

## File critici

- `primat-linkage-unit/.../lu/postprocessing/center_clustering/CenterClusteringEngine.java` (nuovo)
- `primat-linkage-unit/.../lu/postprocessing/center_clustering/CenterClusteringPostprocessor.java` (nuovo)
- `primat-linkage-unit/.../lu/postprocessing/center_clustering/data_structures/CenterClusteringConfig.java` (nuovo)
- `primat-linkage-unit-service/.../lu/service/config/LinkageUnitConfigLoader.java` + DTO/enum/eccezione (nuovi, ~10 file piccoli)
- `primat-linkage-unit-service/.../lu/service/LinkageUnitOrchestrator.java` (modifiche pesanti)
- `primat-linkage-unit-service/.../lu/service/MultiSourceLinkage.java` (nuovo `runCenterClustering`, parametro `DbConnection` su `runMscdAp`)
- `primat-linkage-unit-service/.../lu/service/PersistentLinkTableBuilder.java` (parametro `DbConnection`)
- `primat-linkage-unit/.../lu/database/DbConnection.java` (enum → classe)
- `primat-linkage-unit/src/main/resources/META-INF/persistence.xml` (1 → 3 persistence-unit)
- `primat-linkage-unit-service/.../lu/service/LinkageUnitOrchestratorRoutingTest.java` → rinominato/riscritto

## Verifica

1. **Build**: `mvn -pl primat-linkage-unit,primat-linkage-unit-service -am install -DskipTests` (compilazione, nessuna dipendenza nuova da aggiungere — Gson già transitiva).
2. **Unit test puri** (nessun DB richiesto): `mvn -pl primat-linkage-unit,primat-linkage-unit-service test -Dtest=CenterClusteringEngineTest,CenterClusteringPostprocessorTest,LinkageUnitConfigLoaderTest,LinkageUnitOrchestratorValidationTest` — copertura di determinismo, convenzione output a clique, e ogni regola di validazione della config.
3. **End-to-end manuale con Postgres** (come da `TESTING.md` oggi, esteso a 3 DB): creare 3 database (`primat_center_clustering`, `primat_mscd_ap`, `primat_mscd_ap_no_clean`) in un container Docker throwaway, un file JSON di esempio per strategia con la `database.url` corrispondente, lanciare `LinkageUnitOrchestrator.main(<json>)` per ciascuna delle 4 strategie e verificare: MCL produce `mcl_debug_output.csv` (se abilitato) e nessuna scrittura DB; le altre 3 scrivono cluster stabili nel proprio DB, con un secondo run sullo stesso JSON che riconferma gli stessi `Cluster.id` (stessa verifica di stabilità già documentata per MSCD-AP in `TESTING.md`).
4. **Confronto qualità** (motivazione originale della richiesta): usare lo stesso dataset/script di valutazione già in `TESTING.md`/`evaluate_mcl.py` per confrontare Center Clustering vs MCL vs MSCD-AP-no-clean su party tutte-dirty, prima di considerare ulteriori decisioni su MCL.
