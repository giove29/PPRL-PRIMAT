#!/usr/bin/env python3
"""Valuta offline le performance del clustering MCL (Markov Clustering).

Legge il CSV di debug che `LinkageUnitOrchestrator.writeMclDebugFile(...)`
sovrascrive ad ogni run risolto con la strategia MCL, quando
`LinkageUnitOrchestrator.MCL_DEBUG_EXPORT` e' `true` (vedi CLAUDE.md /
ARCHITECTURE_FLOW.md / TESTING.md). Non richiede Java, Maven ne' Postgres:
tutto il necessario (cluster assegnato + GLOBAL_ID di ground truth per ogni
record) e' gia' nel CSV.

Formato atteso (una riga per record, con header):
    cluster_id,party,record_id,global_id

Uso:
    python evaluate_mcl.py [percorso_csv]

Se omesso, percorso_csv default a "mcl_debug_output.csv" nella directory
corrente (lo stesso path scritto da LinkageUnitOrchestrator quando lanciato
dalla root del repo).
"""

import sys

# Logica di caricamento/valutazione condivisa con evaluate_mscd_ap.py e con
# la GUI Streamlit (evaluation_app.py) — vedi eval_core.py per i dettagli
# implementativi e il perche' della condivisione.
from eval_core import evaluate, load_from_csv, print_report


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "mcl_debug_output.csv"
    try:
        records = load_from_csv(path)
    except FileNotFoundError:
        print("File non trovato: {}".format(path))
        print("Eseguire prima un run MCL (tutte le party dirty) con "
              "persistence.csvOutputPath configurato.")
        sys.exit(1)

    if not records:
        print("Il file {} e' vuoto: nessun record da valutare.".format(path))
        sys.exit(0)

    result = evaluate(records)
    print_report(result, title="MCL")


if __name__ == "__main__":
    main()
