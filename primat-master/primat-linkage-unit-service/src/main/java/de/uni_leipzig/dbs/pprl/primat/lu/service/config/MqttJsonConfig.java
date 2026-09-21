/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/** Sezione {@code mqtt} del JSON, tutti campi opzionali (default = hardcoded odierno). */
public class MqttJsonConfig {

	private String brokerUrl;
	private Long rbfCollectionTimeoutSeconds;
	private Long rbfRepublishIntervalSeconds;

	public String getBrokerUrl() {
		return brokerUrl;
	}

	public Long getRbfCollectionTimeoutSeconds() {
		return rbfCollectionTimeoutSeconds;
	}

	public Long getRbfRepublishIntervalSeconds() {
		return rbfRepublishIntervalSeconds;
	}
}
