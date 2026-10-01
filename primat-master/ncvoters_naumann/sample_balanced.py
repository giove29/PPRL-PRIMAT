"""Campionamento ridotto-ma-bilanciato: campiona CLUSTER interi (mai singoli
record), stratificati per dimensione-cluster, per preservare sia il rapporto
duplicati/non-duplicati sia la distribuzione delle dimensioni-cluster
dell'originale (stessa logica della 'sampling technique... without disrupting
the ratios of duplicates' descritta sulla pagina Naumann, qui riprodotta in
Python invece che con il template Excel non pubblico)."""
from __future__ import annotations

from typing import Any

import numpy as np


def stratify_clusters_by_size(clusters: dict[str, list[int]]) -> dict[int, list[str]]:
    buckets: dict[int, list[str]] = {}
    for gid, members in clusters.items():
        buckets.setdefault(len(members), []).append(gid)
    return buckets


def _largest_remainder_budgets(quotas: dict[int, float], target_total: int) -> dict[int, int]:
    floors = {k: int(np.floor(v)) for k, v in quotas.items()}
    remainder_total = target_total - sum(floors.values())
    remainders = sorted(quotas.items(), key=lambda kv: (kv[1] - np.floor(kv[1])), reverse=True)
    budgets = dict(floors)
    for k, _ in remainders[: max(remainder_total, 0)]:
        budgets[k] += 1
    return budgets


def sample_target_size(
    clusters: dict[str, list[int]],
    target_total_records: int,
    seed: int,
) -> dict[str, list[int]]:
    """Ritorna il sottoinsieme di cluster selezionato (gid -> record_id list)."""
    buckets = stratify_clusters_by_size(clusters)
    total_records = sum(len(v) for v in clusters.values())
    if target_total_records >= total_records:
        return dict(clusters)

    quotas = {
        size: (size * len(gids) / total_records) * target_total_records
        for size, gids in buckets.items()
    }
    budgets = _largest_remainder_budgets(quotas, target_total_records)

    selected: dict[str, list[int]] = {}
    for size, gids in buckets.items():
        budget = budgets.get(size, 0)
        if budget <= 0:
            continue
        rng = np.random.default_rng(seed + size)
        order = rng.permutation(len(gids))
        running = 0
        for idx in order:
            if running >= budget:
                break
            gid = gids[idx]
            selected[gid] = clusters[gid]
            running += size
    return selected


def sampling_report(original_stats: dict[str, Any], sampled_stats: dict[str, Any]) -> dict[str, Any]:
    return {
        "original_duplicate_ratio": original_stats["duplicate_ratio"],
        "sampled_duplicate_ratio": sampled_stats["duplicate_ratio"],
        "original_cluster_size_distribution": original_stats["cluster_size_distribution"],
        "sampled_cluster_size_distribution": sampled_stats["cluster_size_distribution"],
        "original_total_records": original_stats["total_records"],
        "sampled_total_records": sampled_stats["total_records"],
    }
