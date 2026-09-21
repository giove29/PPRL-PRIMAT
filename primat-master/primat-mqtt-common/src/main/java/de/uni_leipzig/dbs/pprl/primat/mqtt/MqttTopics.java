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
	 * Topic su cui un Data Owner pubblica lo stato/ack relativo a un run.
	 *
	 * @param runId identificativo del run
	 * @param party nome del party
	 * @return topic "primat/lu/{runId}/status/{party}"
	 */
	public static String statusTopic(String runId, String party) {
		return "primat/lu/" + runId + "/status/" + party;
	}
}
