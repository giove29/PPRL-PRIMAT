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
package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author mfranke
 *
 */
public final class CombinatoricsUtils {

	//TODO: merge with SetUtils
	
	private CombinatoricsUtils() {
		throw new RuntimeException();
	}

	public static <T> List<List<T>> generatePermutations(List<T> sequence) {
		final List<List<T>> permutations = new ArrayList<>();
	    generatePermutations(sequence, permutations, 0);
	    return permutations;
	}

	private static <T> void generatePermutations(List<T> sequence, List<List<T>> results, int index) {
	    if (index == sequence.size() - 1) {
	    	results.add(new ArrayList<>(sequence));
	    }

	    for (int i = index; i < sequence.size(); i++) {
	        Collections.swap(sequence, i, index);
	        generatePermutations(sequence, results, index + 1);
	        Collections.swap(sequence, i, index);
	    }
	}
	
	public static <T> List<List<T>> combinations(List<T> inputSet, int k) {
        final List<List<T>> results = new ArrayList<>();
        combinations(inputSet, k, results, new ArrayList<>(), 0);
        return results;
    }

    private static <T> void combinations(List<T> inputSet, int k, List<List<T>> results, ArrayList<T> accumulator, int index) {
        final int leftToAccumulate = k - accumulator.size();
        final int possibleToAcculumate = inputSet.size() - index;

        if (accumulator.size() == k) {
            results.add(new ArrayList<>(accumulator));
        } 
        else if (leftToAccumulate <= possibleToAcculumate) {
        	combinations(inputSet, k, results, accumulator, index + 1);
            accumulator.add(inputSet.get(index));
            combinations(inputSet, k, results, accumulator, index + 1);
            accumulator.remove(accumulator.size() - 1);
        }
    }

    public static <T> List<List<T>> powerSet(List<T> sequence) {
        final List<List<T>> results = new ArrayList<>();
        powerSet(sequence, results, new ArrayList<>(), 0);
        return results;
    }

    private static <T> void powerSet(List<T> set, List<List<T>> powerSet, List<T> accumulator, int index) {
        if (index == set.size()) {
            powerSet.add(new ArrayList<>(accumulator));
        } 
        else {
            accumulator.add(set.get(index));
            powerSet(set, powerSet, accumulator, index + 1);
            accumulator.remove(accumulator.size() - 1);
            powerSet(set, powerSet, accumulator, index + 1);
        }
    }
}

