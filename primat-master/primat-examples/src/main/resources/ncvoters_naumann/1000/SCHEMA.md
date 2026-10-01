# Dataset NC Voters (Naumann) — campione '1000'

Formato: CSV `;`-delimitato, **senza riga di header** (`DatasetReader`/`DataOwnerConfigLoader` di PRIMAT leggono per posizione).

## Colonne (posizionali)

| idx | nome | ruolo | tipo | fonte |
|---|---|---|---|---|
| 0 | PARTY | PARTY | - | generata (A/B) |
| 1 | GLOBAL_ID | GLOBAL_ID | - | **ground truth**: stesso valore = stessa entità reale |
| 2 | ID | ID | - | locale, 1..n per file |
| 3 | first_name | QID | testo | colonna reale `first_name` di ncvoters.tsv |
| 4 | midl_name | QID | testo | colonna reale `midl_name` di ncvoters.tsv |
| 5 | last_name | QID | testo | colonna reale `last_name` di ncvoters.tsv |
| 6 | age | QID | numerico | colonna reale `age` di ncvoters.tsv |
| 7 | sex_code | QID | testo | colonna reale `sex_code` di ncvoters.tsv |
| 8 | race_code | QID | testo | colonna reale `race_code` di ncvoters.tsv |
| 9 | res_city_desc | QID | testo | colonna reale `res_city_desc` di ncvoters.tsv |
| 10 | street_name | QID | testo | colonna reale `street_name` di ncvoters.tsv |
| 11 | house_num | QID | numerico | colonna reale `house_num` di ncvoters.tsv |
| 12 | zip_code | QID | numerico | colonna reale `zip_code` di ncvoters.tsv |
| 13 | birth_place | QID | testo | colonna reale `birth_place` di ncvoters.tsv |

## Ground truth e bilanciamento (originale vs campione)

- Record totali originali: 14183
- Record totali nel campione: 1018
- Rapporto duplicati originale: 0.9270
- Rapporto duplicati nel campione: 0.9283
- Coppie cross-party (GLOBAL_ID condiviso tra A e B) nel campione: 584

Distribuzione dimensione-cluster (dimensione: numero di cluster):
- originale: {1: 1035, 2: 4202, 3: 1166, 4: 220, 5: 51, 6: 16, 7: 1, 8: 1}
- campione:  {1: 73, 2: 296, 3: 83, 4: 16, 5: 4, 6: 2, 8: 1}

Fonte: https://hpi.de/naumann/projects/repeatability/datasets/ncvoters-dataset.html (snapshot NC SBE VR_Snapshot_20181106).