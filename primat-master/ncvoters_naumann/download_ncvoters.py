"""Scarica il dataset reale NC Voters dalla pagina di Naumann (HPI).

Tenta prima un download HTTP diretto (scoprendo gli URL reali dagli <a href>
della pagina); se il sito blocca la richiesta (403, redirect a pagina generica,
ecc.) stampa istruzioni di download manuale chiare e termina: non genera mai
un dataset fittizio al posto di quello reale.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from urllib.parse import urljoin

import requests

NAUMANN_PAGE = "https://hpi.de/naumann/projects/repeatability/datasets/ncvoters-dataset.html"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)
EXPECTED_FILES = ("ncvoters.tsv", "ncvoters_DPL.tsv", "ncvoters_NDPL.tsv")


class DownloadDiscoveryError(RuntimeError):
    pass


def discover_download_links(session: requests.Session, page_url: str) -> dict[str, str]:
    """Scarica la pagina HTML e trova gli <a href="..."> che puntano ai 3 file attesi."""
    resp = session.get(page_url, headers={"User-Agent": USER_AGENT}, timeout=30)
    resp.raise_for_status()
    html = resp.text

    links: dict[str, str] = {}
    for name in EXPECTED_FILES:
        match = re.search(r'href="([^"]*' + re.escape(name) + r')"', html)
        if match:
            links[name] = urljoin(page_url, match.group(1))

    missing = [f for f in EXPECTED_FILES if f not in links]
    if missing:
        raise DownloadDiscoveryError(
            f"Non ho trovato i link di download per: {', '.join(missing)} "
            f"nella pagina {page_url}"
        )
    return links


def download_file(url: str, dest: Path, session: requests.Session, timeout: int = 120) -> None:
    tmp = dest.with_suffix(dest.suffix + ".tmp")
    with session.get(url, headers={"User-Agent": USER_AGENT}, timeout=timeout, stream=True) as resp:
        resp.raise_for_status()
        with open(tmp, "wb") as fh:
            for chunk in resp.iter_content(chunk_size=1 << 16):
                if chunk:
                    fh.write(chunk)
    tmp.replace(dest)


def _manual_instructions(raw_dir: Path) -> str:
    return (
        "\nDownload automatico non riuscito. Procedura manuale:\n"
        f"  1. apri nel browser: {NAUMANN_PAGE}\n"
        "  2. scarica i 3 file collegati a 'ncvoters.tsv' (14,183 objects),\n"
        "     'ncvoters_DPL.tsv' (9,819 objects), 'ncvoters_NDPL.tsv' (98,142 objects)\n"
        f"  3. copiali in: {raw_dir}\n"
        "     con questi nomi esatti: " + ", ".join(EXPECTED_FILES) + "\n"
        "  4. rilancia questo script: li troverà già presenti e non riproverà il download.\n"
    )


def ensure_raw_files(raw_dir: Path, force: bool = False) -> dict[str, Path]:
    raw_dir.mkdir(parents=True, exist_ok=True)
    existing = {name: raw_dir / name for name in EXPECTED_FILES}

    if not force and all(p.exists() and p.stat().st_size > 0 for p in existing.values()):
        print(f"I 3 file sono già presenti in {raw_dir}, riuso quelli esistenti.")
        return existing

    session = requests.Session()
    try:
        links = discover_download_links(session, NAUMANN_PAGE)
    except Exception as exc:  # qualunque fallimento di rete/parsing
        print(f"Impossibile scoprire i link di download: {exc}", file=sys.stderr)
        print(_manual_instructions(raw_dir), file=sys.stderr)
        sys.exit(1)

    for name, url in links.items():
        dest = raw_dir / name
        print(f"Scarico {name} da {url} ...")
        try:
            download_file(url, dest, session)
        except Exception as exc:
            print(f"Download di {name} fallito: {exc}", file=sys.stderr)
            print(_manual_instructions(raw_dir), file=sys.stderr)
            sys.exit(1)
        print(f"  -> {dest} ({dest.stat().st_size} bytes)")

    return existing


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--raw-dir", default="ncvoters_naumann/raw")
    parser.add_argument("--force", action="store_true", help="Riscarica anche se i file esistono già")
    args = parser.parse_args()

    ensure_raw_files(Path(args.raw_dir), force=args.force)


if __name__ == "__main__":
    main()
