/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

/**
 * Dettagli della sorgente database, sezione {@code dataSource.db} del JSON.
 * Letti da {@code JdbcRecordSource} (primat-data-owner-service/.../service/io/)
 * per aprire la connessione JDBC e leggere l'intera tabella ({@code SELECT *
 * FROM tableName}).
 */
public class DbSourceConfig {

	private String tableName;
	private String jdbcUrl;
	private String username;
	private String password;

	public String getTableName() {
		return tableName;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
}
