/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Una colonna dello schema del Data Owner, cosi' come dichiarata nel JSON di
 * configurazione. Popolata da Gson per riflessione (nessun costruttore
 * esplicito necessario), i campi {@code dataType}/{@code hashFunctions}/
 * {@code salt} hanno senso solo per {@code role == QID} e sono opzionali:
 * {@link #getHashFunctionsOrDefault()} e {@link #getSaltOrDefault()}
 * risolvono i default quando omessi nel JSON.
 */
public class ColumnConfig {

	/** Numero di hash function usato per una colonna QID se non specificato nel JSON. */
	public static final int DEFAULT_HASH_FUNCTIONS = 10;

	/**
	 * Numero di token sintetici generati per un attributo vuoto quando
	 * {@code missingValueTokenCount} e' omesso e la Constant Weight Encoding
	 * non e' abilitata per questa colonna (altrimenti si riusa
	 * {@code constantWeightEncoding.minTrigrams}, per un "peso" comparabile a
	 * un valore normale).
	 */
	public static final int DEFAULT_MISSING_VALUE_TOKEN_COUNT = 7;

	private int index;
	private String name;
	private ColumnRole role;
	private ColumnDataType dataType;
	private Integer hashFunctions;
	private String salt;
	private ConstantWeightEncodingJsonConfig constantWeightEncoding;
	private Integer missingValueTokenCount;

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public ColumnRole getRole() {
		return role;
	}

	public ColumnDataType getDataType() {
		return dataType;
	}

	/** @return il numero di hash function esplicitamente configurato, o {@code null} se omesso. */
	public Integer getHashFunctions() {
		return hashFunctions;
	}

	/** @return il salt esplicitamente configurato, o {@code null} se omesso. */
	public String getSalt() {
		return salt;
	}

	/**
	 * @return {@link #getHashFunctions()} se presente, altrimenti
	 *         {@link #DEFAULT_HASH_FUNCTIONS}.
	 */
	public int getHashFunctionsOrDefault() {
		return hashFunctions != null ? hashFunctions : DEFAULT_HASH_FUNCTIONS;
	}

	/**
	 * @return {@link #getSalt()} se presente, altrimenti {@code name + "_"}
	 *         (stessa convenzione gia' usata per le colonne NCVR hardcoded).
	 */
	public String getSaltOrDefault() {
		return salt != null ? salt : name + "_";
	}

	/** @return la sezione {@code constantWeightEncoding} di questa colonna, o {@code null} se assente. */
	public ConstantWeightEncodingJsonConfig getConstantWeightEncoding() {
		return constantWeightEncoding;
	}

	/** @return {@code true} se la Constant Weight Encoding e' esplicitamente abilitata per questa colonna. */
	public boolean isConstantWeightEncodingEnabled() {
		return constantWeightEncoding != null && Boolean.TRUE.equals(constantWeightEncoding.getEnabled());
	}

	/** @return il numero di token sintetici esplicitamente configurato per un attributo vuoto, o {@code null} se omesso. */
	public Integer getMissingValueTokenCount() {
		return missingValueTokenCount;
	}

	/**
	 * @return {@link #getMissingValueTokenCount()} se presente; altrimenti
	 *         {@code constantWeightEncoding.minTrigrams} se la CWE e' abilitata
	 *         per questa colonna (stesso "peso" di un valore normale);
	 *         altrimenti {@link #DEFAULT_MISSING_VALUE_TOKEN_COUNT}.
	 */
	public int getMissingValueTokenCountOrDefault() {
		if (missingValueTokenCount != null) {
			return missingValueTokenCount;
		}
		if (isConstantWeightEncodingEnabled()) {
			return constantWeightEncoding.getMinTrigrams();
		}
		return DEFAULT_MISSING_VALUE_TOKEN_COUNT;
	}
}
