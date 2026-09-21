/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/**
 * Sezione {@code database} del JSON: credenziali del Postgres dedicato alla
 * strategia scelta. Obbligatoria se {@code clusteringMethod != MCL}, nessun
 * default (mai scrivere per sbaglio sul DB di qualcun altro).
 */
public class DatabaseJsonConfig {

	private String url;
	private String user;
	private String password;

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
}
