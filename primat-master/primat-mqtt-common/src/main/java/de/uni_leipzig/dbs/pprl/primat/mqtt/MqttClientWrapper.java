/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Thin wrapper su Paho {@link MqttClient}, condiviso da Data Owner e Linkage
 * Unit in modo che la gestione della connessione (QoS 1, retry, opzioni) non
 * sia duplicata nei due servizi.
 */
public class MqttClientWrapper {

	private static final int QOS = 1;
	private static final long RETRY_DELAY_MILLIS = 2000L;
	private static final long PUBLISH_WAIT_POLL_MILLIS = 200L;
	private static final long PUBLISH_WAIT_TIMEOUT_SECONDS = 30L;

	private final MqttClient client;
	private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

	/**
	 * @param brokerUrl URL del broker MQTT (es. "tcp://localhost:1883")
	 * @param clientId  identificativo univoco del client sul broker
	 * @throws MqttException se il client Paho non può essere istanziato
	 */
	public MqttClientWrapper(String brokerUrl, String clientId) throws MqttException {
		this.client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
		this.client.setCallback(new ReconnectCallback());
	}

	/**
	 * Connette al broker, riprovando a intervalli fissi finché non riesce.
	 * Il broker e' un processo indipendente, quindi il primo tentativo puo'
	 * fallire finche' non e' stato avviato: a differenza della riconnessione
	 * automatica di Paho (attiva solo dopo una connessione gia' riuscita), qui
	 * il retry copre anche il primo tentativo.
	 *
	 * @throws MqttException se il thread viene interrotto durante l'attesa tra
	 *                        un tentativo e il successivo
	 */
	public void connect() throws MqttException {
		connect(Long.MAX_VALUE);
	}

