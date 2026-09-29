/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.StringAttribute;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging.BlankMerger;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging.DotMerger;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging.Merger;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging.SimpleMerger;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.merging.SlashMerger;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.AccentRemover;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.NonDigitRemover;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.Normalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.NormalizerChain;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.SpecialCharacterRemover;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.SubstringNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.TrimNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.UpperCaseNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.BlankSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.CommaSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.DotSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.HyphenSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.PositionSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.PunctuationSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.RegexSplitter;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting.Splitter;

/**
 * Unica fonte di verita' per la catena {@code preprocessing} di una colonna
 * QID: valida gli step dichiarati nel JSON ({@link #validateColumnSteps}/
 * {@link #validateCrossColumnConsistency}), li traduce nella corrispondente
 * {@link NormalizerChain} eseguita dalla pipeline ({@link #build}), li
 * descrive in forma canonica per il digest ({@link #describe}), e - per il
 * primo step MERGE/SPLIT di una colonna QID "virtuale" (senza {@code index})
 * - calcola il suo valore iniziale da altre colonne {@code RAW}
 * ({@link #applyFieldTransforms}), riusando direttamente le implementazioni
 * esistenti di {@link Merger}/{@link Splitter} (gia' presenti in
 * {@code preprocessing.merging}/{@code preprocessing.splitting}) - non
 * duplica logica di trasformazione stringa, e non tocca ne' riusa il driver
 * legacy {@code FieldMerger}/{@code FieldSplitter}/{@code RecordSchema.INSTANCE}
 * (incompatibile col modello JSON-driven, e in parte rotto: vedi
 * {@code RecordSchema.merge()}/{@code .remove()}, che chiamano
 * {@code TreeBidiMap.replaceAll(...)} - non supportato, causa nota del
 * fallimento di {@code FieldMergerTest}). Tutte le viste devono restare
 * consistenti tra loro, quindi vivono in un solo posto invece che duplicate
 * in {@code DataOwnerConfigLoader}/{@code DataOwnerPipeline}/
 * {@code DataOwnerConfig}.
 */
public final class PreprocessingStepFactory {

	private static final String TRIM = "TRIM";
	private static final String UPPERCASE = "UPPERCASE";
	private static final String REMOVE_ACCENTS = "REMOVE_ACCENTS";
	private static final String REMOVE_SPECIAL_CHARS = "REMOVE_SPECIAL_CHARS";
	private static final String REMOVE_NON_DIGITS = "REMOVE_NON_DIGITS";
	private static final String TRUNCATE = "TRUNCATE";
	private static final String MERGE = "MERGE";
	private static final String SPLIT = "SPLIT";

	private static final Set<String> KNOWN_TYPES = Set.of(TRIM, UPPERCASE, REMOVE_ACCENTS, REMOVE_SPECIAL_CHARS,
			REMOVE_NON_DIGITS, TRUNCATE, MERGE, SPLIT);
	private static final Set<String> MERGER_TYPES = Set.of("BLANK", "DOT", "SLASH", "SIMPLE");
	private static final Set<String> SPLITTER_TYPES = Set.of("BLANK", "COMMA", "DOT", "HYPHEN", "PUNCTUATION",
			"POSITION", "REGEX");

	private PreprocessingStepFactory() {
	}

	private static boolean isFieldTransformType(String type) {
		return MERGE.equals(type) || SPLIT.equals(type);
	}

	// --------------------------------------------------------------------
	// Validazione
	// --------------------------------------------------------------------

