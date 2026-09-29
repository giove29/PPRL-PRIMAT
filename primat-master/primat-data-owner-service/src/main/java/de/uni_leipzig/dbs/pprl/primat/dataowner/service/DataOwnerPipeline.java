/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram.ConstantWeightTrigramExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.RandomFactory;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.Encoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterEncoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.HashingMethod;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.RandomHashing;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.FieldNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.NormalizeDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.Preprocessor;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.NormalizerChain;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ColumnConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ColumnRole;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ConstantWeightEncodingJsonConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.PreprocessingStepFactory;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.RecordSource;

/**
 * Pipeline locale del Data Owner: legge i record dalla {@link RecordSource}
 * configurata, li pre-processa (via {@link FieldNormalizer}/
 * {@link NormalizerChain} gia' esistenti, catena esplicita per ciascuna
 * colonna QID dichiarata nel campo {@code preprocessing} del JSON di
 * configurazione, vedi {@link PreprocessingStepFactory}) e li codifica in un
 * unico Record-level Bloom
 * Filter (RBF) per record, riusando {@link BloomFilterEncoder} con una sola
 * {@link BloomFilterDefinition} che unisce gli estrattori di tutti gli
 * attributi — stesso pattern di {@code DataSources.getEncodedNCVR}, ma
 * schema/tuning ora derivati da {@link DataOwnerConfig} invece che
 * hardcoded per il solo dataset NCVR. I dati in chiaro non lasciano mai
 * questa pipeline: il chiamante riceve solo record con un
 * {@code BitSetAttribute}/{@code XorBitSetAttribute} RBF.
 */
public class DataOwnerPipeline {

	private final RecordSource recordSource;
	private final DataOwnerConfig config;

	/**
	 * @param recordSource sorgente dati locale (oggi CSV, sostituibile)
	 * @param config        configurazione validata del Data Owner (schema colonne, tuning RBF)
	 */
	public DataOwnerPipeline(RecordSource recordSource, DataOwnerConfig config) {
		this.recordSource = recordSource;
		this.config = config;
	}

	/**
	 * Costruisce lo schema di lettura a partire da {@link DataOwnerConfig#getColumns()}:
	 * un {@code NonQidAttributeType} per le colonne di ruolo PARTY/GLOBAL_ID/ID,
	 * un {@code QidAttributeType.STRING} per ogni colonna QID (la catena
	 * {@code preprocessing} dichiarata nel JSON sceglie solo la
	 * {@link NormalizerChain} in {@link #buildPreprocessor()}, non il tipo di
	 * attributo: l'estrazione trigram-based dell'RBF opera comunque sulla
	 * rappresentazione stringa).
	 *
	 * @return lo schema di lettura del CSV del party
	 */
	private RecordSchemaConfiguration buildSchema() {
		final NamedRecordSchemaConfiguration.Builder builder = new NamedRecordSchemaConfiguration.Builder();
		for (final ColumnConfig column : config.getColumns()) {
			switch (column.getRole()) {
				case PARTY:
					builder.add(column.getIndex(), NonQidAttributeType.PARTY);
					break;
				case GLOBAL_ID:
					builder.add(column.getIndex(), NonQidAttributeType.GLOBAL_ID);
					break;
				case ID:
					builder.add(column.getIndex(), NonQidAttributeType.ID);
					break;
				case QID:
					if (column.getIndex() != null) {
						builder.add(column.getIndex(), QidAttributeType.STRING, column.getName());
					}
					break;
				case RAW:
					builder.add(column.getIndex(), QidAttributeType.STRING, column.getName());
					break;
			}
		}
		return builder.build();
	}

