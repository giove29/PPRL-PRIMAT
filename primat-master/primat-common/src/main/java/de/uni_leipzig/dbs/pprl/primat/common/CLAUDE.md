# Indice del package Common
- `/blocking`: Classi, interfacce e modelli base condivisi per le tecniche di blocking.
- `/csv`: Classi e utilità per l'I/O ottimizzato di file CSV.
- `/extraction`: Strumenti per estrarre e parsare strutture dati dai file.
- `/model`: Definizione dei modelli di base utilizzati su tutto il framework (es. Record).
- `/utils`: Funzioni di utilità generali per I/O, matematica, stringhe.
- `/model/Cluster` (2026-09-21): id con `@SequenceGenerator` dedicato `cluster_seq` (allocationSize=50) per evitare il conflitto sulla `hibernate_sequence` condivisa e permettere batching degli insert.
- `/extraction/qgram/ConstantWeightTrigramExtractor` (2026-09-22): estende `TrigramExtractor`, normalizza il numero di trigrammi unici per attributo dentro una banda `[minTrigrams, maxTrigrams]` configurabile (hash-ranking deterministico se in eccesso, filler da hash del valore se in difetto su valore non vuoto); `enabled=false` e' equivalente byte-per-byte a `TrigramExtractor`. Usa `/utils/DeterministicHashing` (nuova, HMAC-SHA384 a chiave fissa per ranking/bucketing) e `/utils/QidAttributeUtils` (nuova, predicato condiviso di "attributo vuoto").
