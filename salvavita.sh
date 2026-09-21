#!/bin/bash
# PreToolUse Hook: Blocca la lettura diretta di file dati
COMMAND=$1
TARGET=$2

# Se il comando è di lettura e il target è un file dati
if [[ "$COMMAND" == "cat" || "$COMMAND" == "less" || "$COMMAND" == "head" || "$COMMAND" == "tail" ]]; then
    if [[ "$TARGET" == *.csv || "$TARGET" == *.parquet || "$TARGET" == *.jsonl ]]; then
        echo "ERRORE HOOK: Tentativo di lettura diretta di un file dati bloccato per risparmio token."
        echo "AZIONE RICHIESTA: Leggi /data/CLAUDE.md per lo schema, oppure usa uno script usa-e-getta limitato a 5 righe."
        exit 1 # Blocca l'azione di Claude Code
    fi
fi

exit 0 # Consente l'esecuzione di tutti gli altri comandi