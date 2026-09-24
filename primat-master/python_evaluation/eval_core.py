#!/usr/bin/env python3
"""Logica di valutazione condivisa tra le due pipeline di evaluation offline.

Estratta da evaluate_mcl.py ed evaluate_mscd_ap.py, che avevano le stesse
identiche funzioni `evaluate()`/`record_label()` duplicate: MCL (sorgente
CSV di debug) e MSCD-AP (sorgente Postgres persistita) producono lo stesso
schema logico di record valutabile — {party, record_id, cluster_id,
global_id} — quindi la metrica pairwise cross-party e' identica a valle,
cambia solo il modo in cui i record vengono caricati.

Questo modulo contiene SOLO codice puro (nessuna I/O di rete/terminale se
non la lettura del CSV/DB stesso), cosi' da poter essere importato sia dagli
script CLI esistenti sia dalla GUI Streamlit (evaluation_app.py) senza
side-effect su stdout/argv.
"""

import csv
from collections import defaultdict
from itertools import combinations


def load_from_csv(path):
    """Carica la linking table dal CSV di debug scritto da
    LinkageUnitOrchestrator.writeMclDebugFile (formato:
    cluster_id,party,record_id,global_id,party_dirty).

    `party_dirty` (true/false) e' la negazione di Party.isDuplicateFree() lato
    Java: decide se le coppie within-party di quella party contano (stessa
    regola per-party della LU). Un CSV storico senza la colonna viene letto
    come "nessuna party dirty" (solo coppie cross-party) con un avviso.

    Ritorna una lista di dict {cluster_id, party, record_id, global_id,
    party_dirty}.
    """
    records = []
    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        has_dirty = "party_dirty" in (reader.fieldnames or [])
        if not has_dirty:
            print("ATTENZIONE: {} non ha la colonna party_dirty (CSV scritto da una "
                  "versione precedente della LU): le coppie within-party non "
                  "vengono contate, le metriche possono non coincidere con quelle "
                  "della LU.".format(path))
        for row in reader:
            records.append({
                "cluster_id": int(row["cluster_id"]),
                "party": row["party"],
                "record_id": row["record_id"],
                "global_id": row["global_id"],
                "party_dirty": has_dirty and row["party_dirty"].strip().lower() == "true",
            })
    return records


def _derive_party(record_id, party_names_by_length_desc):
    """Ricava il party di un record dal prefisso del suo id.

    Necessario solo per la sorgente Postgres: record.party e' @Transient
    lato Java (vedi DATABASE_SCHEMA.md), quindi la colonna non esiste sul
    DB e va ricostruita euristicamente. L'ordine per lunghezza decrescente
    evita match ambigui quando un nome party e' prefisso di un altro
    (es. "A" prefisso di "AB").
    """
    for name in party_names_by_length_desc:
        if record_id.startswith(name):
            return name
    return "UNKNOWN"


def load_from_postgres(host, port, dbname, user, password):
    """Si connette a Postgres e ricostruisce la linking table corrente
    dalle tabelle record/party (stesso schema letto da evaluate_mscd_ap.py).

    Import di psycopg2 fatto qui dentro (non a livello modulo) cosi' che
    eval_core resti importabile anche in ambienti senza il driver DB
    installato, purche' si usi solo il path CSV (es. GUI in modalita' MCL).

    Ritorna una lista di dict {cluster_id, party, record_id, global_id},
    con cluster_id/global_id eventualmente None (record non ancora
    clusterizzato / senza ground truth), coerentemente con lo schema DB.
    """
    import psycopg2

    conn = psycopg2.connect(host=host, port=port, dbname=dbname,
                             user=user, password=password)
    try:
        with conn.cursor() as cur:
            try:
                cur.execute("SELECT name, duplicatefree FROM party")
            except psycopg2.errors.UndefinedTable:
                # Lo schema di questo DB viene creato da Hibernate
                # (hbm2ddl.auto=update) solo al primo avvio della Linkage
                # Unit Service con la strategia di persistenza corrispondente
                # (vedi CLAUDE.md, "3 Postgres distinti"): tabella assente
                # significa che quel run non e' mai stato eseguito, non un
                # errore di connessione. Messaggio esplicito al posto
                # dell'eccezione psycopg2 grezza che risalirebbe fino alla
                # messagebox di evaluation_app.py.
                raise RuntimeError(
                    "Il database '{}' non ha ancora lo schema: nessun run e' "
                    "mai stato eseguito con questa strategia di persistenza "
                    "(le tabelle vengono create automaticamente al primo "
                    "avvio della Linkage Unit Service configurata per questa "
                    "strategia).".format(dbname))
            party_rows = cur.fetchall()
            party_names = [row[0] for row in party_rows]
            # duplicatefree=false (NULL trattato come clean) => party dirty.
            dirty_by_party = {row[0]: row[1] is False for row in party_rows}

            cur.execute("SELECT id, cluster_id, gid FROM record")
            rows = cur.fetchall()
    finally:
        conn.close()

    # Piu' lungo prima: vedi _derive_party per il perche'.
    party_names_by_length_desc = sorted(party_names, key=len, reverse=True)

    records = []
    for record_id, cluster_id, gid in rows:
        party = _derive_party(record_id, party_names_by_length_desc)
        if party == "UNKNOWN":
            print("ATTENZIONE: nessun party noto e' prefisso del record id '{}': "
                  "assegnato a UNKNOWN.".format(record_id))
        records.append({
            "record_id": record_id,
            "cluster_id": cluster_id,
            "global_id": gid,
            "party": party,
            "party_dirty": dirty_by_party.get(party, False),
        })
    return records


