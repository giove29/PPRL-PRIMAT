# DBLP-Scholar — dataset con GLOBAL_ID per PRIMAT

Benchmark classico di entity resolution (Köpcke, Thor, Rahm, "Evaluation of
entity resolution approaches on real-world match problems", VLDB 2010),
ospitato dal Database Group di Lipsia: 2.616 record DBLP + 64.263 record
Google Scholar, 5.347 coppie nel "perfect mapping" ufficiale.

A differenza di NC Voters, qui il ground truth è nativamente un problema di
linkage a **2 sorgenti separate** (non deduplicazione a sorgente singola):
ogni record appartiene o a DBLP o a Scholar, nessuno split artificiale è
necessario — il `perfect mapping` collega direttamente i due id.

## Uso

```bash
pip install -r dblp_scholar/requirements.txt
python dblp_scholar/download_dblp_scholar.py
python dblp_scholar/build_dataset.py
```

Output:

```
primat-examples/src/main/resources/dblp_scholar/
├── party_DBLP.csv      # PARTY;GLOBAL_ID;ID;title;authors;venue;year
├── party_Scholar.csv
├── combined.csv        # entrambe le party insieme, nessun header
└── SCHEMA.md

primat-data-owner-service/src/main/resources/config/dblp_scholar/
├── party_DBLP_local.json   # party/debug/dataSource, mai riscritto dal processo
├── party_DBLP_live.json    # mqttBrokerUrl/bloomFilter/missingValueHandling/columns, pushabile dalla SMU
├── party_Scholar_local.json
└── party_Scholar_live.json
```

## Nota su dirty/clean

Il mapping `idDBLP -> idScholar` non è 1:1: un `idDBLP` può comparire fino a
20 volte (voci Scholar duplicate per lo stesso paper) e un `idScholar` fino a
4 volte — quest'ultimo caso fa sì che alcuni cluster contengano **più di un
record DBLP** (tipicamente versione rivista + versione conferenza dello
stesso paper): la party DBLP non è quindi perfettamente duplicate-free, ma
lo è per la stragrande maggioranza dei cluster (vedi percentuale in
`SCHEMA.md`). La party Scholar è invece chiaramente dirty. Dettagli e numeri
esatti in `SCHEMA.md` dopo il build.
