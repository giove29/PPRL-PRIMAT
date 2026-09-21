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
package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.hungarian;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.Postprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;


/**
 * 
 * @author mfranke
 *
 */
public final class HungarianPostprocessor<T extends Linkable> implements Postprocessor<T> {

	private BidiMap<Record, Integer> sourceRecordPositionAssignment;
	private BidiMap<T, Integer> targetRecordPositionAssignment;

	@Override
	public MatchStrategy<T> clean(MatchStrategy<T> matches) {
		final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<>();
		matches.accept(vis);
		final SimilarityGraph<T> graph = vis.getSimilarityGraph();

		final Set<SimilarityGraph<T>> connectedComponents = graph.getConnectedComponentsSimGraph();
		System.out.println("Number of connected components: " + connectedComponents.size());

		for (final SimilarityGraph<T> cc : connectedComponents) {
			final int edges = cc.edgeSet().size();
			System.out.println("CC of size " + edges);

			if (edges == 1) {
				// TODO: this is a already clean cluster; add to result or do
				// nothing
			}
			else {
				final double[][] clusterCostMatrix = this.constructCostMatrix(cc);
				final HungarianAlgorithm hungarianAlgorithm = new HungarianAlgorithm(clusterCostMatrix);
				final int[] clusterAssignmentMatrix = hungarianAlgorithm.execute();
				this.applyAssignmentMatrix(cc, clusterAssignmentMatrix);
				// TODO: add candidatesInCluster to result or do nothing
			}
		}

		return null;
	}

	private double[][] constructCostMatrix(SimilarityGraph<T> connectedComponent) {
		this.sourceRecordPositionAssignment = new DualHashBidiMap<>();
		this.targetRecordPositionAssignment = new DualHashBidiMap<>();

		final Set<Record> sourceRecords = new HashSet<>();
		final Set<T> targetRecords = new HashSet<>();

		// TODO: Get bi-partition for connected components
		final Set<SimilarityVector> edges = connectedComponent.edgeSet();

		for (final SimilarityVector edge : edges) {
			final Record source = connectedComponent.getEdgeSource(edge);
			sourceRecords.add(source);
			final T target = connectedComponent.getEdgeTarget(edge);
			targetRecords.add(target);
		}

		final int numberOfsourceRecs = sourceRecords.size();
		final int numberOftargetRecs = targetRecords.size();

		final double[][] costMatrix = new double[numberOfsourceRecs][numberOftargetRecs];

		int sourceIndex = 0;
		int targetIndex = 0;

		for (final Record source : sourceRecords) {
			sourceRecordPositionAssignment.put(source, sourceIndex);

			final Set<SimilarityVector> pairs = connectedComponent.edgesOf(source);

			for (final SimilarityVector pair : pairs) {
				final T targetRecord = connectedComponent.getEdgeTarget(pair);
				// TODO: ?!?
				final double sim = pair.getAggregatedValue();

				if (!targetRecordPositionAssignment.containsKey(targetRecord)) {
					targetRecordPositionAssignment.put(targetRecord, targetIndex);
					costMatrix[sourceIndex][targetIndex] = sim;
					targetIndex++;
				}
				else {
					final int targetIndexPosition = targetRecordPositionAssignment.get(targetRecord);
					costMatrix[sourceIndex][targetIndexPosition] = sim;
				}
			}
			sourceIndex++;
		}

		for (int i = 0; i < numberOfsourceRecs; i++) {

			for (int j = 0; j < numberOftargetRecs; j++) {
				final double sim = costMatrix[i][j];

				if (sim == 0d) {
					costMatrix[i][j] = 1d;
				}
				else {
					costMatrix[i][j] = 1d - sim;
				}
			}
		}

		return costMatrix;
	}

	private void applyAssignmentMatrix(SimilarityGraph<T> connectedComponent, int[] assignmentMatrix) {

		for (int sourceIndex = 0; sourceIndex < assignmentMatrix.length; sourceIndex++) {
			final int targetIndex = assignmentMatrix[sourceIndex];
			final Record source = this.sourceRecordPositionAssignment.getKey(sourceIndex);

			if (targetIndex != -1) {
				final T target = this.targetRecordPositionAssignment.getKey(targetIndex);
				final Set<SimilarityVector> edges = connectedComponent.getAllEdges(source, target);

				if (edges == null || edges.size() < 1) {
					// ERROR
				}
				else {
					// Add this link to result or do nothing
				}
			}
			else {
				// No assignment for this source node
				connectedComponent.removeVertexFromSource(source);
				// TODO: Are there any target records that have to be removed
				// ???
			}
		}
		// classifiedMatches.retainAll(cleanCands);
	}

	@Override
	public String toString() {
		return "Hungarian Algorithm (Maximum Weight Matching)";
	}
}