# Config Data Owner

Ogni Data Owner è configurato da **due** file JSON, ma avviato con un solo argomento CLI:

```
mvn -pl primat-data-owner-service exec:java -Dexec.args="<nome>_local.json"
```

- **`<nome>_local.json`** ("locale", immutabile — mai riscritto dal processo in esecuzione):
  `party`, `debug`, `dataSource`, `rbfChunkSize` (opzionale, default 2000 — numero massimo di
  record RBF per messaggio MQTT: dal 2026-10-02 un Data Owner con molti record pubblica i propri
  RBF in più chunk sequenziali invece di un unico messaggio gigante, vedi
  `DataOwnerService#handleStartCommand`), più `liveConfigPath` (percorso del file "live", relativo
  alla cartella del file locale se non assoluto) e `bootstrapMqttBrokerUrl` (broker usato solo
  finché il file live non esiste/non è ancora una configurazione reale).
- **`<nome>_live.json`** ("live", pushabile dalla SMU via `ConfigPush`): `mqttBrokerUrl`, `columns`
  (schema + preprocessing), `bloomFilter`, `missingValueHandling`, `hmacKey`, `version`.

**Il file live può essere assente.** Se `liveConfigPath` punta a un file che non esiste ancora, il
Data Owner lo crea da solo con il solo contenuto `{"version": "NOT_FOUND"}`, si avvia comunque
(connesso al broker di bootstrap, in ascolto su MQTT) e resta in questo stato "pending" — qualunque
`checkVersion` risponde `NOT_FOUND`, quindi risulta sempre "fuori fase" finché la SMU non pusha una
vera configurazione. Se invece il file live esiste ma non è leggibile (permessi, un percorso che
punta a una directory, ecc.) il processo termina con un errore di configurazione esplicito.

Per questo, dopo l'ultima riorganizzazione, **in questa cartella esistono solo i file `_local.json`
— i file `_live.json` sono stati rimossi deliberatamente** per esercitare questo comportamento
("parte con un solo file, il live arriva dopo dalla SMU").

## Schema completo

`examples/full_reference_local.json` + `examples/full_reference_live.json` mostrano, in un solo
file live, tutte le funzionalità disponibili insieme: colonne `PARTY`/`GLOBAL_ID` a valore costante
(nessun `index`), una colonna QID "virtuale" costruita con un primo step `MERGE` da due colonne
`RAW`, `constantWeightEncoding`/`missingValueTokenCount` per colonna, `bloomFilter.hardeningChain`
(BLIP + XOR_FOLD) e `missingValueHandling`. È un file puramente documentale (il `dataSource`
sottostante è un placeholder), non pensato per essere eseguito così com'è.

## Dataset disponibili

- **`febrl/`** — 4 scenari del benchmark FEBRL (Database Group Leipzig): `febrl2_dirty`/
  `febrl3_dirty` (una sola party `org`, tutta dirty, self-linkage), `febrl4_1_mixed` (`org` clean +
  `org1` dirty), `febrl4_clean` (`org`+`org1` entrambe clean).
- **`dblp_scholar/`** — benchmark DBLP-Scholar (Köpcke/Thor/Rahm), party `DBLP` (dichiarata clean,
  ~97.9% duplicate-free) + `Scholar` (dirty, fino a 20 duplicati per paper) — scenario "mixed".
  Vedi `primat-examples/src/main/resources/dblp_scholar/SCHEMA.md`.
- **`ncvoters_naumann/`** — dataset reale NC Voters (HPI/Naumann), party `A`+`B`, **entrambe dirty**
  (duplicati interni confermati empiricamente sui CSV generati), disponibile in 5 taglie
  (`500`/`1000`/`2000`/`5000`/`full`, stesso schema per tutte). Vedi
  `primat-examples/src/main/resources/ncvoters_naumann/<taglia>/SCHEMA.md`.
- **`examples/`** — config generiche non legate a un dataset reale, una per feature (sorgente
  CSV/DB, hardening, colonne a valore costante, ecc.) più `full_reference_*` (vedi sopra).
