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

package de.uni_leipzig.dbs.pprl.primat.examples.lu.quality_estimation;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.QualityEstimator;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Block;
import de.uni_leipzig.dbs.pprl.primat.lu.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.*;
import de.uni_leipzig.dbs.pprl.primat.lu.matching.Matcher;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.Postprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_classification.SimilarityClassification;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

import java.util.*;


/**
 *
 * @author mfranke
 *
 */
public class BatchMatcher implements Matcher<Record> {

    private Blocker blocker;
    private SimilarityClassification simClass;
    private Postprocessor<Record> postprocessor;
    private QualityEstimator estimator;


    private boolean isGraphSerialized;
    private String simGraphPath;

    public BatchMatcher(Blocker blocker, SimilarityClassification simClass, Postprocessor<Record> postprocessor,
                        QualityEstimator estimator) {
        this.blocker = blocker;
        this.simClass = simClass;
        this.postprocessor = postprocessor;
        this.estimator = estimator;
    }

    public BatchMatcher(Blocker blocker, SimilarityClassification simClass, Postprocessor<Record> postprocessor) {
        this(blocker, simClass, postprocessor, null);
    }

    public BatchMatcher(Blocker blocker, SimilarityClassification simClass) {
        this(blocker, simClass, null, null);
    }

    @Override
    public LinkageResult<Record> match(Map<Party, Collection<Record>> input) {
        final Collection<Block> blocks = this.blocker.getBlocks(input);
        System.out.println("Blocking done.");
        final LinkageResult<Record> linkageResult = this.simClass.classifyRecords(input.keySet(), blocks);
        System.out.println("Classification done.");
        if (estimator != null) {
            this.estimator.computeQuality(linkageResult);
        }
        final Collection<LinkageResultPartition<Record>> partitions = linkageResult.getPartitions();
        for (final LinkageResultPartition<Record> part : partitions) {
            final MatchStrategy<Record> matchStrat = part.getMatchStrategy();
            if(postprocessor != null){
                this.postprocessor.clean(matchStrat);
            }
        }
        return linkageResult;
    }

