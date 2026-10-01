"""Converte il benchmark DBLP-Scholar nel formato PRIMAT: CSV `;`-delimitato
con GLOBAL_ID come colonna (ground truth), piu' JSON Data Owner per le due
party naturali del dataset (DBLP e Scholar).

A differenza di NC Voters, qui il ground truth e' gia' un problema di
linkage a 2 sorgenti (non un dedup a sorgente singola): ogni riga di
DBLP1.csv appartiene alla party "DBLP", ogni riga di Scholar.csv alla party
"Scholar" - nessuno split artificiale necessario. Il file
DBLP-Scholar_perfectMapping.csv (idDBLP,idScholar) NON e' una relazione 1:1:
un idDBLP puo' comparire fino a 20 volte (piu' voci Scholar duplicate per lo
stesso paper) e un idScholar fino a 4 volte -> si usa union-find esattamente
come per NC Voters, perche' alcuni cluster finiscono per contenere piu' di
un record DBLP (es. versione conferenza + versione rivista dello stesso
paper, 53 cluster su ~2.616 nel dataset reale): la party "DBLP" e' quindi
QUASI ma non perfettamente duplicate-free.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

import pandas as pd

sys.stdout.reconfigure(encoding="utf-8")

TEXT_PREPROCESSING = [
    {"type": "TRIM"},
    {"type": "UPPERCASE"},
    {"type": "REMOVE_ACCENTS"},
    {"type": "REMOVE_SPECIAL_CHARS"},
    {"type": "TRUNCATE", "from": 0, "to": 60},
]
NUMERIC_PREPROCESSING = [
    {"type": "TRIM"},
    {"type": "REMOVE_NON_DIGITS"},
]
QID_COLUMNS = ["title", "authors", "venue", "year"]
NUMERIC_QID_COLUMNS = {"year"}


class UnionFind:
    def __init__(self, items):
        self.parent = {x: x for x in items}
        self.rank = {x: 0 for x in items}

    def find(self, x):
        root = x
        while self.parent[root] != root:
            root = self.parent[root]
        while self.parent[x] != root:
            self.parent[x], x = root, self.parent[x]
        return root

    def union(self, a, b):
        ra, rb = self.find(a), self.find(b)
        if ra == rb:
            return
        if self.rank[ra] < self.rank[rb]:
            ra, rb = rb, ra
        self.parent[rb] = ra
        if self.rank[ra] == self.rank[rb]:
            self.rank[ra] += 1


def _sanitize(value: Any) -> str:
    return str(value).replace(";", " ").replace("\t", " ").replace("\n", " ").replace("\r", " ").strip()


def load_source(path: Path, encoding: str) -> pd.DataFrame:
    df = pd.read_csv(path, dtype=str, keep_default_na=False, encoding=encoding)
    df.columns = [c.replace("﻿", "").strip() for c in df.columns]
    return df


def build_global_ids(dblp_ids: list[str], scholar_ids: list[str], pairs: list[tuple[str, str]]) -> dict[str, str]:
    keys = [f"D:{i}" for i in dblp_ids] + [f"S:{i}" for i in scholar_ids]
    uf = UnionFind(keys)
    for d, s in pairs:
        uf.union(f"D:{d}", f"S:{s}")

    root_to_gid: dict[str, str] = {}
    next_idx = 1
    global_id_of: dict[str, str] = {}
    for key in keys:
        root = uf.find(key)
        if root not in root_to_gid:
            root_to_gid[root] = f"G{next_idx:06d}"
            next_idx += 1
        global_id_of[key] = root_to_gid[root]
    return global_id_of


def build_rows(party: str, df: pd.DataFrame, key_prefix: str, global_id_of: dict[str, str]) -> list[dict[str, Any]]:
    rows = []
    for _, rec in df.iterrows():
        gid = global_id_of[f"{key_prefix}:{rec['id']}"]
        row = {"PARTY": party, "GLOBAL_ID": gid}
        for col in QID_COLUMNS:
            row[col] = _sanitize(rec.get(col, ""))
        rows.append(row)
    return rows


def write_csv_no_header(rows: list[dict[str, Any]], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as fh:
        for i, row in enumerate(rows, start=1):
            fields = [row["PARTY"], row["GLOBAL_ID"], str(i)] + [row[c] for c in QID_COLUMNS]
            fh.write(";".join(fields) + "\n")


def write_data_owner_json(party: str, csv_rel_path: str, out_path: Path) -> None:
    """Scrive la coppia di file locale/live attesa da DataOwnerConfigLoader.load(local, live):
    party/debug/dataSource nel file "locale" (mai riscritto dal processo), il resto - incluso
    mqttBrokerUrl, oggi pushabile/hot-riconfigurabile dalla SMU - nel file "live". ``out_path``
    resta lo stem condiviso (es. .../party_DBLP.json): i due file reali sono
    ``<stem>_local.json``/``<stem>_live.json``."""
    columns = [
        {"index": 0, "name": "PARTY", "role": "PARTY"},
        {"index": 1, "name": "GLOBAL_ID", "role": "GLOBAL_ID"},
        {"index": 2, "name": "ID", "role": "ID"},
    ]
    for i, col in enumerate(QID_COLUMNS):
        preprocessing = NUMERIC_PREPROCESSING if col in NUMERIC_QID_COLUMNS else TEXT_PREPROCESSING
        columns.append({"index": 3 + i, "name": col, "role": "QID", "preprocessing": preprocessing})

    local_config = {
        "party": party,
        "debug": True,
        "dataSource": {"type": "CSV", "csv": {"delimiter": ";", "filePath": csv_rel_path}},
    }
    live_config = {
        "mqttBrokerUrl": "tcp://localhost:1883",
        "bloomFilter": {"length": 1024, "hardeningChain": []},
        "missingValueHandling": {"enabled": True, "anchorPriority": ["title", "authors", "venue", "year"]},
        "columns": columns,
    }
    out_path.parent.mkdir(parents=True, exist_ok=True)
    stem = out_path.with_suffix("")
    stem.with_name(stem.name + "_local.json").write_text(
        json.dumps(local_config, indent=2, ensure_ascii=False), encoding="utf-8")
    stem.with_name(stem.name + "_live.json").write_text(
        json.dumps(live_config, indent=2, ensure_ascii=False), encoding="utf-8")


def write_schema_doc(path: Path, stats: dict[str, Any]) -> None:
    lines = [
        "# Dataset DBLP-Scholar (Köpcke/Thor/Rahm, Database Group Leipzig)",
        "",
        "Formato: CSV `;`-delimitato, **senza riga di header**.",
        "",
        "## Colonne (posizionali)",
        "",
        "| idx | nome | ruolo | tipo | fonte |",
        "|---|---|---|---|---|",
        "| 0 | PARTY | PARTY | - | `DBLP` o `Scholar`, per riga sorgente |",
        "| 1 | GLOBAL_ID | GLOBAL_ID | - | **ground truth**: union-find su `DBLP-Scholar_perfectMapping.csv` |",
        "| 2 | ID | ID | - | locale, 1..n per file |",
        "| 3 | title | QID | testo | colonna reale `title` |",
        "| 4 | authors | QID | testo | colonna reale `authors` |",
        "| 5 | venue | QID | testo | colonna reale `venue` |",
        "| 6 | year | QID | numerico | colonna reale `year` |",
        "",
        "## Statistiche",
        "",
        f"- Record DBLP: {stats['dblp_records']}",
        f"- Record Scholar: {stats['scholar_records']}",
        f"- Coppie nel perfect mapping: {stats['mapping_pairs']}",
        f"- Cluster (entità) totali dopo union-find: {stats['total_clusters']}",
        f"- Cluster con più di 1 record DBLP (stesso paper, 2 versioni): {stats['dblp_multi_clusters']} "
        "— la party DBLP non è quindi *perfettamente* duplicate-free, ma lo è al "
        f"{stats['dblp_clean_pct']:.1f}%.",
        f"- Cluster con più di 1 record Scholar: {stats['scholar_multi_clusters']} "
        "(party Scholar chiaramente dirty, fino a 20 voci per lo stesso paper).",
        "",
        "Scelta per PRIMAT: `party DBLP` dichiarata `duplicateFree: true` (convenzione standard in "
        "letteratura per questo benchmark, DBLP trattata come sorgente \"pulita\"), `party Scholar` "
        "`duplicateFree: false`. Adatto a **MSCD_AP** (richiede almeno una party clean).",
        "",
        "Fonte: https://dbs.uni-leipzig.de/research/projects/object_matching/benchmark_datasets_for_entity_resolution "
        "(Köpcke, Thor, Rahm, \"Evaluation of entity resolution approaches on real-world match problems\", VLDB 2010).",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--raw-dir", default="dblp_scholar/raw")
    parser.add_argument("--examples-out", default="primat-examples/src/main/resources/dblp_scholar")
    parser.add_argument("--dataowner-out", default="primat-data-owner-service/src/main/resources/config/dblp_scholar")
    args = parser.parse_args()

    raw_dir = Path(args.raw_dir)
    dblp_df = load_source(raw_dir / "DBLP1.csv", encoding="latin-1")
    scholar_df = load_source(raw_dir / "Scholar.csv", encoding="utf-8-sig")
    mapping_df = pd.read_csv(raw_dir / "DBLP-Scholar_perfectMapping.csv", dtype=str, encoding="utf-8-sig")

    dblp_ids = dblp_df["id"].tolist()
    scholar_ids = scholar_df["id"].tolist()
    pairs = list(zip(mapping_df["idDBLP"], mapping_df["idScholar"]))

    print(f"DBLP: {len(dblp_ids)} record, Scholar: {len(scholar_ids)} record, mapping: {len(pairs)} coppie")
    print("Costruisco il ground truth (union-find su idDBLP/idScholar)...")
    global_id_of = build_global_ids(dblp_ids, scholar_ids, pairs)

    dblp_rows = build_rows("DBLP", dblp_df, "D", global_id_of)
    scholar_rows = build_rows("Scholar", scholar_df, "S", global_id_of)

    from collections import Counter

    dblp_cluster_count = Counter(r["GLOBAL_ID"] for r in dblp_rows)
    scholar_cluster_count = Counter(r["GLOBAL_ID"] for r in scholar_rows)
    dblp_multi = sum(1 for c in dblp_cluster_count.values() if c > 1)
    scholar_multi = sum(1 for c in scholar_cluster_count.values() if c > 1)
    total_clusters = len(set(global_id_of.values()))

    stats = {
        "dblp_records": len(dblp_rows),
        "scholar_records": len(scholar_rows),
        "mapping_pairs": len(pairs),
        "total_clusters": total_clusters,
        "dblp_multi_clusters": dblp_multi,
        "scholar_multi_clusters": scholar_multi,
        "dblp_clean_pct": 100.0 * (len(dblp_cluster_count) - dblp_multi) / max(len(dblp_cluster_count), 1),
    }

    examples_out = Path(args.examples_out)
    dataowner_out = Path(args.dataowner_out)

    party_dblp_csv = examples_out / "party_DBLP.csv"
    party_scholar_csv = examples_out / "party_Scholar.csv"
    combined_csv = examples_out / "combined.csv"
    schema_md = examples_out / "SCHEMA.md"

    write_csv_no_header(dblp_rows, party_dblp_csv)
    write_csv_no_header(scholar_rows, party_scholar_csv)
    write_csv_no_header(dblp_rows + scholar_rows, combined_csv)
    write_schema_doc(schema_md, stats)

    write_data_owner_json("DBLP", str(party_dblp_csv.as_posix()), dataowner_out / "party_DBLP.json")
    write_data_owner_json("Scholar", str(party_scholar_csv.as_posix()), dataowner_out / "party_Scholar.json")

    print(f"\nEntità (cluster) totali: {total_clusters}")
    print(f"Cluster con >1 record DBLP: {dblp_multi} (party DBLP pulita al {stats['dblp_clean_pct']:.1f}%)")
    print(f"Cluster con >1 record Scholar: {scholar_multi}")
    print(f"\nScritto: {party_dblp_csv}, {party_scholar_csv}, {combined_csv}, {schema_md}")
    print(f"Scritto: {dataowner_out / 'party_DBLP_local.json'}, {dataowner_out / 'party_DBLP_live.json'}, "
          f"{dataowner_out / 'party_Scholar_local.json'}, {dataowner_out / 'party_Scholar_live.json'}")


if __name__ == "__main__":
    main()
