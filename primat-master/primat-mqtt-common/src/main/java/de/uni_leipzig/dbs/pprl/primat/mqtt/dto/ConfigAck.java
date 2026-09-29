/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * DTO di wire-format (serializzato via Gson) usato da un Data Owner per
 * confermare alla SMU l'esito dell'adozione di una {@link ConfigPush}: solo
 * conferma tecnica (il file locale e' stato riscritto, ricaricato e ha
 * superato lo smoke-test), mai un consenso/voto sul contenuto della
 * configurazione, che la SMU decide e spinge unilateralmente.
 */
public class ConfigAck {

	private String party;
	private String version;
	private String status;
	private String detail;

	public ConfigAck() {
	}

	public ConfigAck(String party, String version, String status, String detail) {
		this.party = party;
		this.version = version;
		this.status = status;
		this.detail = detail;
	}

	public String getParty() {
		return party;
	}

	public void setParty(String party) {
		this.party = party;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/** @return {@code "OK"} se il reconfigure e' stato applicato con successo, {@code "ERROR"} altrimenti. */
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}
}
