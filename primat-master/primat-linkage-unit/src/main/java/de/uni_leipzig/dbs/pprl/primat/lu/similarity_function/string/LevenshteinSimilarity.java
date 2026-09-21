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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_function.string;

/**
 * 
 * @author mfranke
 *
 */
public class LevenshteinSimilarity extends StringSimilarityFunction {

	public LevenshteinSimilarity() {}

	@Override
	public double calculateSim(String left, String right) {
		final int levenshteinDistance = this.getLevenshteinDistance(left, right);
		return 1.0d - (1.0d * levenshteinDistance / Math.max(left.length(), right.length()));
	}

	private int getLevenshteinDistance(String left, String right) {
		int leftLength = left.length();
		int rightLength = right.length();

		if (leftLength == 0) {
			return rightLength;
		}
		else if (rightLength == 0) {
			return leftLength;
		}

		if (leftLength > rightLength) {
			final String tmp = left;
			left = right;
			right = tmp;
			leftLength = rightLength;
			rightLength = right.length();
		}

		final int[] p = new int[leftLength + 1];

		int i, j;
		int upperLeft, upper;

		char rightJ;
		int cost;

		for (i = 0; i <= leftLength; i++) {
			p[i] = i;
		}

		for (j = 1; j <= rightLength; j++) {
			upperLeft = p[0];
			rightJ = right.charAt(j - 1);
			p[0] = j;

			for (i = 1; i <= leftLength; i++) {
				upper = p[i];
				cost = left.charAt(i - 1) == rightJ ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left
				// and up +cost
				p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upperLeft + cost);
				upperLeft = upper;
			}
		}

		return p[leftLength];
	}
}