    @SuppressWarnings("unused")
    private void postPostProcessing(LinkageResult<Record> linkageResult) {
        // ----------------------------------------------------------------------------------------
        // /*
        final PartyPair partyPairAA = new PartyPair(new Party("A"), new Party("A"));

        final LinkageResultPartition<Record> linkResPartA = linkageResult.getPartition(partyPairAA);
        final MatchStrategy<Record> matchStratA = linkResPartA.getMatchStrategy();
        final SimilarityGraph<Record> simGraphA = ((SimilarityGraphMatchStrategy<Record>) matchStratA)
                .getSimilarityGraph();

        // MetricCollector.INSTANCE.getMetrics();

        System.out.println("Test");

        System.out.println("Intra-Souce-Links A: " + matchStratA.getMatches().size());

        // this.postprocessor.clean(matchStratA);
        // System.out.println("MaxBoth Intra-Souce-Links A: " +
        // matchStratA.getMatches().size());

        final PartyPair partyPairBB = new PartyPair(new Party("B"), new Party("B"));

        final LinkageResultPartition<Record> linkResPartB = linkageResult.getPartition(partyPairBB);
        final MatchStrategy<Record> matchStratB = linkResPartB.getMatchStrategy();
        final SimilarityGraph<Record> simGraphB = ((SimilarityGraphMatchStrategy<Record>) matchStratB)
                .getSimilarityGraph();

        System.out.println("Intra-Souce-Links B: " + matchStratA.getMatches().size());

        // this.postprocessor.clean(matchStratB);
        // System.out.println("MaxBoth Intra-Souce-Links B: " +
        // matchStratA.getMatches().size());

        // */
        // ----------------------------------------------------------------------------------------

        final PartyPair partyPairAB = new PartyPair(new Party("A"), new Party("B"));

        final LinkageResultPartition<Record> linkResPart = linkageResult.getPartition(partyPairAB);
        final MatchStrategy<Record> matchStrat = linkResPart.getMatchStrategy();
        final SimilarityGraph<Record> simGraph = ((SimilarityGraphMatchStrategy<Record>) matchStrat)
                .getSimilarityGraph();

        /*
         * links = simGraph.links(); source = simGraph.sourceVertices() -
         * simGraph.unmappedSourceVertices(); target = simGraph.targetVertices()
         * - simGraph.unmappedTargetVertices();
         *
         * sourceAll = input.get("A").size(); targetAll = input.get("B").size();
         *
         *
         * System.out.println("AFTER"); System.out.println("E-Recall: " +
         * QualityMetrics.getRecall(links, 12527));
         * System.out.println("P-Recall1: " +
         * QualityMetrics.getPseudoRecall(links, sourceAll, targetAll));
         * System.out.println("P-Recall2: " +
         * QualityMetrics.getPseudoRecallAlt(source, target, sourceAll,
         * targetAll)); System.out.println("P-Recall2: " +
         * QualityMetrics.getPseudoRecallAltMin(source, target, sourceAll,
         * targetAll));
         *
         * System.out.println("Critic Selectivity: " + cntCritic);
         * System.out.println("Critic Selectivity %: " + cntCritic / (double)
         * links);
         *
         * final DescriptiveStatistics simStat =
         * simGraph.getEdgeWeightStatistic();
         * System.out.println("Sim Statistic: " + simStat);
         * System.out.println("Lower t-Limit: " + (simStat.getMean() -
         * simStat.getStandardDeviation())); System.out.println("P-Precision1: "
         * + QualityMetrics.getPseudoPrecision(source, links));
         * System.out.println("P-Precision2: " +
         * QualityMetrics.getPseudoPrecisionMin(source, target, links));
         * System.out.println("P-Precision3: " +
         * QualityMetrics.getPseudoPrecisionSum(source, target, links));
         * System.out.println("P-Precision4: " + (1.0d - (cntCritic / (double)
         * links)));
         */

        // System.out.println(simGraph);

        // /*
        final Set<LinkedPair<Record>> cleanedMatches = matchStrat.getMatches();

        int criticCount = 0;
        int actualCritics = 0;

        int actualCriticsWithBothSideIntraSim = 0;
        int falseCriticsWithBithSideIntraSim = 0;

        final List<SimilarityVector> intraSourceUncertainLinks = new ArrayList<>();

        /*
         * for (final ComparedRecordClusterPair cleanMatch : cleanedMatches) {
         * final Record left = cleanMatch.getRecord(); final Cluster right =
         * cleanMatch.getCluster();
         *
         * double leftIntraSourceMaxSim = 0;
         *
         * if (simGraphA.containsSourceVertex(left)) { leftIntraSourceMaxSim =
         * simGraphA.maxEdgeOf(left).getAggregatedValue(); }
         *
         * double rightIntraSourceMaxSim = 0;
         *
         * if (simGraphB.containsSourceVertex(right)) { rightIntraSourceMaxSim =
         * simGraphB.maxEdgeOf(right).getAggregatedValue(); }
         *
         * final boolean bothSideIntraSim = leftIntraSourceMaxSim > 0d &&
         * rightIntraSourceMaxSim > 0d; final double maxIntraSourceSim =
         * Math.max(leftIntraSourceMaxSim, rightIntraSourceMaxSim);
         *
         * if (cleanMatch.getAggregatedSim() < 0.8d) {
         *
         * }
         *
         * if (Math.max(leftIntraSourceMaxSim, rightIntraSourceMaxSim) + 0.05d
         * >= cleanMatch.getAggregatedSim()) { criticCount++; if
         * (!cleanMatch.getRecordPair().getLeft().getId().equals(cleanMatch.
         * getRecordPair().getRight().getId())){ actualCritics++; if
         * (bothSideIntraSim) actualCriticsWithBothSideIntraSim++; } else {
         * System.out.println("!!! : " + cleanMatch.getRecordPair() +
         * cleanMatch.getAggregatedSim() + " ::: " + maxIntraSourceSim); if
         * (bothSideIntraSim) falseCriticsWithBithSideIntraSim++;
         *
         * } intraSourceUncertainLinks.add(cleanMatch.getSimVec()); } }
         */

        System.out.println("Critic Count: " + criticCount);
        System.out.println("Actual Critics: " + actualCritics);

        System.out.println(actualCriticsWithBothSideIntraSim + " vs " + falseCriticsWithBithSideIntraSim);
        // */

        for (SimilarityVector sv : intraSourceUncertainLinks) {
            System.out.println("ISUL: " + sv);
        }

        // /*
        System.out.println("IntraSourceUncertainLinks Size: " + intraSourceUncertainLinks.size());
        System.out.println("MatchStrat: " + matchStrat.getMatches().size());

        final SimilarityGraphVisitor<Record> vis = new SimilarityGraphVisitor<Record>();
        matchStrat.accept(vis);
        final SimilarityGraph<Record> graph = vis.getSimilarityGraph();

        System.out.println(graph.edgeSet().size());

        for (SimilarityVector edge : graph.edgeSet()) {
            //

            if (intraSourceUncertainLinks.contains(edge)) {
                System.out.println("CONTAINED: " + graph.getEdgeSource(edge) + " - " + graph.getEdgeTarget(edge));
            }
            else {
                System.out.println("NOT: " + graph.getEdgeSource(edge) + " - " + graph.getEdgeTarget(edge));
            }
        }
        // */

        System.out.println("--------------");

        graph.removeAllEdges(intraSourceUncertainLinks);

        // System.out.println(graph.edgeSet().size());

        // for (SimilarityVector edge : graph.edgeSet()) {
        // System.out.println(graph.getEdgeSource(edge) + " - " +
        // graph.getEdgeTarget(edge));
        // }

        System.out.println("After: " + matchStrat.getMatches().size());
        // */
    }

    public void setSimilarityGraphPath(String simGraphPath) {
        this.simGraphPath = simGraphPath;
    }


}


