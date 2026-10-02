/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt;

/**
 * Punto di verità unico per il naming dei topic MQTT usati nella pipeline PPRL
 * Data Owner / Linkage Unit. Costruire i nomi dei topic solo tramite questa
 * classe evita collisioni tra party/run per costruzione.
 */
public final class MqttTopics {

	private MqttTopics() {
	}

	/**
	 * Topic su cui un Data Owner resta in ascolto per ricevere il comando di
	 * avvio di un run dalla Linkage Unit.
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/do/{party}/cmd"
	 */
	public static String commandTopic(String party) {
		return "primat/do/" + party + "/cmd";
	}

	/**
	 * Topic su cui un Data Owner pubblica l'RBF calcolato per un dato run.
	 *
	 * @param runId identificativo del run
	 * @param party nome del party
	 * @return topic "primat/lu/{runId}/rbf/{party}"
	 */
	public static String rbfTopic(String runId, String party) {
		return "primat/lu/" + runId + "/rbf/" + party;
	}

	/**
	 * Topic filter usato dalla Linkage Unit per sottoscriversi agli RBF di
	 * tutti i party per un dato run.
	 *
	 * @param runId identificativo del run
	 * @return topic filter "primat/lu/{runId}/rbf/+"
	 */
	public static String rbfTopicWildcard(String runId) {
		return "primat/lu/" + runId + "/rbf/+";
	}

	/**
	 * Estrae il nome del party dall'ultimo segmento di un topic ricevuto sulla
	 * wildcard {@link #rbfTopicWildcard(String)}.
	 *
	 * @param topic topic concreto ricevuto (es. "primat/lu/run1/rbf/A")
	 * @return il segmento finale del topic, cioè il nome del party
	 */
	public static String partyFromRbfTopic(String topic) {
		return topic.substring(topic.lastIndexOf('/') + 1);
	}

	/**
	 * Topic su cui un Data Owner resta in ascolto per ricevere una push di
	 * configurazione (schema+encoding fusi in un unico messaggio) dalla SMU.
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/do/{party}/config"
	 */
	public static String configTopic(String party) {
		return "primat/do/" + party + "/config";
	}

	/**
	 * Topic su cui un Data Owner pubblica l'esito (ack) dell'adozione di una
	 * push di configurazione ricevuta dalla SMU.
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/smu/{party}/ack"
	 */
	public static String configAckTopic(String party) {
		return "primat/smu/" + party + "/ack";
	}

	/**
	 * Topic su cui un Data Owner resta in ascolto per una richiesta di
	 * checkVersion dalla SMU (protocollo StartCommand, fase 1: la SMU verifica
	 * che tutti i Data Owner abbiano gia' applicato la sua ultima
	 * configurazione prima di avviare un run).
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/do/{party}/checkversion"
	 */
	public static String checkVersionTopic(String party) {
		return "primat/do/" + party + "/checkversion";
	}

	/**
	 * Topic su cui un Data Owner risponde a una richiesta di checkVersion con
	 * la propria versione di configurazione correntemente applicata.
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/smu/{party}/version"
	 */
	public static String versionReportTopic(String party) {
		return "primat/smu/" + party + "/version";
	}

	/**
	 * Topic filter usato dalla SMU per sottoscriversi alle risposte di
	 * checkVersion di tutti i Data Owner in un solo colpo.
	 *
	 * @return topic filter "primat/smu/+/version"
	 */
	public static String versionReportTopicWildcard() {
		return "primat/smu/+/version";
	}

	/**
	 * Topic su cui un Data Owner riporta alla SMU l'esito (successo o errore)
	 * dell'elaborazione di un run avviato da un {@link
	 * de.uni_leipzig.dbs.pprl.primat.mqtt.dto.StartCommand}. Sostituisce il
	 * precedente topic di stato verso la Linkage Unit (mai letto da essa): la
	 * regia del run e la ricezione degli errori sono entrambe della SMU.
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/smu/{party}/run"
	 */
	public static String doRunStatusTopic(String party) {
		return "primat/smu/" + party + "/run";
	}

