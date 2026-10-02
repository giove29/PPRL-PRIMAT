/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * see http://www.apache.org/licenses/LICENSE-2.0
 */
package de.uni_leipzig.dbs.pprl.primat.dataowner.service;

import java.util.List;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.utils.DeterministicHashing;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.BloomFilterHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ColumnConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.ColumnRole;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataOwnerConfigLoader;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DataSourceType;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.DbSourceConfig;
import de.uni_leipzig.dbs.pprl.primat.dataowner.service.config.PreprocessingStepFactory;

/**
 * Configurazione locale di un Data Owner Service: mai trasmessa in rete,
 * caricata da un file JSON tramite {@link DataOwnerConfigLoader} e letta da
 * ciascun processo indipendentemente. Identifica il party, la sorgente dati
 * locale (colonne, ruoli, tipi), il tuning dell'RBF (lunghezza, hash
 * function/salt per colonna, hardening) e l'endpoint del broker MQTT verso
 * cui pubblicare gli RBF.
 */
public class DataOwnerConfig {

	private final String party;
	private final DataSourceType dataSourceType;
	private final String csvFilePath;
	private final boolean csvHasHeader;
	private final char csvDelimiter;
	private final DbSourceConfig dbConfig;
	private final String mqttBrokerUrl;
	private final String mqttClientId;
	private final List<ColumnConfig> columns;
	private final int bloomFilterLength;
	private final BloomFilterHardener hardener;
	private final boolean debug;
	private final boolean missingValueHandlingEnabled;
	private final List<String> missingValueAnchorPriority;
	private final List<String> hardeningDescriptions;
	private final String hmacKey;
	private final String version;
	private final boolean pending;
	private final int rbfChunkSize;

	/**
	 * Costruito esclusivamente da {@link DataOwnerConfigLoader} dopo la
	 * validazione del JSON: nessun controllo aggiuntivo viene fatto qui, questa
	 * classe e' un semplice contenitore immutabile.
	 *
	 * @param party             identificativo del party (es. "A")
	 * @param dataSourceType    tipo di sorgente dati: sia {@code CSV}
	 *                          ({@link de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.CsvRecordSource})
	 *                          sia {@code DB}
	 *                          ({@link de.uni_leipzig.dbs.pprl.primat.dataowner.service.io.JdbcRecordSource})
	 *                          sono effettivamente leggibili; {@link UnsupportedDataSourceException}
	 *                          resta solo come guardia per valori enum futuri non gestiti
	 * @param csvFilePath       percorso del CSV, {@code null} se {@code dataSourceType != CSV}
	 * @param dbConfig          dettagli della sorgente DB, {@code null} se {@code dataSourceType != DB}
	 * @param mqttBrokerUrl     endpoint del broker MQTT (es. "tcp://localhost:1883")
	 * @param columns           schema completo delle colonne, gia' validato
	 * @param bloomFilterLength lunghezza in bit dell'RBF
	 * @param hardener          tecnica di hardening da applicare all'RBF dopo la codifica
	 * @param debug             se {@code true}, la pipeline stampa a schermo i primi 2 record ad ogni passo di preprocessing
	 * @param missingValueHandlingEnabled se {@code true}, un attributo QID vuoto viene codificato tramite
	 *                                     {@code MissingValueBucketing} invece dei trigrammi di padding standard
	 * @param missingValueAnchorPriority  nomi di colonna QID in ordine di priorita' per la scelta dell'anchor,
	 *                                     vuota se {@code missingValueHandlingEnabled} e' {@code false}
	 * @param hardeningDescriptions       descrizione leggibile di ciascuno step della catena di hardening
	 *                                     (solo per {@link #describe()}), vuota se nessun hardening e' configurato
	 * @param hmacKey                     chiave HMAC usata da {@code RandomHashing} per l'hashing dei bit
	 *                                     dell'RBF, {@code null}/vuota per usare il {@code DEFAULT_KEY} di
	 *                                     fallback; configurabile via la SMU (funzionalita' "Configura i DO")
	 * @param version                     versione della configurazione applicata, cosi' come scritta nel file
	 *                                     JSON (stesso campo aggiornato da {@code DataOwnerService.handleConfigPush}
	 *                                     ad ogni {@code ConfigPush} accettata); {@code "0"} se il file non e'
	 *                                     mai stato toccato da una push
	 * @param pending                      {@code true} se questa e' una configurazione "bootstrap" (il file
	 *                                     live non esiste ancora/non e' ancora una configurazione reale, vedi
	 *                                     {@code DataOwnerConfigLoader#loadBootstrap(Path)}) - in tal caso
	 *                                     {@code columns}/{@code bloomFilterLength}/{@code hardener}/ecc. sono
	 *                                     placeholder innocui, mai letti finche' {@link #isPending()} non torna
	 *                                     {@code false}
	 * @param rbfChunkSize                 numero massimo di record RBF per messaggio MQTT pubblicato da
	 *                                     {@code DataOwnerService#handleStartCommand}; campo di competenza del
	 *                                     file "locale" (tuning di performance dell'istanza, mai pushato dalla SMU)
	 */
	public DataOwnerConfig(String party, DataSourceType dataSourceType, String csvFilePath, boolean csvHasHeader,
			char csvDelimiter, DbSourceConfig dbConfig, String mqttBrokerUrl, List<ColumnConfig> columns, int bloomFilterLength,
			BloomFilterHardener hardener, boolean debug, boolean missingValueHandlingEnabled,
			List<String> missingValueAnchorPriority, List<String> hardeningDescriptions, String hmacKey,
			String version, boolean pending, int rbfChunkSize) {
		this.party = party;
		this.dataSourceType = dataSourceType;
		this.csvFilePath = csvFilePath;
		this.csvHasHeader = csvHasHeader;
		this.csvDelimiter = csvDelimiter;
		this.dbConfig = dbConfig;
		this.mqttBrokerUrl = mqttBrokerUrl;
		this.mqttClientId = "data-owner-" + party;
		this.columns = columns;
		this.bloomFilterLength = bloomFilterLength;
		this.hardener = hardener;
		this.debug = debug;
		this.missingValueHandlingEnabled = missingValueHandlingEnabled;
		this.missingValueAnchorPriority = missingValueAnchorPriority;
		this.hardeningDescriptions = hardeningDescriptions;
		this.hmacKey = hmacKey;
		this.version = version;
		this.pending = pending;
		this.rbfChunkSize = rbfChunkSize;
	}

