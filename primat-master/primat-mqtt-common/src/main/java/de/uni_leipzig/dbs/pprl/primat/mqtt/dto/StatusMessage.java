/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * DTO di wire-format (serializzato via Gson) usato da un Data Owner per
 * comunicare alla Linkage Unit lo stato di avanzamento/esito di un run.
 */
public class StatusMessage {

	private String runId;
	private String party;
	private String status;
	private String detail;

	public StatusMessage() {
	}

	public StatusMessage(String runId, String party, String status, String detail) {
		this.runId = runId;
		this.party = party;
		this.status = status;
		this.detail = detail;
	}

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public String getParty() {
		return party;
	}

	public void setParty(String party) {
		this.party = party;
	}

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
