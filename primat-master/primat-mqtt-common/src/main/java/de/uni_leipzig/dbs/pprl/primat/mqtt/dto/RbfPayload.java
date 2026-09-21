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
 * inclusi. Disaccoppiato dal modello di dominio {@code Record}.
 */
public class RbfPayload {

	private String runId;
	private String party;
	private List<RbfRecord> records;

	public RbfPayload() {
	}

	public RbfPayload(String runId, String party, List<RbfRecord> records) {
		this.runId = runId;
		this.party = party;
		this.records = records;
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