	/**
	 * Come {@link #connect()}, ma rinuncia dopo {@code maxWaitSeconds}.
	 *
	 * @throws MqttException          se il thread viene interrotto durante l'attesa
	 * @throws IllegalStateException se il broker non e' raggiungibile entro il timeout
	 */
	public void connect(long maxWaitSeconds) throws MqttException {
		final MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setAutomaticReconnect(true);

		final long deadlineNanos = maxWaitSeconds >= Long.MAX_VALUE / 1_000_000_000L ? Long.MAX_VALUE
				: System.nanoTime() + maxWaitSeconds * 1_000_000_000L;
		while (true) {
			try {
				client.connect(options);
				return;
			} catch (MqttException e) {
				if (deadlineNanos != Long.MAX_VALUE && System.nanoTime() - deadlineNanos >= 0) {
					throw new IllegalStateException("Broker MQTT non raggiungibile su " + client.getServerURI()
							+ " entro " + maxWaitSeconds + "s: avviare prima il broker (vedi TESTING.md)", e);
				}
				try {
					Thread.sleep(RETRY_DELAY_MILLIS);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	/**
	 * Pubblica un payload testuale (tipicamente JSON) con QoS 1. Se il client è
	 * momentaneamente disconnesso (es. keep-alive scaduto sotto carico), attende
	 * la riconnessione automatica di Paho fino a {@link #PUBLISH_WAIT_TIMEOUT_SECONDS}
	 * invece di fallire subito con un criptico "Client non connesso".
	 *
	 * @param topic   topic di destinazione
	 * @param payload corpo del messaggio, codificato UTF-8
	 * @throws MqttException          se la pubblicazione fallisce
	 * @throws IllegalStateException se il client non si riconnette entro il timeout
	 */
	public void publish(String topic, String payload) throws MqttException {
		waitUntilConnected(topic);
		final MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
		message.setQos(QOS);
		client.publish(topic, message);
	}

	/**
	 * Attende, con polling, che {@link #isConnected()} torni {@code true} - la
	 * riconnessione automatica di Paho (attiva dopo la prima connessione, vedi
	 * {@link #connect(long)}) non e' sincrona con {@code publish()}, quindi senza
	 * questa attesa un publish durante la finestra di riconnessione fallirebbe
	 * con reason code 32104 anche quando il client si sarebbe ririconnesso un
	 * istante dopo.
	 *
	 * @throws IllegalStateException se il client resta disconnesso oltre {@link #PUBLISH_WAIT_TIMEOUT_SECONDS}
	 */
	private void waitUntilConnected(String topic) throws MqttException {
		if (client.isConnected()) {
			return;
		}
		final long deadlineNanos = System.nanoTime() + PUBLISH_WAIT_TIMEOUT_SECONDS * 1_000_000_000L;
		while (!client.isConnected()) {
			if (System.nanoTime() - deadlineNanos >= 0) {
				throw new IllegalStateException("Client MQTT non riconnesso al broker " + client.getServerURI()
						+ " entro " + PUBLISH_WAIT_TIMEOUT_SECONDS + "s: impossibile pubblicare su " + topic);
			}
			try {
				Thread.sleep(PUBLISH_WAIT_POLL_MILLIS);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new MqttException(ie);
			}
		}
	}

	/**
	 * Sottoscrive un topic (o topic filter) con QoS 1, invocando il listener per
	 * ogni messaggio ricevuto.
	 *
	 * @param topicFilter topic o topic filter (con wildcard "+"/"#")
	 * @param listener    callback invocata alla ricezione di un messaggio
	 * @throws MqttException se la sottoscrizione fallisce
	 */
	public void subscribe(String topicFilter, IMqttMessageListener listener) throws MqttException {
		subscriptions.add(new Subscription(topicFilter, listener));
		client.subscribe(topicFilter, QOS, listener);
	}

	/**
	 * Disconnette e chiude il client. Da invocare solo all'arresto definitivo
	 * del processo, non tra un run e il successivo.
	 *
	 * @throws MqttException se la disconnessione fallisce
	 */
	public void disconnect() throws MqttException {
		if (client.isConnected()) {
			client.disconnect();
		}
		client.close();
	}

	/**
	 * @return {@code true} se il client è attualmente connesso al broker
	 */
	public boolean isConnected() {
		return client.isConnected();
	}

	/**
	 * Ri-sottoscrive tutte le subscription registrate dopo ogni riconnessione
	 * automatica. Con {@code cleanSession(true)} il broker parte da una
	 * sessione vuota ad ogni (ri)connessione: senza questo callback, dopo un
	 * riavvio del broker il client Paho risulterebbe connesso ma senza
	 * subscription attive, e i messaggi successivi andrebbero persi in
	 * silenzio.
	 */
	private final class ReconnectCallback implements MqttCallbackExtended {

		@Override
		public void connectComplete(boolean reconnect, String serverURI) {
			if (!reconnect) {
				// Prima connessione esplicita: le subscribe() richieste dal
				// chiamante dopo connect() bastano, non c'e' ancora nulla da
				// ripetere.
				return;
			}
			for (Subscription subscription : subscriptions) {
				try {
					client.subscribe(subscription.topicFilter, QOS, subscription.listener);
				} catch (MqttException e) {
					System.err.println("Ri-sottoscrizione fallita per il topic " + subscription.topicFilter + ": "
							+ e.getMessage());
				}
			}
		}

		@Override
		public void connectionLost(Throwable cause) {
			// Nessuna azione: la riconnessione è gestita da automaticReconnect,
			// il ripristino delle subscription da connectComplete().
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) {
			// Non usato: ogni subscribe() registra un IMqttMessageListener
			// dedicato, instradato da Paho senza passare da questo callback.
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			// Non usato: publish() e' fire-and-forget, nessun token da tracciare.
		}
	}

	private static final class Subscription {
		private final String topicFilter;
		private final IMqttMessageListener listener;

		private Subscription(String topicFilter, IMqttMessageListener listener) {
			this.topicFilter = topicFilter;
			this.listener = listener;
		}
	}
}
