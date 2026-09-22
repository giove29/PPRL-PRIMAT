/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Riproduce lo scenario riportato: un client si connette e si sottoscrive,
 * il broker viene fermato e riavviato, e un messaggio pubblicato dopo il
 * riavvio deve comunque raggiungere il listener originale. Prima del fix in
 * {@link MqttClientWrapper} (nessuna ri-sottoscrizione dopo una riconnessione
 * automatica) questo test falliva per timeout: il client risultava connesso
 * ma senza alcuna subscription attiva sul broker riavviato.
 */
class MqttClientWrapperReconnectTest {

	private static final String TOPIC = "test/reconnect";

	@Test
	void resubscribesAfterBrokerRestart() throws Exception {
		final int port = findFreePort();
		final String brokerUrl = "tcp://localhost:" + port;

		EmbeddedBrokerLauncher broker = new EmbeddedBrokerLauncher(port);
		broker.start();

		final BlockingQueue<String> received = new ArrayBlockingQueue<>(10);
		final MqttClientWrapper subscriber = new MqttClientWrapper(brokerUrl, "subscriber");
		try {
			subscriber.connect();
			subscriber.subscribe(TOPIC,
					(topic, message) -> received.add(new String(message.getPayload(), StandardCharsets.UTF_8)));

			broker.stop();
			broker = new EmbeddedBrokerLauncher(port);
			broker.start();

			assertTrue(waitUntilConnected(subscriber, 15), "il client non si e' riconnesso al broker riavviato");

			final MqttClientWrapper publisher = new MqttClientWrapper(brokerUrl, "publisher");
			try {
				publisher.connect();
				publisher.publish(TOPIC, "hello-after-restart");
			} finally {
				publisher.disconnect();
			}

			final String message = received.poll(10, TimeUnit.SECONDS);
			assertEquals("hello-after-restart", message);
		} finally {
			subscriber.disconnect();
			broker.stop();
		}
	}

	@Test
	void publishWaitsForReconnectionInsteadOfFailing() throws Exception {
		final int port = findFreePort();
		final String brokerUrl = "tcp://localhost:" + port;

		EmbeddedBrokerLauncher broker = new EmbeddedBrokerLauncher(port);
		broker.start();

		final BlockingQueue<String> received = new ArrayBlockingQueue<>(10);
		final MqttClientWrapper subscriber = new MqttClientWrapper(brokerUrl, "subscriber2");
		final MqttClientWrapper publisher = new MqttClientWrapper(brokerUrl, "publisher2");
		try {
			subscriber.connect();
			subscriber.subscribe(TOPIC,
					(topic, message) -> received.add(new String(message.getPayload(), StandardCharsets.UTF_8)));
			publisher.connect();

			broker.stop();
			broker = new EmbeddedBrokerLauncher(port);
			broker.start();

			// Il subscriber deve aver ri-sottoscritto prima del publish, altrimenti il
			// messaggio va perso lato broker indipendentemente dalla fix qui testata
			// (cleanSession=true: nessuna sessione persistente tra un riavvio e l'altro).
			// Sul PUBLISHER invece non c'e' nessuna attesa esplicita: publish() deve
			// attendere internamente la propria riconnessione da solo.
			assertTrue(waitUntilConnected(subscriber, 15), "il subscriber non si e' riconnesso al broker riavviato");
			publisher.publish(TOPIC, "hello-after-restart-2");

			final String message = received.poll(15, TimeUnit.SECONDS);
			assertEquals("hello-after-restart-2", message);
		} finally {
			subscriber.disconnect();
			publisher.disconnect();
			broker.stop();
		}
	}

	private static boolean waitUntilConnected(MqttClientWrapper client, int timeoutSeconds) throws InterruptedException {
		final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
		while (System.currentTimeMillis() < deadline) {
			if (client.isConnected()) {
				return true;
			}
			Thread.sleep(200);
		}
		return client.isConnected();
	}

	private static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
