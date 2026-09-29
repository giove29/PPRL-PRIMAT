/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * DTO di wire-format (serializzato via Gson) pubblicato dalla SMU sul topic
 * di configurazione di un Data Owner. Un solo messaggio per l'intera
 * operazione: {@link #getConfigJson()} contiene gia' schema (mapping
 * colonna→QID) ed encoding comune fusi insieme dalla SMU, come frammento JSON
 * grezzo con le sole chiavi {@code columns}/{@code bloomFilter}/
 * {@code missingValueHandling}/{@code hmacKey} — il Data Owner lo fonde nel
 * proprio file di configurazione locale sovrascrivendo solo quelle chiavi
 * (identita' di party/broker/sorgente dati restano locali, mai spedite dalla
 * SMU). Disaccoppiato dal modello di dominio.
 */
public class ConfigPush {

	private String version;
	private String configJson;

	public ConfigPush() {
	}

	public ConfigPush(String version, String configJson) {
		this.version = version;
		this.configJson = configJson;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/** @return frammento JSON grezzo con le sole chiavi da sovrascrivere nel file locale del Data Owner. */
	public String getConfigJson() {
		return configJson;
	}

	public void setConfigJson(String configJson) {
		this.configJson = configJson;
	}
}
