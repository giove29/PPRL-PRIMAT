package de.uni_leipzig.dbs.pprl.primat.examples.setup.io;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVGraphReader {

    public SimilarityGraph<Record> readGraph(String edgeFile, List<Record> recordList) throws IOException {
        Map<String, Record> recordMap = new HashMap<>();
        SimilarityGraph<Record> similarityGraph = new SimilarityGraph<>();
        for(Record r : recordList){
            recordMap.put(r.getId(), r);
            similarityGraph.addVertexToSource(r);
            similarityGraph.addVertexToTarget(r);
        }
        BufferedReader br = new BufferedReader(new FileReader(edgeFile));
        while (br.ready()) {
            String csvLine = br.readLine();
            String[] tokens = csvLine.split(";");
            Record source = recordMap.get(tokens[0]);
            Record target = recordMap.get(tokens[1]);
            SimilarityVector sv = new SimilarityVector(Double.parseDouble(tokens[2]));
            if (similarityGraph.containsSourceVertex(source) && similarityGraph.containsTargetVertex(target)) {
                similarityGraph.addEdge(source, target, sv, sv.getAggregatedValue());
            } else if (similarityGraph.containsSourceVertex(target) && similarityGraph.containsTargetVertex(source)) {
                similarityGraph.addEdge(target, source, sv, sv.getAggregatedValue());
            } else if (similarityGraph.containsSourceVertex(source) && similarityGraph.containsSourceVertex(target)) {
                similarityGraph.addEdge(source, target, sv, sv.getAggregatedValue());
            } else if (similarityGraph.containsTargetVertex(target) && similarityGraph.containsTargetVertex(source)) {
                similarityGraph.addEdge(target, source, sv, sv.getAggregatedValue());
            }
        }
        return similarityGraph;
    }
}
