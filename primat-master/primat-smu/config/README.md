# Config SMU

La SMU fonde, per ciascun Data Owner, uno schema per-party (`do_<party>.json`: mapping
colonna→QID, `index`/`name`/`role`/`preprocessing`) con un encoding comune a tutte le party
(`encoding.json`: `bloomFilter`, `missingValueHandling`, `hmacKey`, e per ogni colonna QID
`hashFunctions`/`salt`/`constantWeightEncoding`/`missingValueTokenCount`), e pusha il risultato in
un solo messaggio MQTT (`ConfigPush`) sul file "live" di ogni Data Owner.

## ⚠️ `smu.py` legge solo `config/` flat

`smu.py` (`CONFIG_DIR`/`ENCODING_PATH`/`_do_schema_path`/`discovered_parties()`) non ha alcuna
consapevolezza di dataset o sottocartelle: scansiona solo i file `do_*.json` **direttamente dentro**
`primat-smu/config/`, non ricorsivamente. Questo task ha riorganizzato i file per dataset a scopo di
chiarezza/manutenzione, **senza modificare `smu.py`** (scelta esplicita). Per usare davvero un
dataset, copia (o sposta) i suoi `do_*.json` + `encoding.json` da una delle sottocartelle qui sotto
direttamente dentro `primat-smu/config/`, poi lancia `python smu.py`. `broker.json`/`state.json`/
`preprocessing_catalog.json` restano sempre al livello flat (dataset-agnostici).

## Dataset disponibili

- **`febrl/`** — `do_org.json`+`do_org1.json`+`encoding.json`, scenario FEBRL4_1_mixed (`org`
  clean + `org1` dirty, 6 colonne QID). È il dataset oggi effettivamente verificato end-to-end.
- **`dblp_scholar/`** — `do_DBLP.json`+`do_Scholar.json`+`encoding.json`, 4 colonne QID
  (`title`/`authors`/`venue`/`year`). `DBLP` clean (~97.9%), `Scholar` dirty — scenario "mixed".
  `hashFunctions` **provvisorio** (10 per tutte le colonne): da ricalcolare manualmente.
- **`ncvoters_naumann/`** — solo `do_A.json`+`do_B.json`+`encoding.json` (stesso schema per tutte
  le taglie 500/1000/2000/5000/full del dataset lato Data Owner), 11 colonne QID. Entrambe le party
  **dirty**. `hashFunctions` **provvisorio** (10 per tutte le colonne): da ricalcolare manualmente.

## Schema completo

`examples/do_full_reference.json` mostra una colonna QID "virtuale" costruita con un primo step
`MERGE` da due colonne `RAW` — fuori da ogni sottocartella dataset apposta, così
`discovered_parties()` non lo trova mai durante un uso normale. Vedi `preprocessing_catalog.json`
(livello flat) per il catalogo completo di tutti gli step di preprocessing disponibili
(`TRIM`/`UPPERCASE`/`REMOVE_ACCENTS`/`REMOVE_SPECIAL_CHARS`/`REMOVE_NON_DIGITS`/`TRUNCATE`/
`MERGE`/`SPLIT`).
