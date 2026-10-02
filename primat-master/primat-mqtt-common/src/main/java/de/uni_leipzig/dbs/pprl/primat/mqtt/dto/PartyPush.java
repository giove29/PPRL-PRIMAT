/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * Elemento del roster di party incluso in {@link LuConfigPush}: nome e tipo
 * (clean/dirty) di un Data Owner atteso per il run, cosi' come noti alla SMU.
 * Mirror minimale, nel modulo wire-format (che non dipende da
 * primat-linkage-unit-service), della forma gia' usata lato Linkage Unit per
 * dichiarare i propri party nel JSON locale ({@code PartyJsonConfig}).
 */
public class PartyPush {

	private String name;
	private boolean duplicateFree;

	public PartyPush() {
	}

	public PartyPush(String name, boolean duplicateFree) {
		this.name = name;
		this.duplicateFree = duplicateFree;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isDuplicateFree() {
		return duplicateFree;
	}

	public void setDuplicateFree(boolean duplicateFree) {
		this.duplicateFree = duplicateFree;
	}
}
