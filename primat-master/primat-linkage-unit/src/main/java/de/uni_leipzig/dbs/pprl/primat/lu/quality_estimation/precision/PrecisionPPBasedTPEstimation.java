package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PrecisionPPBasedTPEstimation extends PrecisionEstimation {


    protected PrecisionPPBasedTPEstimation(String name, int foundMatches) {
        super(name, foundMatches);
    }

    public PrecisionPPBasedTPEstimation(int foundMatches) {
        this("p_pp_dup", foundMatches);
    }

    @Override
    public double estimateQuality(SimilarityGraph<Record> similarityGraph) {
        DescriptiveStatistics edgeStat = similarityGraph.getEdgeStatistics();
        double minSim = edgeStat.getMin();
        double maxSim = edgeStat.getMax();
        Set<SimilarityGraph<Record>> connectedComponents = similarityGraph.getConnectedComponentsSimGraph();
        double tpScore = 0;
        foundMatches = 0;
        for(SimilarityGraph<Record> clusterGraph: connectedComponents) {
            DirectedWeightedMultigraph<Linkable, SimilarityVector> graph = getDirectedGraph(clusterGraph.getGraph());
            Map<Record, Map<Record, Double>> pageRankAll = new HashMap<>();
            for (Linkable r : clusterGraph.vertexSet()) {
                PersonalizedPageRank personalizedPageRank = new PersonalizedPageRank(graph);
                personalizedPageRank.setPersonalNode(r);
                Map<Record, Double> scores = personalizedPageRank.getScores();
                pageRankAll.put((Record) r, scores);
            }

            for(SimilarityVector sv : clusterGraph.edgeSet()) {
                Record source = clusterGraph.getEdgeSource(sv);
                Record target = clusterGraph.getEdgeTarget(sv);
                if(!source.getParty().getName().equals(target.getParty().getName())) {
                    double prob = (1 - Math.abs(pageRankAll.get(source).get(target) - (1 / (double) clusterGraph.vertices())))
                            * (clusterGraph.getEdgeWeight(sv) - (0.3 - 0.01)) / (1 - (0.3 - 0.01))
                            * (1 - Math.abs(pageRankAll.get(target).get(source) - (1 / (double) clusterGraph.vertices())));
                    tpScore += prob;
                    foundMatches++;
                }
            }
        }
        System.out.println("estimated tp:" + tpScore);
        this.tps = tpScore;
        return this.tps/(double)foundMatches;


    }

    private DirectedWeightedMultigraph<Linkable, SimilarityVector>
    getDirectedGraph(AsUnmodifiableGraph<Linkable, SimilarityVector> graph) {
        DirectedWeightedMultigraph<Linkable, SimilarityVector> directedGraph =
                new DirectedWeightedMultigraph<>(SimilarityVector.class);
        for(Linkable v :graph.vertexSet()) {
            directedGraph.addVertex(v);
        }
        for (SimilarityVector e : graph.edgeSet()) {
            Linkable source = graph.getEdgeSource(e);
            Linkable target = graph.getEdgeTarget(e);
            double weight = graph.getEdgeWeight(e);
            SimilarityVector sv = new SimilarityVector(weight);
            SimilarityVector reverseSV = new SimilarityVector(weight);
            directedGraph.addEdge(source, target, sv);
            directedGraph.addEdge(target, source, reverseSV);
            directedGraph.setEdgeWeight(sv, weight);
            directedGraph.setEdgeWeight(reverseSV, weight);
        }
        return directedGraph;
    }
}
