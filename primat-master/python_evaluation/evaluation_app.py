#!/usr/bin/env python3
"""GUI desktop nativa (Tkinter) per la valutazione offline del record
linkage (MCL, non persistente, + le 4 strategie persistenti su Postgres:
MSCD-AP, Center Clustering, Global Greedy, CLIP — vedi
CLAUDE.md, 2026-09-16/2026-09-17).

Sostituisce l'ispezione manuale dell'output testuale di evaluate_mcl.py /
evaluate_mscd_ap.py / evaluate_all.py con una finestra unica interattiva:
metriche di qualita' (recall, precision, F1, matrice di confusione
pairwise), le stesse sezioni di dettaglio gia' presenti negli script CLI
(entita' split, cluster misti, singleton, non-clusterizzati) e una vista
esplorabile della linking table (cluster_id, GID, record_id, party) con
filtri.

Tkinter e' nella libreria standard di Python (nessuna dipendenza pip da
installare/servire) e produce un eseguibile a finestra singola: niente
server locale ne' browser da aprire manualmente, a differenza di una GUI
web-based — scelta fatta apposta per l'uso desktop diretto di questo tool.

Non duplica alcuna logica di caricamento/valutazione: si appoggia
interamente a eval_core.py, lo stesso modulo importato dai due script CLI,
cosi' che i numeri mostrati qui siano garantiti identici a quelli stampati
da `python evaluate_mcl.py` / `python evaluate_mscd_ap.py`.

Avvio:
    python evaluation_app.py
"""

import tkinter as tk
from tkinter import filedialog, messagebox, ttk

from matplotlib import colormaps
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from matplotlib.figure import Figure

from eval_core import evaluate, load_from_csv, load_from_postgres, record_label

# Sorgente Postgres -> (dbname di default, etichetta per titoli/report),
# stessi default di persistence.xml/evaluate_mscd_ap.py.STRATEGIES. La voce
# "CSV (MCL)" non e' qui: e' l'unica sorgente non-Postgres, gestita a parte.
DB_SOURCES = {
    "Database (MSCD-AP)": ("primat_mscd_ap", "MSCD-AP"),
    "Database (Center Clustering)": ("primat_center_clustering", "Center Clustering"),
    "Database (Global Greedy)": ("primat_global_greedy", "Global Greedy"),
    "Database (CLIP)": ("primat_clip", "CLIP"),
}

# Palette qualitativa ciclica usata per colorare le righe della linking
# table per cluster_id (vedi _refresh_linking_tab): 20 colori distinti sono
# piu' che sufficienti per riconoscere a colpo d'occhio i cluster in una
# singola schermata, oltre si ricicla.
PALETTE = colormaps["tab20"].colors

# Palette dell'interfaccia: neutra e a basso contrasto, per un aspetto
# discreto invece dei rilievi "groove" 3D di default di Tk.
BG = "#f4f5f7"
CARD_BG = "#ffffff"
BORDER = "#dfe2e8"
TEXT_DARK = "#1f2430"
TEXT_MUTED = "#6b7280"


def _fmt_ratio(value):
    """Formatta una metrica in [0,1], gestendo il caso NaN (denominatore
    nullo, es. nessuna coppia di ground truth) senza propagare 'nan' a UI."""
    return "N/A" if value != value else "{:.3f}".format(value)


