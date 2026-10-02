/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * Richiesta di pre-flight ("questo broker e' raggiungibile da te?"), inviata
 * dalla SMU a un Data Owner o alla Linkage Unit PRIMA di una migrazione di
 * broker vera e propria (vedi {@link LuBrokerPush}/{@code ConfigPush} per il
 * commit reale). Nessuna persistenza/switch viene mai fatta a fronte di
 * questo messaggio: il destinatario apre una connessione di prova usa-e-getta
 * e risponde pronto/errore, nulla di piu'.
 */
public class BrokerCheckRequest {

	private String mqttBrokerUrl;

	public BrokerCheckRequest() {
	}

	public BrokerCheckRequest(String mqttBrokerUrl) {
		this.mqttBrokerUrl = mqttBrokerUrl;
	}

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	public void setMqttBrokerUrl(String mqttBrokerUrl) {
		this.mqttBrokerUrl = mqttBrokerUrl;
	}
}
