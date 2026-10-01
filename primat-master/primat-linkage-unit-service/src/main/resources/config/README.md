# Config Linkage Unit

Ogni JSON esegue **una sola** strategia di clustering (`clusteringMethod`), scelta in base a quali
party del run sono dichiarate `duplicateFree` (pulite/senza duplicati interni) o meno — vincolo
imposto da `LinkageUnitConfigLoader` all'avvio, non solo una convenzione:

| Composizione party | Strategie valide | Note |
|---|---|---|
| **Tutte dirty** (una sola party, self-linkage, o più party tutte dirty) | `CENTER_CLUSTERING`, `MCL` | MCL non persiste mai su DB (solo CSV) |
| **Mista** (almeno una clean, almeno una dirty) | `MSCD_AP` | Richiede almeno una party `duplicateFree:true` |
| **Tutte clean** | `GLOBAL_GREEDY`, `CLIP` | Richiedono **tutte** le party `duplicateFree:true` |

I nomi delle `parties` devono coincidere esattamente (case-insensitive) con i `party` dichiarati
lato Data Owner per lo stesso dataset.

## Dataset disponibili

- **`febrl/`** — 4 scenari: `febrl2_dirty`/`febrl3_dirty` (party `org` dirty, self-linkage) →
  `CENTER_CLUSTERING`+`MCL`; `febrl4_1_mixed` (`org` clean + `org1` dirty) → solo `MSCD_AP`;
  `febrl4_clean` (`org`+`org1` entrambe clean) → `CLIP`+`GLOBAL_GREEDY`.
- **`dblp_scholar/`** — `mscd_ap.json`, party `DBLP` (clean, ~97.9% duplicate-free) + `Scholar`
  (dirty) — scenario "mixed", stesso schema di `febrl4_1_mixed`.
- **`ncvoters_naumann/`** — `center_clustering.json`+`mcl.json`, party `A`+`B` **entrambe dirty**
  (duplicati interni confermati empiricamente sui CSV generati) — nessuna strategia "clean"/"mixed"
  applicabile.

## Persistenza

Tutti i config di scenario in questo albero (root, `febrl/*`, `dblp_scholar/*`,
`ncvoters_naumann/*`, più i due demo in `examples/`) usano `persistence.enabled:false` +
`csvOutputPath: "<algoritmo>_debug_output.csv"` — nessun database richiesto per eseguirli. Attenzione:
più dataset che usano lo stesso algoritmo condividono lo stesso nome di file di default (es.
`mcl_debug_output.csv` sia per NCVR root sia per `febrl2_dirty`/`febrl3_dirty`/`ncvoters_naumann`) —
limite preesistente nel repo, non introdotto da questa riorganizzazione: sposta/rinomina il CSV tra
un run e l'altro se vuoi conservarli.

**Eccezione**: `examples/full_reference.json` resta con `persistence.enabled:true` + `database`
popolati — è l'unico file pensato come documentazione completa di *tutti* i campi validi, quel
campo incluso. Vedi quel file per lo schema JSON completo con un commento `_<campo>` per ciascuna
chiave.
