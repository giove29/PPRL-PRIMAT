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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


/**
 * 
 * @author mfranke
 *
 */
public class MapUtils {

	private MapUtils() {
		throw new RuntimeException();
	}

	public static <S, T extends Number> DescriptiveStatistics getValueStatistics(Map<S, T> map) {
		final DescriptiveStatistics stats = new DescriptiveStatistics();

		for (final Entry<S, T> entry : map.entrySet()) {
			stats.addValue(entry.getValue().doubleValue());
		}
		return stats;
	}

	public static <S, T extends Comparable<? super T>> List<Entry<S, T>> sortByValueToList(Map<S, T> map) {
		final List<Entry<S, T>> list = new ArrayList<>(map.entrySet());
		list.sort(Entry.comparingByValue());
		Collections.reverse(list);
		return list;
	}

	public static <S, T extends Comparable<? super T>> Map<S, T> sortByValueToMap(Map<S, T> map) {
		final List<Entry<S, T>> sortedList = sortByValueToList(map);
		final Map<S, T> sortedMap = new LinkedHashMap<>(sortedList.size());

		for (final Entry<S, T> entry : sortedList) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}

	public static <S, T> Map<S, HomogenPair<T>> union(Map<S, T> left, Map<S, T> right) {
		final Map<S, HomogenPair<T>> result = new HashMap<>();

		for (final Entry<S, T> le : left.entrySet()) {
			result.put(le.getKey(), new HomogenPair<T>(le.getValue(), null));
		}

		for (final Entry<S, T> re : right.entrySet()) {

			if (result.containsKey(re.getKey())) {
				result.get(re.getKey()).setRight(re.getValue());
			}
			else {
				result.put(re.getKey(), new HomogenPair<T>(null, re.getValue()));
			}
		}

		return result;
	}

	public static <S, T> Map<S, HomogenPair<T>> intersect(Map<S, T> left, Map<S, T> right) {
		final Map<S, HomogenPair<T>> result = new HashMap<>();

		final Set<S> intersection = SetUtils.intersection(left.keySet(), right.keySet());

		for (final S e : intersection) {
			result.put(e, new HomogenPair<T>(left.get(e), right.get(e)));
		}

		return result;
	}

	public static <S, T> Map<S, T> difference(Map<S, T> left, Map<S, T> right) {
		final Map<S, T> result = new HashMap<>();

		final Set<S> difference = SetUtils.difference(left.keySet(), right.keySet());

		for (final S e : difference) {
			result.put(e, left.get(e));
		}

		return result;
	}

	public static <S, T> Map<S, T> symmetricDifference(Map<S, T> left, Map<S, T> right) {
		final Map<S, T> result = new HashMap<>();

		final Set<S> difference = SetUtils.symmetricDifference(left.keySet(), right.keySet());

		for (final S e : difference) {
			result.put(e, left.get(e));
		}

		return result;
	}
}
