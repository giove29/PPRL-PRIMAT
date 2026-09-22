/*******************************************************************************
 *  Copyright © 2017 - 2022 Leipzig University (Database Research Group)
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under 
 * the License.
 *******************************************************************************/
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtraction;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.utils.QidAttributeUtils;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.Encoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.BloomFilterHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.HashingMethod;


/**
 * Bloom-filter-based encoding of sensitive information.
 * 
 * The basic approach is that relevant attribute values are transformed into a
 * set of features. These features are (hash) mapped into one or several Bloom
 * filters, e.g. one Bloom filter for each attribute value or one Bloom filter
 * for <underline>all</underline> attribute values.
 * 
 * @author mfranke
 *
 */
public final class BloomFilterEncoder implements Encoder {

	private final List<BloomFilterDefinition> bfDefs;
	private final boolean debug;
	private final String partyLabel;
	private int debugRecordsPrinted = 0;

	/**
	 * Constructs a new encoder.
	 *
	 * @param bfDefs a list of {@link BloomFilterDefinition} objects that define
	 *               how and how many Bloom filters are constructed.
	 */
	public BloomFilterEncoder(List<BloomFilterDefinition> bfDefs) {
		this(bfDefs, false, null);
	}

	/**
	 * Constructs a new encoder with optional debug printing of the first 2
	 * records processed (feature extraction with padding, RBF pre-hardening).
	 *
	 * @param bfDefs     a list of {@link BloomFilterDefinition} objects that define
	 *                   how and how many Bloom filters are constructed.
	 * @param debug      se {@code true}, stampa a schermo le feature paddate e l'RBF
	 *                   grezzo (pre-hardening) dei primi 2 record codificati.
	 * @param partyLabel etichetta del party da anteporre alle righe di debug.
	 */
	public BloomFilterEncoder(List<BloomFilterDefinition> bfDefs, boolean debug, String partyLabel) {
		this.bfDefs = bfDefs;
		this.debug = debug;
		this.partyLabel = partyLabel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Record encode(Record record) {
		final Record encodedRec = new Record();
		encodedRec.setIdAttribute(record.getIdAttribute());
		encodedRec.setGlobalIdAttribute(record.getGlobalIdAttribute());
		encodedRec.setPartyAttribute(record.getPartyAttribute());;

		final boolean printThis = debug && debugRecordsPrinted < 2;

		for (final BloomFilterDefinition bfDef : bfDefs) {
			this.buildBloomFilter(bfDef, record, encodedRec, printThis);
		}

		if (printThis) {
			debugRecordsPrinted++;
		}

		return encodedRec;
	}

	private void buildBloomFilter(BloomFilterDefinition bfDef, Record record, Record encodedRecord,
			boolean printThis) {
		final int bfLength = bfDef.getBfLength();
		final BloomFilter bf = new BloomFilter(bfLength);

		final Set<Integer> positions = this.getBloomFilterPositions(bfDef, record, printThis);
		bf.setPositions(positions);

		if (printThis) {
			System.out.println("[" + partyLabel + "] DEBUG RBF (pre-hardening) - record " + record.getId() + ": "
					+ bf);
		}

		final BloomFilterHardener hardener = bfDef.getHardener();
		final QidAttribute<?> hardBf = hardener.harden(bf);
		encodedRecord.addQidAttribute(hardBf);
	}

	private Set<Integer> getBloomFilterPositions(BloomFilterDefinition bfDef, Record record, boolean printThis) {
		final List<BloomFilterExtractorDefinition> exDefs = bfDef.getFeatureExtractors();
		final HashingMethod hashingMethod = bfDef.getHashingMethod();
		final Set<Integer> positions = new HashSet<>();

		final String recordSalt;

		if (bfDef.hasRecordSalt()) {
			final int recSaltCol = bfDef.getRecordSaltColumn();
			recordSalt = record.getQidAttribute(recSaltCol).getStringValue();
		}
		else {
			recordSalt = "";
		}

		for (final BloomFilterExtractorDefinition exDef : exDefs) {
			final Set<String> features = new HashSet<String>(extractFeatures(bfDef, exDef, record));
			final int hashFunctions = exDef.getNumberOfHashFunctions();

			if (printThis) {
				System.out.println("[" + partyLabel + "] DEBUG feature paddate (colonne " + exDef.getColumns() + ") - record "
						+ record.getId() + ": " + features);
			}

			final String attributeSalt = exDef.getSalt();
			final String salt = attributeSalt + recordSalt;

			hashingMethod.setSalt(salt);
			final Set<Integer> currentPositionSet = hashingMethod.hash(features, hashFunctions);
			positions.addAll(currentPositionSet);
		}

		return positions;
	}

	/**
	 * Estrae le feature per un attributo: se il missing-value handling e'
	 * abilitato su questo {@link BloomFilterDefinition} e l'attributo di
	 * {@code exDef} risulta vuoto (dopo normalizzazione), bypassa del tutto
	 * l'estrazione a q-gram e genera i token sintetici di
	 * {@link MissingValueBucketing} al suo posto. Il bypass si applica solo
	 * per un {@code exDef} con esattamente una colonna (sempre il caso per le
	 * colonne prodotte da {@code DataOwnerPipeline.buildRbfDefinition()}); un
	 * futuro extractor multi-colonna disabilita silenziosamente la tecnica per
	 * quell'attributo, senza eccezioni.
	 */
	private List<String> extractFeatures(BloomFilterDefinition bfDef, BloomFilterExtractorDefinition exDef,
			Record record) {
		if (bfDef.isMissingValueHandlingEnabled() && exDef.getColumns().size() == 1) {
			final QidAttribute<?> attr = record.getQidAttribute(exDef.getColumns().get(0));
			if (QidAttributeUtils.isEmpty(attr)) {
				return MissingValueBucketing.generateTokens(record, bfDef.getMissingValueAnchorPriority(),
						exDef.getMissingValueTokenCount());
			}
		}
		return new FeatureExtraction(exDef).getFeatures(record);
	}

	public List<String> getSchema() {
		final List<String> schema = new ArrayList<>();
		schema.add("ID");
		schema.add("GID");
		schema.add("PARTY");

		for (final BloomFilterDefinition def : this.bfDefs) {
			final String name = def.getName();
			schema.add(name);
		}

		return schema;
	}
}