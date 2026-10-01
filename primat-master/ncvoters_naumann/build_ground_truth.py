"""Costruisce il ground truth (cluster di entita' reali / GLOBAL_ID) a partire
dalle coppie duplicate di ncvoters_DPL.tsv, con union-find.
"""
from __future__ import annotations

from pathlib import Path
from typing import Any

import pandas as pd


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


def load_record_ids(ncvoters_path: Path, record_id_column: str) -> list[int]:
    # usecols confronta per nome esatto: leggiamo l'header a parte e ripuliamo
    # eventuali BOM residui (osservati prima di 'snapshot_dt') prima di
    # individuare la colonna richiesta, cosi' usecols funziona anche se
    # record_id_column non contiene BOM ma altre colonne si'.
    header = pd.read_csv(ncvoters_path, sep="\t", nrows=0, encoding="utf-8-sig").columns
    clean_to_real = {c.replace("﻿", ""): c for c in header}
    real_col = clean_to_real.get(record_id_column, record_id_column)

    df = pd.read_csv(
        ncvoters_path, sep="\t", dtype=str, keep_default_na=False, encoding="utf-8-sig",
        usecols=[real_col],
    )
    return pd.to_numeric(df[real_col], errors="coerce").dropna().astype("int64").tolist()


def load_dpl_pairs(dpl_path: Path) -> list[tuple[int, int]]:
    df = pd.read_csv(dpl_path, sep="\t", dtype=str, keep_default_na=False, encoding="utf-8-sig")
    id_cols = [c for c in df.columns if c.lower() in ("id1", "id2")] or list(df.columns[:2])
    a = pd.to_numeric(df[id_cols[0]], errors="coerce")
    b = pd.to_numeric(df[id_cols[1]], errors="coerce")
    pairs = []
    for x, y in zip(a, b):
        if pd.notna(x) and pd.notna(y):
            pairs.append((int(x), int(y)))
    return pairs


def build_clusters(record_ids: list[int], pairs: list[tuple[int, int]]) -> dict[int, str]:
    """Ritorna {record_id: global_id}. GLOBAL_ID stabile/leggibile ('G000001', ...),
    assegnato nell'ordine di primo incontro dei record_id (riproducibile)."""
    uf = UnionFind(record_ids)
    for a, b in pairs:
        if a in uf.parent and b in uf.parent:
            uf.union(a, b)

    root_to_gid: dict[int, str] = {}
    next_idx = 1
    global_id_of: dict[int, str] = {}
    for rid in record_ids:
        root = uf.find(rid)
        if root not in root_to_gid:
            root_to_gid[root] = f"G{next_idx:06d}"
            next_idx += 1
        global_id_of[rid] = root_to_gid[root]
    return global_id_of


def clusters_by_global_id(global_id_of: dict[int, str]) -> dict[str, list[int]]:
    clusters: dict[str, list[int]] = {}
    for rid, gid in global_id_of.items():
        clusters.setdefault(gid, []).append(rid)
    return clusters


def cluster_size_distribution(clusters: dict[str, list[int]]) -> dict[int, int]:
    """{dimensione_cluster: numero_cluster_di_quella_dimensione}."""
    dist: dict[int, int] = {}
    for members in clusters.values():
        dist[len(members)] = dist.get(len(members), 0) + 1
    return dist


def ground_truth_stats(clusters: dict[str, list[int]]) -> dict[str, Any]:
    total_records = sum(len(m) for m in clusters.values())
    duplicate_records = sum(len(m) for m in clusters.values() if len(m) > 1)
    dist = cluster_size_distribution(clusters)
    return {
        "total_clusters": len(clusters),
        "total_records": total_records,
        "duplicate_records": duplicate_records,
        "singleton_records": total_records - duplicate_records,
        "duplicate_ratio": duplicate_records / total_records if total_records else 0.0,
        "cluster_size_distribution": dict(sorted(dist.items())),
    }


def build(ncvoters_path: Path, dpl_path: Path, record_id_column: str):
    record_ids = load_record_ids(ncvoters_path, record_id_column)
    pairs = load_dpl_pairs(dpl_path)
    global_id_of = build_clusters(record_ids, pairs)
    clusters = clusters_by_global_id(global_id_of)
    stats = ground_truth_stats(clusters)
    return global_id_of, clusters, stats