	public String getParty() {
		return party;
	}

	public DataSourceType getDataSourceType() {
		return dataSourceType;
	}

	public String getCsvFilePath() {
		return csvFilePath;
	}

	/** @return {@code true} se il CSV ha una riga di intestazione da saltare. */
	public boolean isCsvHasHeader() {
		return csvHasHeader;
	}

	/** @return separatore di campo del CSV. */
	public char getCsvDelimiter() {
		return csvDelimiter;
	}

	/** @return dettagli della sorgente DB (tabella/connessione), {@code null} se non configurata. */
	public DbSourceConfig getDbConfig() {
		return dbConfig;
	}

	public String getMqttBrokerUrl() {
		return mqttBrokerUrl;
	}

	public String getMqttClientId() {
		return mqttClientId;
	}

	/** @return lo schema completo delle colonne (ruolo, tipo, tuning RBF), gia' validato dal loader. */
	public List<ColumnConfig> getColumns() {
		return columns;
	}

	public int getBloomFilterLength() {
		return bloomFilterLength;
	}

	/** @return la tecnica di hardening da applicare all'RBF dopo la codifica ({@code NoHardener} se nessuna). */
	public BloomFilterHardener getHardener() {
		return hardener;
	}

	/**
	 * @return la chiave HMAC usata da {@code RandomHashing} per derivare le
	 *         posizioni di bit dell'RBF, {@code null}/vuota se non configurata
	 *         (in tal caso {@code RandomHashing} ricade sul proprio
	 *         {@code DEFAULT_KEY}). Mai trasmessa in rete.
	 */
	public String getHmacKey() {
		return hmacKey;
	}

	/** @return versione della configurazione applicata, cosi' come scritta nel file JSON ({@code "0"} se mai aggiornata da una ConfigPush). */
	public String getVersion() {
		return version;
	}

	/**
	 * @return {@code true} se il file live non esiste ancora/non e' ancora una
	 *         configurazione reale (il Data Owner e' comunque connesso e in
	 *         ascolto, ma {@code columns}/{@code bloomFilterLength}/
	 *         {@code hardener}/ecc. sono placeholder - non eseguire mai una
	 *         pipeline su una config pending, vedi {@code DataOwnerService.handleStartCommand}).
	 */
	public boolean isPending() {
		return pending;
	}

