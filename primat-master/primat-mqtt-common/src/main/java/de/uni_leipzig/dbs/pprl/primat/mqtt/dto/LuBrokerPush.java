/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * Push dedicata di un nuovo {@code mqttBrokerUrl} per la Linkage Unit,
 * pubblicata dalla SMU su {@link de.uni_leipzig.dbs.pprl.primat.mqtt.MqttTopics#luBrokerTopic()}.
 * Canale separato da {@link LuConfigPush} (quella resta legata all'avvio di un
 * run vero e proprio) — mirror lato LU di {@link ConfigPush} lato Data Owner.
 */
public class LuBrokerPush {

	private String mqttBrokerUrl;

	public LuBrokerPush() {
	}

	public LuBrokerPush(String mqttBrokerUrl) {
		this.mqttBrokerUrl = mqttBrokerUrl;
	}

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	public void setMqttBrokerUrl(String mqttBrokerUrl) {
		this.mqttBrokerUrl = mqttBrokerUrl;
	}
}
