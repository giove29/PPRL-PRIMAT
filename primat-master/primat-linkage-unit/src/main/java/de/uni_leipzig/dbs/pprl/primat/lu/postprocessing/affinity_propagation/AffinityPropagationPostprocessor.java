package de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.Pair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphVisitor;
import de.uni_leipzig.dbs.pprl.primat.lu.model.MultiPartiteSimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.LinkStrength;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.MultipartiteClusteringStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.Postprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.affinity_propagation.data_structures.ApConfig;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision.PersonalizedPageRank;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.*;

public class AffinityPropagationPostprocessor <T extends Linkable> implements Postprocessor<T>, MultipartiteClusteringStrategy {

    private double preferenceClnSource;

    private double preferenceDirtySource;

    private double damping;

    private ApConfig apConfig;

    public AffinityPropagationPostprocessor(double preferenceClnSrc, double preferenceDirtySrc, double damping,
                                            ApConfig apConfig) {
        this.preferenceClnSource = preferenceClnSrc;
        this.preferenceDirtySource = preferenceDirtySrc;
        this.damping = damping;
        this.apConfig = apConfig;
    }

    @Override
    public MatchStrategy<T> clean(MatchStrategy<T> matches) {
        System.out.println(matches.size());
        final SimilarityGraphVisitor<T> vis = new SimilarityGraphVisitor<T>();
        matches.accept(vis);
        final SimilarityGraph<Record> simGraph = (SimilarityGraph<Record>) vis.getSimilarityGraph();
        System.out.println("edges " + simGraph.edges());
        final Set<SimilarityVector> edges = simGraph.edgeSet();

        Set<SimilarityGraph<Record>> connectedComponents = simGraph.getConnectedComponentsSimGraph();
        Set<SimilarityVector> retainEdges = new HashSet<>();
        Set<Pair<Record, Record>> keptEdges = new HashSet<>();
        Set<String> cleanSources = new HashSet<>(apConfig.getCleanSources());
        for(SimilarityGraph<Record> clusterGraph: connectedComponents) {
            Map<Record, Integer> recordIndexMap = new HashMap<>();
            Map<Integer, Record> indexRecordMap = new HashMap<>();
            if (clusterGraph.vertexSet().size()>2)
                System.out.println(clusterGraph.edges());
            Multimap<String, Integer> cleanSrcElementIndices = ArrayListMultimap.create();
            double[][]simMatrix = new double[clusterGraph.vertexSet().size()][clusterGraph.vertexSet().size()];
            for(SimilarityVector sv : clusterGraph.edgeSet()) {
                Record source = clusterGraph.getEdgeSource(sv);
                Record target = clusterGraph.getEdgeTarget(sv);
                if (!recordIndexMap.containsKey(source)){
                    recordIndexMap.put(source, recordIndexMap.size());
                    if(cleanSources.contains(source.getParty().getName())) {
                        cleanSrcElementIndices.put(source.getParty().getName(), recordIndexMap.get(source));
                    }
                }
                if (!recordIndexMap.containsKey(target)){
                    recordIndexMap.put(target, recordIndexMap.size());
                    if(cleanSources.contains(target.getParty().getName())) {
                        cleanSrcElementIndices.put(target.getParty().getName(), recordIndexMap.get(target));
                    }
                }
                int srcId = recordIndexMap.get(source);
                int targetId = recordIndexMap.get(target);
                simMatrix[srcId][targetId] = sv.getAggregatedValue();
                indexRecordMap.put(srcId, source);
                indexRecordMap.put(targetId, target);
            }
            SparseMscdAffinityPropagationSeq ap = new SparseMscdAffinityPropagationSeq(simMatrix,
                    this.preferenceDirtySource, this.preferenceClnSource, this.damping, apConfig,
                    cleanSrcElementIndices, -1d);
            ap.fit();
            int labels[] = ap.getLabels();
            Map<Integer, List<Record>> clusters = new HashMap<>();
            for(int index: indexRecordMap.keySet()){
                int clusterId = labels[index];
                if(!clusters.containsKey(clusterId)) {
                    clusters.put(clusterId, new ArrayList<>());
                }
                List<Record> c = clusters.get(clusterId);
                c.add(indexRecordMap.get(index));
            }
            for(Map.Entry<Integer, List<Record>> e : clusters.entrySet()) {
                for(Record r : e.getValue()) {
                    for(Record s : e.getValue()){
                        if(r != s) {
                            keptEdges.add(new Pair<>(r, s));
                        }
                    }
                }
            }
        }
        for (SimilarityVector edge: edges) {
            final Record left = simGraph.getEdgeSource(edge);
            final Record right = simGraph.getEdgeTarget(edge);
            Pair<Record, Record> p = new Pair(left, right);
            Pair<Record, Record> p2 = new Pair(right, left);
            if(keptEdges.contains(p) || keptEdges.contains(p2)){
                retainEdges.add(edge);
            }
        }
        simGraph.retainAllEdges(retainEdges);
        return null;
    }

