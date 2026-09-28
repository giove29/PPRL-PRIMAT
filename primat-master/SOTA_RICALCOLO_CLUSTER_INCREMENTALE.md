# Ricalcolo periodico del clustering incrementale: design proposto

> ⚠️ **Design proposto, non ancora implementato.** Questo documento raccoglie una discussione di progettazione (originata da una mail del professore che cita Gruenheid/Dong/Srivastava PVLDB 2014 e Vatsalan/Christen/Rahm DKE 2020, più due paper correlati) su come correggere un limite strutturale dell'incremental linkage attuale. Nessun codice qui descritto esiste ancora nel repo. Punti di innesto futuri: `primat-linkage-unit-service/.../lu/service/ClusterAssignmentPlanner.java`, `PersistentLinkTableBuilder.java`, `LinkageUnitConfig.getDbConnection()`.

## 1. Scopo e stato

Riassume una strategia per permettere alla pipeline di correggere periodicamente gli errori di linkage accumulati dall'incremental matching, senza reintrodurre merge/split nel path online (che è stato deliberatamente rimosso) e senza downtime del servizio. Sezioni 3-11 descrivono l'architettura decisa; la sezione 12 elenca esplicitamente cosa resta da decidere prima di poter passare all'implementazione.

## 2. Il problema

