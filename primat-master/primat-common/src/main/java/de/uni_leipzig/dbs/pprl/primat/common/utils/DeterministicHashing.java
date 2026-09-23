/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.Base64;

/**
 * Utility di hashing deterministico condivisa da
 * {@code ConstantWeightTrigramExtractor} (hash-ranking dei trigrammi in
 * eccesso, generazione dei filler per attributi con troppi pochi trigrammi) e
 * da {@code MissingValueBucketing} (scelta del bucket e generazione dei token
 * sintetici per attributi vuoti). Stesso stile di {@code RandomHashing}
 * (HMAC-SHA384 con chiave fissa), ma senza il passo di riseminare un
 * {@link java.util.Random}: qui serve solo un intero riproducibile per
 * ranking/bucketing, non posizioni nel Bloom Filter.
 */
public final class DeterministicHashing {

	public static final String DEFAULT_KEY = "PRIMAT_DETERMINISTIC";

	private DeterministicHashing() {
		throw new RuntimeException();
	}

	public static int toPositiveInt(String input) {
		final byte[] digest = HashUtils.getHmac(HMacAlgorithm.HMAC_SHA_384, input, DEFAULT_KEY);
		return HashUtils.toPositiveIntHash(digest, Integer.MAX_VALUE);
	}

	public static int toBucket(String input, int modulus) {
		return toPositiveInt(input) % modulus;
	}

	/**
	 * @param  input stringa qualunque da cui derivare un digest deterministico.
	 * @return       il digest HMAC-SHA384 di {@code input}, codificato in
	 *                Base64 (stesso stile di codifica gia' usato da
	 *                {@code RbfCodec} per i bitset) — un identificativo
	 *                opaco e non reversibile, utile per verificare
	 *                l'uguaglianza tra due configurazioni senza mai
	 *                trasmetterle in chiaro.
	 */
	public static String digestBase64(String input) {
		final byte[] digest = HashUtils.getHmac(HMacAlgorithm.HMAC_SHA_384, input, DEFAULT_KEY);
		return Base64.getEncoder().encodeToString(digest);
	}
}
