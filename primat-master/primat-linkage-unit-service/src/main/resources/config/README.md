# Config Linkage Unit

Ogni JSON esegue **una sola** strategia di clustering (`clusteringMethod`). Dal 2026-10-02
`parties`/`rbfSize` non sono piu' campi di questo JSON: la SMU li spinge ad ogni avvio di run
(protocollo StartCommand, insieme nello stesso `LuConfigPush`), la Linkage Unit li tiene in memoria
solo per la durata del run e li dimentica subito dopo — nessun file li contiene mai. La
compatibilita' tra composizione dirty/clean del roster spinto e `clusteringMethod` **locale**
(questo si', letto dal JSON) resta verificata ad ogni push da `LinkageUnitConfigLoader.resolvePartyRoster`:

| Composizione party (dal push SMU) | Strategie `clusteringMethod` compatibili | Note |
|---|---|---|
| **Tutte dirty** (una sola party, self-linkage, o più party tutte dirty) | `CENTER_CLUSTERING`, `MCL` | MCL non persiste mai su DB (solo CSV) |
| **Mista** (almeno una clean, almeno una dirty) | `MSCD_AP` | Richiede almeno una party `duplicateFree:true` |
| **Tutte clean** | `GLOBAL_GREEDY`, `CLIP` | Richiedono **tutte** le party `duplicateFree:true` |

Un roster incompatibile con il `clusteringMethod` configurato in questo file fa rifiutare il run
con un ack di errore alla SMU, prima di qualunque elaborazione.

## File in questa cartella

- **5 config canonici alla radice** (`mcl.json`, `center_clustering.json`, `mscd_ap.json`,
  `global_greedy.json`, `clip.json`) — uno per algoritmo, tuning minimo, `persistence.enabled:false`
  + `csvOutputPath` (nessun database richiesto per eseguirli).
- **`examples/full_reference.json`** — riferimento esaustivo con *tutti* i campi validi (commento
  `_<campo>` per ciascuna chiave), incluse le 5 sezioni di tuning per-algoritmo
  (`mscdAp`/`centerClustering`/`mcl`/`globalGreedy`/`clip`) e `persistence.enabled:true` + `database`
  popolati (unico file con questa sezione, a scopo di documentazione).
- **`examples/example_complete.json`** — configurazione realistica completa (broker, soglia,
  blocking, timeout MQTT, persistenza/database), ma **senza** i blocchi di tuning per-algoritmo
  (gia' documentati sopra) — pensato per essere copiato come punto di partenza.

Nota: `mvn exec:java -Dexec.args="src/main/resources/config/mscd_ap.json"` avvia la Linkage Unit con
uno di questi file. Il processo resta in ascolto passivo (`primat/lu/config`) finche' la SMU non
spinge un run — vedi `TESTING.md` alla radice del repo per il flusso end-to-end.
