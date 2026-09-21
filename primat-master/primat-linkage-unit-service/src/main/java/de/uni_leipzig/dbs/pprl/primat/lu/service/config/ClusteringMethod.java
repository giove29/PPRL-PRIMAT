/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Chiave di dispatch pubblica della config JSON della Linkage Unit — quale
 * delle 5 strategie di clustering eseguire in questo processo/run. Distinto
 * da {@code MultiSourceLinkage.LinkStrategy} (quello resta un tag di
 * risultato interno, non usato per il dispatch).
 *
 * <p>{@code GLOBAL_GREEDY} e {@code CLIP} sono dedicate allo scenario in cui
 * TUTTE le party sono duplicate-free (Clean): a differenza di
 * {@code MSCD_AP} (che richiede solo almeno una party clean), il loro
 * vincolo union-find di source-consistency presuppone che nessuna sorgente
 * abbia duplicati interni veri.
 */
public enum ClusteringMethod {
	CENTER_CLUSTERING,
	MSCD_AP,
	MCL,
	GLOBAL_GREEDY,
	CLIP
}
