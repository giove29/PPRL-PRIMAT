# NC Voters (Naumann) — dataset ridotti e bilanciati per PRIMAT

Scarica il dataset reale "NC Voters" pubblicato da Naumann (HPI) —
https://hpi.de/naumann/projects/repeatability/datasets/ncvoters-dataset.html,
snapshot NC SBE `VR_Snapshot_20181106`, 14.183 record con 9.819 coppie
duplicate dichiarate — e produce dataset di test più piccoli ma
proporzionalmente fedeli all'originale (stesso rapporto duplicati/non-duplicati
e stessa distribuzione delle dimensioni-cluster), con il ground truth scritto
come colonna `GLOBAL_ID` in ciascun file prodotto (stesso valore tra record di
party diverse = match vero), pronti per la pipeline Data Owner Service di
PRIMAT.

## Uso

```bash
pip install -r ncvoters_naumann/requirements.txt

# 1. scarica i 3 file sorgente (fallback manuale stampato se il sito blocca il download)
python ncvoters_naumann/download_ncvoters.py

# 2. ispeziona lo schema reale e genera ncvoters_naumann/column_mapping.json
python ncvoters_naumann/inspect_schema.py
# -> rivedi column_mapping.json (colonne QID di default, modificabile liberamente),
#    imposta "_verified": true

# 3. costruisce ground truth + campioni bilanciati + CSV/JSON PRIMAT
python ncvoters_naumann/build_all.py --sizes 500,1000,2000,5000 --include-full
```

Output per ogni dimensione `<size>`:

```
primat-examples/src/main/resources/ncvoters_naumann/<size>/
├── ncvoters_<size>.csv   # combinato A+B, ';'-delimitato, NESSUN header
├── party_A.csv           # solo righe PARTY=A
├── party_B.csv           # solo righe PARTY=B
└── SCHEMA.md             # colonne, provenienza, statistiche cluster/ratio

primat-data-owner-service/src/main/resources/config/ncvoters_naumann/<size>/
├── party_A.json
└── party_B.json
```

Le colonne QID usate sono configurabili: `inspect_schema.py` propone un
sottoinsieme di default (nomi campo REALI del dataset NCVR, non forzati su
convenzioni sintetiche tipo FN/MN/LN) in `column_mapping.json` — modificabile
a mano prima del passo 3.

## Note

- `raw/` (i 3 file scaricati) non è versionato (vedi `.gitignore` root).
- Seed fisso (default 42, coerente col resto del repo) per riproducibilità.
- Il confronto numerico con i risultati pubblicati da Naumann e qualsiasi
  visualizzazione sono fuori scope di questi script — passo successivo.
