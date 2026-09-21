/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Una party dichiarata nel JSON di configurazione della Linkage Unit.
 * Popolata da Gson per riflessione, {@code duplicateFree} e' opzionale
 * ({@code null} se omesso, risolto a {@code false} dal loader).
 */
public class PartyJsonConfig {

	private String name;
	private Boolean duplicateFree;

	public String getName() {
		return name;
	}

	public Boolean getDuplicateFree() {
		return duplicateFree;
	}

	public boolean isDuplicateFreeOrDefault() {
		return duplicateFree != null && duplicateFree;
	}
}
