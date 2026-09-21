/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

import java.util.BitSet;
import java.util.Base64;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;

/**
 * Conversione tra il modello di dominio {@code Record} (con un unico
 * {@code BitSetAttribute} RBF, prodotto da {@code BloomFilterEncoder}) e il
 * DTO di wire-format {@link RbfPayload.RbfRecord}. Usata sia lato Data Owner
 * (serializzazione in uscita) sia lato Linkage Unit (deserializzazione in
 * ingresso), cosi' il formato del bitset sul filo (Base64 dei byte del
 * {@link BitSet}) e' definito in un solo punto.
 */
public final class RbfCodec {

	private RbfCodec() {
	}

	/**
	 * Estrae l'RBF di un record già codificato e lo converte nel DTO di
	 * trasporto.
	 *
	 * @param record record con esattamente un {@code BitSetAttribute} (l'RBF)
	 * @return il DTO pronto per essere incluso in un {@link RbfPayload}
	 */
	public static RbfPayload.RbfRecord toRbfRecord(Record record) {
		final BitSet bitSet = record.getBitSetAttributes().get(0).getValue();
		final String bitsetBase64 = Base64.getEncoder().encodeToString(bitSet.toByteArray());
		return new RbfPayload.RbfRecord(record.getId(), record.getGlobalId(), record.getParty().getName(),
				bitsetBase64);
	}

	/**
	 * Ricostruisce, lato Linkage Unit, un {@code Record} contenente solo l'RBF
	 * ricevuto via MQTT (nessun attributo in chiaro attraversa mai la rete).
	 *
	 * @param rbfRecord DTO ricevuto nell'{@link RbfPayload}
	 * @param party     istanza di {@code Party} da associare al record (la
	 *                  stessa usata come chiave nella mappa di input del
	 *                  matcher, cosi' l'identità è garantita anche se
	 *                  {@code Party.equals} si basasse solo sul nome)
	 * @return il record ricostruito, pronto per blocking/matching
	 */
	public static Record toRecord(RbfPayload.RbfRecord rbfRecord, Party party) {
		final BitSet bitSet = BitSet.valueOf(Base64.getDecoder().decode(rbfRecord.getBitsetBase64()));
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(rbfRecord.getId()));
		record.setGlobalIdAttribute(new GlobalIdAttribute(rbfRecord.getGlobalId()));
		record.setParty(party);
		record.addAttribute(new BitSetAttribute(bitSet));
		return record;
	}
}
