/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * DTO di wire-format (serializzato via Gson) pubblicato dalla SMU sul topic
 * di checkVersion di un Data Owner (protocollo StartCommand, fase 1): un
 * semplice ping, nessun parametro oltre il timestamp di invio. Il Data Owner
 * risponde con un {@link ConfigVersionReport}.
 */
public class CheckVersionCommand {

	private long timestamp;

	public CheckVersionCommand() {
	}

	public CheckVersionCommand(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
