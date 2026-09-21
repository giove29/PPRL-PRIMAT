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

import java.util.Arrays;

/**
 * 
 * @author mfranke
 *
 */
public class JaroWinklerSimilarity extends StringSimilarityFunction {

	public JaroWinklerSimilarity() {}

	@Override
	protected double calculateSim(String left, String right) {

		final int[] mtp = this.matches(left, right);
		final double m = mtp[0];

		if (m == 0) {
			return 0d;
		}

		final double j = ((m / left.length() + m / right.length() + (m - (double) mtp[1] / 2) / m)) / 3;
		final double jw = j < 0.7d ? j : j + 0.1d * mtp[2] * (1d - j);

		return jw;
	}

	private int[] matches(String first, String second) {
		final String max;
		final String min;

		if (first.length() > second.length()) {
			max = first;
			min = second;
		}
		else {
			max = second;
			min = first;
		}

		final int range = Math.max(max.length() / 2 - 1, 0);
		final int[] matchIndexes = new int[min.length()];
		Arrays.fill(matchIndexes, -1);
		final boolean[] matchFlags = new boolean[max.length()];
		int matches = 0;

		for (int mi = 0; mi < min.length(); mi++) {
			final char c1 = min.charAt(mi);

			for (int xi = Math.max(mi - range, 0), xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {

				if (!matchFlags[xi] && c1 == max.charAt(xi)) {
					matchIndexes[mi] = xi;
					matchFlags[xi] = true;
					matches++;
					break;
				}
			}
		}

		final char[] ms1 = new char[matches];
		final char[] ms2 = new char[matches];

		for (int i = 0, si = 0; i < min.length(); i++) {

			if (matchIndexes[i] != -1) {
				ms1[si] = min.charAt(i);
				si++;
			}
		}

		for (int i = 0, si = 0; i < max.length(); i++) {

			if (matchFlags[i]) {
				ms2[si] = max.charAt(i);
				si++;
			}
		}

		int halfTranspositions = 0;

		for (int mi = 0; mi < ms1.length; mi++) {

			if (ms1[mi] != ms2[mi]) {
				halfTranspositions++;
			}
		}

		int prefix = 0;

		for (int mi = 0; mi < Math.min(4, min.length()); mi++) {

			if (first.charAt(mi) == second.charAt(mi)) {
				prefix++;
			}
			else {
				break;
			}
		}
		return new int[] {
			matches, halfTranspositions, prefix
		};
	}
}