`ClusterAssignmentPlanner` (vedi il suo javadoc, righe 27-50) impone tre regole: i cluster storici sono **congelati** (mai fusi tra loro da un run), un gruppo di record freschi entra in **un solo** cluster storico scelto per punteggio migliore, e non esiste alcuna scissione. È un design volutamente semplice e veloce, ma ha una conseguenza strutturale: se il clustering iniziale di un cluster è sbagliato (due entità distinte fuse per errore, o un'entità spezzata in due cluster), **nessuna quantità di nuova evidenza futura può correggerlo**, perché i cluster storici non vengono mai rivisitati.

Gruenheid, Dong, Srivastava (PVLDB 7(9), 2014, "Incremental Record Linkage") formalizzano esattamente questo rischio con l'Example 1.1/2.2 del loro paper: un record inserito successivamente può fornire l'evidenza mancante per correggere un errore precedente (es. due cluster che in realtà sono la stessa entità, o un record inserito nel cluster sbagliato per un valore rumoroso), ma solo se l'algoritmo incrementale è progettato per sfruttarla. Definiscono inoltre un insieme di proprietà (*locality*, *connectivity*, *monotonicity*, *exchangeability*, *separability* — Def. 3.1-3.7, Theorem 3.8-3.9) che determinano quando un algoritmo incrementale può essere *provabilmente ottimale* rispetto al batch. Il design "cluster congelato, solo append" di PRIMAT non soddisfa queste proprietà per costruzione: è una scelta di velocità/prevedibilità, non di correttezza asintotica.

## 3. Architettura: doppio DB alternato (blue-green)

Per correggere periodicamente questi errori senza pagarne il costo ad ogni singolo inserimento, si usa un ricalcolo batch completo (vero clustering da zero sull'intero storico, come `F(D+ΔD)` in Gruenheid et al.) eseguito a intervalli, reso non bloccante tramite due database alternati:

- **DB attivo (A)**: serve tutte le richieste online, riceve gli inserimenti incrementali normali.
- **DB shadow (B)**: idle finché non parte un ciclo di ricalcolo, poi diventa il bersaglio del ricalcolo pieno.

A ricalcolo completato i due ruoli si scambiano (B diventa attivo, A diventa shadow per il ciclo successivo) — nessuna interruzione del servizio durante il ricalcolo, che può essere lungo: il costo del clustering a grafo (in particolare Affinity Propagation/MSCD-AP) non scala linearmente, come osservato empiricamente in modalità `similarityThreshold: "range"` (vedi `CLAUDE.md`, bullet 2026-09-28) — soglie più basse producono grafi più densi e tempi di convergenza AP significativamente più alti.

## 4. Trigger del ricalcolo

Un ricalcolo pieno parte al verificarsi del **primo tra due eventi** (whichever first):
- **Trigger di volume**: il DB attivo ha ricevuto nuovi record pari al **15-20% della sua dimensione all'ultimo ricalcolo** (soglia percentuale, non un N fisso) — scala naturalmente con la crescita del DB, a differenza di un conteggio assoluto.
- **Trigger temporale**: è trascorso un intervallo massimo fisso (es. ogni tot giorni/settimane, valore esatto da tarare) dall'ultimo ricalcolo, indipendentemente dal volume accumulato.

Il trigger temporale copre il caso in cui il traffico è troppo basso perché la soglia percentuale scatti mai da sola (un DB che cresce lentamente accumulerebbe errori non corretti per un tempo indefinito senza un limite massimo di calendario) — garantisce quindi una cadenza minima di correzione anche in assenza di volume, complementare (non alternativa) al trigger percentuale del §4.

Nota di costo: la soglia percentuale scala bene il *trigger di volume*, ma non protegge dal fatto che il costo **per ciclo** cresce comunque nel tempo (grafo più grande da ricalcolare ogni volta, più il comportamento non lineare di AP descritto sopra). Va monitorato il tempo di ricalcolo per ciclo; se cresce in modo insostenibile la soglia andrà resa adattiva (abbassata) invece di restare fissa al 15-20% — vedi punto aperto in §12.

## 5. Ciclo di vita di un ricalcolo

1. **Snapshot**: al superamento della soglia, si fissa uno snapshot del DB attivo A nell'istante del trigger.
2. **Ricalcolo pieno su B**: si esegue clustering batch completo a partire dallo snapshot, scrivendo il risultato sul DB shadow B (con la compressione dei cluster grandi descritta in §7, per contenere il costo).
3. **Pending durante il ricalcolo**: A resta attivo e continua a servire il traffico normale — ogni nuovo record arrivato dopo lo snapshot passa per il path incrementale veloce esistente su A, invariato. Questi sono i record **pending** (§6).
4. **Replay**: terminato il ricalcolo su B, si applicano tutti i pending accumulati nel frattempo su A, sulla nuova baseline di B, con la normale logica incrementale (`ClusterAssignmentPlanner`, nessuna logica nuova).
5. **Replay iterativo**: se durante il replay stesso arrivano altri pending, si ripete finché il delta residuo è trascurabile.
6. **Swap**: B diventa il DB attivo, A diventa lo shadow per il prossimo ciclo. Lo swap deve essere un cambio atomico di puntatore/alias a livello applicativo (non un rename di tabelle a caldo), per evitare che una query in corso veda uno stato incoerente — coerente con il pattern lazy/single-connection già usato da `LinkageUnitConfig.getDbConnection()`.
7. **Re-trigger immediato**: se al termine dello swap la soglia del §4 è già di nuovo superata (per via del traffico arrivato durante il ricalcolo), si riparte subito un nuovo ciclo sul DB ora shadow — corner case legittimo, non genera inconsistenza, ma sotto un tasso di inserimento sostenuto rischia una "rincorsa" continua senza mai stabilizzarsi: da monitorare operativamente, non un problema di correttezza.

## 6. Terminologia: pending e tombstone

- **Pending**: record inseriti tra uno snapshot e il ricalcolo successivo (§5, passo 3). Sempre gestiti con la logica incrementale esistente, mai persi, sempre riapplicati in blocco a fine ciclo.
- **Tombstone**: non necessario oggi — la pipeline gestisce solo `Insert` (`ClusterAssignmentPlanner` non ha alcun path di cancellazione o modifica). È il gancio naturale per un'estensione futura se si volesse supportare anche `Delete`/`Change` (i tre tipi di update definiti da Gruenheid et al., §2.1 del loro paper): un record cancellato verrebbe marcato come tombstone invece di essere rimosso subito, e verrebbe eliminato fisicamente solo al ricalcolo pieno successivo — stesso ciclo blue-green già descritto, zero logica incrementale aggiuntiva per gestirlo.

## 7. Compressione dei cluster grandi prima del ricalcolo

**Perché serve.** Il costo del clustering a grafo (soprattutto AP/MSCD-AP) scala con il numero di nodi/archi del grafo di similarità, non con il numero di record reali che rappresentano. Un cluster storico molto grande e già consolidato pesa sul ricalcolo pieno proporzionalmente alla sua dimensione reale anche se non ha bisogno di essere rivisto nel dettaglio.

**Criterio di selezione dei cluster da comprimere** (parzialmente aperto, vedi §12):
- **Criterio di sicurezza (gratuito)**: un cluster mai referenziato in `evidence` (le coppie fresco↔storico raccolte da `ClusterAssignmentPlanner.plan()`) dall'ultimo ricalcolo non è mai stato toccato da nuova informazione — candidato sicuro alla compressione, non solo euristico.
- **Criterio euristico secondario**: coesione/solidità interna (silhouette, o la nozione di coesione di Gruenheid et al. — 1 meno la similarità media intra-cluster, §2.3 del loro paper) per decidere se comprimere anche cluster toccati marginalmente, accettando un rischio calcolato.
- Dimensione minima oltre la quale la compressione conviene: **non ancora deciso**.

**Metodo scelto: Lightweight Coreset Sampling** (Bachem, Lucic, Krause, KDD 2018, "Scalable and Distributed Clustering via Lightweight Coresets"). A differenza del framework classico di Feldman & Langberg (STOC 2011), che richiede prima una clusterizzazione bicriterio approssimata per stimare la sensitivity di ogni punto, il metodo *lightweight* stima l'importanza di ogni punto in un solo passaggio O(n), usando solo la distanza dal centroide globale:

```
q(x) = ½ · (1/|X|)  +  ½ · d(x, X̄)² / Σ_{x'∈X} d(x', X̄)²
```

(metà peso uniforme, per non azzerare mai la probabilità di nessun punto; metà peso proporzionale alla distanza dal centro, per privilegiare i punti periferici/outlier — i più informativi per rivelare una sotto-struttura nascosta). Si campionano `m` punti da questa distribuzione, ciascuno con peso di ricostruzione `w(x) = 1/(m·q(x))`, usato a valle per non falsare i conteggi TP/FP/GT quando si rivalutano le metriche sul grafo compresso.

**Adattamento al dominio PRIMAT** (Jaccard su `BitSet`, non spazio euclideo): il centroide `X̄` diventa il centroide bit-a-bit a maggioranza (o, in alternativa, il medoide reale del cluster), e la distanza `d(x, X̄)` diventa `1 − Jaccard(x, X̄)` (riusando `BinarySimilarity.JACCARD_SIMILARITY`, già presente in `ClusterAssignmentPlanner.jaccard()`).

**Nota onesta sui limiti**: la garanzia formale (1±ε) di preservazione del costo di clustering, dimostrata nel paper originale, vale per k-means/k-median in spazio euclideo — non per il clustering a grafo (AP/MCL/Global Greedy/CLIP/Center Clustering) su similarità Jaccard usato da PRIMAT. Il *meccanismo* (campionamento per importanza pesato + reweighting) si prende in prestito come euristica ben fondata, non come teorema applicabile alla lettera nel nostro dominio.

**Alternative valutate**:
| Metodo | Perché scartato / quando preferirlo |
|---|---|
| CBF (Counting Bloom Filter, somma vettoriale dei BF del cluster) | Scartato: comprime in un solo vettore aggregato, perde per costruzione la capacità di rilevare sotto-strutture/split interni — utile solo come score di coesione a costo O(l), non come rappresentazione per il ricalcolo. |
| k-center / Gonzalez farthest-point | Baseline economico (2-approssimazione, O(n·k)), ma garantisce solo copertura geometrica, non preservazione della decisione di clustering a valle. |
| k-medoids / PAM / CLARA | Più bilanciato di k-center (minimizza dissimilarità totale, non solo il raggio), ma PAM puro è O(n²) per iterazione — CLARA/CLARANS necessari per cluster grandi. |
| Facility-location / submodular maximization | Concettualmente è ciò su cui si basa l'Affinity Propagation già presente in pipeline (`AffinityPropagationPostprocessor`) — riusabile in modalità "auto-riassunto" su un singolo cluster per ottenere k esemplari, zero algoritmo nuovo da scrivere. |
| Determinantal Point Process (DPP) | Garanzie di diversità più rigorose in letteratura, ma campionamento esatto O(n³) — costo non giustificato per un ciclo periodico. |

## 8. Espansione post-ricalcolo

Dopo il clustering batch sul grafo compresso, un cluster storico originale finisce in uno di questi casi:

- **Nessuno split rilevato** (tutti i rappresentanti restano nello stesso cluster risultante): i membri esclusi dal coreset non erano mai stati messi in dubbio, restano dov'erano — nessun lavoro.
- **Split rilevato** (i rappresentanti finiscono su 2+ cluster figli): i membri esclusi vanno riassegnati. Si riusa `ClusterAssignmentPlanner.score()` (max Jaccard, media Jaccard, blocking key condivise) e il vincolo clean-source già esistenti, ma con il dominio dei candidati ristretto ai soli cluster figli nati da quello split — economico perché sono pochi. La riassegnazione va fatta **in blocco** (tutti i membri esclusi contro i pochi cluster figli insieme), non sequenzialmente una alla volta, per non reintrodurre lo stesso problema greedy/order-dependent isolato in §9.
- **Membro "orfano"**: se un escluso non supera la soglia di similarità con nessuno dei figli, non va forzato in un figlio a caso — diventa un nuovo cluster singleton. Segnale utile (non un bug): indica un'appartenenza già debole nel cluster originale.
- **Fusione rilevata invece di split**: banale — i membri esclusi di entrambi i cluster originali confluiscono nell'unico cluster risultante senza alcuna decisione di matching.

## 9. Assegnazione ottimale invece di greedy — ✅ implementato

`ClusterAssignmentPlanner.plan()` assegnava in modo greedy sequenziale (ordina tutti i candidati per punteggio, assegna il primo migliore libero) — dipendente dall'ordine di elaborazione quando due gruppi competono per lo stesso cluster. Vatsalan, Christen, Rahm (DKE 2020, §3, Def. 3.2) confrontano empiricamente questo approccio ("greedy/best-link", equivalente al best-link di Kendrick) con un **mapping bipartito ottimale** (algoritmo ungherese, Kuhn-Munkres) e trovano quest'ultimo significativamente migliore in qualità di linkage.

**Implementazione** (nuova classe `HungarianAssignment`, package-private in `primat-linkage-unit-service/.../lu/service/`): Kuhn-Munkres O(n³) a peso massimo su matrici rettangolari, con celle non ammesse marcate da un peso sentinella (`UNASSIGNED_WEIGHT = -1`, mai un punteggio reale). Nessuna dipendenza esterna.

**Modello scelto** (opzione (a): un solve per party, mirror esatto dell'iterazione per-party di Vatsalan et al.) — in PRIMAT non è un bijection 1-1 puro come nel loro modello (dove ogni database è interamente deduplicato): un cluster storico può ricevere più gruppi freschi diversi purché non condividano una party `duplicateFree`. `ClusterAssignmentPlanner.plan()` ora:
1. assegna subito i gruppi senza alcun record `duplicateFree` al loro miglior candidato (nessuna competizione possibile);
2. per ogni party `duplicateFree` coinvolta, in ordine alfabetico, risolve un `HungarianAssignment` tra i gruppi non ancora assegnati che la contengono e i loro cluster candidati (peso = `maxSimilarity` dominante, `averageSimilarity`/`sharedBlockingKeys` come spareggio — stesso ordine di `Score.BEST_FIRST`, solo risolto globalmente invece che sequenzialmente);
3. rete di sicurezza finale: un gruppo ancora scoperto tenta comunque il suo miglior candidato residuo, come prima.

Il vincolo "un cluster storico resta invariato o esteso, mai fuso" resta invariato. Test: `HungarianAssignmentTest` (algoritmo puro) e `ClusterAssignmentPlannerTest.optimalAssignmentBeatsGreedyWhenTwoCleanGroupsCompete` (scenario a due gruppi che dimostra la divergenza dal greedy).

## 10. Funzione di ordinamento dei party (`ord`)

Vatsalan et al. (DKE 2020, §3) ordinano esplicitamente i database prima dell'incremental clustering — per dimensione decrescente (meno merge necessari) o per qualità dei dati decrescente (cluster iniziali più affidabili). Oggi PRIMAT processa gli arrivi RBF nell'ordine di arrivo MQTT (`LinkageUnitOrchestrator.collectRbf`), arbitrario. Elaborare per primi i party noti per dati più puliti (es. duplicate-free) formerebbe cluster-ancora più affidabili prima delle assegnazioni greedy successive — richiede solo di ordinare l'input prima del blocking, nessuna modifica alla logica di persistenza.

## 11. Privacy: nota per lavori futuri (fuori scope)

Asse ortogonale a questo design, citato solo per completezza: i paper Vatsalan/Christen/Rahm su Counting Bloom Filter descrivono un'aggregazione via secure summation (i DO sommano i propri BF prima che la Linkage Unit li veda, che riceve solo il vettore aggregato) per ridurre l'esposizione dei singoli RBF alla LU (Proposition 4.1: `Pr(Ri|CBF) < Pr(Ri|BF)` per x>1 party aggregati). Non collegato al ricalcolo/compressione descritti sopra — è un possibile hardening di privacy indipendente, non necessario per implementare questo design.

## 12. Punti aperti / da decidere

- Soglia esatta di "cluster troppo grande" oltre la quale conviene comprimere (§7).
- Dimensione del coreset `m` (quanti rappresentanti estrarre per cluster compresso) (§7).
- Soglia di coesione/silhouette per il criterio euristico di skip (§7).
- Se rendere adattiva nel tempo la soglia percentuale del trigger (§4), vista la crescita non lineare del costo per ciclo.
- Valore esatto dell'intervallo massimo del trigger temporale (§4) — giorni o settimane, da tarare sul volume di traffico atteso.
- Gestione operativa del corner case "ricalcolo che rincorre" sotto traffico sostenuto (§5, passo 7).

~~Se e quando introdurre l'assegnazione ottimale (§9)~~ e ~~granularità esatta del mapping ottimale~~ — **risolti**: implementato come solve Hungarian per party (opzione (a)), vedi §9.

## 13. Riferimenti bibliografici

- A. Gruenheid, X. L. Dong, D. Srivastava, "Incremental Record Linkage", PVLDB 7(9), 2014.
- D. Vatsalan, P. Christen, E. Rahm, "Incremental Clustering Techniques for Multi-Party Privacy-Preserving Record Linkage", Data & Knowledge Engineering, 2020.
- D. Vatsalan, P. Christen, E. Rahm, "Scalable Multi-Database Privacy-Preserving Record Linkage using Counting Bloom Filters", IEEE ICDM Workshop on Privacy and Discrimination in Data Mining (PDDM), 2016 (versione estesa).
- S. I. Khan, A. B. A. Khan, A. S. M. L. Hoque, "Privacy preserved incremental record linkage", Journal of Big Data 9:105, 2022.
- O. Bachem, M. Lucic, A. Krause, "Scalable and Distributed Clustering via Lightweight Coresets", KDD 2018.
- D. Feldman, M. Langberg, "A unified framework for approximating and clustering data", STOC 2011 (framework coreset classico, citato per contesto).
