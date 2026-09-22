/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.common.utils;

import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;

/**
 * Definizione condivisa di "attributo QID vuoto", usata sia dalla Constant
 * Weight Encoding ({@code ConstantWeightTrigramExtractor}) sia dal missing
 * value bucketing ({@code MissingValueBucketing}) sia da
 * {@code BloomFilterEncoder} per decidere quando bypassare l'estrazione a
 * q-gram. Valutata dopo la normalizzazione (un campo con soli spazi diventa
 * {@code ""} prima che questo controllo giri).
 */
public final class QidAttributeUtils {

	private QidAttributeUtils() {
		throw new RuntimeException();
	}

	public static boolean isEmpty(QidAttribute<?> attr) {
		return attr == null || attr.isNull() || attr.getStringValue() == null || attr.getStringValue().isBlank();
	}
}