def record_label(rec):
    """Identificatore univoco cross-party di un record, usato come chiave
    nei set di coppie (party+record_id da solo non basta a distinguere
    record con lo stesso id locale su sorgenti diverse)."""
    return "{}:{}".format(rec["party"], rec["record_id"])


def evaluate(records):
    """Calcola le metriche di qualita' del linkage rispetto al ground
    truth (GLOBAL_ID) con la STESSA regola per-party della LU
    (MultiSourceLinkage.countGroundTruthMatches/countsForOutcome/
    PerformanceMetrics.getMaxComparisons con ComparisonStrategy.DIRTY_AWARE):

    - una coppia cross-party conta sempre;
    - una coppia within-party conta solo se quella party e' dirty
      (record["party_dirty"], assente = clean).

    Ground truth e predizione (tutte le coppie di record nello stesso
    cluster) usano la stessa regola, cosi' TP+FN = GT e
    TP+FP+TN+FN = max_comparisons come nella riga "Linkage:" della LU.

    Nota: i record con cluster_id None (MSCD-AP, mai riconciliati su DB)
    non generano coppie predette (non sono un cluster); restano nel GT e
    nel numero di confronti massimi.
    """
    by_cluster = defaultdict(list)
    by_global_id = defaultdict(list)
    dirty_parties = set()
    count_by_party = defaultdict(int)
    for rec in records:
        by_cluster[rec["cluster_id"]].append(rec)
        count_by_party[rec["party"]] += 1
        if rec.get("party_dirty"):
            dirty_parties.add(rec["party"])
        if rec["global_id"]:
            by_global_id[rec["global_id"]].append(rec)

    def counts(a, b):
        return a["party"] != b["party"] or a["party"] in dirty_parties

    # Ground truth (formula chiusa per GLOBAL_ID, come countGroundTruthMatches):
    # prodotto tra party distinte + C(n,2) per ogni party dirty del gruppo.
    gt = 0
    for gid, members in by_global_id.items():
        per_party = defaultdict(int)
        for m in members:
            per_party[m["party"]] += 1
        counts_list = list(per_party.items())
        for i, (party_i, n_i) in enumerate(counts_list):
            if party_i in dirty_parties:
                gt += n_i * (n_i - 1) // 2
            for _party_j, n_j in counts_list[i + 1:]:
                gt += n_i * n_j

    # Predetto: ogni coppia di record nello stesso cluster (un record sta in
    # un solo cluster, quindi nessuna coppia e' contata due volte).
    tp = fp = 0
    for cid, members in by_cluster.items():
        if cid is None:
            continue
        for a, b in combinations(members, 2):
            if not counts(a, b):
                continue
            if a["global_id"] and a["global_id"] == b["global_id"]:
                tp += 1
            else:
                fp += 1
    fn = gt - tp

    # Confronti massimi: come PerformanceMetrics.getMaxComparisons con
    # DIRTY_AWARE (cross-party + within-party per le sole party dirty).
    party_counts = list(count_by_party.items())
    max_comparisons = 0
    for i, (party_i, n_i) in enumerate(party_counts):
        if party_i in dirty_parties:
            max_comparisons += n_i * (n_i - 1) // 2
        for _party_j, n_j in party_counts[i + 1:]:
            max_comparisons += n_i * n_j
    tn = max_comparisons - tp - fp - fn

    recall = tp / (tp + fn) if (tp + fn) else float("nan")
    precision = tp / (tp + fp) if (tp + fp) else float("nan")
    f1 = (2 * precision * recall / (precision + recall)
          if (precision + recall) and precision == precision and recall == recall
          else float("nan"))

    # Entita' (GLOBAL_ID multi-party, o con duplicati in una party dirty) i cui record sono finiti in cluster
    # diversi: link mancati (false negative a livello di entita').
    split_entities = []
    for gid, members in by_global_id.items():
        parties = {m["party"] for m in members}
        has_dirty_duplicates = any(
            p in dirty_parties and sum(1 for m in members if m["party"] == p) > 1
            for p in parties)
        if len(parties) < 2 and not has_dirty_duplicates:
            continue
        clusters_used = {m["cluster_id"] for m in members}
        if len(clusters_used) > 1:
            split_entities.append((gid, members, clusters_used))

    # Cluster che mescolano GLOBAL_ID diversi: link scorretti (merge errato).
    mixed_clusters = []
    for cid, members in by_cluster.items():
        gids = {m["global_id"] for m in members if m["global_id"]}
        if len(gids) > 1:
            mixed_clusters.append((cid, members, gids))

    # Record rimasti soli (cluster singleton): non linkati a nessuno.
    # cluster_id None (MSCD-AP non riconciliato) e' gestito a parte da
    # chi chiama, non e' un vero "singleton" nel senso di questa metrica.
    singleton_records = [
        members[0] for cid, members in by_cluster.items()
        if cid is not None and len(members) == 1
    ]

    # Record senza cluster_id: significativo solo per MSCD-AP (persistito
    # su DB), sempre vuoto per MCL (ogni run riclusterizza tutto da zero).
    unclustered_records = list(by_cluster.get(None, []))

    return {
        "tp": tp, "fp": fp, "fn": fn, "tn": tn, "gt": gt,
        "max_comparisons": max_comparisons,
        "recall": recall, "precision": precision, "f1": f1,
        "n_records": len(records),
        "n_clusters": len([c for c in by_cluster if c is not None]),
        "split_entities": split_entities,
        "mixed_clusters": mixed_clusters,
        "singleton_records": singleton_records,
        "unclustered_records": unclustered_records,
    }