	/**
	 * @throws DataOwnerConfigException se la lista e' nulla/vuota, uno step ha
	 *                                   un {@code type} sconosciuto, una colonna
	 *                                   virtuale (senza {@code index}) non ha
	 *                                   MERGE/SPLIT come primo step (o una
	 *                                   colonna fisica ce l'ha), MERGE/SPLIT
	 *                                   compare oltre il primo step, o i
	 *                                   parametri di {@code TRUNCATE}/MERGE/SPLIT
	 *                                   non sono coerenti col tipo dichiarato
	 */
	public static void validateColumnSteps(List<PreprocessingStepJsonConfig> steps, ColumnConfig column,
			Path jsonPath) throws DataOwnerConfigException {
		if (steps == null || steps.isEmpty()) {
			throw new DataOwnerConfigException("Colonna QID '" + column.getName()
					+ "' priva del campo 'preprocessing' obbligatorio (lista di step) in " + jsonPath);
		}
		final boolean virtual = column.getIndex() == null;
		final boolean firstIsTransform = isFieldTransformType(steps.get(0).getType());
		if (virtual && !firstIsTransform) {
			throw new DataOwnerConfigException("Colonna QID '" + column.getName()
					+ "' non ha 'index' (colonna virtuale): il primo step di 'preprocessing' deve essere MERGE o SPLIT in "
					+ jsonPath);
		}
		if (!virtual && firstIsTransform) {
			throw new DataOwnerConfigException("Colonna QID '" + column.getName()
					+ "' ha 'index' (colonna fisica): non puo' avere uno step MERGE/SPLIT, solo una colonna QID senza 'index' puo' averlo in "
					+ jsonPath);
		}

		for (int i = 0; i < steps.size(); i++) {
			final PreprocessingStepJsonConfig step = steps.get(i);
			if (step.getType() == null || !KNOWN_TYPES.contains(step.getType())) {
				throw new DataOwnerConfigException("Colonna QID '" + column.getName()
						+ "' ha uno step di 'preprocessing' sconosciuto: '" + step.getType() + "' in " + jsonPath);
			}
			if (i > 0 && isFieldTransformType(step.getType())) {
				throw new DataOwnerConfigException("Colonna QID '" + column.getName()
						+ "': MERGE/SPLIT puo' comparire solo come primo step di 'preprocessing' in " + jsonPath);
			}
			if (TRUNCATE.equals(step.getType()) && (step.getFrom() == null || step.getTo() == null)) {
				throw new DataOwnerConfigException(
						"Colonna QID '" + column.getName() + "' ha uno step TRUNCATE privo di 'from'/'to' in "
								+ jsonPath);
			}
			if (MERGE.equals(step.getType())) {
				validateMergeStep(step, column.getName(), jsonPath);
			}
			if (SPLIT.equals(step.getType())) {
				validateSplitStep(step, column.getName(), jsonPath);
			}
		}
	}

	private static void validateMergeStep(PreprocessingStepJsonConfig step, String columnName, Path jsonPath)
			throws DataOwnerConfigException {
		if (step.getSources() == null || step.getSources().size() < 2) {
			throw new DataOwnerConfigException(
					"Colonna QID '" + columnName + "': lo step MERGE richiede almeno 2 'sources' in " + jsonPath);
		}
		validateMerger(step.getMerger(), columnName, jsonPath);
	}

	private static void validateSplitStep(PreprocessingStepJsonConfig step, String columnName, Path jsonPath)
			throws DataOwnerConfigException {
		if (step.getSource() == null || step.getSource().isBlank()) {
			throw new DataOwnerConfigException(
					"Colonna QID '" + columnName + "': lo step SPLIT richiede 'source' in " + jsonPath);
		}
		if (step.getParts() == null || step.getParts() < 2) {
			throw new DataOwnerConfigException(
					"Colonna QID '" + columnName + "': lo step SPLIT richiede 'parts' >= 2 in " + jsonPath);
		}
		if (step.getPart() == null || step.getPart() < 0 || step.getPart() >= step.getParts()) {
			throw new DataOwnerConfigException(
					"Colonna QID '" + columnName + "': lo step SPLIT richiede 'part' in [0, parts) in " + jsonPath);
		}
		validateSplitter(step.getSplitter(), columnName, jsonPath);
		if (step.getSplitter() != null && "POSITION".equals(step.getSplitter().getType()) && step.getParts() != 2) {
			throw new DataOwnerConfigException("Colonna QID '" + columnName
					+ "': lo splitter POSITION produce sempre esattamente 2 parti, 'parts' dichiarato = "
					+ step.getParts() + " in " + jsonPath);
		}
	}

