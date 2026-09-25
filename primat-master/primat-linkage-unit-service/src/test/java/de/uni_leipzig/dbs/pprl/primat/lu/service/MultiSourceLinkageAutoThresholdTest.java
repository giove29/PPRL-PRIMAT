package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.lsh.JaccardLshKeyGenerator;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.BitSetAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.GlobalIdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.lsh.LshBlocker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.LshPassProbability;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdEstimate;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.threshold.ThresholdMode;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.markov_clustering.data_structures.MclConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.service.MultiSourceLinkage.LinkageOutcome;
import de.uni_leipzig.dbs.pprl.primat.lu.service.config.SimilarityThresholdSpec;

/** Soglia automatica end-to-end su RBF sintetici: coppie di duplicati rumorosi + non-match casuali. */
class MultiSourceLinkageAutoThresholdTest {

	private static final int BITS = 1024;
	private static final int KEY_SIZE = 3;
	private static final int KEYS = 20;

	private static Record record(String id, String globalId, Party party, BitSet bits) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setGlobalIdAttribute(new GlobalIdAttribute(globalId));
		record.setParty(party);
		record.addAttribute(new BitSetAttribute(bits));
		return record;
	}

	/** Due party dirty: ogni entita' ha un record in A e uno in B, B = A con ogni bit invertito con probabilita' {@code noise}. */
	private static Map<Party, Collection<Record>> input(long seed, int entities, double noise) {
		final Random random = new Random(seed);
		final Party a = new Party("A", false);
		final Party b = new Party("B", false);
		final List<Record> recordsA = new ArrayList<>();
		final List<Record> recordsB = new ArrayList<>();
		for (int e = 0; e < entities; e++) {
			final BitSet base = new BitSet(BITS);
			for (int i = 0; i < BITS; i++) {
				if (random.nextBoolean()) {
					base.set(i);
				}
			}
			final BitSet noisy = (BitSet) base.clone();
			for (int i = 0; i < BITS; i++) {
				if (random.nextDouble() < noise) {
					noisy.flip(i);
				}
			}
			recordsA.add(record("A" + e, "g" + e, a, base));
			recordsB.add(record("B" + e, "g" + e, b, noisy));
		}
		final Map<Party, Collection<Record>> input = new HashMap<>();
		input.put(a, recordsA);
		input.put(b, recordsB);
		return input;
	}

	private static Blocker blocker() {
		return new LshBlocker(new JaccardLshKeyGenerator(KEY_SIZE, KEYS, BITS, 42L));
	}

	private static MultiSourceLinkage linkage() {
		final MultiSourceLinkage linkage = new MultiSourceLinkage();
		linkage.setLshPassProbability(new LshPassProbability(KEY_SIZE, KEYS));
		return linkage;
	}

	@Test
	void autoThresholdIsEstimatedFromTheCandidatePairsAndLinksTheDuplicates() {
		final MultiSourceLinkage linkage = linkage();
		// debug spento (come "debug": false nel JSON): la stima della soglia non ne dipende,
		// il flag governa solo CSV e righe diagnostiche scritti dall'orchestratore
		linkage.setSimilarityHistogramEnabled(false);
		final LinkageOutcome outcome = linkage.runMcl(input(1, 300, 0.10), blocker(), new MclConfig(),
				SimilarityThresholdSpec.auto(ThresholdMode.AUTO, 0.03));

		final ThresholdEstimate estimate = linkage.getLastThresholdEstimate();
		assertNotNull(estimate);
		assertTrue(estimate.getThreshold() > 0.45 && estimate.getThreshold() < 0.80,
				"soglia stimata " + estimate.getThreshold());
		assertTrue(estimate.getReliability() >= 0.5, "affidabilita' " + estimate.getReliability());
		assertTrue(outcome.getRecall() >= 0.95, "recall " + outcome.getRecall());
		assertTrue(outcome.getPrecision() >= 0.95, "precision " + outcome.getPrecision());
		// il profilo e' l'istogramma di tutte le coppie candidate, con la risoluzione della modalita' automatica
		assertNotNull(linkage.getLastSimilarityHistogram());
		assertEquals(MultiSourceLinkage.AUTO_HISTOGRAM_BINS, linkage.getLastSimilarityHistogram().getAll().getBins());
	}

	@Test
	void epsilonMovesTheAppliedThresholdAwayFromTheKnee() {
		final MultiSourceLinkage precision = linkage();
		precision.runMcl(input(1, 300, 0.10), blocker(), new MclConfig(),
				SimilarityThresholdSpec.auto(ThresholdMode.AUTO_PRECISION, 0.05));
		final ThresholdEstimate high = precision.getLastThresholdEstimate();
		assertEquals(high.getKnee() + 0.05, high.getThreshold(), 1e-9);

		final MultiSourceLinkage recall = linkage();
		recall.runMcl(input(1, 300, 0.10), blocker(), new MclConfig(),
				SimilarityThresholdSpec.auto(ThresholdMode.AUTO_RECALL, 0.05));
		final ThresholdEstimate low = recall.getLastThresholdEstimate();
		assertEquals(low.getKnee() - 0.05, low.getThreshold(), 1e-9);
		assertEquals(high.getKnee(), low.getKnee(), 1e-9);
	}

	@Test
	void fixedThresholdSkipsTheProfilePassAndKeepsThePreviousBehaviour() {
		final MultiSourceLinkage linkage = linkage();
		final LinkageOutcome outcome = linkage.runMcl(input(1, 300, 0.10), blocker(), new MclConfig(),
				SimilarityThresholdSpec.fixed(0.6));
		assertNull(linkage.getLastThresholdEstimate());
		assertNull(linkage.getLastSimilarityHistogram());
		assertTrue(outcome.getRecall() >= 0.95, "recall " + outcome.getRecall());
	}
}
