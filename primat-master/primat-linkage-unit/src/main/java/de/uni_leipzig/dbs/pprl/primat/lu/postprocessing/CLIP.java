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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


public class CLIP<T extends Linkable> implements Postprocessor<T> {

	private double weightSimilarity;
	private double weightLinkStrength;
	private Map<LinkStrength, Double> valueLinkStrength;
	private double weightLinkDegree;
	private boolean ignoreWeakLinks;

	public CLIP() {
		this.weightSimilarity = 0.5d;
		this.weightLinkStrength = 0.4d;
		this.valueLinkStrength = Map.of(LinkStrength.STRONG, 1d, LinkStrength.NORMAL, 0.5d, LinkStrength.WEAK, 0d);
		this.weightLinkDegree = 0.1d;
		this.ignoreWeakLinks = true;
	}

	private double getLinkPriority(double similarity, double linkDegree, LinkStrength linkStrength) {
		return this.weightSimilarity * similarity + this.weightLinkDegree * linkDegree
			+ this.weightLinkStrength * this.valueLinkStrength.get(linkStrength);
	}

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<T>();
		matches.accept(vis);
		final SimilarityGraph<T> simGraph = vis.getSimilarityGraph();

		final Set<SimilarityVector> edges = simGraph.edgeSet();
		final Map<SimilarityVector, Double> edgesPrio = new HashedMap<>(edges.size());
		final Set<SimilarityVector> weakLinks = new HashSet<>();

		for (final SimilarityVector edge : edges) {
			final double similarity = simGraph.getEdgeWeight(edge);
			final double linkDegree = simGraph.getEdgeDegree(edge);

			final Record source = simGraph.getEdgeSource(edge);
			final SimilarityVector sourceMaxLink = simGraph.maxEdgeOf(source);

			final T target = simGraph.getEdgeTarget(edge);
			final SimilarityVector targetMaxLink = simGraph.maxEdgeOf(target);
			final boolean leftMax = edge.equals(sourceMaxLink);
			final boolean rightMax = edge.equals(targetMaxLink);

			final LinkStrength linkStrength = LinkStrength.getLinkStrength(leftMax, rightMax);

			if (ignoreWeakLinks && linkStrength.equals(LinkStrength.WEAK)) {
				weakLinks.add(edge);
			}
			else {
				final double linkPriority = this.getLinkPriority(similarity, linkDegree, linkStrength);
				edgesPrio.put(edge, linkPriority);
			}
		}

		simGraph.removeAllEdges(weakLinks);

		final List<SimilarityVector> sortedEdges = edgesPrio.entrySet().stream()
			.sorted(Entry.comparingByValue(Comparator.reverseOrder())).map(e -> e.getKey())
			.collect(Collectors.toList());

		final Set<Record> leftVisited = new HashSet<>();
		final Set<T> rightVisited = new HashSet<>();

		for (final SimilarityVector edge : sortedEdges) {

			if (!simGraph.containsEdge(edge)) {
				continue;
			}

			final Record left = simGraph.getEdgeSource(edge);
			final T right = simGraph.getEdgeTarget(edge);

			if (leftVisited.contains(left) || rightVisited.contains(right)) {
				continue;
			}
			else {
				simGraph.retainEdgeOf(left, edge);
				simGraph.retainEdgeOf(right, edge);
				leftVisited.add(left);
				rightVisited.add(right);
			}
		}

		return null;
	}

}