    /**
     * Come {@link #clean(MatchStrategy)}, ma opera su un unico grafo N-ario
     * ({@link MultiPartiteSimilarityGraph}, invece che per singola
     * {@code PartyPair}) e ritorna direttamente le coppie linkate, senza
     * mutare il grafo in place (non c'è più nulla da mutare in questo
     * flusso).
     */
    @Override
    public List<LinkedPair<Record>> cluster(MultiPartiteSimilarityGraph graph) {
        final List<LinkedPair<Record>> matches = new ArrayList<>();
        final Set<String> cleanSources = new HashSet<>(apConfig.getCleanSources());

        for (final MultiPartiteSimilarityGraph component : graph.getConnectedComponentsSimGraph()) {
            final Map<Record, Integer> recordIndexMap = new HashMap<>();
            final Map<Integer, Record> indexRecordMap = new HashMap<>();
            final Multimap<String, Integer> cleanSrcElementIndices = ArrayListMultimap.create();
            final double[][] simMatrix = new double[component.vertexSet().size()][component.vertexSet().size()];

            for (final SimilarityVector sv : component.edgeSet()) {
                final Record source = component.getEdgeSource(sv);
                final Record target = component.getEdgeTarget(sv);
                if (!recordIndexMap.containsKey(source)) {
                    recordIndexMap.put(source, recordIndexMap.size());
                    if (cleanSources.contains(source.getParty().getName())) {
                        cleanSrcElementIndices.put(source.getParty().getName(), recordIndexMap.get(source));
                    }
                }
                if (!recordIndexMap.containsKey(target)) {
                    recordIndexMap.put(target, recordIndexMap.size());
                    if (cleanSources.contains(target.getParty().getName())) {
                        cleanSrcElementIndices.put(target.getParty().getName(), recordIndexMap.get(target));
                    }
                }
                final int srcId = recordIndexMap.get(source);
                final int targetId = recordIndexMap.get(target);
                simMatrix[srcId][targetId] = sv.getAggregatedValue();
                indexRecordMap.put(srcId, source);
                indexRecordMap.put(targetId, target);
            }

            final SparseMscdAffinityPropagationSeq ap = new SparseMscdAffinityPropagationSeq(simMatrix,
                    this.preferenceDirtySource, this.preferenceClnSource, this.damping, apConfig,
                    cleanSrcElementIndices, -1d);
            ap.fit();
            final int[] labels = ap.getLabels();

            final Map<Integer, List<Record>> clusters = new HashMap<>();
            for (final int index : indexRecordMap.keySet()) {
                clusters.computeIfAbsent(labels[index], l -> new ArrayList<>()).add(indexRecordMap.get(index));
            }

            for (final List<Record> members : clusters.values()) {
                for (int i = 0; i < members.size(); i++) {
                    for (int j = i + 1; j < members.size(); j++) {
                        matches.add(new LinkedPair<>(members.get(i), members.get(j)));
                    }
                }
            }
        }

        return matches;
    }
}
