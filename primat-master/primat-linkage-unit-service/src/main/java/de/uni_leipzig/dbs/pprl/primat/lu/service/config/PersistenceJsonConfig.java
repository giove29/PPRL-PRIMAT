/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Sezione {@code persistence} del JSON. Default: {@code true} se
 * {@code clusteringMethod != MCL}; {@code true} esplicito con {@code MCL} è
 * un errore di validazione (vedi {@link LinkageUnitConfigLoader}). Quando
 * {@code enabled} è (effettivamente) {@code false}, la Link Table non tocca
 * alcun DB e viene invece scritta su {@code csvOutputPath} (vedi
 * {@code de.uni_leipzig.dbs.pprl.primat.lu.service.ClusterCsvWriter}).
 */
public class PersistenceJsonConfig {

	private Boolean enabled;
	private String csvOutputPath;

	public Boolean getEnabled() {
		return enabled;
	}

	/** @return percorso del CSV di export, o {@code null} se non specificato (si applica un default per-metodo). */
	public String getCsvOutputPath() {
		return csvOutputPath;
	}
}
