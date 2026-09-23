/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.Gson;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigException;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DbSourceConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.CsvRecordSource;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.JdbcRecordSource;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.RecordSource;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttClientWrapper;
import de.uni_leipzig.dbs.pprl.primat.mqtt.MqttTopics;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfCodec;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfPayload;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StartCommand;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StatusMessage;

/**
 * Servizio long-running lato Data Owner: resta in ascolto all'infinito sul
 * proprio topic di comando MQTT. Alla ricezione di uno {@link StartCommand}
 * esegue {@link DataOwnerPipeline} sui dati locali e pubblica il risultato
 * (solo RBF, mai dati in chiaro) sul topic RBF riservato del run, per poi
 * tornare in ascolto — può quindi gestire più run consecutivi senza essere
 * riavviato.
 */
public class DataOwnerService {

	private final DataOwnerConfig config;
	private final MqttClientWrapper client;
	private final Gson gson = new Gson();
	/**
	 * Esegue {@link #handleStartCommand} fuori dal thread di callback di Paho
	 * ({@code CommsCallback}), che è lo stesso thread su cui vengono anche
	 * notificati i completamenti (ack) delle {@code publish()} sincrone e la
	 * consegna di ogni messaggio successivo per questo client: elaborare la
	 * pipeline (potenzialmente lunga, minuti su CSV grandi) e poi pubblicare
	 * l'RBF direttamente dentro il listener di {@link #start()} blocca quel
	 * thread, quindi qualunque comando ripubblicato nel frattempo dalla
	 * Linkage Unit resta in coda e non viene mai gestito finché la chiamata
	 * corrente non ritorna — visto dall'esterno, il Data Owner sembra
	 * "in ascolto" (la connessione TCP resta viva) ma non risponde più a
	 * nulla. Un solo thread basta: i comandi per questo party vanno comunque
	 * gestiti in sequenza, non in parallelo.
	 */
	private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "data-owner-command-worker");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * @param config configurazione locale del Data Owner (party, sorgente dati,
	 *               endpoint broker)
	 * @throws MqttException se il client MQTT non può essere istanziato
	 */
	public DataOwnerService(DataOwnerConfig config) throws MqttException {
		this.config = config;
		this.client = new MqttClientWrapper(config.getMqttBrokerUrl(), config.getMqttClientId());
	}

	/**
	 * Si connette al broker e si sottoscrive al proprio topic di comando.
	 * Ritorna subito dopo la sottoscrizione: i comandi vengono gestiti in modo
	 * asincrono dal thread di rete di Paho, chiamare {@link #awaitForever()} per
	 * mantenere vivo il processo.
	 *
	 * @throws MqttException se connessione o sottoscrizione falliscono
	 */
	public void start() throws MqttException {
		client.connect();
		client.subscribe(MqttTopics.commandTopic(config.getParty()), (topic, message) -> {
			final StartCommand command = gson.fromJson(new String(message.getPayload(), StandardCharsets.UTF_8),
					StartCommand.class);
			commandExecutor.submit(() -> handleStartCommand(command));
		});
		System.out.println("[" + config.getParty() + "] in ascolto su " + MqttTopics.commandTopic(config.getParty()));
	}

	/**
	 * Esegue la pipeline locale e pubblica l'RBF risultante per il run
	 * richiesto. Eventuali errori vengono comunicati alla Linkage Unit tramite
	 * {@link StatusMessage} sul topic di stato del run, senza interrompere
	 * l'ascolto per i run successivi.
	 *
	 * @param command comando di avvio ricevuto dalla Linkage Unit
	 */
	private void handleStartCommand(StartCommand command) {
		final String runId = command.getRunId();
		System.out.println("[" + config.getParty() + "] comando ricevuto per run " + runId);
		try {
			final RecordSource source = newRecordSource();
			final List<Record> encodedRecords = new DataOwnerPipeline(source, config).run();

			final List<RbfPayload.RbfRecord> rbfRecords = encodedRecords.stream()
					.map(RbfCodec::toRbfRecord)
					.collect(Collectors.toList());

			final RbfPayload payload = new RbfPayload(runId, config.getParty(), rbfRecords,
					config.computeEffectiveRbfBitLength(), config.computeConfigHash());
			client.publish(MqttTopics.rbfTopic(runId, config.getParty()), gson.toJson(payload));

			client.publish(MqttTopics.statusTopic(runId, config.getParty()),
					gson.toJson(new StatusMessage(runId, config.getParty(), "DONE", rbfRecords.size() + " record")));
			System.out.println("[" + config.getParty() + "] RBF pubblicato (" + rbfRecords.size() + " record) su "
					+ MqttTopics.rbfTopic(runId, config.getParty()));
		} catch (Exception e) {
			System.err.println("[" + config.getParty() + "] errore durante l'elaborazione del run " + runId + ":");
			e.printStackTrace();
			try {
				client.publish(MqttTopics.statusTopic(runId, config.getParty()),
						gson.toJson(new StatusMessage(runId, config.getParty(), "FAILED", e.getMessage())));
			} catch (MqttException publishFailure) {
				System.err.println("[" + config.getParty() + "] impossibile pubblicare lo stato di errore: "
						+ publishFailure.getMessage());
			}
		}
	}

	private RecordSource newRecordSource() {
		switch (config.getDataSourceType()) {
			case CSV:
				return new CsvRecordSource(config.getCsvFilePath(), config.isCsvHasHeader(), config.getCsvDelimiter());
			case DB: {
				final DbSourceConfig dbConfig = config.getDbConfig();
				return new JdbcRecordSource(dbConfig.getJdbcUrl(), dbConfig.getUsername(), dbConfig.getPassword(),
						dbConfig.getTableName());
			}
			default:
				throw new UnsupportedDataSourceException("Tipo di sorgente dati non gestito: "
						+ config.getDataSourceType());
		}
	}

	/**
	 * Blocca il thread chiamante all'infinito, cosi' il processo resta vivo e in
	 * ascolto (i comandi MQTT sono gestiti in modo asincrono dal thread di rete
	 * di Paho).
	 *
	 * @throws InterruptedException se il thread viene interrotto (es. shutdown)
	 */
	public void awaitForever() throws InterruptedException {
		final Object lock = new Object();
		synchronized (lock) {
			while (true) {
				lock.wait();
			}
		}
	}

	/**
	 * Avvia il Data Owner Service come processo standalone.
	 *
	 * @param args {@code configJsonPath}, es.
	 *             {@code primat-data-owner-service/src/main/resources/config/party_A.json}
	 * @throws Exception se l'avvio del servizio fallisce
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Uso: DataOwnerService <configJsonPath>");
			System.exit(1);
		}

		final DataOwnerConfig config;
		try {
			config = DataOwnerConfigLoader.load(Paths.get(args[0]));
		}
		catch (DataOwnerConfigException e) {
			// Errore di configurazione utente: messaggio leggibile, niente stack
			// trace grezzo.
			System.err.println("Errore di configurazione: " + e.getMessage());
			System.exit(1);
			return;
		}

		System.out.println(config.describe());

		final DataOwnerService service = new DataOwnerService(config);
		service.start();
		service.awaitForever();
	}
}
