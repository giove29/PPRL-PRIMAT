#!/usr/bin/env python3
"""Valuta offline le performance di una delle 4 strategie di clustering
persistenti su Postgres: MSCD-AP, Center Clustering,
Global Greedy, CLIP (dal 2026-09-16/2026-09-17 ciascuna ha il proprio
database dedicato, vedi persistence.xml/CLAUDE.md — prima esisteva solo
MSCD-AP su un unico DB condiviso "primat", da cui il nome storico di questo
script).

Si connette direttamente a Postgres (stesse tabelle descritte in
DATABASE_SCHEMA.md, replicate identiche in tutte le persistence-unit) e ricava
la Link Table dalle righe di `record`/`cluster` correnti: nessun file
intermedio, legge sempre lo stato piu' recente del DB (a differenza di
`evaluate_mcl.py`, che legge invece un CSV di debug scritto dalla pipeline,
perche' MCL non e' persistito su database).

Richiede il driver `psycopg2` (`pip install psycopg2-binary`).

La colonna `record.party` non esiste (vedi DATABASE_SCHEMA.md: il campo Java
e' @Transient): il party di ogni record viene derivato facendo il match tra
il prefisso del suo `id` e i nomi party noti in `party.name`, la stessa
euristica gia' documentata in DATABASE_SCHEMA.md (funziona con la
convenzione usata dai CSV sintetici del progetto, es. id "A1" -> party "A").

Uso:
    python evaluate_mscd_ap.py [--strategy {mscd-ap,center-clustering,global-greedy,clip}]
                                [--host HOST] [--port PORT] [--dbname DBNAME]
                                [--user USER] [--password PASSWORD]

`--strategy` (default `mscd-ap`, invariato per compatibilita' con l'uso
storico di questo script) seleziona sia il `--dbname` di default sia
l'etichetta stampata nel report; `--dbname` esplicito sovrascrive comunque
il default derivato dalla strategia. I default di dbname corrispondono ai
nomi di database di default in
primat-linkage-unit/src/main/resources/META-INF/persistence.xml.
"""

import argparse
import sys

# Logica di caricamento/valutazione condivisa con evaluate_mcl.py, con
# evaluate_all.py e con la GUI Streamlit (evaluation_app.py) — vedi
# eval_core.py per i dettagli implementativi e il perche' della condivisione.
from eval_core import evaluate, load_from_postgres, print_report

try:
    import psycopg2
except ImportError:
    psycopg2 = None

# Mappa strategia -> (dbname di default, etichetta per il report), allineata
# ai default di persistence.xml e al nome di ClusteringMethod lato Java.
STRATEGIES = {
    "mscd-ap": ("primat_mscd_ap", "MSCD-AP"),
    "center-clustering": ("primat_center_clustering", "Center Clustering"),
    "global-greedy": ("primat_global_greedy", "Global Greedy"),
    "clip": ("primat_clip", "CLIP"),
}


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--strategy", choices=sorted(STRATEGIES), default="mscd-ap")
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", default=5432, type=int)
    parser.add_argument("--dbname", default=None, help="default: derivato da --strategy")
    parser.add_argument("--user", default="primat")
    parser.add_argument("--password", default="primat")
    return parser.parse_args()


def main():
    args = parse_args()
    default_dbname, title = STRATEGIES[args.strategy]
    dbname = args.dbname if args.dbname is not None else default_dbname

    if psycopg2 is None:
        print("Il modulo 'psycopg2' non e' installato. Installarlo con:")
        print("  pip install psycopg2-binary")
        sys.exit(1)

    try:
        records = load_from_postgres(host=args.host, port=args.port, dbname=dbname,
                                      user=args.user, password=args.password)
    except psycopg2.OperationalError as e:
        print("Impossibile connettersi a Postgres ({}): {}".format(dbname, e))
        sys.exit(1)

    if not records:
        print("Nessun record trovato sul database {}.".format(dbname))
        sys.exit(0)

    result = evaluate(records)
    print_report(result, title=title)


if __name__ == "__main__":
    main()
