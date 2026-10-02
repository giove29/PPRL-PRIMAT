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
 * solo {@link #getConfigHash()} (un digest non reversibile), mai una
 * descrizione di salt/hashFunctions/hardening/CWE. La lunghezza dell'RBF non
 * è più riportata dal Data Owner (era {@code effectiveRbfBitLength}): la LU
 * usa il proprio {@code rbfSize}, spinto dalla SMU, come unica fonte di
 * verità (vedi {@code LinkageUnitConfig#getRbfSize()}). Disaccoppiato dal
 * modello di dominio {@code Record}.
 * <p>
 * Dal 2026-10-02 un Data Owner con molti record pubblica i propri RBF in più
 * messaggi ("chunk") sullo stesso topic invece di un unico payload gigante
 * (vedi {@code DataOwnerService#handleStartCommand}): {@link #chunkIndex}/
 * {@link #totalChunks} identificano la posizione di questo messaggio nella
 * sequenza (party piccoli: un solo chunk, {@code chunkIndex=0},
 * {@code totalChunks=1}). {@link #configHash} è ripetuto identico su ogni
 * chunk dello stesso party, cosi' la Linkage Unit può verificarlo "fail
 * fast" già al primo chunk ricevuto invece di aspettare la raccolta
 * completa (vedi {@code LinkageUnitOrchestrator#waitForRbf}).
 */
public class RbfPayload {

	private String runId;
	private String party;
	private List<RbfRecord> records;
	private String configHash;
	private int chunkIndex;
	private int totalChunks;

	public RbfPayload() {
	}

	public RbfPayload(String runId, String party, List<RbfRecord> records, String configHash, int chunkIndex,
			int totalChunks) {
		this.runId = runId;
		this.party = party;
		this.records = records;
		this.configHash = configHash;
		this.chunkIndex = chunkIndex;
		this.totalChunks = totalChunks;
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

	/** @return posizione (0-based) di questo chunk nella sequenza di pubblicazione RBF di questo party per questo run. */
	public int getChunkIndex() {
		return chunkIndex;
	}

	public void setChunkIndex(int chunkIndex) {
		this.chunkIndex = chunkIndex;
	}

	/** @return numero totale di chunk dichiarati dal Data Owner per questo party/run (1 se non frazionato). */
	public int getTotalChunks() {
		return totalChunks;
	}

	public void setTotalChunks(int totalChunks) {
		this.totalChunks = totalChunks;
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