	/**
	 * @return numero massimo di record RBF per messaggio MQTT pubblicato da
	 *         {@code DataOwnerService#handleStartCommand}: un Data Owner con
	 *         molti record pubblica i propri RBF in più chunk invece di un
	 *         unico messaggio gigante (vedi {@code RbfPayload#getChunkIndex()}).
	 */
	public int getRbfChunkSize() {
		return rbfChunkSize;
	}

	/**
	 * @return la dimensione reale, in bit, dell'RBF dopo l'hardening (es. dimezzata
	 *         rispetto a {@link #getBloomFilterLength()} se {@code hardener} è uno
	 *         {@code XorFolder}) — il valore dichiarato con certezza dalla
	 *         configurazione stessa, non dedotto a posteriori dal contenuto di un
	 *         bitset ricevuto (che può sottostimarlo se i byte finali sono a zero).
	 *         Non e' piu' trasmessa alla Linkage Unit (che usa il proprio
	 *         {@code rbfSize}, spinto dalla SMU, come unica fonte di verita'):
	 *         resta utile solo per lo smoke-test locale dopo un reconfigure (vedi
	 *         {@code DataOwnerService}) e per debug/{@link #describe()}. Per il
	 *         confronto piu' severo prima di ogni persistenza, che copre anche
	 *         cambi di salt/hashFunctions/CWE, vedi {@link #computeConfigHash()}.
	 */
	public int computeEffectiveRbfBitLength() {
		return hardener.resultingLength(bloomFilterLength);
	}

	/**
	 * @return un digest deterministico (non reversibile, vedi
	 *         {@link DeterministicHashing#digestBase64(String)}) dell'intera
	 *         configurazione di encoding di questo party: lunghezza RBF,
	 *         hardening, missing-value handling, e per ogni colonna QID
	 *         preprocessing/hashFunctions/salt/CWE/missingValueTokenCount. La
	 *         stringa in chiaro che genera il digest non lascia mai questo
	 *         metodo: solo il suo hash viene trasmesso alla Linkage Unit
	 *         ({@code RbfPayload.configHash}), che puo' cosi' verificare se
	 *         la configurazione di un party e' cambiata rispetto a un run
	 *         precedente sullo stesso DB persistente
	 *         ({@code DbConnection.checkEncodingState}) senza mai vedere
	 *         salt/hashFunctions/CWE: coerente con il principio di
	 *         minimizzazione dei dati verso la Linkage Unit in un sistema PPRL.
	 */
	public String computeConfigHash() {
		final StringBuilder sb = new StringBuilder();
		sb.append("rbfLength=").append(bloomFilterLength);
		sb.append(";hardening=").append(String.join(">", hardeningDescriptions));
		sb.append(";missingValueHandling=").append(missingValueHandlingEnabled);
		if (missingValueHandlingEnabled) {
			sb.append("(anchorPriority=").append(missingValueAnchorPriority).append(')');
		}
		for (final ColumnConfig column : columns) {
			if (column.getRole() != ColumnRole.QID) {
				continue;
			}
			sb.append(";col=").append(column.getName())
					.append(",preprocessing=").append(PreprocessingStepFactory.describe(column.getPreprocessing()))
					.append(",hashFunctions=").append(column.getHashFunctionsOrDefault())
					.append(",salt=").append(column.getSaltOrDefault())
					.append(",cwe=");
			if (column.isConstantWeightEncodingEnabled()) {
				sb.append("On(minTrigrams=").append(column.getConstantWeightEncoding().getMinTrigrams())
						.append(",maxTrigrams=").append(column.getConstantWeightEncoding().getMaxTrigrams())
						.append(')');
			}
			else {
				sb.append("Off");
			}
			sb.append(",missingValueTokenCount=").append(column.getMissingValueTokenCountOrDefault());
		}
		return DeterministicHashing.digestBase64(sb.toString());
	}

	/** @return {@code true} se la pipeline deve stampare a schermo i primi 2 record ad ogni passo di preprocessing. */
	public boolean isDebug() {
		return debug;
	}

	/** @return {@code true} se un attributo QID vuoto va codificato tramite {@code MissingValueBucketing}. */
	public boolean isMissingValueHandlingEnabled() {
		return missingValueHandlingEnabled;
	}

	/** @return nomi di colonna QID in ordine di priorita' per la scelta dell'anchor (lista vuota se la tecnica e' disabilitata). */
	public List<String> getMissingValueAnchorPriority() {
		return missingValueAnchorPriority;
	}