	private static void validateMerger(MergerJsonConfig merger, String columnName, Path jsonPath)
			throws DataOwnerConfigException {
		if (merger == null || merger.getType() == null || !MERGER_TYPES.contains(merger.getType())) {
			throw new DataOwnerConfigException("Colonna QID '" + columnName + "': 'merger.type' sconosciuto: '"
					+ (merger != null ? merger.getType() : null) + "' in " + jsonPath);
		}
		if ("SIMPLE".equals(merger.getType()) && (merger.getSeparator() == null || merger.getSeparator().isEmpty())) {
			throw new DataOwnerConfigException("Colonna QID '" + columnName
					+ "': 'merger.type=SIMPLE' richiede 'separator' non vuoto in " + jsonPath);
		}
	}

	private static void validateSplitter(SplitterJsonConfig splitter, String columnName, Path jsonPath)
			throws DataOwnerConfigException {
		if (splitter == null || splitter.getType() == null || !SPLITTER_TYPES.contains(splitter.getType())) {
			throw new DataOwnerConfigException("Colonna QID '" + columnName + "': 'splitter.type' sconosciuto: '"
					+ (splitter != null ? splitter.getType() : null) + "' in " + jsonPath);
		}
		if ("POSITION".equals(splitter.getType())
				&& (splitter.getPosition() == null || splitter.getPosition() < 0)) {
			throw new DataOwnerConfigException("Colonna QID '" + columnName
					+ "': 'splitter.type=POSITION' richiede 'position' non negativo in " + jsonPath);
		}
		if ("REGEX".equals(splitter.getType()) && (splitter.getPattern() == null || splitter.getPattern().isEmpty())) {
			throw new DataOwnerConfigException(
					"Colonna QID '" + columnName + "': 'splitter.type=REGEX' richiede 'pattern' non vuoto in "
							+ jsonPath);
		}
	}

