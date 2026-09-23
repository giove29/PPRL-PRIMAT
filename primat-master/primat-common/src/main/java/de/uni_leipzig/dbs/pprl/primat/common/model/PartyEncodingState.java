/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.common.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Stato dell'encoding RBF con cui un {@link Party} ha persistito i suoi
 * {@link Record} in questo specifico DB persistente (una riga per party, non
 * per run). Usata da {@code DbConnection.checkEncodingState(...)} per
 * rilevare, prima di scrivere qualunque riga, se la configurazione di
 * encoding di un party è cambiata rispetto a un run precedente sullo stesso
 * DB: le blocking key JaccardLSH derivate dall'RBF cambierebbero, rompendo
 * silenziosamente il riconoscimento "stesso record fisico già visto" su cui
 * si basa la persistenza incrementale (vedi
 * {@code DbConnection.getCandidateClusters}).
 *
 * <p>Due campi indipendenti, entrambi deliberatamente **non** una descrizione
 * completa dell'encoding (niente salt/hashFunctions/CWE in chiaro, mai
 * trasmessi dal Data Owner in un sistema PPRL): {@link #getBitLength()} è un
 * intero in chiaro (non sensibile, serve anche altrove per la guardia
 * rbfSize/valueRange), {@link #getConfigHash()} è un digest non reversibile
 * dell'intera configurazione (cattura anche un cambio di salt/hashFunctions/
 * CWE che lasci invariata la lunghezza, cosa che il solo confronto sulla
 * lunghezza non può rilevare) — è quest'ultimo il confronto decisivo per
 * `checkEncodingState`.
 */
@Entity
public class PartyEncodingState {

	@Id
	private String party;

	private int bitLength;

	@Column(length = 100)
	private String configHash;

	public PartyEncodingState() {
	}

	public PartyEncodingState(String party, int bitLength, String configHash) {
		this.party = party;
		this.bitLength = bitLength;
		this.configHash = configHash;
	}

	public String getParty() {
		return party;
	}

	public void setParty(String party) {
		this.party = party;
	}

	public int getBitLength() {
		return bitLength;
	}

	public void setBitLength(int bitLength) {
		this.bitLength = bitLength;
	}

	public String getConfigHash() {
		return configHash;
	}

	public void setConfigHash(String configHash) {
		this.configHash = configHash;
	}
}