	/**
	 * @return descrizione leggibile, riga per riga, dell'intera configurazione
	 *         ereditata dal JSON: ogni tecnica opzionale (hardening,
	 *         missing-value handling, CWE per colonna) e' etichettata "On"/"Off"
	 *         con i relativi parametri quando attiva. Pensata per essere
	 *         stampata a schermo all'avvio del Data Owner (nessun dato in
	 *         chiaro delle colonne, solo tuning/struttura).
	 */
	public String describe() {
		final StringBuilder sb = new StringBuilder();
		sb.append("==================== Data Owner [").append(party).append("] ====================\n");
		if (pending) {
			sb.append("Stato:                  NON ANCORA CONFIGURATO (in attesa della prima ConfigPush dalla SMU)\n");
		}
		sb.append("Versione:               ").append(version).append('\n');
		sb.append("Broker MQTT:            ").append(mqttBrokerUrl).append('\n');
		sb.append("Sorgente dati:          ").append(dataSourceType);
		if (dataSourceType == DataSourceType.CSV) {
			sb.append(" (file=").append(csvFilePath).append(", delimiter='").append(csvDelimiter)
					.append("', hasHeader=").append(csvHasHeader).append(')');
		}
		else if (dbConfig != null) {
			sb.append(" (table=").append(dbConfig.getTableName()).append(')');
		}
		sb.append('\n');
		sb.append("Debug:                  ").append(debug ? "On" : "Off").append('\n');
		sb.append("RBF chunk size:         ").append(rbfChunkSize).append(" record/messaggio\n");
		sb.append("RBF length:             ").append(bloomFilterLength).append(" bit\n");
		sb.append("Hardening:              ");
		if (hardeningDescriptions.isEmpty()) {
			sb.append("Off\n");
		}
		else {
			sb.append("On -> ").append(String.join(" -> ", hardeningDescriptions)).append('\n');
		}
		sb.append("Missing-value handling: ").append(missingValueHandlingEnabled ? "On" : "Off");
		if (missingValueHandlingEnabled) {
			sb.append(" (anchorPriority=").append(missingValueAnchorPriority).append(')');
		}
		sb.append('\n');
		if (pending) {
			sb.append("=======================================================");
			return sb.toString();
		}
		sb.append("Colonne QID:\n");
		for (final ColumnConfig column : columns) {
			if (column.getRole() != ColumnRole.QID) {
				continue;
			}
			sb.append("  - ").append(column.getName())
					.append(" [preprocessing=").append(PreprocessingStepFactory.describe(column.getPreprocessing()))
					.append(", hashFunctions=").append(column.getHashFunctionsOrDefault())
					.append(", salt=\"").append(column.getSaltOrDefault()).append('"')
					.append(", CWE=");
			if (column.isConstantWeightEncodingEnabled()) {
				sb.append("On (minTrigrams=").append(column.getConstantWeightEncoding().getMinTrigrams())
						.append(", maxTrigrams=").append(column.getConstantWeightEncoding().getMaxTrigrams())
						.append(')');
			}
			else {
				sb.append("Off");
			}
			sb.append(", missingValueTokenCount=").append(column.getMissingValueTokenCountOrDefault())
					.append("]\n");
		}
		sb.append("=======================================================");
		return sb.toString();
	}

	/**
	 * Versione concisa di {@link #describe()}, pensata per essere ristampata
	 * ad ogni {@link de.uni_leipzig.dbs.pprl.primat.mqtt.dto.ConfigPush}
	 * accettata (non solo all'avvio): una riga sola, senza banner ne'
	 * ripetizione per esteso dei parametri di ogni colonna.
	 *
	 * @return riepilogo su una riga della configurazione attiva
	 */
	public String describeCompact() {
		final StringBuilder sb = new StringBuilder();
		sb.append("RBF ").append(bloomFilterLength).append("bit");
		sb.append(", hardening ").append(hardeningDescriptions.isEmpty() ? "off"
				: String.join(">", hardeningDescriptions));
		sb.append(", missing-value ").append(missingValueHandlingEnabled ? "on" : "off");
		final List<ColumnConfig> qidColumns = columns.stream()
				.filter(c -> c.getRole() == ColumnRole.QID)
				.collect(Collectors.toList());
		sb.append(", ").append(qidColumns.size()).append(" colonne QID: ");
		sb.append(qidColumns.stream()
				.map(c -> c.getName() + "(h=" + c.getHashFunctionsOrDefault()
						+ (c.isConstantWeightEncodingEnabled() ? ",cwe" : "") + ")")
				.collect(Collectors.joining(", ")));
		return sb.toString();
	}
}
