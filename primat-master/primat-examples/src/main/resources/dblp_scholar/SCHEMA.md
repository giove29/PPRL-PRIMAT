# Dataset DBLP-Scholar (Köpcke/Thor/Rahm, Database Group Leipzig)

Formato: CSV `;`-delimitato, **senza riga di header**.

## Colonne (posizionali)

| idx | nome | ruolo | tipo | fonte |
|---|---|---|---|---|
| 0 | PARTY | PARTY | - | `DBLP` o `Scholar`, per riga sorgente |
| 1 | GLOBAL_ID | GLOBAL_ID | - | **ground truth**: union-find su `DBLP-Scholar_perfectMapping.csv` |
| 2 | ID | ID | - | locale, 1..n per file |
| 3 | title | QID | testo | colonna reale `title` |
| 4 | authors | QID | testo | colonna reale `authors` |
| 5 | venue | QID | testo | colonna reale `venue` |
| 6 | year | QID | numerico | colonna reale `year` |

## Statistiche

- Record DBLP: 2616
- Record Scholar: 64263
- Coppie nel perfect mapping: 5347
- Cluster (entità) totali dopo union-find: 61604
- Cluster con più di 1 record DBLP (stesso paper, 2 versioni): 53 — la party DBLP non è quindi *perfettamente* duplicate-free, ma lo è al 97.9%.
- Cluster con più di 1 record Scholar: 1205 (party Scholar chiaramente dirty, fino a 20 voci per lo stesso paper).

Scelta per PRIMAT: `party DBLP` dichiarata `duplicateFree: true` (convenzione standard in letteratura per questo benchmark, DBLP trattata come sorgente "pulita"), `party Scholar` `duplicateFree: false`. Adatto a **MSCD_AP** (richiede almeno una party clean).

Fonte: https://dbs.uni-leipzig.de/research/projects/object_matching/benchmark_datasets_for_entity_resolution (Köpcke, Thor, Rahm, "Evaluation of entity resolution approaches on real-world match problems", VLDB 2010).