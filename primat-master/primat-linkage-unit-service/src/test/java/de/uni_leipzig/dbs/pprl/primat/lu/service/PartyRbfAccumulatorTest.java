package de.uni_leipzig.dbs.pprl.primat.lu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.lu.service.LinkageUnitOrchestrator.PartyRbfAccumulator;
import de.uni_leipzig.dbs.pprl.primat.mqtt.dto.RbfPayload;

/**
 * Copre {@link PartyRbfAccumulator} e le funzioni di supporto usate da
 * {@code LinkageUnitOrchestrator#waitForRbf} per riassemblare gli RBF
 * pubblicati a chunk da un Data Owner (vedi {@code DataOwnerService#handleStartCommand}):
 * dedup per indice, riordino indipendente dall'ordine di arrivo MQTT (non
 * garantito), e il messaggio d'errore esplicito quando mancano chunk alla
 * ricostruzione finale.
 */
class PartyRbfAccumulatorTest {

	private static RbfPayload chunk(int index, int total, String... ids) {
		final List<RbfPayload.RbfRecord> records = List.of(ids).stream()
				.map(id -> new RbfPayload.RbfRecord(id, "g-" + id, "A", "AAA="))
				.collect(java.util.stream.Collectors.toList());
		return new RbfPayload("run-1", "A", records, "digest", index, total);
	}

	@Test
	void singleChunkPartyCompletesImmediately() {
		final PartyRbfAccumulator acc = new PartyRbfAccumulator(1);
		assertTrue(acc.addChunk(chunk(0, 1, "r1", "r2")));
		assertTrue(acc.isComplete());
		assertEquals(2, acc.assembleOrdered().size());
	}

	@Test
	void outOfOrderChunksAreReassembledInIndexOrder() {
		final PartyRbfAccumulator acc = new PartyRbfAccumulator(3);
		assertFalse(acc.addChunk(chunk(2, 3, "c")));
		assertFalse(acc.addChunk(chunk(0, 3, "a")));
		assertTrue(acc.addChunk(chunk(1, 3, "b")));
		assertTrue(acc.isComplete());
		final List<String> ids = acc.assembleOrdered().stream().map(RbfPayload.RbfRecord::getId)
				.collect(java.util.stream.Collectors.toList());
		assertEquals(List.of("a", "b", "c"), ids);
	}

	@Test
	void duplicateChunkIsIgnoredAndDoesNotDoubleCount() {
		final PartyRbfAccumulator acc = new PartyRbfAccumulator(2);
		assertFalse(acc.addChunk(chunk(0, 2, "a")));
		assertFalse(acc.addChunk(chunk(0, 2, "a-duplicate")));
		assertEquals(1, acc.receivedCount());
		assertTrue(acc.addChunk(chunk(1, 2, "b")));
		assertEquals(List.of("a", "b"),
				acc.assembleOrdered().stream().map(RbfPayload.RbfRecord::getId).collect(java.util.stream.Collectors.toList()));
	}

	@Test
	void missingIndexesListsOnlyTheGaps() {
		final PartyRbfAccumulator acc = new PartyRbfAccumulator(4);
		acc.addChunk(chunk(0, 4, "a"));
		acc.addChunk(chunk(2, 4, "c"));
		assertEquals(List.of(1, 3), acc.missingIndexes());
		assertFalse(acc.isComplete());
	}

	@Test
	void isCollectionCompleteFalseWhenAPartyNeverAppeared() {
		final Party a = new Party("A", true);
		final Party b = new Party("B", true);
		final PartyRbfAccumulator accA = new PartyRbfAccumulator(1);
		accA.addChunk(chunk(0, 1, "a"));
		final Map<String, PartyRbfAccumulator> accumulators = Map.of("A", accA);
		assertFalse(LinkageUnitOrchestrator.isCollectionComplete(List.of(a, b), accumulators));
	}

	@Test
	void buildMissingChunksMessageListsPartyAndMissingIndexes() {
		final Party a = new Party("A", true);
		final Party b = new Party("B", true);
		final PartyRbfAccumulator accA = new PartyRbfAccumulator(2);
		accA.addChunk(chunk(0, 2, "a"));
		final Map<String, PartyRbfAccumulator> accumulators = Map.of("A", accA);

		final String message = LinkageUnitOrchestrator.buildMissingChunksMessage("run-1", List.of(a, b), accumulators);

		assertTrue(message.contains("run-1"));
		assertTrue(message.contains("A (ricevuti 1/2, mancanti indici [1])"));
		assertTrue(message.contains("B (nessun chunk ricevuto)"));
	}
}