class EvaluationApp(tk.Tk):
    """Finestra principale: barra di selezione sorgente in alto, tre tab
    sotto (Metriche / Dettagli / Linking table) popolate dallo stesso
    `result = evaluate(records)` dopo ogni caricamento."""

    def __init__(self):
        super().__init__()
        self.title("PRIMAT — Valutazione offline del record linkage")
        self.geometry("1150x780")
        self.minsize(900, 600)
        self.configure(bg=BG)

        # WM_DELETE_WINDOW non e' gestito da un handler di default: senza
        # un protocol esplicito la X della finestra non garantisce che
        # mainloop() ritorni, lasciando il processo Python vivo in background
        # (visibile solo da un Ctrl+C nel terminale che l'ha lanciato).
        self.protocol("WM_DELETE_WINDOW", self._on_close)

        self._setup_style()

        self.records = None
        self.result = None
        self._link_rows = []
        self._cluster_colors = {}

        self._build_source_bar()
        self._build_notebook()

    def _on_close(self):
        self.quit()
        self.destroy()

    def _setup_style(self):
        """Tema ttk unificato: 'clam' e' l'unico tema che rende i colori di
        sfondo/bordo impostati qui sotto (le varianti native di Windows
        ignorano in gran parte queste opzioni)."""
        style = ttk.Style(self)
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass

        style.configure("TFrame", background=BG)
        style.configure("TLabel", background=BG, foreground=TEXT_DARK, font=("Segoe UI", 10))
        style.configure("TButton", font=("Segoe UI", 9), padding=6)
        style.configure("TCombobox", padding=3)

        style.configure("Card.TFrame", background=CARD_BG, relief="solid", borderwidth=1)
        style.configure("CardInner.TFrame", background=CARD_BG)
        style.configure("CardLabel.TLabel", background=CARD_BG, foreground=TEXT_MUTED,
                         font=("Segoe UI", 9))
        style.configure("CardText.TLabel", background=CARD_BG, foreground=TEXT_DARK,
                         font=("Segoe UI", 9))
        style.configure("CardValue.TLabel", background=CARD_BG, foreground=TEXT_DARK,
                         font=("Segoe UI", 16, "bold"))
        style.configure("Caption.TLabel", background=BG, foreground=TEXT_MUTED, font=("Segoe UI", 9))
        style.configure("SectionTitle.TLabel", background=BG, foreground=TEXT_DARK,
                         font=("Segoe UI", 11, "bold"))

        style.configure("TNotebook", background=BG, borderwidth=0)
        style.configure("TNotebook.Tab", padding=(14, 7), font=("Segoe UI", 9))

        style.configure("Treeview", font=("Segoe UI", 9), rowheight=24,
                         background=CARD_BG, fieldbackground=CARD_BG, bordercolor=BORDER)
        style.configure("Treeview.Heading", font=("Segoe UI", 9, "bold"))

    # ------------------------------------------------------------------
    # Barra superiore: selezione sorgente dati. Le due strategie hanno
    # storage radicalmente diversi (CSV di debug per MCL, mai persistito;
    # tabelle Postgres per MSCD-AP, persistite tra run — vedi CLAUDE.md),
    # quindi mostrano campi di input differenti a seconda della tendina.
    # ------------------------------------------------------------------
    def _build_source_bar(self):
        outer = ttk.Frame(self, padding=(16, 14, 16, 10))
        outer.pack(side=tk.TOP, fill=tk.X)

        bar = ttk.Frame(outer, style="Card.TFrame", padding=14)
        bar.pack(fill=tk.X)

        ttk.Label(bar, text="Sorgente:", style="CardLabel.TLabel").grid(row=0, column=0, sticky="w")
        self.source_var = tk.StringVar(value="CSV (MCL)")
        source_combo = ttk.Combobox(
            bar, textvariable=self.source_var, state="readonly", width=26,
            values=["CSV (MCL)"] + list(DB_SOURCES),
        )
        source_combo.grid(row=0, column=1, padx=(4, 16))
        source_combo.bind("<<ComboboxSelected>>", lambda _e: self._sync_source_fields())

        self.csv_frame = ttk.Frame(bar, style="CardInner.TFrame")
        ttk.Label(self.csv_frame, text="Percorso CSV:", style="CardText.TLabel").pack(side=tk.LEFT)
        self.csv_path_var = tk.StringVar(value="mcl_debug_output.csv")
        ttk.Entry(self.csv_frame, textvariable=self.csv_path_var, width=42).pack(side=tk.LEFT, padx=4)
        ttk.Button(self.csv_frame, text="Sfoglia…", command=self._browse_csv).pack(side=tk.LEFT)

        self.db_frame = ttk.Frame(bar, style="CardInner.TFrame")
        self.db_vars = {
            "host": tk.StringVar(value="localhost"),
            "port": tk.StringVar(value="5432"),
            "dbname": tk.StringVar(value="primat"),
            "user": tk.StringVar(value="primat"),
            "password": tk.StringVar(value="primat"),
        }
        db_fields = [
            ("Host", "host", 12, None), ("Port", "port", 6, None),
            ("DB", "dbname", 10, None), ("Utente", "user", 10, None),
            ("Password", "password", 10, "*"),
        ]
        for i, (label, key, width, mask) in enumerate(db_fields):
            ttk.Label(self.db_frame, text=label + ":", style="CardText.TLabel").pack(
                side=tk.LEFT, padx=(10 if i else 0, 2))
            ttk.Entry(self.db_frame, textvariable=self.db_vars[key], width=width, show=mask).pack(side=tk.LEFT)

        ttk.Button(bar, text="Carica ed esegui evaluation", command=self._load_and_evaluate).grid(
            row=0, column=3, padx=(16, 0),
        )

        self.status_var = tk.StringVar(value="Nessun dato caricato.")
        ttk.Label(outer, textvariable=self.status_var, style="Caption.TLabel").pack(
            anchor="w", pady=(8, 0),
        )

        self._sync_source_fields()

    def _sync_source_fields(self):
        self.csv_frame.grid_forget()
        self.db_frame.grid_forget()
        source = self.source_var.get()
        if source == "CSV (MCL)":
            self.csv_frame.grid(row=0, column=2, sticky="w")
        else:
            self.db_frame.grid(row=0, column=2, sticky="w")
            # Le 5 sorgenti Postgres condividono host/porta/credenziali
            # (stesso container — vedi CLAUDE.md, "1 container, 5 database"):
            # solo il nome del DB pre-compilato cambia in base alla strategia.
            default_dbname, _title = DB_SOURCES[source]
            self.db_vars["dbname"].set(default_dbname)

    def _browse_csv(self):
        path = filedialog.askopenfilename(filetypes=[("CSV", "*.csv"), ("Tutti i file", "*.*")])
        if path:
            self.csv_path_var.set(path)

    # ------------------------------------------------------------------
    # Notebook principale: tre tab indipendenti, tutte ripopolate da
    # _load_and_evaluate() dopo ogni caricamento riuscito.
    # ------------------------------------------------------------------
    def _build_notebook(self):
        self.notebook = ttk.Notebook(self)
        self.notebook.pack(fill=tk.BOTH, expand=True, padx=16, pady=(0, 12))

        self.metrics_tab = ttk.Frame(self.notebook)
        self.details_tab = ttk.Notebook(self.notebook)
        self.linking_tab = ttk.Frame(self.notebook)

        self.notebook.add(self.metrics_tab, text="Metriche")
        self.notebook.add(self.details_tab, text="Dettagli")
        self.notebook.add(self.linking_tab, text="Linking table")

        self._build_metrics_tab()
        self._build_details_tab()
        self._build_linking_tab()

    @staticmethod
    def _make_treeview(parent, columns):
        """Treeview con scrollbar verticale, riusata identica per tutte le
        tabelle di dettaglio e per la linking table."""
        tree = ttk.Treeview(parent, columns=columns, show="headings", height=12)
        for col in columns:
            tree.heading(col, text=col)
            tree.column(col, width=150, anchor="w")
        vsb = ttk.Scrollbar(parent, orient="vertical", command=tree.yview)
        tree.configure(yscrollcommand=vsb.set)
        tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        vsb.pack(side=tk.RIGHT, fill=tk.Y)
        return tree

    @staticmethod
    def _clear_tree(tree):
        tree.delete(*tree.get_children())

    # ------------------------------------------------------------------
    # Tab "Metriche": card numeriche + matrice di confusione incorporata
    # direttamente nella finestra via FigureCanvasTkAgg (nessun file
    # immagine intermedio da salvare/ricaricare).
    #
    # La Figure e' costruita con l'API `matplotlib.figure.Figure` diretta,
    # NON con `pyplot.subplots`: pyplot registra ogni figura in un Gcf
    # (global figure cache) e ne lega il ciclo di vita al proprio backend,
    # il che con TkAgg puo' impedire l'uscita del processo alla chiusura
    # della finestra principale. Bypassando pyplot non c'e' alcuno stato
    # globale a cui la figura resta agganciata.
    # ------------------------------------------------------------------
    def _build_metrics_tab(self):
        header = ttk.Frame(self.metrics_tab, padding=(16, 16, 16, 8))
        header.pack(side=tk.TOP, fill=tk.X)

        self.metric_vars = {
            "recall": tk.StringVar(value="—"),
            "precision": tk.StringVar(value="—"),
            "f1": tk.StringVar(value="—"),
            "n_records": tk.StringVar(value="—"),
            "n_clusters": tk.StringVar(value="—"),
        }
        cards = [
            ("recall", "Recall"), ("precision", "Precision"), ("f1", "F1-score"),
            ("n_records", "Record totali"), ("n_clusters", "Cluster totali"),
        ]
        for i, (key, label) in enumerate(cards):
            card = ttk.Frame(header, style="Card.TFrame", padding=12)
            card.grid(row=0, column=i, padx=(0 if i == 0 else 8, 0), sticky="ew")
            header.columnconfigure(i, weight=1)
            ttk.Label(card, text=label, style="CardLabel.TLabel").pack()
            ttk.Label(card, textvariable=self.metric_vars[key], style="CardValue.TLabel").pack(pady=(4, 0))

        self.pairs_caption = tk.StringVar(value="")
        ttk.Label(header, textvariable=self.pairs_caption, style="Caption.TLabel").grid(
            row=1, column=0, columnspan=len(cards), sticky="w", pady=(10, 0),
        )

        # La matrice di confusione resta deliberatamente piccola e centrata
        # (non fill/expand): e' un riepilogo puntuale, non deve dominare
        # visivamente la tab.
        chart_area = ttk.Frame(self.metrics_tab)
        chart_area.pack(side=tk.TOP, pady=(4, 16))

        self.fig = Figure(figsize=(3.6, 3.3), dpi=100)
        self.fig.patch.set_facecolor(CARD_BG)
        self.ax_cm = self.fig.add_subplot(111)
        self.canvas = FigureCanvasTkAgg(self.fig, master=chart_area)
        self.canvas.get_tk_widget().pack()

    def _refresh_metrics_tab(self, title):
        r = self.result
        self.metric_vars["recall"].set(_fmt_ratio(r["recall"]))
        self.metric_vars["precision"].set(_fmt_ratio(r["precision"]))
        self.metric_vars["f1"].set(_fmt_ratio(r["f1"]))
        self.metric_vars["n_records"].set(str(r["n_records"]))
        self.metric_vars["n_clusters"].set(str(r["n_clusters"]))
        self.pairs_caption.set(
            "[{}] Coppie (cross-party + within-party delle party dirty) — TP={} FP={} TN={} FN={} GT={}".format(
                title, r["tp"], r["fp"], r["tn"], r["fn"], r["gt"]))

        # Matrice di confusione pairwise, stessa regola per-party della LU
        # (eval_core.evaluate): TN = confronti massimi - TP - FP - FN.
        self.ax_cm.clear()
        matrix = [[r["tp"], r["fp"]], [r["fn"], r["tn"]]]
        self.ax_cm.imshow(matrix, cmap="Blues", vmin=0)
        cell_labels = [
            ["TP\n{}".format(r["tp"]), "FP\n{}".format(r["fp"])],
            ["FN\n{}".format(r["fn"]), "TN\n{}".format(r["tn"])],
        ]
        for i in range(2):
            for j in range(2):
                self.ax_cm.text(j, i, cell_labels[i][j], ha="center", va="center", fontsize=9)
        self.ax_cm.set_xticks([0, 1], ["Reale: match", "Reale: non-match"], fontsize=8)
        self.ax_cm.set_yticks([0, 1], ["Predetto: match", "Predetto: non-match"], fontsize=8)
        self.ax_cm.set_title("Matrice di confusione", fontsize=10)

        self.fig.tight_layout()
        self.canvas.draw()

    # ------------------------------------------------------------------
    # Tab "Dettagli": stesse categorie di anomalie gia' riportate a
    # terminale da print_report(), qui come sotto-tab con Treeview
    # esplorabili invece che righe di testo concatenate.
    # ------------------------------------------------------------------
    def _build_details_tab(self):
        self.detail_trees = {}
        self.detail_tab_frames = {}
        specs = [
            ("split", "Entità split", ("GID", "record", "cluster_id")),
            ("mixed", "Cluster misti", ("cluster_id", "record", "GID")),
            ("singleton", "Singleton", ("party", "record_id", "cluster_id", "GID")),
            ("unclustered", "Non clusterizzati", ("party", "record_id", "GID")),
        ]
        for key, title, columns in specs:
            frame = ttk.Frame(self.details_tab, padding=10)
            self.details_tab.add(frame, text=title)
            self.detail_trees[key] = self._make_treeview(frame, columns)
            self.detail_tab_frames[key] = frame

    def _refresh_details_tab(self):
        r = self.result

        tree = self.detail_trees["split"]
        self._clear_tree(tree)
        for gid, members, _clusters_used in r["split_entities"]:
            for m in members:
                tree.insert("", "end", values=(gid, record_label(m), m["cluster_id"]))
        self.details_tab.tab(self.detail_tab_frames["split"],
                              text="Entità split ({})".format(len(r["split_entities"])))

        tree = self.detail_trees["mixed"]
        self._clear_tree(tree)
        for cid, members, _gids in r["mixed_clusters"]:
            for m in members:
                tree.insert("", "end", values=(cid, record_label(m), m["global_id"]))
        self.details_tab.tab(self.detail_tab_frames["mixed"],
                              text="Cluster misti ({})".format(len(r["mixed_clusters"])))

        tree = self.detail_trees["singleton"]
        self._clear_tree(tree)
        for m in r["singleton_records"]:
            tree.insert("", "end", values=(m["party"], m["record_id"], m["cluster_id"], m["global_id"]))
        self.details_tab.tab(self.detail_tab_frames["singleton"],
                              text="Singleton ({})".format(len(r["singleton_records"])))

        tree = self.detail_trees["unclustered"]
        self._clear_tree(tree)
        for m in r["unclustered_records"]:
            tree.insert("", "end", values=(m["party"], m["record_id"], m["global_id"]))
        self.details_tab.tab(self.detail_tab_frames["unclustered"],
                              text="Non clusterizzati ({})".format(len(r["unclustered_records"])))

    # ------------------------------------------------------------------
    # Tab "Linking table": cluster_id, GID, record_id, party per ogni
    # record, filtrabile per cluster/party (Listbox multi-selezione, dato
    # che il Combobox nativo di Tkinter non supporta selezione multipla)
    # ed evidenziata per cluster via i "tag" del Treeview.
    # ------------------------------------------------------------------
    def _build_linking_tab(self):
        listbox_opts = dict(
            selectmode=tk.EXTENDED, exportselection=False, relief="flat",
            borderwidth=1, highlightthickness=1, highlightbackground=BORDER,
            background=CARD_BG, font=("Segoe UI", 9),
        )

        filter_bar = ttk.Frame(self.linking_tab, style="Card.TFrame", padding=12)
        filter_bar.pack(side=tk.TOP, fill=tk.X, padx=10, pady=10)

        ttk.Label(filter_bar, text="Cluster", style="CardLabel.TLabel").grid(row=0, column=0, sticky="nw")
        self.cluster_listbox = tk.Listbox(filter_bar, height=5, width=16, **listbox_opts)
        self.cluster_listbox.grid(row=1, column=0, padx=(0, 20))

        ttk.Label(filter_bar, text="Party", style="CardLabel.TLabel").grid(row=0, column=1, sticky="nw")
        self.party_listbox = tk.Listbox(filter_bar, height=5, width=12, **listbox_opts)
        self.party_listbox.grid(row=1, column=1, padx=(0, 20))

        button_col = ttk.Frame(filter_bar, style="CardInner.TFrame")
        button_col.grid(row=1, column=2, sticky="n")
        ttk.Button(button_col, text="Applica filtro", command=self._apply_link_filter).pack(fill=tk.X, pady=(0, 4))
        ttk.Button(button_col, text="Reset filtri", command=self._reset_link_filter).pack(fill=tk.X)

        table_frame = ttk.Frame(self.linking_tab)
        table_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=(0, 6))
        self.link_tree = self._make_treeview(table_frame, ("cluster_id", "GID", "record_id", "party"))

        self.link_caption = tk.StringVar(value="")
        ttk.Label(self.linking_tab, textvariable=self.link_caption, style="Caption.TLabel").pack(
            anchor="w", padx=10, pady=(0, 10),
        )

    @staticmethod
    def _cluster_tag(cluster_id):
        return "cluster_{}".format(cluster_id)

    def _refresh_linking_tab(self):
        self._link_rows = [
            {"cluster_id": rec["cluster_id"], "GID": rec["global_id"],
             "record_id": rec["record_id"], "party": rec["party"]}
            for rec in self.records
        ]

        clustered_ids = sorted({row["cluster_id"] for row in self._link_rows if row["cluster_id"] is not None})
        has_unclustered = any(row["cluster_id"] is None for row in self._link_rows)
        cluster_options = [str(c) for c in clustered_ids] + (["non clusterizzato"] if has_unclustered else [])
        self.cluster_listbox.delete(0, tk.END)
        for opt in cluster_options:
            self.cluster_listbox.insert(tk.END, opt)

        party_options = sorted({row["party"] for row in self._link_rows})
        self.party_listbox.delete(0, tk.END)
        for opt in party_options:
            self.party_listbox.insert(tk.END, opt)

        # Colore stabile per cluster_id, assegnato nell'ordine di prima
        # apparizione nei record caricati — cosi' i record della stessa
        # entita' predetta restano riconoscibili a colpo d'occhio anche
        # filtrando o ricaricando la stessa sorgente.
        unique_clusters = []
        for row in self._link_rows:
            if row["cluster_id"] not in unique_clusters:
                unique_clusters.append(row["cluster_id"])
        self._cluster_colors = {cid: PALETTE[i % len(PALETTE)] for i, cid in enumerate(unique_clusters)}
        for cid, rgb in self._cluster_colors.items():
            hexcolor = "#{:02x}{:02x}{:02x}".format(*(int(c * 255) for c in rgb))
            self.link_tree.tag_configure(self._cluster_tag(cid), background=hexcolor)

        self._render_link_rows(self._link_rows)

    def _render_link_rows(self, rows):
        self._clear_tree(self.link_tree)
        rows_sorted = sorted(
            rows,
            key=lambda row: (
                row["cluster_id"] is None,
                row["cluster_id"] if row["cluster_id"] is not None else 0,
                row["party"], row["record_id"],
            ),
        )
        for row in rows_sorted:
            self.link_tree.insert(
                "", "end", tags=(self._cluster_tag(row["cluster_id"]),),
                values=(
                    row["cluster_id"] if row["cluster_id"] is not None else "—",
                    row["GID"] if row["GID"] else "—",
                    row["record_id"], row["party"],
                ),
            )
        self.link_caption.set("{} record su {} mostrati.".format(len(rows_sorted), len(self._link_rows)))

    def _apply_link_filter(self):
        selected_clusters = {self.cluster_listbox.get(i) for i in self.cluster_listbox.curselection()}
        selected_parties = {self.party_listbox.get(i) for i in self.party_listbox.curselection()}

        def matches(row):
            if selected_clusters:
                label = "non clusterizzato" if row["cluster_id"] is None else str(row["cluster_id"])
                if label not in selected_clusters:
                    return False
            if selected_parties and row["party"] not in selected_parties:
                return False
            return True

        self._render_link_rows([row for row in self._link_rows if matches(row)])

    def _reset_link_filter(self):
        self.cluster_listbox.selection_clear(0, tk.END)
        self.party_listbox.selection_clear(0, tk.END)
        self._render_link_rows(self._link_rows)

    # ------------------------------------------------------------------
    # Caricamento dati: confine di sistema (input utente: path/credenziali)
    # — a differenza del resto della pipeline, qui la gestione errori e'
    # appropriata: copre sia psycopg2 assente sia connessione/CSV malformati.
    # ------------------------------------------------------------------
    def _load_and_evaluate(self):
        source = self.source_var.get()
        try:
            if source == "CSV (MCL)":
                records = load_from_csv(self.csv_path_var.get())
                title = "MCL"
            else:
                _default_dbname, title = DB_SOURCES[source]
                records = load_from_postgres(
                    host=self.db_vars["host"].get(),
                    port=int(self.db_vars["port"].get()),
                    dbname=self.db_vars["dbname"].get(),
                    user=self.db_vars["user"].get(),
                    password=self.db_vars["password"].get(),
                )
        except FileNotFoundError:
            messagebox.showerror("File non trovato", "File non trovato: {}".format(self.csv_path_var.get()))
            return
        except Exception as exc:
            messagebox.showerror("Errore nel caricamento", str(exc))
            return

        if not records:
            messagebox.showwarning("Nessun dato", "Nessun record trovato per la sorgente selezionata.")
            return

        self.records = records
        self.result = evaluate(records)
        self.status_var.set("Sorgente: {} — {} record caricati.".format(title, len(records)))

        self._refresh_metrics_tab(title)
        self._refresh_details_tab()
        self._refresh_linking_tab()


def main():
    app = EvaluationApp()
    app.mainloop()


if __name__ == "__main__":
    main()