	/**
	 * Normalizzazione degli attributi QID prima dell'estrazione delle feature
	 * per l'RBF, una {@link NormalizerChain} per colonna costruita dalla catena
	 * {@code preprocessing} esplicitamente dichiarata nel JSON di
	 * configurazione (vedi {@link PreprocessingStepFactory#build}). Opera sul
	 * {@link Record} gia' ristrutturato da {@link #applyFieldTransforms}, quindi
	 * vede solo le colonne QID finali (fisiche + virtuali).
	 *
	 * @return il preprocessor da applicare ai record letti
	 */
	private Preprocessor buildPreprocessor() {
		final NormalizeDefinition normalizeDefinition = new NormalizeDefinition("json_configured");

		// Record.getQidAttribute(int) indicizza per posizione nella lista di QID
		// attribute effettivamente presenti nel record, non per indice di colonna
		// del CSV originale (vedi il TODO "ID Column problem" in Record.java) — la
		// posizione e' quindi l'ordine di PreprocessingStepFactory.resolveFinalQidOrder(...),
		// la stessa usata per ricostruire il Record in applyFieldTransforms e per
		// buildRbfDefinition() — le tre viste devono restare sincronizzate.
		final List<ColumnConfig> finalQidColumns = resolveFinalQidColumns();

		for (int position = 0; position < finalQidColumns.size(); position++) {
			normalizeDefinition.setNormalizer(position, buildChainFor(finalQidColumns.get(position)));
		}
		return new FieldNormalizer(normalizeDefinition);
	}

	private NormalizerChain buildChainFor(ColumnConfig column) {
		return PreprocessingStepFactory.build(column.getPreprocessing());
	}

	/**
	 * @return le colonne QID finali (fisiche con {@code index}, seguite dalle
	 *         virtuali il cui primo step {@code preprocessing} e' MERGE/SPLIT),
	 *         nell'ordine di {@link PreprocessingStepFactory#resolveFinalQidOrder}
	 *         — unica fonte di verita' condivisa da {@link #buildPreprocessor()},
	 *         {@link #buildRbfDefinition()} e {@link #applyFieldTransforms}.
	 */
	private List<ColumnConfig> resolveFinalQidColumns() {
		final Map<String, ColumnConfig> qidColumnsByName = config.getColumns().stream()
				.filter(column -> column.getRole() == ColumnRole.QID)
				.collect(Collectors.toMap(ColumnConfig::getName, column -> column));
		return PreprocessingStepFactory.resolveFinalQidOrder(config.getColumns()).stream()
				.map(qidColumnsByName::get)
				.collect(Collectors.toList());
	}

	/**
	 * Esegue, su ogni record appena letto e prima di qualunque normalizzazione
	 * per-colonna, il primo step MERGE/SPLIT di ciascuna colonna QID virtuale —
	 * ristruttura lo schema QID (nomi/numero di colonne) mantenendo intatti
	 * party/id/global-id. Senza colonne virtuali e' un no-op strutturale
	 * (l'ordine finale coincide con quello fisico), eseguito comunque per
	 * uniformita' di codice.
	 */
	private void applyFieldTransforms(List<Record> records) {
		final List<String> physicalOrder = PreprocessingStepFactory.resolvePhysicalOrder(config.getColumns());
		final List<String> finalOrder = PreprocessingStepFactory.resolveFinalQidOrder(config.getColumns());
		for (final Record record : records) {
			PreprocessingStepFactory.applyFieldTransforms(record, config.getColumns(), physicalOrder, finalOrder);
		}
	}

