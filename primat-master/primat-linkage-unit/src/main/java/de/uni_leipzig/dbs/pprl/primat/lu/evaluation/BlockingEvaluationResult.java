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

/**
 * Immutable result of {@link BlockingEvaluator#evaluate}, exposing the
 * standard blocking evaluation metrics (Reduction Ratio, Pairs Completeness,
 * Pairs Quality) alongside the raw counts they were derived from.
 *
 */
public final class BlockingEvaluationResult {

	private final long candidatePairs;
	private final long trueMatchesInBlocks;
	private final double reductionRatio;
	private final double pairsCompleteness;
	private final double pairsQuality;

	BlockingEvaluationResult(long candidatePairs, long trueMatchesInBlocks, double reductionRatio,
		double pairsCompleteness, double pairsQuality) {
		this.candidatePairs = candidatePairs;
		this.trueMatchesInBlocks = trueMatchesInBlocks;
		this.reductionRatio = reductionRatio;
		this.pairsCompleteness = pairsCompleteness;
		this.pairsQuality = pairsQuality;
	}

	public long getCandidatePairs() {
		return this.candidatePairs;
	}

	public long getTrueMatchesInBlocks() {
		return this.trueMatchesInBlocks;
	}

	public double getReductionRatio() {
		return this.reductionRatio;
	}

	public double getPairsCompleteness() {
		return this.pairsCompleteness;
	}

	public double getPairsQuality() {
		return this.pairsQuality;
	}

	@Override
	public String toString() {
		return "BlockingEvaluationResult{" + "candidatePairs=" + this.candidatePairs + ", trueMatchesInBlocks="
			+ this.trueMatchesInBlocks + ", reductionRatio=" + this.reductionRatio + ", pairsCompleteness="
			+ this.pairsCompleteness + ", pairsQuality=" + this.pairsQuality + '}';
	}
}
