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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.bag.CollectionBag;
import org.apache.commons.collections4.bag.HashBag;


/**
 * 
 * @author mfranke
 *
 */
public final class ListUtils {

	private ListUtils() {
		throw new RuntimeException();
	}

	public static <T> String toString(List<T> list, CharSequence prefix, CharSequence suffix, String delimiter) {
		final StringJoiner sj = new StringJoiner(delimiter, prefix, suffix);
		list.forEach(element -> sj.add(element.toString()));
		return sj.toString();
	}

	public static <T> String toShortString(List<T> list) {
		return toString(list, "", "", "");
	}

	public static <T> int getDistinctElements(List<T> list) {
		return new HashSet<>(list).size();
	}
	
	public static <T> Map<T, Long> getCounts(List<T> list){
		return list.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}
	
	public static <T> CollectionBag<T> toBag(List<T> list) {
		return new CollectionBag<>(new HashBag<>(list));
	}
}
