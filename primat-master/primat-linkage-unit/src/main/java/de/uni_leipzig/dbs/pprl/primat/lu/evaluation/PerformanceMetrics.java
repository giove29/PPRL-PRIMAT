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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.ComparisonStrategy;


/**
 * 
 * @author mfranke
 *
 */
public final class PerformanceMetrics {

	private PerformanceMetrics() {
		throw new RuntimeException();
	}

	@Deprecated
	public static long getMaxComparisons(boolean self, int... sizes) {
		int result = 0;

		for (int i = 0; i < sizes.length; i++) {

			for (int j = self ? 0 : 1; j < sizes.length; j++) {
				result = result + (sizes[i] * sizes[j]);
			}
		}

		return result;
	}

	public static int getNumberOfParties(Map<Party, Collection<Record>> input) {
		return input.keySet().size();
	}

	public static long getTotalNumberOfRecords(Map<Party, Collection<Record>> input) {
		int totalRecords = 0;

		for (final Collection<Record> set : input.values()) {
			totalRecords = totalRecords + set.size();
		}
		return totalRecords;
	}

	public static int getMaxComparisons(Map<Party, Collection<Record>> input, ComparisonStrategy compStrat) {
		final Set<Party> parties = input.keySet();
		final Set<PartyPair> partyPairs = compStrat.getPartyPairs(parties);

		int result = 0;

		for (final PartyPair partyPair : partyPairs) {
			final int sizeA = input.get(partyPair.getLeftParty()).size();
			if (partyPair.getLeftParty().equals(partyPair.getRightParty())) {
				// coppie non ordinate di record distinti all'interno della stessa party
				// (deduplicazione a sorgente singola): n*n conterebbe anche un record con
				// se stesso e ogni coppia due volte.
				result = result + (sizeA * (sizeA - 1) / 2);
			}
			else {
				final int sizeB = input.get(partyPair.getRightParty()).size();
				result = result + (sizeA * sizeB);
			}
		}

		return result;
	}

	public static double getAverageBlockSize(Collection<Block> blocks) {
		return blocks.stream().mapToInt(block -> block.getSize()).average().getAsDouble();
	}

	public static int getMaxBlockSize(Collection<Block> blocks) {
		return blocks.stream().mapToInt(block -> block.getSize()).max().getAsInt();
	}

	public static double getReductionRatio(Map<Party, Collection<Record>> input, ComparisonStrategy compStrat,
		int actualComparisons) {
		final int maxComps = getMaxComparisons(input, compStrat);
		return getReductionRatio(actualComparisons, maxComps);
	}

	public static double getReductionRatio(long actualComparisons, long maxComparisons) {
		return 1.0d * actualComparisons / maxComparisons;
	}

}
