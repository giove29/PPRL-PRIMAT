/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * DTO di wire-format (serializzato via Gson) con cui un Data Owner risponde a
 * un {@link CheckVersionCommand} della SMU (protocollo StartCommand, fase 1),
 * riportando la versione di configurazione correntemente applicata. {@code
 * "0"} e' la sentinella usata quando il processo non ha ancora applicato
 * nessuna {@link ConfigPush} della SMU da quando e' partito (versione
 * tracciata solo in memoria, non persistita nel file di configurazione).
 */
public class ConfigVersionReport {

	private String party;
	private String appliedVersion;

	public ConfigVersionReport() {
	}

	public ConfigVersionReport(String party, String appliedVersion) {
		this.party = party;
		this.appliedVersion = appliedVersion;
	}

	public String getParty() {
		return party;
	}

	public void setParty(String party) {
		this.party = party;
	}

	public String getAppliedVersion() {
		return appliedVersion;
	}

	public void setAppliedVersion(String appliedVersion) {
		this.appliedVersion = appliedVersion;
	}
}
