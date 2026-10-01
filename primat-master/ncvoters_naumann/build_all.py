"""Orchestratore: scarica (se serve), verifica il mapping, costruisce il ground
truth, campiona in modo bilanciato per ogni dimensione richiesta, assegna le
party e scrive CSV + JSON Data Owner + SCHEMA.md pronti per PRIMAT.

Uso tipico:
    python ncvoters_naumann/inspect_schema.py            # genera/rivedi column_mapping.json
    # -> rivedi ncvoters_naumann/column_mapping.json, imposta "_verified": true
    python ncvoters_naumann/build_all.py --sizes 500,1000,2000,5000 --include-full
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")

import build_ground_truth as gt
import sample_balanced as sb
import assign_parties as ap
import write_primat_datasets as wp
from download_ncvoters import ensure_raw_files


def parse_sizes(text: str) -> list[int]:
    return [int(s.strip()) for s in text.split(",") if s.strip()]


def load_mapping(mapping_path: Path) -> dict:
    if not mapping_path.exists():
        print(
            f"Non trovo {mapping_path}. Lancia prima: python ncvoters_naumann/inspect_schema.py",
            file=sys.stderr,
        )
        sys.exit(1)
    mapping = json.loads(mapping_path.read_text(encoding="utf-8"))
    if not mapping.get("_verified"):
        print(
            f"{mapping_path} non è stato verificato (\"_verified\": false). "
            "Rivedilo e imposta _verified=true prima di procedere.",
            file=sys.stderr,
        )
        sys.exit(1)
    if not mapping.get("record_id_column"):
        print(f"{mapping_path}: record_id_column mancante.", file=sys.stderr)
        sys.exit(1)
    if not mapping.get("qid_columns"):
        print(f"{mapping_path}: qid_columns vuoto.", file=sys.stderr)
        sys.exit(1)
    return mapping


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--raw-dir", default="ncvoters_naumann/raw")
    parser.add_argument("--mapping", default="ncvoters_naumann/column_mapping.json")
    parser.add_argument("--sizes", default="500,1000,2000,5000")
    parser.add_argument("--include-full", action="store_true")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--examples-out", default="primat-examples/src/main/resources/ncvoters_naumann")
    parser.add_argument("--dataowner-out", default="primat-data-owner-service/src/main/resources/config/ncvoters_naumann")
    parser.add_argument("--force-download", action="store_true")
    args = parser.parse_args()

    raw_dir = Path(args.raw_dir)
    ensure_raw_files(raw_dir, force=args.force_download)

    mapping = load_mapping(Path(args.mapping))
    record_id_column = mapping["record_id_column"]
    qid_columns = mapping["qid_columns"]
    numeric_qid_columns = set(mapping.get("numeric_qid_columns", []))

    ncvoters_path = raw_dir / mapping["ncvoters_file"]
    dpl_path = raw_dir / mapping["dpl_file"]

    print("Costruisco il ground truth (union-find sulle coppie DPL)...")
    global_id_of, all_clusters, original_stats = gt.build(ncvoters_path, dpl_path, record_id_column)
    print(f"  cluster totali: {original_stats['total_clusters']}, "
          f"record totali: {original_stats['total_records']}, "
          f"rapporto duplicati: {original_stats['duplicate_ratio']:.4f}")

    qid_table = wp.load_qid_table(ncvoters_path, record_id_column, qid_columns)

    sizes = parse_sizes(args.sizes)
    if args.include_full:
        sizes.append(original_stats["total_records"])

    examples_out = Path(args.examples_out)
    dataowner_out = Path(args.dataowner_out)

    summary_rows = []
    for size in sizes:
        label = "full" if size >= original_stats["total_records"] else str(size)
        print(f"\n=== Dimensione richiesta: {label} ===")

        selected = sb.sample_target_size(all_clusters, size, seed=args.seed)
        sampled_stats = gt.ground_truth_stats(selected)
        report = sb.sampling_report(original_stats, sampled_stats)

        party_of = ap.assign_parties(selected, seed=args.seed)
        rows = wp.build_rows(selected, party_of, qid_table, qid_columns)
        cross_party_matches = wp.count_cross_party_true_matches(rows)

        size_dir = examples_out / label
        combined_csv = size_dir / f"ncvoters_{label}.csv"
        party_a_csv = size_dir / "party_A.csv"
        party_b_csv = size_dir / "party_B.csv"
        schema_md = size_dir / "SCHEMA.md"

        wp.write_csv_no_header(rows, qid_columns, combined_csv, party=None)
        wp.write_csv_no_header(rows, qid_columns, party_a_csv, party="A")
        wp.write_csv_no_header(rows, qid_columns, party_b_csv, party="B")
        wp.write_schema_doc(schema_md, qid_columns, numeric_qid_columns, label, report, cross_party_matches)

        do_dir = dataowner_out / label
        wp.write_data_owner_json(3, "A", str(party_a_csv.as_posix()), qid_columns, numeric_qid_columns, do_dir / "party_A.json")
        wp.write_data_owner_json(3, "B", str(party_b_csv.as_posix()), qid_columns, numeric_qid_columns, do_dir / "party_B.json")

        print(f"  record campionati: {sampled_stats['total_records']} (cluster: {sampled_stats['total_clusters']})")
        print(f"  rapporto duplicati: originale {report['original_duplicate_ratio']:.4f} "
              f"vs campione {report['sampled_duplicate_ratio']:.4f}")
        print(f"  coppie cross-party attese (gold matches): {cross_party_matches}")
        print(f"  scritto: {combined_csv}, {party_a_csv}, {party_b_csv}, {schema_md}")
        print(f"  scritto: {do_dir / 'party_A.json'}, {do_dir / 'party_B.json'}")

        summary_rows.append((label, sampled_stats["total_records"],
                              report["original_duplicate_ratio"], report["sampled_duplicate_ratio"],
                              cross_party_matches))

    print("\n=== Riepilogo ===")
    print(f"{'size':>8} {'record':>8} {'ratio_orig':>11} {'ratio_sample':>13} {'gold_matches':>13}")
    for label, n, r_orig, r_sample, matches in summary_rows:
        print(f"{label:>8} {n:>8} {r_orig:>11.4f} {r_sample:>13.4f} {matches:>13}")


if __name__ == "__main__":
    main()
