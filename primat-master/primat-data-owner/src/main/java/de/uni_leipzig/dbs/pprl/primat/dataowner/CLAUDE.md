# Indice del package Data Owner
- `/encoding`: Algoritmi e strategie di codifica e mascheramento (es. Bloom Filters, privacy-preserving encoding).
- `/preprocessing`: Metodi per il preprocessing, normalizzazione e pulizia dei dati prima della codifica.
- `/encoding/bloomfilter/MissingValueBucketing` (2026-09-22): genera i token sintetici per un attributo QID vuoto (bypassando l'estrazione a q-gram), scelti su un bucket deterministico (`NUM_BUCKETS=64`, hardcoded) derivato dall'hash del primo attributo QID non vuoto del record secondo una lista di priorita' configurabile. Bypass innescato da `BloomFilterEncoder.extractFeatures` quando `BloomFilterDefinition.isMissingValueHandlingEnabled()`.
