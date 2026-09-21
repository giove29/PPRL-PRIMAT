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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;


/**
 * 
 * @author mfranke
 *
 */
public final class SetUtils {

	private SetUtils() {
		throw new RuntimeException();
	}

	public static <T> Set<T> getSingletons(List<T> elementList) {
		final Set<T> singletons = new HashSet<T>(elementList);
		return singletons;
	}

	public static <E> boolean hasDuplicates(List<E> elementList) {
		return elementList.size() != getSingletons(elementList).size();
	}
	
	public static <T extends Comparable<T>> Set<T> removeDuplicatesMaintainOrdering(List<T> elementList){
		return new TreeSet<T>(elementList);
	}

	public static <T> Set<T> getDuplicates(List<T> elementList) {
		final Set<T> singletons = new HashSet<T>(elementList.size());
		final Set<T> duplicates = new HashSet<T>();

		for (final T element : elementList) {
			final boolean isDuplicate = !singletons.add(element);

			if (isDuplicate) {
				duplicates.add(element);
			}
		}
		return duplicates;
	}

	/**
	 * Complement of the identity relation, i. e. the set without any pair (x,x)
	 * where x is element of X. As a consequence, no element is related to
	 * itself. The irreflexive closure is also known as reflexive reduction or
	 * irreflexive kernel.
	 * 
	 * @param  <E>
	 * @param  set
	 * @return
	 */
	public static <E> Set<HomogenPair<E>> getIrreflexiveClosure(Set<E> set) {
		return getIrreflexiveClosure(new ArrayList<>(set));
	}
	
	public static <E> Set<HomogenPair<E>> getIrreflexiveClosure(List<E> list) {
		final Set<HomogenPair<E>> result = new HashSet<>(list.size() * list.size() - list.size());
		
		for (int i = 0; i < list.size(); i++) {

			for (int j = 1 + i; j < list.size(); j++) {
				final HomogenPair<E> pair = new HomogenPair<E>(list.get(i), list.get(j));
				result.add(pair);
			}
		}
		return result;
	}

	//TODO: different semeantic of tuples and set, eg. set of sets or set of tuples
	
	
	/**
	 * The reflexive closure of a relation R on a set X is the smallest
	 * reflexive relation on X that contains R. The reflexive closure of R is
	 * the union of R with the identity relation on X.
	 * 
	 * @param  <E>
	 * @param  homogenRelation
	 * @param  set
	 * @return
	 */
	public static <E> Set<HomogenPair<E>> getReflexiveClosure(Set<HomogenPair<E>> homogenRelation, Set<E> set) {
		final Set<HomogenPair<E>> result = new HashSet<>(homogenRelation);
		result.addAll(SetUtils.getIdentityRelation(set));
		return result;
	}

	/**
	 * The universal relation is defined as the cartesian square on a set.
	 * 
	 * @param  <E>
	 * @param  set
	 * @return
	 */
	public static <E> Set<HomogenPair<E>> getUniversalRelation(Set<E> set) {
		return cartesianSquare(set);
	}

	/**
	 * Identity relation is defined as the set of all pairs (x, x) where x is
	 * element of X.
	 * 
	 * @param  <E>
	 * @param  set
	 * @return
	 */
	public static <E> Set<HomogenPair<E>> getIdentityRelation(Set<E> set) {
		final Set<HomogenPair<E>> result = new HashSet<>(set.size());

		for (final E element : set) {
			final HomogenPair<E> pair = new HomogenPair<E>(element, element);
			result.add(pair);
		}

		return result;
	}

	public static <E> Set<HomogenPair<E>> cartesianSquare(Set<E> set) {
		final Set<HomogenPair<E>> result = new HashSet<>(set.size() * set.size());

		for (final E e1 : set) {

			for (final E e2 : set) {
				final HomogenPair<E> pair = new HomogenPair<E>(e1, e2);
				result.add(pair);
			}
		}
		return result;
	}

	public static <E> Set<HomogenPair<E>> antiSymmetricCartesianSquare(Set<E> set) {
		final Set<HomogenPair<E>> result = new HashSet<>(set.size() * set.size());

		for (final E e1 : set) {

			for (final E e2 : set) {
				final HomogenPair<E> inverse = new HomogenPair<E>(e2, e1);

				if (result.contains(inverse)) {
					continue;
				}
				else {
					final HomogenPair<E> pair = new HomogenPair<E>(e1, e2);
					result.add(pair);
				}
			}
		}
		return result;
	}

	public static <E extends Comparable<E>> Set<HomogenPair<E>> orderedAntiSymmetricCartesianSquare(Set<E> set) {
		final Set<HomogenPair<E>> result = new HashSet<>(set.size() * set.size());

		for (final E e1 : set) {

			for (final E e2 : set) {

				if (e1.compareTo(e2) <= 0) {
					final HomogenPair<E> pair = new HomogenPair<E>(e1, e2);
					result.add(pair);
				}
			}
		}
		return result;
	}

	public static <S, T> Set<Pair<S, T>> join(Set<S> leftSet, Set<T> rightSet, BiFunction<S, T, Boolean> theta) {
		final Set<Pair<S, T>> result = new HashSet<>();

		for (final S left : leftSet) {

			for (final T right : rightSet) {

				if (theta.apply(left, right)) {
					result.add(new Pair<S, T>(left, right));
				}
			}
		}
		return result;
	}

	public static <S, T> Set<Pair<S, T>> cartesianProduct(Set<S> leftSet, Set<T> rightSet) {
		final Set<Pair<S, T>> result = new HashSet<>(leftSet.size() * rightSet.size());

		for (final S left : leftSet) {

			for (final T right : rightSet) {
				final Pair<S, T> pair = new Pair<S, T>(left, right);
				result.add(pair);
			}
		}
		return result;
	}

	public static <T> MultiValuedMap<T, T> crossAndGroup(Set<T> leftSet, Set<T> rightSet) {
		final MultiValuedMap<T, T> result = new HashSetValuedHashMap<>(leftSet.size() * rightSet.size());

		for (final T left : leftSet) {
			result.putAll(left, rightSet);
		}

		return result;
	}

	public static <T> Set<T> intersection(Set<T> left, Set<T> right) {
		final Set<T> result = new HashSet<>(left);
		result.retainAll(right);
		return result;
	}

	public static <T> Set<T> union(Set<T> left, Set<T> right) {
		final Set<T> result = new HashSet<>(left);
		result.addAll(right);
		return result;
	}

	public static <T> Set<T> difference(Set<T> left, Set<T> right) {
		final Set<T> result = new HashSet<>(left);
		result.removeAll(right);
		return result;
	}

	public static <T> Set<T> symmetricDifference(Set<T> left, Set<T> right) {
		final Set<T> union = union(left, right);
		final Set<T> intersection = intersection(left, right);
		return difference(union, intersection);
	}
	
	private static boolean bitwiseAnding(int val, int indx){
	    int mask = 1 << (indx);
	    return (val & mask) != 0;
	}

	public static <T> Set<Set<T>> powerSet(Set<T> input) {
		return powerSet(new ArrayList<T>(input));
	}
	
	public static <T> Set<Set<T>> powerSet(List<T> input) {
		final Set<Set<T>> sets = new LinkedHashSet<>();
		
		sets.add(new LinkedHashSet<T>());
		
		for (int i = 1; i < Math.pow(2, input.size()); i++) {
			final Set<T> currentSet = new HashSet<T>();
			
			for (int j = 0; j < input.size(); j++) {
				if (bitwiseAnding(i, j)) {
					currentSet.add(input.get(j));
				}
			}		
			sets.add(currentSet);
		}
		
		return sets;
	}
}