	/**
	 * Verifica cross-column: ogni colonna {@code RAW} deve essere referenziata
	 * o da esattamente uno step MERGE (mai da piu' di uno, mai insieme a uno
	 * SPLIT), oppure coperta esattamente da un gruppo di step SPLIT che
	 * condividono lo stesso {@code source} - stesso {@code splitter}/
	 * {@code parts}, {@code part} tutte diverse, che coprano {@code [0,parts)}.
	 *
	 * @throws DataOwnerConfigException se una colonna RAW non e' referenziata,
	 *                                   e' referenziata in modo ambiguo/incoerente,
	 *                                   o uno step referenzia una sorgente che
	 *                                   non e' una colonna {@code RAW} dichiarata
	 */
	public static void validateCrossColumnConsistency(List<ColumnConfig> columns, Path jsonPath)
			throws DataOwnerConfigException {
		final Set<String> rawNames = columns.stream().filter(column -> column.getRole() == ColumnRole.RAW)
				.map(ColumnConfig::getName).collect(Collectors.toSet());

		final Map<String, String> mergeConsumedBy = new HashMap<>();
		final Map<String, SplitGroup> splitGroups = new HashMap<>();

		for (final ColumnConfig column : columns) {
			if (column.getRole() != ColumnRole.QID || column.getIndex() != null) {
				continue;
			}
			final List<PreprocessingStepJsonConfig> steps = column.getPreprocessing();
			if (steps == null || steps.isEmpty()) {
				continue; // gia' segnalato da validateColumnSteps
			}
			final PreprocessingStepJsonConfig first = steps.get(0);
			if (MERGE.equals(first.getType()) && first.getSources() != null) {
				for (final String sourceName : first.getSources()) {
					requireRawName(sourceName, rawNames, column.getName(), jsonPath);
					if (mergeConsumedBy.containsKey(sourceName) || splitGroups.containsKey(sourceName)) {
						throw new DataOwnerConfigException("Colonna RAW '" + sourceName
								+ "' referenziata da piu' di uno step MERGE/SPLIT in " + jsonPath);
					}
					mergeConsumedBy.put(sourceName, column.getName());
				}
			}
			else if (SPLIT.equals(first.getType()) && first.getSource() != null) {
				final String sourceName = first.getSource();
				requireRawName(sourceName, rawNames, column.getName(), jsonPath);
				if (mergeConsumedBy.containsKey(sourceName)) {
					throw new DataOwnerConfigException(
							"Colonna RAW '" + sourceName + "' referenziata sia da MERGE che da SPLIT in " + jsonPath);
				}
				final String description = first.getSplitter() != null && first.getParts() != null
						? first.getParts() + "/" + describeSplitter(first.getSplitter())
						: null;
				final SplitGroup group = splitGroups.computeIfAbsent(sourceName,
						key -> new SplitGroup(description, first.getParts()));
				if (description != null && !description.equals(group.description)) {
					throw new DataOwnerConfigException("Colonna RAW '" + sourceName
							+ "': i frammenti SPLIT dichiarano 'splitter'/'parts' incoerenti tra loro in " + jsonPath);
				}
				if (first.getPart() != null && !group.partsSeen.add(first.getPart())) {
					throw new DataOwnerConfigException("Colonna RAW '" + sourceName + "': 'part'=" + first.getPart()
							+ " dichiarata da piu' di una colonna in " + jsonPath);
				}
			}
		}

		for (final String rawName : rawNames) {
			final boolean mergedFully = mergeConsumedBy.containsKey(rawName);
			final SplitGroup group = splitGroups.get(rawName);
			final boolean splitFully = group != null && group.parts != null
					&& group.partsSeen.size() == group.parts;
			if (mergedFully || splitFully) {
				continue;
			}
			if (group != null) {
				throw new DataOwnerConfigException("Colonna RAW '" + rawName + "': lo SPLIT non copre tutte le "
						+ group.parts + " parti (trovate " + group.partsSeen.size() + ") in " + jsonPath);
			}
			throw new DataOwnerConfigException(
					"Colonna RAW '" + rawName + "' non e' referenziata da nessuno step MERGE/SPLIT in " + jsonPath);
		}
	}

	private static void requireRawName(String name, Set<String> rawNames, String columnName, Path jsonPath)
			throws DataOwnerConfigException {
		if (!rawNames.contains(name)) {
			throw new DataOwnerConfigException("Colonna QID '" + columnName + "' referenzia '" + name
					+ "' come sorgente, ma non e' una colonna 'role: RAW' dichiarata in " + jsonPath);
		}
	}

	private static final class SplitGroup {
		private final String description;
		private final Integer parts;
		private final Set<Integer> partsSeen = new HashSet<>();

		private SplitGroup(String description, Integer parts) {
			this.description = description;
			this.parts = parts;
		}
	}

	// --------------------------------------------------------------------
	// Costruzione della NormalizerChain (salta un eventuale MERGE/SPLIT iniziale,
	// gia' "eseguito" dal pre-pass di applyFieldTransforms)
	// --------------------------------------------------------------------

	/** @return la catena di {@link Normalizer} corrispondente agli step normalizzatori, gia' validati. */
	public static NormalizerChain build(List<PreprocessingStepJsonConfig> steps) {
		final List<Normalizer> normalizers = new ArrayList<>();
		final int start = !steps.isEmpty() && isFieldTransformType(steps.get(0).getType()) ? 1 : 0;
		for (int i = start; i < steps.size(); i++) {
			normalizers.add(toNormalizer(steps.get(i)));
		}
		return new NormalizerChain(normalizers);
	}

