package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Cluster;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.IdAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.PartyAttribute;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.AffinityPropagationPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

/**
 * Test di regressione per il vincolo clean-source GLOBALE, chiuso dalla
 * migrazione di MSCD-AP a un grafo N-ario unico ({@link MultiPartiteSimilarityGraph}).
 * Prima della migrazione, MSCD-AP girava per singola {@code PartyPair}: due
 * record della STESSA sorgente clean, collegati solo indirettamente tramite
 * una catena a1-B-C-a2 che attraversa due {@code PartyPair} distinte
 * (A-B e A-C, mai A-A), non comparivano mai nello stesso grafo per-coppia,
 * quindi nessun controllo poteva impedire a {@code LinkTableBuilder} di
 * fonderli nello stesso cluster finale dopo la chiusura transitiva.
 * Con il grafo unico, AffinityPropagationPostprocessor vede l'intera
 * componente connessa in un solo passo e deve tenerli separati.
 */
class AffinityPropagationCleanSourceConstraintTest {

	private static Record record(String id, Party party) {
		final Record record = new Record();
		record.setIdAttribute(new IdAttribute(id));
		record.setPartyAttribute(new PartyAttribute(party));
		return record;
	}

	@Test
	void sameCleanSourceRecordsLinkedOnlyTransitivelyStayInDifferentClusters() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", false);
		final Party partyC = new Party("C", false);

		final Record a1 = record("a1", partyA);
		final Record a2 = record("a2", partyA);
		final Record b1 = record("b1", partyB);
		final Record c1 = record("c1", partyC);

		// a1 e a2 (stessa sorgente clean A) non vengono mai confrontati
		// direttamente: sono collegati solo dalla catena transitiva
		// a1-b1-c1-a2 attraverso le sorgenti dirty B e C.
		final MultiPartiteSimilarityGraph graph = new MultiPartiteSimilarityGraph();
		graph.addEdge(a1, b1, new SimilarityVector(0.9), 0.9);
		graph.addEdge(b1, c1, new SimilarityVector(0.9), 0.9);
		graph.addEdge(c1, a2, new SimilarityVector(0.9), 0.9);

		final ApConfig apConfig = new ApConfig();
		apConfig.addCleanSource("A");

		final AffinityPropagationPostprocessor<Record> postprocessor = new AffinityPropagationPostprocessor<>(-0.1,
				-0.5, apConfig.getDampingFactor(), apConfig);
		final List<LinkedPair<Record>> matches = postprocessor.cluster(graph);

		final Set<Cluster> linkTable = LinkTableBuilder.build(matches, List.of(a1, a2, b1, c1));

		final Cluster clusterOfA1 = clusterContaining(linkTable, a1);
		final Cluster clusterOfA2 = clusterContaining(linkTable, a2);

		assertNotEquals(clusterOfA1, clusterOfA2,
				"due record della stessa sorgente clean non possono finire nello stesso cluster finale");
	}

	private static Cluster clusterContaining(Set<Cluster> linkTable, Record record) {
		return linkTable.stream().filter(c -> c.getRecords().contains(record)).findFirst()
				.orElseThrow(() -> new AssertionError("nessun cluster contiene " + record.getId()));
	}
}
