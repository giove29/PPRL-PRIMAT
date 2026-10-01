"""Scrive l'output nel formato PRIMAT: CSV combinato + CSV per party + JSON
Data Owner, a partire dai cluster campionati e dall'assegnazione party.
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pandas as pd

TEXT_PREPROCESSING = [
    {"type": "TRIM"},
    {"type": "UPPERCASE"},
    {"type": "REMOVE_ACCENTS"},
    {"type": "REMOVE_SPECIAL_CHARS"},
    {"type": "TRUNCATE", "from": 0, "to": 30},
]
NUMERIC_PREPROCESSING = [
    {"type": "TRIM"},
    {"type": "REMOVE_NON_DIGITS"},
]


def _sanitize(value: str) -> str:
    """Il delimitatore e' ';': un valore sorgente che la contenesse romperebbe
    il parsing posizionale. Il dataset Naumann e' gia' ripulito da caratteri
    speciali, ma sanifichiamo comunque per sicurezza."""
    return str(value).replace(";", " ").replace("\t", " ").replace("\n", " ").strip()


def load_qid_table(ncvoters_path: Path, record_id_column: str, qid_columns: list[str]) -> pd.DataFrame:
    # vedi build_ground_truth.load_record_ids: alcune colonne del file sorgente
    # (es. l'ultima, 'snapshot_dt') portano un BOM ﻿ residuo nel nome.
    header = pd.read_csv(ncvoters_path, sep="\t", nrows=0, encoding="utf-8-sig").columns
    clean_to_real = {c.replace("﻿", ""): c for c in header}
    real_id_col = clean_to_real.get(record_id_column, record_id_column)
    real_qid_cols = [clean_to_real.get(c, c) for c in qid_columns]

    usecols = [real_id_col] + real_qid_cols
    df = pd.read_csv(
        ncvoters_path, sep="\t", dtype=str, keep_default_na=False, encoding="utf-8-sig",
        usecols=usecols,
    )
    df[real_id_col] = pd.to_numeric(df[real_id_col], errors="coerce").astype("Int64")
    df = df.dropna(subset=[real_id_col])
    df[real_id_col] = df[real_id_col].astype("int64")
    df = df.set_index(real_id_col)
    # read_csv con usecols preserva l'ordine delle colonne nel FILE, non quello
    # di usecols: riordiniamo esplicitamente prima di rinominare.
    df = df[real_qid_cols]
    df.columns = qid_columns  # ripristina i nomi "puliti" richiesti dal chiamante
    return df


def build_rows(
    selected_clusters: dict[str, list[int]],
    party_of: dict[int, str],
    qid_table: pd.DataFrame,
    qid_columns: list[str],
) -> list[dict[str, Any]]:
    rows = []
    for gid, members in selected_clusters.items():
        for rid in members:
            qid_values = qid_table.loc[rid]
            row = {"GLOBAL_ID": gid, "PARTY": party_of[rid], "_SOURCE_ID": rid}
            for col in qid_columns:
                row[col] = _sanitize(qid_values[col])
            rows.append(row)
    return rows


def write_csv_no_header(rows: list[dict[str, Any]], qid_columns: list[str], path: Path, party: str | None = None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    selected = [r for r in rows if party is None or r["PARTY"] == party]
    with open(path, "w", encoding="utf-8", newline="") as fh:
        local_id = 0
        for row in selected:
            local_id += 1
            fields = [row["PARTY"], row["GLOBAL_ID"], str(local_id)] + [row[c] for c in qid_columns]
            fh.write(";".join(fields) + "\n")


def write_data_owner_json(
    template_columns_start_index: int,
    party: str,
    csv_rel_path: str,
    qid_columns: list[str],
    numeric_qid_columns: set[str],
    out_path: Path,
) -> None:
    columns = [
        {"index": 0, "name": "PARTY", "role": "PARTY"},
        {"index": 1, "name": "GLOBAL_ID", "role": "GLOBAL_ID"},
        {"index": 2, "name": "ID", "role": "ID"},
    ]
    for i, col in enumerate(qid_columns):
        index = template_columns_start_index + i
        preprocessing = NUMERIC_PREPROCESSING if col in numeric_qid_columns else TEXT_PREPROCESSING
        columns.append({"index": index, "name": col, "role": "QID", "preprocessing": preprocessing})

    anchor_priority = sorted(qid_columns, key=lambda c: (c not in ("last_name", "first_name"), c))

    config = {
        "party": party,
        "debug": True,
        "mqttBrokerUrl": "tcp://localhost:1883",
        "dataSource": {
            "type": "CSV",
            "csv": {"delimiter": ";", "filePath": csv_rel_path},
        },
        "bloomFilter": {"length": 1024, "hardeningChain": []},
        "missingValueHandling": {"enabled": True, "anchorPriority": anchor_priority},
        "columns": columns,
    }

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(config, indent=2, ensure_ascii=False), encoding="utf-8")


def write_schema_doc(
    path: Path,
    qid_columns: list[str],
    numeric_qid_columns: set[str],
    size_label: str,
    report: dict[str, Any],
    cross_party_true_matches: int,
) -> None:
    lines = [
        f"# Dataset NC Voters (Naumann) — campione '{size_label}'",
        "",
        "Formato: CSV `;`-delimitato, **senza riga di header** "
        "(`DatasetReader`/`DataOwnerConfigLoader` di PRIMAT leggono per posizione).",
        "",
        "## Colonne (posizionali)",
        "",
        "| idx | nome | ruolo | tipo | fonte |",
        "|---|---|---|---|---|",
        "| 0 | PARTY | PARTY | - | generata (A/B) |",
        "| 1 | GLOBAL_ID | GLOBAL_ID | - | **ground truth**: stesso valore = stessa entità reale |",
        "| 2 | ID | ID | - | locale, 1..n per file |",
    ]
    for i, col in enumerate(qid_columns, start=3):
        kind = "numerico" if col in numeric_qid_columns else "testo"
        lines.append(f"| {i} | {col} | QID | {kind} | colonna reale `{col}` di ncvoters.tsv |")

    lines += [
        "",
        "## Ground truth e bilanciamento (originale vs campione)",
        "",
        f"- Record totali originali: {report['original_total_records']}",
        f"- Record totali nel campione: {report['sampled_total_records']}",
        f"- Rapporto duplicati originale: {report['original_duplicate_ratio']:.4f}",
        f"- Rapporto duplicati nel campione: {report['sampled_duplicate_ratio']:.4f}",
        f"- Coppie cross-party (GLOBAL_ID condiviso tra A e B) nel campione: {cross_party_true_matches}",
        "",
        "Distribuzione dimensione-cluster (dimensione: numero di cluster):",
        f"- originale: {report['original_cluster_size_distribution']}",
        f"- campione:  {report['sampled_cluster_size_distribution']}",
        "",
        "Fonte: https://hpi.de/naumann/projects/repeatability/datasets/ncvoters-dataset.html "
        "(snapshot NC SBE VR_Snapshot_20181106).",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8")


def count_cross_party_true_matches(rows: list[dict[str, Any]]) -> int:
    from collections import Counter

    per_gid_parties: dict[str, list[str]] = {}
    for row in rows:
        per_gid_parties.setdefault(row["GLOBAL_ID"], []).append(row["PARTY"])
    matches = 0
    for parties in per_gid_parties.values():
        c = Counter(parties)
        matches += c.get("A", 0) * c.get("B", 0)
    return matches