	private static Normalizer toNormalizer(PreprocessingStepJsonConfig step) {
		switch (step.getType()) {
			case TRIM:
				return new TrimNormalizer();
			case UPPERCASE:
				return new UpperCaseNormalizer();
			case REMOVE_ACCENTS:
				return new AccentRemover();
			case REMOVE_SPECIAL_CHARS:
				return new SpecialCharacterRemover();
			case REMOVE_NON_DIGITS:
				return new NonDigitRemover();
			case TRUNCATE:
				return new SubstringNormalizer(step.getFrom(), step.getTo());
			default:
				throw new IllegalStateException("Step di preprocessing sconosciuto: " + step.getType());
		}
	}

	// --------------------------------------------------------------------
	// Ordine delle colonne QID finali + esecuzione di MERGE/SPLIT su un Record
	// --------------------------------------------------------------------

	/**
	 * @return colonne QID fisiche (con {@code index}) ordinate per index, seguite
	 *         dalle colonne QID virtuali (senza {@code index}) nell'ordine in cui
	 *         compaiono in {@code columns[]} - l'unico ordine usato da
	 *         {@code DataOwnerPipeline.buildPreprocessor()}/{@code buildRbfDefinition()}
	 *         e da {@link #applyFieldTransforms}, cosi' che le tre viste restino
	 *         sempre allineate.
	 */
	public static List<String> resolveFinalQidOrder(List<ColumnConfig> columns) {
		final List<String> order = columns.stream()
				.filter(column -> column.getRole() == ColumnRole.QID && column.getIndex() != null)
				.sorted(Comparator.comparingInt(ColumnConfig::getIndex))
				.map(ColumnConfig::getName)
				.collect(Collectors.toCollection(ArrayList::new));
		for (final ColumnConfig column : columns) {
			if (column.getRole() == ColumnRole.QID && column.getIndex() == null) {
				order.add(column.getName());
			}
		}
		return order;
	}

	/**
	 * @return nomi delle colonne {@code QID}+{@code RAW} fisiche (con
	 *         {@code index}), nell'ordine in cui {@code CsvRecordSource}/
	 *         {@code JdbcRecordSource} le legge in un {@link Record} appena
	 *         letto - la vista "prima della trasformazione" consumata da
	 *         {@link #applyFieldTransforms}.
	 */
	public static List<String> resolvePhysicalOrder(List<ColumnConfig> columns) {
		return columns.stream()
				.filter(column -> (column.getRole() == ColumnRole.QID || column.getRole() == ColumnRole.RAW)
						&& column.getIndex() != null)
				.sorted(Comparator.comparingInt(ColumnConfig::getIndex))
				.map(ColumnConfig::getName)
				.collect(Collectors.toList());
	}

	/**
	 * Calcola il valore iniziale di ogni colonna QID virtuale (dal suo primo
	 * step MERGE/SPLIT, leggendo le colonne {@code RAW} appena lette) e
	 * ricostruisce da zero la lista di attributi del {@link Record}
	 * nell'ordine di {@code finalOrder} - niente manipolazioni incrementali di
	 * indici (evita la classe di bug di {@code RecordSchema.merge()}/
	 * {@code .remove()}).
	 */
	public static void applyFieldTransforms(Record record, List<ColumnConfig> columns, List<String> physicalOrder,
			List<String> finalOrder) {
		final Map<String, String> valuesByName = new HashMap<>();
		final List<QidAttribute<?>> physicalAttributes = record.getAttributes();
		for (int i = 0; i < physicalOrder.size(); i++) {
			valuesByName.put(physicalOrder.get(i), physicalAttributes.get(i).getStringValue());
		}

		final Map<String, ColumnConfig> virtualColumnsByName = columns.stream()
				.filter(column -> column.getRole() == ColumnRole.QID && column.getIndex() == null)
				.collect(Collectors.toMap(ColumnConfig::getName, column -> column));

		for (final String name : finalOrder) {
			final ColumnConfig virtualColumn = virtualColumnsByName.get(name);
			if (virtualColumn == null) {
				continue; // colonna fisica, il valore e' gia' in valuesByName
			}
			valuesByName.put(name, computeVirtualValue(virtualColumn.getPreprocessing().get(0), valuesByName));
		}

		record.setAttributes(new ArrayList<>());
		for (final String name : finalOrder) {
			record.addQidAttribute(new StringAttribute(valuesByName.get(name)));
		}
	}

