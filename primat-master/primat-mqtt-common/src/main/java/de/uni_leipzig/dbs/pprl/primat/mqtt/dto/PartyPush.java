/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.mqtt.dto;

/**
 * Elemento del roster di party incluso in {@link LuConfigPush}: nome e tipo
 * (clean/dirty) di un Data Owner atteso per il run, cosi' come noti alla SMU.
 * Dal 2026-10-02 e' l'UNICA fonte di questa informazione per la Linkage Unit
 * (nessun campo {@code parties} resta nel suo JSON locale): validato ad ogni
 * push da {@code LinkageUnitConfigLoader#resolvePartyRoster}.
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
