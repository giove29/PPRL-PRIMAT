/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * DTO di wire-format (serializzato via Gson) pubblicato dalla SMU sul topic
 * di configurazione della Linkage Unit (protocollo StartCommand, fase 2):
 * identifica il run che sta per iniziare, il digest atteso (calcolato dalla
 * SMU sulla stessa configurazione gia' confermata da tutti i Data Owner in
 * fase di checkVersion) contro cui la Linkage Unit verifichera' ogni RBF
 * ricevuto, e la dimensione RBF attesa. Mai alcun dettaglio di encoding in
 * chiaro (salt/hashFunctions/CWE): solo il digest, come gia' per {@code
 * RbfPayload.configHash}.
 */
public class LuConfigPush {

	private String runId;
	private String expectedDigest;
	private int rbfSize;

	public LuConfigPush() {
	}

	public LuConfigPush(String runId, String expectedDigest, int rbfSize) {
		this.runId = runId;
		this.expectedDigest = expectedDigest;
		this.rbfSize = rbfSize;
	}

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public String getExpectedDigest() {
		return expectedDigest;
	}

	public void setExpectedDigest(String expectedDigest) {
		this.expectedDigest = expectedDigest;
	}

	public int getRbfSize() {
		return rbfSize;
	}

	public void setRbfSize(int rbfSize) {
		this.rbfSize = rbfSize;
	}
}
