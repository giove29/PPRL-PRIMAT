/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * DTO di wire-format (serializzato via Gson) pubblicato dalla Linkage Unit sul
 * topic di comando di un Data Owner per avviare un nuovo run. Disaccoppiato
 * dal modello di dominio {@code Record}.
 */
public class StartCommand {

	private String runId;
	private long timestamp;

	public StartCommand() {
	}

	public StartCommand(String runId, long timestamp) {
		this.runId = runId;
		this.timestamp = timestamp;
	}

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
