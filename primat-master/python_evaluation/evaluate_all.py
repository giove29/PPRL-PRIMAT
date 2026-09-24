#!/usr/bin/env python3
"""Confronta in un colpo solo tutte e 5 le modalita' di clustering/persistenza
della Linkage Unit: MCL (CSV di debug, non persistente) + le 4 strategie
persistenti su Postgres (Center Clustering, MSCD-AP,
Global Greedy, CLIP, ognuna sul proprio database — vedi
persistence.xml/CLAUDE.md, 2026-09-16 e 2026-09-17).

E' il complemento di `evaluate_mcl.py`/`evaluate_mscd_ap.py` (che restano gli
script da usare per il dettaglio di una singola strategia: entita' split,
cluster misti, record senza cluster): qui la stessa logica di valutazione di
`eval_core.py` viene applicata a tutte e 5 le fonti una dopo l'altra e i
risultati stampati come tabella comparativa. Una fonte non raggiungibile
(container Postgres non ancora popolato per quella strategia, CSV MCL non
ancora scritto) viene riportata come riga "N/A" invece di interrompere lo
script: e' normale non aver ancora eseguito tutte e 5 le strategie.

Richiede il driver `psycopg2` per le 4 fonti Postgres (`pip install
psycopg2-binary`); la fonte MCL (CSV) non lo richiede.

Uso:
    python evaluate_all.py [--host HOST] [--port PORT] [--user USER]
                            [--password PASSWORD] [--mcl-csv PATH]

I default coincidono con quelli di `evaluate_mscd_ap.py`/`evaluate_mcl.py`
(stesso host/porta/credenziali per le 4 persistence-unit, che vivono sullo
stesso container Postgres — solo il nome del database cambia).
"""

import argparse

from eval_core import evaluate, load_from_csv, load_from_postgres

try:
    import psycopg2
except ImportError:
    psycopg2 = None

# (dbname di default, etichetta) per ciascuna delle 4 strategie persistenti,
# stesso ordine/nomi di evaluate_mscd_ap.py.STRATEGIES.
POSTGRES_STRATEGIES = [
    ("primat_center_clustering", "Center Clustering"),
    ("primat_mscd_ap", "MSCD-AP"),
    ("primat_global_greedy", "Global Greedy"),
    ("primat_clip", "CLIP"),
]


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", default=5432, type=int)
    parser.add_argument("--user", default="primat")
    parser.add_argument("--password", default="primat")
    parser.add_argument("--mcl-csv", default="mcl_debug_output.csv")
    return parser.parse_args()


def evaluate_mcl(path):
    try:
        records = load_from_csv(path)
    except FileNotFoundError:
        return None, "CSV non trovato: {} (eseguire prima un run MCL)".format(path)
    if not records:
        return None, "CSV vuoto: {}".format(path)
    return evaluate(records), None


def evaluate_postgres(args, dbname):
    if psycopg2 is None:
        return None, "psycopg2 non installato (pip install psycopg2-binary)"
    try:
        records = load_from_postgres(host=args.host, port=args.port, dbname=dbname,
                                      user=args.user, password=args.password)
    except (psycopg2.OperationalError, RuntimeError) as e:
        return None, "connessione a {} fallita: {}".format(dbname, e)
    if not records:
        return None, "nessun record su {} (eseguire prima un run per questa strategia)".format(dbname)
    return evaluate(records), None


def print_comparison_row(label, source, result, error):
    if result is None:
        print("{:<28} {:<28} N/A — {}".format(label, source, error))
        return
    print("{:<28} {:<28} TP={} FP={} FN={} GT={}  recall={:.3f}  precision={:.3f}  F1={:.3f}  cluster={:<4} record={:<4} "
          "split={:<3} misti={:<3} singleton={:<3}".format(
              label, source, result["tp"], result["fp"], result["fn"], result["gt"],
              result["recall"], result["precision"], result["f1"],
              result["n_clusters"], result["n_records"], len(result["split_entities"]),
              len(result["mixed_clusters"]), len(result["singleton_records"])))


def main():
    args = parse_args()

    print("=== Confronto di tutti gli algoritmi di clustering e le modalita' di salvataggio ===\n")
    print("{:<28} {:<28} {}".format("strategia", "sorgente", "metriche (rispetto a GLOBAL_ID)"))
    print("-" * 110)

    mcl_result, mcl_error = evaluate_mcl(args.mcl_csv)
    print_comparison_row("MCL", "CSV: " + args.mcl_csv, mcl_result, mcl_error)

    for dbname, label in POSTGRES_STRATEGIES:
        result, error = evaluate_postgres(args, dbname)
        print_comparison_row(label, "Postgres: " + dbname, result, error)

    print()
    print("Per il dettaglio di una singola strategia (entita' split, cluster misti, record "
          "senza cluster): python evaluate_mcl.py [csv] oppure python evaluate_mscd_ap.py "
          "--strategy {mscd-ap,center-clustering,global-greedy,clip}")


if __name__ == "__main__":
    main()
