"""Scarica il benchmark DBLP-Scholar (Database Group Leipzig / Köpcke, Thor,
Rahm — VLDB 2010) ed estrae i 3 CSV: DBLP1.csv, Scholar.csv,
DBLP-Scholar_perfectMapping.csv.
"""
from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path

import requests

URL = "https://dbs.uni-leipzig.de/files/datasets/DBLP-Scholar.zip"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)
EXPECTED_FILES = ("DBLP1.csv", "Scholar.csv", "DBLP-Scholar_perfectMapping.csv")


def _manual_instructions(raw_dir: Path) -> str:
    return (
        "\nDownload automatico non riuscito. Procedura manuale:\n"
        f"  1. apri: {URL}\n"
        f"  2. estrai lo zip in: {raw_dir}\n"
        "     (deve contenere: " + ", ".join(EXPECTED_FILES) + ")\n"
        "  3. rilancia questo script: troverà i file già presenti.\n"
    )


def ensure_raw_files(raw_dir: Path, force: bool = False) -> dict[str, Path]:
    raw_dir.mkdir(parents=True, exist_ok=True)
    existing = {name: raw_dir / name for name in EXPECTED_FILES}

    if not force and all(p.exists() and p.stat().st_size > 0 for p in existing.values()):
        print(f"I 3 file sono già presenti in {raw_dir}, riuso quelli esistenti.")
        return existing

    zip_path = raw_dir / "DBLP-Scholar.zip"
    print(f"Scarico {URL} ...")
    try:
        resp = requests.get(URL, headers={"User-Agent": USER_AGENT}, timeout=120, stream=True)
        resp.raise_for_status()
        with open(zip_path, "wb") as fh:
            for chunk in resp.iter_content(chunk_size=1 << 16):
                if chunk:
                    fh.write(chunk)
        with zipfile.ZipFile(zip_path) as zf:
            zf.extractall(raw_dir)
    except Exception as exc:
        print(f"Download/estrazione falliti: {exc}", file=sys.stderr)
        print(_manual_instructions(raw_dir), file=sys.stderr)
        sys.exit(1)

    missing = [name for name, p in existing.items() if not p.exists()]
    if missing:
        print(f"Mancano dopo l'estrazione: {missing}", file=sys.stderr)
        print(_manual_instructions(raw_dir), file=sys.stderr)
        sys.exit(1)

    for name, p in existing.items():
        print(f"  -> {p} ({p.stat().st_size} bytes)")
    return existing


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--raw-dir", default="dblp_scholar/raw")
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()
    ensure_raw_files(Path(args.raw_dir), force=args.force)


if __name__ == "__main__":
    main()