	/**
	 * Topic filter usato dalla SMU per sottoscriversi ai report di run di
	 * tutti i Data Owner in un solo colpo.
	 *
	 * @return topic filter "primat/smu/+/run"
	 */
	public static String doRunStatusTopicWildcard() {
		return "primat/smu/+/run";
	}

	/**
	 * Topic su cui la Linkage Unit resta in ascolto per la configurazione di
	 * un nuovo run (runId, digest atteso, rbfSize) spinta dalla SMU
	 * (protocollo StartCommand, fase 2).
	 *
	 * @return topic "primat/lu/config"
	 */
	public static String luConfigTopic() {
		return "primat/lu/config";
	}

	/**
	 * Topic su cui la Linkage Unit conferma o rifiuta la configurazione di run
	 * ricevuta su {@link #luConfigTopic()}.
	 *
	 * @return topic "primat/smu/lu/config-ack"
	 */
	public static String luConfigAckTopic() {
		return "primat/smu/lu/config-ack";
	}

	/**
	 * Topic su cui la Linkage Unit pubblica l'esito finale di un run (ack di
	 * successo, o errore — es. digest non corrispondente, matching fallito).
	 * Un solo topic per entrambi gli esiti, cosi' la SMU puo' attendere
	 * entrambi con un'unica subscription.
	 *
	 * @return topic "primat/smu/lu/run-status"
	 */
	public static String luRunStatusTopic() {
		return "primat/smu/lu/run-status";
	}

	/**
	 * Topic su cui la Linkage Unit resta in ascolto per una push dedicata di
	 * {@code mqttBrokerUrl} dalla SMU — canale separato da {@link #luConfigTopic()},
	 * che resta legato all'avvio di un run vero e proprio. Mirror lato LU di
	 * {@link #configTopic(String)} lato Data Owner (riconfigurazione
	 * indipendente dal comando di avvio).
	 *
	 * @return topic "primat/lu/broker"
	 */
	public static String luBrokerTopic() {
		return "primat/lu/broker";
	}

	/**
	 * Topic su cui la Linkage Unit conferma o rifiuta la push di
	 * {@code mqttBrokerUrl} ricevuta su {@link #luBrokerTopic()}.
	 *
	 * @return topic "primat/smu/lu/broker-ack"
	 */
	public static String luBrokerAckTopic() {
		return "primat/smu/lu/broker-ack";
	}

	/**
	 * Topic su cui un Data Owner resta in ascolto per una richiesta di
	 * pre-flight ("questo broker e' raggiungibile?") dalla SMU, PRIMA di una
	 * migrazione reale su {@link #configTopic(String)}. Nessuna persistenza/
	 * switch avviene a fronte di questo messaggio.
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/do/{party}/broker-check"
	 */
	public static String brokerCheckTopic(String party) {
		return "primat/do/" + party + "/broker-check";
	}

	/**
	 * Topic su cui un Data Owner risponde a una richiesta di pre-flight.
	 *
	 * @param party nome del party (es. "A")
	 * @return topic "primat/smu/{party}/broker-check-ack"
	 */
	public static String brokerCheckAckTopic(String party) {
		return "primat/smu/" + party + "/broker-check-ack";
	}

	/**
	 * Topic filter usato dalla SMU per sottoscriversi alle risposte di
	 * pre-flight di tutti i Data Owner in un solo colpo.
	 *
	 * @return topic filter "primat/smu/+/broker-check-ack"
	 */
	public static String brokerCheckAckTopicWildcard() {
		return "primat/smu/+/broker-check-ack";
	}

	/**
	 * Topic su cui la Linkage Unit resta in ascolto per una richiesta di
	 * pre-flight dalla SMU, PRIMA di una migrazione reale su
	 * {@link #luBrokerTopic()}. Mirror di {@link #brokerCheckTopic(String)}
	 * lato Data Owner.
	 *
	 * @return topic "primat/lu/broker-check"
	 */
	public static String luBrokerCheckTopic() {
		return "primat/lu/broker-check";
	}

	/**
	 * Topic su cui la Linkage Unit risponde a una richiesta di pre-flight.
	 *
	 * @return topic "primat/smu/lu/broker-check-ack"
	 */
	public static String luBrokerCheckAckTopic() {
		return "primat/smu/lu/broker-check-ack";
	}
}
