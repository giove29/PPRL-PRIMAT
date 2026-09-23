/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

import java.util.List;

/**
 * DTO di wire-format (serializzato via Gson) pubblicato da un Data Owner sul
 * proprio topic RBF. Contiene, per ogni record locale, il solo Bloom Filter
 * record-level (RBF) già codificato: i dati in chiaro non vengono mai
 * inclusi, e lo stesso vale per la configurazione di encoding — la LU riceve
 * solo {@link #getEffectiveRbfBitLength()} (un intero) e
 * {@link #getConfigHash()} (un digest non reversibile), mai una descrizione
 * di salt/hashFunctions/hardening/CWE. Disaccoppiato dal modello di dominio
 * {@code Record}.
 */
public class RbfPayload {

	private String runId;
	private String party;
	private List<RbfRecord> records;
	private int effectiveRbfBitLength;
	private String configHash;

	public RbfPayload() {
	}

	public RbfPayload(String runId, String party, List<RbfRecord> records, int effectiveRbfBitLength,
			String configHash) {
		this.runId = runId;
		this.party = party;
		this.records = records;
		this.effectiveRbfBitLength = effectiveRbfBitLength;
		this.configHash = configHash;
	}

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public String getParty() {
		return party;
	}

	public void setParty(String party) {
		this.party = party;
	}

	public List<RbfRecord> getRecords() {
		return records;
	}

	public void setRecords(List<RbfRecord> records) {
		this.records = records;
	}

	/**
	 * @return la dimensione reale, in bit, dell'RBF di questo party dopo
	 *         l'hardening (es. {@code DataOwnerConfig#computeEffectiveRbfBitLength()}),
	 *         dichiarata con certezza dalla configurazione — non dedotta dal
	 *         contenuto dei bitset ricevuti. Deliberatamente l'unico dato sulla
	 *         codifica trasmesso dal Data Owner: in un sistema PPRL la Linkage
	 *         Unit non deve mai ricevere salt/hashFunctions/CWE o altri dettagli
	 *         implementativi della codifica.
	 */
	public int getEffectiveRbfBitLength() {
		return effectiveRbfBitLength;
	}

	public void setEffectiveRbfBitLength(int effectiveRbfBitLength) {
		this.effectiveRbfBitLength = effectiveRbfBitLength;
	}

	/**
	 * @return digest deterministico e non reversibile (vedi
	 *         {@code DataOwnerConfig#computeConfigHash()}) dell'intera
	 *         configurazione di encoding di questo party. Permette alla
	 *         Linkage Unit di verificare se la configurazione e' cambiata
	 *         rispetto a un run precedente sullo stesso DB persistente senza
	 *         mai vedere salt/hashFunctions/CWE.
	 */
	public String getConfigHash() {
		return configHash;
	}

	public void setConfigHash(String configHash) {
		this.configHash = configHash;
	}

	/**
	 * Un singolo record RBF: identificatore locale, identificatore globale (usato
	 * solo lato Linkage Unit come ground truth di valutazione, mai come feature
	 * di matching), party di provenienza e il bitset RBF codificato in Base64.
	 */
	public static class RbfRecord {

		private String id;
		private String globalId;
		private String party;
		private String bitsetBase64;

		public RbfRecord() {
		}

		public RbfRecord(String id, String globalId, String party, String bitsetBase64) {
			this.id = id;
			this.globalId = globalId;
			this.party = party;
			this.bitsetBase64 = bitsetBase64;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getGlobalId() {
			return globalId;
		}

		public void setGlobalId(String globalId) {
			this.globalId = globalId;
		}

		public String getParty() {
			return party;
		}

		public void setParty(String party) {
			this.party = party;
		}

		public String getBitsetBase64() {
			return bitsetBase64;
		}

		public void setBitsetBase64(String bitsetBase64) {
			this.bitsetBase64 = bitsetBase64;
		}
	}
}
