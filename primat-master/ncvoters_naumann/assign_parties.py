"""Assegna la PARTY (A/B) ai record selezionati:
- cluster di dimensione 2 (coppie duplicate): un record in A, uno in B, per
  esercitare sempre il linkage cross-party (lo scenario di interesse per il
  PPRL multi-party);
- singleton: A/B casuale bilanciato;
- cluster di dimensione > 2 (emergono per transitivita' nel DPL reale):
  round-robin A/B/A/B... sull'ordine shuffle, cosi' entrambe le party hanno
  sempre almeno un rappresentante del cluster.
"""
from __future__ import annotations

import numpy as np


def assign_parties(selected_clusters: dict[str, list[int]], seed: int) -> dict[int, str]:
    rng = np.random.default_rng(seed)
    party_of: dict[int, str] = {}
    count_a = count_b = 0

    for gid in sorted(selected_clusters.keys()):
        members = list(selected_clusters[gid])
        rng.shuffle(members)

        if len(members) == 1:
            party = "A" if rng.random() < 0.5 else "B"
            party_of[members[0]] = party
            if party == "A":
                count_a += 1
            else:
                count_b += 1
            continue

        for i, rid in enumerate(members):
            party = "A" if i % 2 == 0 else "B"
            party_of[rid] = party
            if party == "A":
                count_a += 1
            else:
                count_b += 1

    total = count_a + count_b
    if total and abs(count_a - count_b) / total > 0.10:
        print(
            f"WARNING: sbilanciamento party oltre soglia: A={count_a} B={count_b} "
            f"(totale {total}) — non corretto forzatamente per non rompere il "
            "campionamento basato sui cluster."
        )

    return party_of
