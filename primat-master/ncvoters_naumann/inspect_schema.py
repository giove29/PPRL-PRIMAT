"""Ispeziona lo schema reale dei 3 file NC Voters scaricati e propone un mapping.

Non forza i nomi di colonna su convenzioni predefinite (FN/MN/LN/...): la
pipeline PRIMAT moderna (Data Owner Service) accetta colonne QID liberamente
configurabili via JSON, quindi qui ci limitiamo a scoprire e documentare i
nomi REALI delle colonne del dataset scaricato.

Verificato una volta sul dataset reale (snapshot VR_Snapshot_20181106):
- ncvoters.tsv: TSV con header, BOM UTF-8, 91 colonne, 14.183 righe dati.
  La colonna 'id' è la chiave di join verso ncvoters_DPL.tsv/ncvoters_NDPL.tsv
  (overlap 100% con gli id citati in DPL). Non sono 'voter_reg_num' né 'ncid'.
- ncvoters_DPL.tsv: TSV con header 'id1\tid2', 9.819 righe (coppie duplicate).
- ncvoters_NDPL.tsv: TSV con header 'id1\tid2\tparticipation', 98.142 righe.

Questo script ri-verifica empiricamente (non assume) la colonna id tramite
overlap massimo con gli id citati nei file di coppie, cosi' da restare
corretto anche se Naumann cambia lo schema in futuro.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import pandas as pd

sys.stdout.reconfigure(encoding="utf-8")

# Sottoinsieme di default proposto come QID: campi identificativi di persona/
# indirizzo, esclusi i campi puramente amministrativi/distrettuali (decine di
# colonne *_abbrv/*_desc di distretti elettorali, non utili al linkage).
# Liberamente modificabile dall'utente in column_mapping.json prima del build.
DEFAULT_QID_CANDIDATES = [
    "first_name",
    "midl_name",
    "last_name",
    "age",
    "sex_code",
    "race_code",
    "res_city_desc",
    "street_name",
    "house_num",
    "zip_code",
    "birth_place",
]

# Colonne QID il cui contenuto e' prevalentemente numerico (catena di
# preprocessing REMOVE_NON_DIGITS invece della catena testuale standard).
NUMERIC_QID_CANDIDATES = {"age", "house_num", "zip_code"}


def read_tsv(path: Path, nrows: int | None = None) -> pd.DataFrame:
    df = pd.read_csv(
        path,
        sep="\t",
        dtype=str,
        keep_default_na=False,
        encoding="utf-8-sig",
        nrows=nrows,
    )
    # Il file sorgente contiene un BOM ﻿ anche non in testa al file
    # (osservato subito prima dell'ultima colonna, 'snapshot_dt'): utf-8-sig
    # rimuove solo quello a inizio file, quindi ripuliamo esplicitamente i
    # nomi colonna da eventuali BOM residui.
    df.columns = [c.replace("﻿", "") for c in df.columns]
    return df


def find_record_id_column(ncvoters_df: pd.DataFrame, pair_ids: set[int]) -> str | None:
    """Trova la colonna di ncvoters.tsv con il maggior overlap numerico con gli
    id citati nei file di coppie (DPL/NDPL) — generico, non assume il nome."""
    best_col, best_overlap = None, -1
    for col in ncvoters_df.columns:
        values = pd.to_numeric(ncvoters_df[col], errors="coerce").dropna()
        if values.empty:
            continue
        overlap = len(set(values.astype("int64").tolist()) & pair_ids)
        if overlap > best_overlap:
            best_col, best_overlap = col, overlap
    return best_col


def load_pair_ids(path: Path) -> set[int]:
    df = read_tsv(path)
    id_cols = [c for c in df.columns if c.lower() in ("id1", "id2")]
    if len(id_cols) != 2:
        # fallback generico: prime 2 colonne
        id_cols = list(df.columns[:2])
    ids: set[int] = set()
    for col in id_cols:
        ids.update(pd.to_numeric(df[col], errors="coerce").dropna().astype("int64").tolist())
    return ids


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--raw-dir", default="ncvoters_naumann/raw")
    parser.add_argument("--out", default="ncvoters_naumann/column_mapping.json")
    parser.add_argument("--sample-rows", type=int, default=5000)
    args = parser.parse_args()

    raw_dir = Path(args.raw_dir)
    ncvoters_path = raw_dir / "ncvoters.tsv"
    dpl_path = raw_dir / "ncvoters_DPL.tsv"
    ndpl_path = raw_dir / "ncvoters_NDPL.tsv"

    print(f"Leggo {ncvoters_path} (fino a {args.sample_rows} righe di campione)...")
    sample_df = read_tsv(ncvoters_path, nrows=args.sample_rows)
    print(f"Colonne trovate ({len(sample_df.columns)}): {list(sample_df.columns)}\n")
    print("Esempio prime 3 righe:")
    print(sample_df.head(3).to_string())

    print(f"\nLeggo {dpl_path} per individuare la colonna id per overlap...")
    dpl_ids = load_pair_ids(dpl_path)
    record_id_col = find_record_id_column(sample_df, dpl_ids)
    print(f"Colonna id rilevata (overlap massimo con DPL): {record_id_col!r}")

    qid_columns = [c for c in DEFAULT_QID_CANDIDATES if c in sample_df.columns]
    missing = [c for c in DEFAULT_QID_CANDIDATES if c not in sample_df.columns]
    if missing:
        print(f"ATTENZIONE: colonne QID di default assenti nel file reale: {missing}")

    mapping = {
        "_verified": False,
        "_note": "Rivedere record_id_column/qid_columns, poi impostare _verified=true.",
        "ncvoters_file": "ncvoters.tsv",
        "ncvoters_columns": list(sample_df.columns),
        "record_id_column": record_id_col,
        "dpl_file": "ncvoters_DPL.tsv",
        "ndpl_file": "ncvoters_NDPL.tsv",
        "qid_columns": qid_columns,
        "numeric_qid_columns": [c for c in qid_columns if c in NUMERIC_QID_CANDIDATES],
    }

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(mapping, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"\nScritto {out_path}. Rivedilo e imposta \"_verified\": true prima di lanciare build_all.py.")


if __name__ == "__main__":
    main()
