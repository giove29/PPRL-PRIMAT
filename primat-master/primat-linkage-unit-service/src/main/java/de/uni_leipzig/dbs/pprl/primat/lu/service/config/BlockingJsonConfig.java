/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.lu.service.config;

/** Sezione {@code blocking} del JSON: oggi solo JaccardLSH (MinHash), unico blocker supportato dalla pipeline. */
public class BlockingJsonConfig {

	private JaccardLshJsonConfig jaccardLsh;

	public JaccardLshJsonConfig getJaccardLsh() {
		return jaccardLsh;
	}
}
