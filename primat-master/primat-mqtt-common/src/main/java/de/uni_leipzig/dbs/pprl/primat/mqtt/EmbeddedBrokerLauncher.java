/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt;

import java.util.Properties;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;

/**
 * Avvia/ferma in-process un broker MQTT Moquette, cosi' la Linkage Unit
 * Orchestrator puo' eseguire il demo end-to-end senza dipendenze da un broker
 * MQTT esterno installato separatamente.
 */
public class EmbeddedBrokerLauncher {

	private final int port;
	private final Server server;

	/**
	 * @param port porta TCP su cui il broker embedded accetta connessioni
	 */
	public EmbeddedBrokerLauncher(int port) {
		this.port = port;
		this.server = new Server();
	}

	/**
	 * Avvia il broker in modalità non persistente (nessun file di storage:
	 * adatto a demo/test, non a un deployment di produzione).
	 *
	 * @throws Exception se Moquette non riesce ad avviare il broker
	 */
	public void start() throws Exception {
		final Properties properties = new Properties();
		properties.setProperty(IConfig.PORT_PROPERTY_NAME, String.valueOf(port));
		properties.setProperty(IConfig.HOST_PROPERTY_NAME, "0.0.0.0");
		properties.setProperty(IConfig.PERSISTENCE_ENABLED_PROPERTY_NAME, Boolean.FALSE.toString());
		final IConfig config = new MemoryConfig(properties);
		server.startServer(config);
	}

	/**
	 * Ferma il broker embedded, rilasciando la porta TCP.
	 */
	public void stop() {
		server.stopServer();
	}
}
