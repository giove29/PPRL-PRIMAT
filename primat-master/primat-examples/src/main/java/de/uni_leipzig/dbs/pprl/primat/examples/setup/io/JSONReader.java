package de.uni_leipzig.dbs.pprl.primat.examples.setup.io;

import com.google.gson.Gson;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.*;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

/**
 * Class for reading Gradoop graphs in Json
 */
public class JSONReader {

    private Map<String, Record> recordMap;

    public JSONReader(){
        recordMap = new HashMap<>();
    }

    public SimilarityGraph<Record> readGradoopGraph(String folder, String sourceName, String targetName) throws IOException {
        File f = new File(folder);
        SimilarityGraph<Record> similarityGraph = new SimilarityGraph<>();
        if (f.isDirectory()){
            File[] files = f.listFiles();
            Arrays.sort(files, Collections.reverseOrder(Comparator.comparing(f1->f1.getName())));

            for (File objectFile: files){
                if (objectFile.isDirectory()) {
                    if (objectFile.getName().equals("vertices.json")) {
                        for (File vertexFile : objectFile.listFiles()){
                            similarityGraph = readVertexFile(similarityGraph, vertexFile, sourceName, targetName);
                        }
                    } else if (objectFile.getName().equals("edges.json"))  {
                        for (File edgeFile : objectFile.listFiles()){
                            similarityGraph = readEdgeFile(similarityGraph, edgeFile);
                        }
                    }
                } else {
                    if (objectFile.getName().equals("vertices.json")) {
                        similarityGraph = readVertexFile(similarityGraph, objectFile, sourceName, targetName);
                    } else if (objectFile.getName().equals("edges.json"))  {
                        similarityGraph = readEdgeFile(similarityGraph, objectFile);
                    }
                }
            }
        }
        return similarityGraph;
    }


    private SimilarityGraph<Record> readVertexFile(SimilarityGraph<Record> similarityGraph, File f,
                                                   String sourceName, String targetName) throws IOException {
        Gson gson = new Gson();
        BufferedReader br = new BufferedReader(new FileReader(f));
        while (br.ready()) {
            String json = br.readLine();
            Vertex v = gson.fromJson(json, Vertex.class);
            Record r = new Record();
            r.setIdAttribute(new IdAttribute(v.getId()));
            r.setGlobalIdAttribute(new GlobalIdAttribute(v.getData().getRecId()));
            if (v.getData().getSrcId().equals(sourceName)) {
                r.setParty(new Party(sourceName));
                similarityGraph.addVertexToSource(r);
                this.recordMap.put(r.getId(), r);
            } else if (v.getData().getSrcId().equals(targetName)) {
                r.setParty(new Party(targetName));
                similarityGraph.addVertexToTarget(r);
                this.recordMap.put(r.getId(), r);
            }
        }
        return similarityGraph;
    }

    public List<Record> readDataset(String folder, NamedRecordSchemaConfiguration nsc, String sourceName, String targetName) throws IOException {
        File f = new File(folder);
        int col = 0;
        for(Map.Entry<Integer, String> e : nsc.getQidAttributeNameMap().entrySet()){
            RecordSchema.INSTANCE.put(e.getValue(), nsc.getQidAttributeMap().get(e.getKey()), col);
            col ++;
        }
        List<Record> records = new ArrayList<>();
        if (f.isDirectory()){
            File[] files = f.listFiles();
            Arrays.sort(files, Collections.reverseOrder(Comparator.comparing(f1->f1.getName())));
            for (File objectFile: files){
                if (objectFile.isDirectory()) {
                    if (objectFile.getName().equals("vertices.json")) {
                        for (File vertexFile : objectFile.listFiles()){
                            records = readDataset(records, nsc, vertexFile, sourceName, targetName);
                        }
                    }
                } else {
                    if (objectFile.getName().equals("vertices.json")) {
                        records = readDataset(records, nsc, objectFile, sourceName, targetName);
                    }
                }
            }
        }
        return records;
    }

    private List<Record> readDataset(List<Record> records, NamedRecordSchemaConfiguration nsc, File f,
                                     String sourceName, String targetName) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(f));
        while (br.ready()) {
            String json = br.readLine();
            JSONObject v = new JSONObject(json);
            if (v.getJSONObject("data").getString("srcId").equals(sourceName) ||
                    v.getJSONObject("data").getString("srcId").equals(targetName)){
                Record r = new Record();
                r.setPartyAttribute(new PartyAttribute(new Party( v.getJSONObject("data").getString("srcId"))));
                r.setIdAttribute(new IdAttribute(v.getString("id")));
                r.setGlobalIdAttribute(new GlobalIdAttribute(v.getJSONObject("data").getString("recId")));
                for (Map.Entry<Integer, QidAttributeType> e : nsc.getQidAttributeMap().entrySet()) {
                    AttributeType attrType = e.getValue();
                    String attributeValue = "";
                    if(v.getJSONObject("data").has(nsc.getQidAttributeNameMap().get(e.getKey()))) {
                        attributeValue = v.getJSONObject("data").getString(nsc.getQidAttributeNameMap().get(e.getKey()));
                    }
                    final Attribute<?> attr = attrType.constructAttribute();
                    attr.setValueFromString(attributeValue);
                    r.addQidAttribute((QidAttribute<?>) attr);
                }

                records.add(r);
            }
        }
        return records;
    }

    private SimilarityGraph<Record> readEdgeFile(SimilarityGraph<Record> similarityGraph, File f) throws IOException {
        Gson gson = new Gson();
        BufferedReader br = new BufferedReader(new FileReader(f));
        int lines = 0;
        while (br.ready()) {
            String json = br.readLine();

            Edge e = gson.fromJson(json, Edge.class);
            Record source = this.recordMap.get(e.getSource());
            Record target = this.recordMap.get(e.getTarget());
            SimilarityVector sv = new SimilarityVector(e.getData().getValue());
            if (similarityGraph.containsSourceVertex(source) && similarityGraph.containsTargetVertex(target)) {
                similarityGraph.addEdge(source, target, sv);
                lines ++;
            }else if (similarityGraph.containsSourceVertex(target) && similarityGraph.containsTargetVertex(source)){
                similarityGraph.addEdge(target, source, sv);
                lines ++;
            }
        }
        return similarityGraph;
    }

    public class Vertex {
        private String id;
        private Data data;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public  class Data {
            public String getSrcId() {
                return srcId;
            }

            public void setSrcId(String srcId) {
                this.srcId = srcId;
            }

            String srcId;

            public String getRecId() {
                return recId;
            }

            public void setRecId(String recId) {
                this.recId = recId;
            }

            String recId;
        }
    }

    public class Edge {
        private String source;
        private String target;
        private Data data;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public  class Data {
            public double getValue() {
                return value;
            }

            public void setValue(double value) {
                this.value = value;
            }

            double value;
        }
    }
}
