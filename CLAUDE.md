# Indice del Progetto
- `/primat-master`: Directory principale contenente il codice sorgente del progetto PRIMAT. (Leggi `/primat-master/CLAUDE.md` per l'indice dettagliato dei moduli).
- `/.idea`: Configurazione dell'ambiente di sviluppo IntelliJ.

# Regole di Comportamento (CRITICO)

- **NO YAPPING (Output coinciso):** Non salutare, non scusarti, non spiegare il codice se non te lo chiedo esplicitamente. Restituisci SOLO i comandi bash o gli snippet di codice strettamente necessari. Esegui l'azione richiesta e fermati. Salva eventuali documentazioni su file in modalità silenziosa, non stamparle nel terminale.
- **Ricerca Chirurgica:** Vieta l'uso di `cat` o `less` per esplorare interi file di codice. Usa ESCLUSIVAMENTE `grep -rn 'termine' --include='*.py' .` stampando massimo 2 righe di contesto. Modifica i file in-place usando formati diff.
- **Debug tramite Script Usa-e-Getta:** Se devi fare debug sui dati, non stampare mai i risultati nello standard output del terminale. Crea invece un file `temp_debug.py`, salva l'output su un file temporaneo `.txt` limitandolo rigorosamente a 10 righe, leggilo, e infine cancella entrambi i file temporanei.
