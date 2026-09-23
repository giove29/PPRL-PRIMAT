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
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy;

class PerformanceMetricsTest {

	private static List<Record> records(int count) {
		final List<Record> records = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			records.add(new Record());
		}
		return records;
	}

	/**
	 * 3 party (A clean/3 record, B dirty/4 record, C dirty/2 record):
	 * atteso cross(A,B)=12 + cross(A,C)=6 + cross(B,C)=8 + within(B)=6 +
	 * within(C)=1 = 33 — nessun termine within per la party clean A.
	 */
	@Test
	void getMaxComparisonsWithDirtyAwareCountsWithinPartyOnlyForDirtyParties() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", false);
		final Party partyC = new Party("C", false);

		final Map<Party, Collection<Record>> input = new LinkedHashMap<>();
		input.put(partyA, records(3));
		input.put(partyB, records(4));
		input.put(partyC, records(2));

		final int maxComparisons = PerformanceMetrics.getMaxComparisons(input, ComparisonStrategy.DIRTY_AWARE);

		assertEquals(33, maxComparisons);
	}

	@Test
	void getMaxComparisonsWithDirtyAwareIsAllCrossPartyWhenAllClean() {
		final Party partyA = new Party("A", true);
		final Party partyB = new Party("B", true);

		final Map<Party, Collection<Record>> input = new LinkedHashMap<>();
		input.put(partyA, records(5));
		input.put(partyB, records(7));

		final int maxComparisons = PerformanceMetrics.getMaxComparisons(input, ComparisonStrategy.DIRTY_AWARE);

		assertEquals(35, maxComparisons);
	}
}
