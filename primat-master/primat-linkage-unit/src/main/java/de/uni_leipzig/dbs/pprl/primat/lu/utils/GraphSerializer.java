package de.uni_leipzig.dbs.pprl.primat.lu.utils;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GraphSerializer {
    /**
     * Save a serialized similarity graph representing the linkage result. The serialized graph <i><b>does not</b><i/> contain the
     * attributes of a record. An record consists of its id, global id, and the name of the party. The serialization of the
     * graph consists of the structure with the similarity vectors as well as source and target sets.
     * @param graphPath path to the saved similarity graph
     * @param linkageResult linkage result where the underlying similarity graph is saved
     * @param firstParty name of the source party
     * @param secondParty name of the second party
     * @throws IOException
     */
    public static void writeGraph(String graphPath, LinkageResult<Record> linkageResult,
                                  String firstParty, String secondParty) throws IOException {
        final PartyPair partyPairAB = new PartyPair(new Party(firstParty), new Party(secondParty));
        final LinkageResultPartition<Record> linkResPart = linkageResult.getPartition(partyPairAB);
        final MatchStrategy<Record> matchStrat = linkResPart.getMatchStrategy();
        final SimilarityGraph<Record> graph = ((SimilarityGraphMatchStrategy<Record>) matchStrat)
                .getSimilarityGraph();
        FileOutputStream fileOutputStream
                = new FileOutputStream(graphPath);
        ObjectOutputStream objectOutputStream
                = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(graph);
        objectOutputStream.close();
    }

    /**
     * Reads an serialized similarity graph from a specified path
     * @param simGraphPath
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static SimilarityGraph<Record> readGraph(String simGraphPath) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream
                = new FileInputStream(simGraphPath);
        ObjectInputStream objectInputStream
                = new ObjectInputStream(fileInputStream);
        SimilarityGraph<Record> similarityGraph = (SimilarityGraph<Record>) objectInputStream.readObject();
        objectInputStream.close();
        return similarityGraph;
    }

    public static LinkageResult<Record> transformToLinkageResult(SimilarityGraph<Record> similarityGraph,
                                                                 String firstParty, String secondParty){
        final MatchStrategyFactory<Record> matchFactory = new SimilarityGraphMatchStrategyFactory<>();
        final NonMatchStrategyFactory<Record> nonMatchFactory = new IgnoreNonMatchesStrategyFactory<>();
        final LinkageResultPartitionFactory<Record> linkResFac = new LinkageResultPartitionFactory<>(matchFactory,
                nonMatchFactory);
        PartyPair partyPair = new PartyPair(new Party(firstParty), new Party(secondParty));
        Set<PartyPair> partyPairs = new HashSet<>(Arrays.asList(partyPair));
        LinkageResult<Record> linkageResult = new LinkageResult<>(partyPairs, linkResFac);
        LinkageResultPartition<Record> resultPartition = linkageResult.getPartition(partyPair);
        for (SimilarityVector e : similarityGraph.edgeSet()) {
            resultPartition.addMatch(similarityGraph.getEdgeSource(e), similarityGraph.getEdgeTarget(e), e, e.getAggregatedValue());
        }
        return linkageResult;
    }




}
