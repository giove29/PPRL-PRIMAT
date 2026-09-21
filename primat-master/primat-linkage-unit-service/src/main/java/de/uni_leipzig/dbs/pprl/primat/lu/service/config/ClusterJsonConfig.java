/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterBlockingKeyStrategy;
import de.uni_leipzig.dbs.pprl.primat.common.model.ClusterRepresentantStrategy;

/**
 * Sezione {@code cluster} del JSON: strategie passate a {@code ClusterFactory}.
 * Gson deserializza direttamente sui due enum del framework (case-sensitive:
 * un valore minuscolo produce un errore leggibile in
 * {@link LinkageUnitConfigLoader}, non uno stack trace grezzo).
 */
public class ClusterJsonConfig {

	private ClusterBlockingKeyStrategy blockingKeyStrategy;
	private ClusterRepresentantStrategy representantStrategy;

	public ClusterBlockingKeyStrategy getBlockingKeyStrategy() {
		return blockingKeyStrategy;
	}

	public ClusterRepresentantStrategy getRepresentantStrategy() {
		return representantStrategy;
	}
}