	private static String computeVirtualValue(PreprocessingStepJsonConfig step, Map<String, String> valuesByName) {
		if (MERGE.equals(step.getType())) {
			final List<String> sources = step.getSources();
			final String[] values = new String[sources.size()];
			for (int i = 0; i < values.length; i++) {
				final String value = valuesByName.get(sources.get(i));
				values[i] = value != null ? value : "";
			}
			return buildMerger(step.getMerger()).merge(values);
		}
		final String sourceValue = valuesByName.get(step.getSource());
		if (sourceValue == null || sourceValue.isEmpty()) {
			return "";
		}
		final String[] parts = buildSplitter(step.getSplitter(), step.getParts()).split(sourceValue);
		final int part = step.getPart();
		return part < parts.length && parts[part] != null ? parts[part] : "";
	}

	private static Merger buildMerger(MergerJsonConfig merger) {
		switch (merger.getType()) {
			case "BLANK":
				return new BlankMerger();
			case "DOT":
				return new DotMerger();
			case "SLASH":
				return new SlashMerger();
			case "SIMPLE":
				return new SimpleMerger(merger.getSeparator());
			default:
				throw new IllegalStateException("Tipo di merger sconosciuto: " + merger.getType());
		}
	}

	private static Splitter buildSplitter(SplitterJsonConfig splitter, int parts) {
		switch (splitter.getType()) {
			case "BLANK":
				return new BlankSplitter(parts);
			case "COMMA":
				return new CommaSplitter(parts);
			case "DOT":
				return new DotSplitter(parts);
			case "HYPHEN":
				return new HyphenSplitter(parts);
			case "PUNCTUATION":
				return new PunctuationSplitter(parts);
			case "POSITION":
				return new PositionSplitter(splitter.getPosition());
			case "REGEX":
				return new RegexSplitter(splitter.getPattern(), parts);
			default:
				throw new IllegalStateException("Tipo di splitter sconosciuto: " + splitter.getType());
		}
	}

	// --------------------------------------------------------------------
	// Descrizione canonica per il digest
	// --------------------------------------------------------------------

	/**
	 * @return descrizione canonica per il digest, es.
	 *         {@code "TRIM>UPPERCASE>TRUNCATE(0,20)"} o
	 *         {@code "MERGE(sources=[a, b],merger=BLANK)>TRIM"}.
	 */
	public static String describe(List<PreprocessingStepJsonConfig> steps) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < steps.size(); i++) {
			if (i > 0) {
				sb.append('>');
			}
			sb.append(describeStep(steps.get(i)));
		}
		return sb.toString();
	}

	private static String describeStep(PreprocessingStepJsonConfig step) {
		if (TRUNCATE.equals(step.getType())) {
			return TRUNCATE + "(" + step.getFrom() + "," + step.getTo() + ")";
		}
		if (MERGE.equals(step.getType())) {
			return MERGE + "(sources=" + step.getSources() + ",merger=" + describeMerger(step.getMerger()) + ")";
		}
		if (SPLIT.equals(step.getType())) {
			return SPLIT + "(source=" + step.getSource() + ",splitter=" + describeSplitter(step.getSplitter())
					+ ",parts=" + step.getParts() + ",part=" + step.getPart() + ")";
		}
		return step.getType();
	}

	private static String describeMerger(MergerJsonConfig merger) {
		if ("SIMPLE".equals(merger.getType())) {
			return merger.getType() + "(separator=" + merger.getSeparator() + ")";
		}
		return merger.getType();
	}

	private static String describeSplitter(SplitterJsonConfig splitter) {
		if ("POSITION".equals(splitter.getType())) {
			return splitter.getType() + "(position=" + splitter.getPosition() + ")";
		}
		if ("REGEX".equals(splitter.getType())) {
			return splitter.getType() + "(pattern=" + splitter.getPattern() + ")";
		}
		return splitter.getType();
	}
}
