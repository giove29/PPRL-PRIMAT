#!/usr/bin/env python3
"""Svuota uno dei database Postgres delle 4 strategie di clustering
persistenti (Center Clustering, MSCD-AP, Global Greedy,
CLIP — vedi CLAUDE.md/DATABASE_SCHEMA.md), cosi' da poter ripetere un run
end-to-end da zero senza cluster storici residui.

Esegue lo stesso reset gia' documentato in DATABASE_SCHEMA.md/TESTING.md:

    DROP SCHEMA public CASCADE; CREATE SCHEMA public;

sul database scelto. Le tabelle vengono ricreate da sole al run successivo
(hibernate.hbm2ddl.auto=update), non serve alcuno script SQL a mano.

Richiede il driver `psycopg2` (`pip install psycopg2-binary`, gia' usato da
evaluate_mscd_ap.py/eval_core.py).

Uso:
    python reset_db.py --strategy global-greedy
    python reset_db.py --strategy clip
    python reset_db.py --strategy mscd-ap --host localhost --port 5432 --user primat --password primat
    python reset_db.py --dbname primat_clip   # bypassa --strategy con un nome esplicito
    python reset_db.py --all                  # svuota tutti e 4 i database, con conferma
"""

import argparse
import sys

try:
    import psycopg2
except ImportError:
    psycopg2 = None

# Stessa mappa strategia -> dbname di default usata da evaluate_mscd_ap.py,
# estesa alle 2 nuove strategie (vedi persistence.xml).
STRATEGIES = {
    "center-clustering": "primat_center_clustering",
    "mscd-ap": "primat_mscd_ap",
    "global-greedy": "primat_global_greedy",
    "clip": "primat_clip",
}


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--strategy", choices=sorted(STRATEGIES), help="quale DB dedicato svuotare")
    group.add_argument("--dbname", help="nome esplicito del database, in alternativa a --strategy")
    group.add_argument("--all", action="store_true", help="svuota tutti e 5 i database delle strategie")
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", default=5432, type=int)
    parser.add_argument("--user", default="primat")
    parser.add_argument("--password", default="primat")
    parser.add_argument("--yes", action="store_true", help="non chiedere conferma prima del reset")
    return parser.parse_args()


def reset_one(host, port, dbname, user, password):
    conn = psycopg2.connect(host=host, port=port, dbname=dbname, user=user, password=password)
    conn.autocommit = True
    try:
        with conn.cursor() as cur:
            cur.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;")
    finally:
        conn.close()
    print("OK - {} svuotato.".format(dbname))


def main():
    args = parse_args()

    if psycopg2 is None:
        print("Il modulo 'psycopg2' non e' installato. Installarlo con:")
        print("  pip install psycopg2-binary")
        sys.exit(1)

    if args.all:
        targets = sorted(set(STRATEGIES.values()))
    elif args.dbname is not None:
        targets = [args.dbname]
    else:
        targets = [STRATEGIES[args.strategy]]

    if not args.yes:
        answer = input("Questo cancella TUTTI i dati in {}. Continuare? [y/N] ".format(", ".join(targets)))
        if answer.strip().lower() not in ("y", "yes", "s", "si"):
            print("Annullato.")
            sys.exit(0)

    for dbname in targets:
        try:
            reset_one(args.host, args.port, dbname, args.user, args.password)
        except psycopg2.OperationalError as e:
            print("Impossibile connettersi a Postgres ({}): {}".format(dbname, e))
            sys.exit(1)


if __name__ == "__main__":
    main()