def print_report(result, title):
    """Stampa a terminale lo stesso report testuale gia' prodotto dai due
    script CLI, riusato identico da entrambi per non alterarne l'output."""
    print("=== {} - valutazione rispetto al ground truth (GLOBAL_ID) ===".format(title))
    print("record totali: {}  cluster totali: {}".format(result["n_records"], result["n_clusters"]))
    print("Linkage:   TP {} | FP {} | TN {} | FN {} | GT {} | confronti max {}".format(
        result["tp"], result["fp"], result["tn"], result["fn"], result["gt"], result["max_comparisons"]))
    if result["tp"] + result["fp"] + result["tn"] + result["fn"] != result["max_comparisons"]:
        print("ATTENZIONE: TP+FP+TN+FN != confronti max (incoerenza nei dati).")
    print("recall={:.3f}  precision={:.3f}  F-measure={:.3f}".format(
        result["recall"], result["precision"], result["f1"]))

    print("\n--- Entita' con record finiti in cluster diversi (link mancati): {} ---"
          .format(len(result["split_entities"])))
    for gid, members, clusters_used in result["split_entities"]:
        member_desc = ", ".join("{} (cluster {})".format(record_label(m), m["cluster_id"]) for m in members)
        print("  GLOBAL_ID={}: {}".format(gid, member_desc))

    print("\n--- Cluster che mescolano GLOBAL_ID diversi (link scorretti): {} ---"
          .format(len(result["mixed_clusters"])))
    for cid, members, gids in result["mixed_clusters"]:
        member_desc = ", ".join("{} (GLOBAL_ID={})".format(record_label(m), m["global_id"]) for m in members)
        print("  cluster {}: {}".format(cid, member_desc))

    print("\n--- Record rimasti senza cluster (singleton, non linkati a nessuno): {} ---"
          .format(len(result["singleton_records"])))
    for rec in result["singleton_records"]:
        print("  {} (GLOBAL_ID={}, cluster {})".format(record_label(rec), rec["global_id"], rec["cluster_id"]))

    if result["unclustered_records"]:
        print("\n--- Record con cluster_id IS NULL (mai riconciliati su DB): {} ---"
              .format(len(result["unclustered_records"])))
        for rec in result["unclustered_records"]:
            print("  {} (GLOBAL_ID={})".format(record_label(rec), rec["global_id"]))
