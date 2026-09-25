"""Disegna l'istogramma delle similarita' scritto dalla LU con `debug: true`,
colorato per ground truth (match vero / non-match).

Un solo istogramma per contesto (le coppie confrontate sono quelle decise dal
contesto clean/dirty, con una sola soglia), in tre pannelli:
  - in alto: entrambe le classi su [0,1] (mostra le code e la sovrapposizione);
  - in basso a sinistra: solo i match veri, zoom da 0.4;
  - in basso a destra: solo i non-match, zoom da 0.4.
Tutti e tre con l'asse Y in scala logaritmica (i bin a 0 non compaiono).

Uso (da qualunque directory; di default CSV e PNG stanno accanto a questo script):
    python python_evaluation/plot_similarity_histogram.py [csv] [soglia] [png]
"""
import csv
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt

HERE = Path(__file__).resolve().parent

TRUTH_LABELS = {"non_match": "non-match", "match": "match vero"}
# Ordine fisso degli slot categorici (blu, arancio): validati con CVD dE >= 24, contrasto >= 3:1.
SURFACE = "#fcfcfb"
INK = "#0b0b0b"
INK_SECONDARY = "#52514e"
COLORS = {"non_match": "#2a78d6", "match": "#eb6834"}
GRID = "#e4e3df"


def load(path):
    data = defaultdict(list)
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            data[row["truth"]].append((float(row["bin_low"]), float(row["bin_high"]), int(row["count"])))
    return data


ZOOM_FROM = 0.4


def draw(ax, bins_by_truth, truths, log, x_from, threshold):
    for truth in truths:
        bins = bins_by_truth.get(truth, [])
        total = sum(b[2] for b in bins)
        ax.bar([b[0] for b in bins], [b[2] for b in bins], width=[b[1] - b[0] for b in bins],
               align="edge", color=COLORS[truth], edgecolor=SURFACE, linewidth=0.4, alpha=0.9,
               label=f"{TRUTH_LABELS[truth]} ({total:,})".replace(",", "."))
    if threshold is not None:
        ax.axvline(threshold, color=INK_SECONDARY, linestyle="--", linewidth=1.2,
                   label=f"soglia config {threshold:g}")
    visible_max = max((b[2] for t in truths for b in bins_by_truth.get(t, []) if b[0] >= x_from), default=1)
    if log:
        ax.set_yscale("log")
        ax.set_ylim(0.6, max(visible_max, 1) * 12)  # spazio sopra i dati per la legenda
    else:
        ax.set_ylim(0, max(visible_max, 1) * 1.4)
    ax.set_xlim(x_from, 1)
    ax.set_xlabel("similarita' Jaccard", color=INK_SECONDARY)
    ax.set_ylabel("coppie (scala log)" if log else "coppie (scala lineare)", color=INK_SECONDARY)
    ax.set_axisbelow(True)
    ax.grid(axis="y", color=GRID, linewidth=0.6)
    for side in ("top", "right"):
        ax.spines[side].set_visible(False)
    for side in ("left", "bottom"):
        ax.spines[side].set_color(GRID)
    ax.tick_params(colors=INK_SECONDARY)
    ax.set_facecolor(SURFACE)
    # legenda dove c'e' spazio: i match veri si concentrano verso 1.0, gli altri a sinistra
    ax.legend(frameon=False, labelcolor=INK, loc="upper left" if truths == ("match",) else "upper right")


def main():
    csv_path = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "similarity_histogram.csv"
    threshold = float(sys.argv[2]) if len(sys.argv) > 2 else None
    out_path = Path(sys.argv[3]) if len(sys.argv) > 3 else HERE / "similarity_histogram.png"

    data = load(csv_path)
    if sum(b[2] for bins in data.values() for b in bins) == 0:
        sys.exit("Nessuna coppia nell'istogramma: " + str(csv_path))

    fig = plt.figure(figsize=(12, 7.6))
    fig.patch.set_facecolor(SURFACE)
    grid = fig.add_gridspec(2, 2)
    panels = (
        (grid[0, :], ("non_match", "match"), True, 0.0, "match e non-match, scala log"),
        (grid[1, 0], ("match",), True, ZOOM_FROM, f"solo match veri, zoom >= {ZOOM_FROM:g}, scala log"),
        (grid[1, 1], ("non_match",), True, ZOOM_FROM, f"solo non-match, zoom >= {ZOOM_FROM:g}, scala log"),
    )
    for spec, truths, log, x_from, title in panels:
        ax = fig.add_subplot(spec)
        draw(ax, data, truths, log, x_from, threshold)
        ax.set_title(title, color=INK, loc="left", fontsize=11)
    fig.tight_layout()
    fig.savefig(out_path, dpi=150, facecolor=SURFACE)
    print("Scritto", out_path)


if __name__ == "__main__":
    main()
