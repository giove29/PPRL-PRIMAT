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

	private int index;
	private String name;
	private ColumnRole role;
	private ColumnDataType dataType;
	private Integer hashFunctions;
	private String salt;

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
}
