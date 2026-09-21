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
 * Broker MQTT Moquette senza installazioni esterne: avviabile come processo
 * autonomo ({@link #main}) o in-process (test).
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
		properties.setProperty(IConfig.NETTY_MAX_BYTES_PROPERTY_NAME, "268435455");
		final IConfig config = new MemoryConfig(properties);
		server.startServer(config);
	}

	/**
	 * Ferma il broker embedded, rilasciando la porta TCP.
	 */
	public void stop() {
		server.stopServer();
	}

	/**
	 * Avvia il broker come processo autonomo e resta in ascolto fino a Ctrl+C.
	 *
	 * @param args {@code [porta]}, default 1883
	 */
	public static void main(String[] args) throws Exception {
		final int port = args.length > 0 ? Integer.parseInt(args[0]) : 1883;
		final EmbeddedBrokerLauncher broker = new EmbeddedBrokerLauncher(port);
		broker.start();
		Runtime.getRuntime().addShutdownHook(new Thread(broker::stop));
		System.out.println("Broker MQTT in ascolto su tcp://0.0.0.0:" + port + " (Ctrl+C per fermarlo)");
		Thread.currentThread().join();
	}
}