	/**
	 * Unica {@link BloomFilterDefinition} "RBF" che unisce gli estrattori di
	 * tutte le colonne QID configurate in un solo bitset per record; numero di
	 * hash function, salt (per colonna, con default se omessi nel JSON),
	 * lunghezza e hardening sono presi da {@link DataOwnerConfig}. Ogni colonna
	 * ha il proprio {@link ConstantWeightTrigramExtractor} (con
	 * {@code enabled=false} e' equivalente byte-per-byte al vecchio
	 * {@code TrigramExtractor} condiviso), cosi' da poter applicare soglie CWE
	 * diverse per attributo; il missing-value handling (bypass dei valori
	 * vuoti verso {@code MissingValueBucketing}) e' propagato a livello di
	 * RBF/estrattore, non nell'estrattore stesso. La posizione di ciascuna
	 * colonna e' risolta localmente da {@link #resolveFinalQidColumns()}
	 * (invece di {@code ExtractorDefinition.setColumnsByName}, che dipende dal
	 * registro globale mutabile {@code RecordSchema.INSTANCE} - non affidabile
	 * per le colonne QID virtuali, che quel registro non conosce).
	 *
	 * @return la definizione di codifica RBF
	 */
	private BloomFilterDefinition buildRbfDefinition() {
		final List<BloomFilterExtractorDefinition> extractorDefinitions = new ArrayList<>();
		final List<ColumnConfig> finalQidColumns = resolveFinalQidColumns();
		for (int position = 0; position < finalQidColumns.size(); position++) {
			final ColumnConfig column = finalQidColumns.get(position);
			final boolean cweEnabled = column.isConstantWeightEncodingEnabled();
			final ConstantWeightEncodingJsonConfig cwe = column.getConstantWeightEncoding();
			final int minTrigrams = cweEnabled ? cwe.getMinTrigrams() : 0;
			final int maxTrigrams = cweEnabled ? cwe.getMaxTrigrams() : Integer.MAX_VALUE;
			final FeatureExtractor featureExtractor = new ConstantWeightTrigramExtractor(true, "_",
					column.getSaltOrDefault(), cweEnabled, minTrigrams, maxTrigrams);

			final BloomFilterExtractorDefinition extractorDefinition = new BloomFilterExtractorDefinition();
			extractorDefinition.setColumns(position);
			extractorDefinition.setExtractors(featureExtractor);
			extractorDefinition.setNumberOfHashFunctions(column.getHashFunctionsOrDefault());
			extractorDefinition.setSalt(column.getSaltOrDefault());
			extractorDefinition.setMissingValueTokenCount(column.getMissingValueTokenCountOrDefault());
			extractorDefinitions.add(extractorDefinition);
		}

		final HashingMethod hashing = new RandomHashing(config.getBloomFilterLength(), config.getHmacKey(),
				RandomFactory.SECURE_RANDOM);

		final BloomFilterDefinition rbfDefinition = new BloomFilterDefinition();
		rbfDefinition.setName("RBF");
		rbfDefinition.setBfLength(config.getBloomFilterLength());
		rbfDefinition.setHashingMethod(hashing);
		rbfDefinition.setFeatureExtractors(extractorDefinitions);
		rbfDefinition.setHardener(config.getHardener());
		rbfDefinition.setMissingValueHandlingEnabled(config.isMissingValueHandlingEnabled());
		rbfDefinition.setMissingValueAnchorPriority(config.getMissingValueAnchorPriority());
		return rbfDefinition;
	}

	/**
	 * Esegue lettura, preprocessing e codifica RBF sui dati locali del party.
	 *
	 * @return i record codificati (un solo attributo RBF ciascuno),
	 *         pronti per essere serializzati e pubblicati via MQTT
	 * @throws IOException se la lettura dalla sorgente dati fallisce
	 */
	public List<Record> run() throws IOException {
		final RecordSchemaConfiguration schema = buildSchema();
		final List<Record> records = recordSource.readAll(schema);
		prefixRecordIds(records);
		if (config.isDebug()) {
			printFirstRecords("estratti dalla sorgente dati", records);
		}

		applyFieldTransforms(records);
		if (config.isDebug()) {
			printFirstRecords("dopo le trasformazioni di schema (MERGE/SPLIT)", records);
		}

		buildPreprocessor().preprocess(records);
		if (config.isDebug()) {
			printFirstRecords("dopo la catena di normalizzazione", records);
		}

		final Encoder encoder = new BloomFilterEncoder(List.of(buildRbfDefinition()), config.isDebug(),
				config.getParty());
		return encoder.encode(records);
	}

	/** Rende gli ID univoci tra sorgenti: id pubblicato = nome party (da JSON) + id locale. */
	private void prefixRecordIds(List<Record> records) {
		for (final Record record : records) {
			record.setIdAttribute(new IdAttribute(config.getParty() + record.getId()));
		}
	}

	/**
	 * Stampa a schermo, solo se {@link DataOwnerConfig#isDebug()}, i primi 2
	 * record della lista cosi' come si trovano nel punto della pipeline
	 * indicato da {@code label}.
	 *
	 * @param label      descrizione del passo di preprocessing appena concluso
	 * @param records    record correnti (letti o normalizzati)
	 */
	private void printFirstRecords(String label, List<Record> records) {
		final int limit = Math.min(2, records.size());
		for (int i = 0; i < limit; i++) {
			final Record record = records.get(i);
			System.out.println("[" + config.getParty() + "] DEBUG " + label + " - record " + i + ": id="
					+ record.getId());
			for (final QidAttribute<?> attribute : record.getAttributes()) {
				System.out.println("[" + config.getParty() + "]   " + attribute.getStringValue());
			}
		}
	}
}
