package de.uni_leipzig.dbs.pprl.primat.examples.setup.io;

import de.uni_leipzig.dbs.pprl.primat.common.model.*;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.*;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class GradoopCSVReader {

    private Map<String, Record> recordMap;

    public GradoopCSVReader(){
        recordMap = new HashMap<>();
    }

    public SimilarityGraph<Record> readGradoopGraph(String folder, String sourceName, String targetName) throws IOException {
        File f = new File(folder);
        RecordSchema.INSTANCE.clear();
        SimilarityGraph<Record> similarityGraph = new SimilarityGraph<>();
        if (f.isDirectory()){
            File[] files = f.listFiles();
            Arrays.sort(files, Collections.reverseOrder(Comparator.comparing(f1->f1.getName())));
            File metaFile = List.of(files).stream().filter(f2 -> f2.getName().equals("metadata.csv")).collect(Collectors.toList()).get(0);
            Map<String, Map<String,Integer>> metaMapping = this.parseMetadata(metaFile);
            this.initSchemaInstance(metaMapping);
            for (File objectFile: files){
                if (objectFile.isDirectory()) {
                    if (objectFile.getName().equals("vertices.csv")) {
                        for (File vertexFile : objectFile.listFiles()){
                            similarityGraph = readVertexFile(similarityGraph, vertexFile, sourceName, targetName, metaMapping);
                        }
                    } else if (objectFile.getName().equals("edges.csv"))  {
                        for (File edgeFile : objectFile.listFiles()){
                            similarityGraph = readEdgeFile(similarityGraph, edgeFile);
                        }
                    }
                } else {
                    if (objectFile.getName().equals("vertices.csv")) {
                        similarityGraph = readVertexFile(similarityGraph, objectFile, sourceName, targetName, metaMapping);
                    } else if (objectFile.getName().equals("edges.csv"))  {
                        similarityGraph = readEdgeFile(similarityGraph, objectFile);
                    }
                }
            }
        }
        return similarityGraph;
    }

    public SimilarityGraph<Record> readGradoopGraph(String folder) throws IOException {
        File f = new File(folder);
        RecordSchema.INSTANCE.clear();
        SimilarityGraph<Record> similarityGraph = new SimilarityGraph<>();
        if (f.isDirectory()){
            File[] files = f.listFiles();
            Arrays.sort(files, Collections.reverseOrder(Comparator.comparing(f1->f1.getName())));
            File metaFile = List.of(files).stream().filter(f2 -> f2.getName().equals("metadata.csv")).collect(Collectors.toList()).get(0);
            Map<String, Map<String,Integer>> metaMapping = this.parseMetadata(metaFile);
            this.initSchemaInstance(metaMapping);
            for (File objectFile: files){
                if (objectFile.isDirectory()) {
                    if (objectFile.getName().equals("vertices.csv")) {
                        for (File vertexFile : objectFile.listFiles()){
                            similarityGraph = readVertexFile(similarityGraph, vertexFile, metaMapping);
                        }
                    } else if (objectFile.getName().equals("edges.csv"))  {
                        for (File edgeFile : objectFile.listFiles()){
                            similarityGraph = readEdgeFile(similarityGraph, edgeFile);
                        }
                    }
                } else {
                    if (objectFile.getName().equals("vertices.csv")) {
                        similarityGraph = readVertexFile(similarityGraph, objectFile, metaMapping);
                    } else if (objectFile.getName().equals("edges.csv"))  {
                        similarityGraph = readEdgeFile(similarityGraph, objectFile);
                    }
                }
            }
        }
        return similarityGraph;
    }

    public void readSchema(String folder) throws IOException {
        File f = new File(folder);
        RecordSchema.INSTANCE.clear();
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            Arrays.sort(files, Collections.reverseOrder(Comparator.comparing(f1 -> f1.getName())));
            File metaFile = List.of(files).stream().filter(f2 -> f2.getName().equals("metadata.csv")).collect(Collectors.toList()).get(0);
            Map<String, Map<String, Integer>> metaMapping = this.parseMetadata(metaFile);
            this.initSchemaInstance(metaMapping);
        }

    }


    private SimilarityGraph<Record> readVertexFile(SimilarityGraph<Record> similarityGraph, File f,
                                                   String sourceName, String targetName, Map<String,
            Map<String,Integer>> metaFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        while (br.ready()) {
            String csvLine = br.readLine();
            String[] tokens = StringEscaper.split(csvLine, ";",4);
            String label = StringEscaper.unescape(tokens[2]);

            Record r = new Record();
            r.setIdAttribute(new IdAttribute(tokens[0]));
            r.setParty(new Party(label));
            r = parseProperties(r, label, tokens[3], metaFile);
            if (label.equals(sourceName)) {
                similarityGraph.addVertexToSource(r);
                this.recordMap.put(r.getId(), r);
            } else if (label.equals(targetName)) {
                similarityGraph.addVertexToTarget(r);
                this.recordMap.put(r.getId(), r);
            }
        }
        return similarityGraph;
    }

    private SimilarityGraph<Record> readVertexFile(SimilarityGraph<Record> similarityGraph, File f, Map<String,
            Map<String,Integer>> metaFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        while (br.ready()) {
            String csvLine = br.readLine();
            String[] tokens = StringEscaper.split(csvLine, ";",4);
            String label = StringEscaper.unescape(tokens[2]);

            Record r = new Record();
            r.setIdAttribute(new IdAttribute(tokens[0]));
            r.setParty(new Party(label));
            r = parseProperties(r, label, tokens[3], metaFile);
            similarityGraph.addVertexToSource(r);
            this.recordMap.put(r.getId(), r);
            similarityGraph.addVertexToTarget(r);
            this.recordMap.put(r.getId(), r);
        }
        return similarityGraph;
    }

    public SimilarityGraph<Record> filterGraph(SimilarityGraph<Record> originalGraph, String sourceName, String targetName) {
        SimilarityGraph<Record> similarityGraph = new SimilarityGraph<>();
        for(Linkable r : originalGraph.vertexSet()) {
            if(((Record)r).getParty().getName().equals(sourceName)) {
                similarityGraph.addVertexToSource((Record) r);
            }else if (((Record)r).getParty().getName().equals(targetName)) {
                similarityGraph.addVertexToTarget((Record) r);
            }
        }
        for(SimilarityVector sv : originalGraph.edgeSet()){
            Record source = originalGraph.getEdgeSource(sv);
            Record target = originalGraph.getEdgeTarget(sv);
            if (similarityGraph.containsSourceVertex(source) && similarityGraph.containsTargetVertex(target)) {
                similarityGraph.addEdge(source, target, sv, sv.getAggregatedValue());
            }else if (similarityGraph.containsSourceVertex(target) && similarityGraph.containsTargetVertex(source)){
                similarityGraph.addEdge(target, source, sv, sv.getAggregatedValue());
            }else  if (similarityGraph.containsSourceVertex(source) && similarityGraph.containsSourceVertex(target)) {
                similarityGraph.addEdge(source, target, sv, sv.getAggregatedValue());
            }else if (similarityGraph.containsTargetVertex(target) && similarityGraph.containsTargetVertex(source)){
                similarityGraph.addEdge(target, source, sv, sv.getAggregatedValue());
            }
        }
        return similarityGraph;
    }


//    private Record parseVertexLine(String csvLine) {
//        String[] tokens = StringEscaper.split(csvLine, ";",4);
//        String label = StringEscaper.unescape(tokens[2]);
//        Record r = new Record();
//        r.setGlobalIdAttribute();
//        return vertexFactory.initVertex(
//                GradoopId.fromString(tokens[0]),
//                label,
//                parseProperties(MetaDataSource.VERTEX_TYPE, label, tokens[3]),
//                parseGradoopIds(tokens[1])
//        );
//    }

    private Map<String, Map<String,Integer>> parseMetadata(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        Map<String, Map<String,Integer>> labelMap = new LinkedHashMap<>();
        while (br.ready()){
            String metaLine = br.readLine();
            String[] fields = metaLine.split(";");
            if(fields[0].equals("v")) {
                Map<String, Integer> propsPos = new LinkedHashMap<>();
                labelMap.put(fields[1], propsPos);
                String[] props = fields[2].split(",");
                for(int i = 0; i<props.length; i++) {
                    String[] p = StringEscaper.split(props[i], ":");
                    propsPos.put(p[0], i);
                }
            }
        }
        return labelMap;
    }

    private void initSchemaInstance(Map<String, Map<String,Integer>> metaMapping) {
        int col = 0;
        for(Map.Entry<String,Map<String, Integer>> e : metaMapping.entrySet()) {
            for(Map.Entry<String, Integer> e2 : e.getValue().entrySet()){
                if (!RecordSchema.INSTANCE.getNameColumnMapping().containsKey(e2.getKey())) {
                    RecordSchema.INSTANCE.put(e2.getKey(), QidAttributeType.STRING, col);
                    col ++;
                }
            }
        }
    }

    private Record parseProperties(Record r, String label, String propertyValueString, Map<String, Map<String, Integer>> labelProperties) {
        String[] propertyValues = StringEscaper
                .split(propertyValueString, "|");
        Map<String, Integer> propertyMapping = labelProperties.get(label);
        int col = propertyMapping.get("gtId");
        r.setGlobalIdAttribute(new GlobalIdAttribute(propertyValues[col]));

        for(Map.Entry<String, QidAttributeType> e: RecordSchema.INSTANCE.getNameAttributeTypeMapping().entrySet()){
            final Attribute<?> attr = e.getValue().constructAttribute();
            attr.setValueFromString("");
            r.addQidAttribute((QidAttribute<?>) attr);
        }

        for (Map.Entry<String, Integer> nameCol : propertyMapping.entrySet()) {
            if (!nameCol.getKey().equals("gtId")) {
                int globalCol = RecordSchema.INSTANCE.getNameColumnMapping().get(nameCol.getKey());
                QidAttributeType qidAttributeType = RecordSchema.INSTANCE.getAttributeType(globalCol);
                String value = propertyValues[nameCol.getValue()];
//                if (nameCol.getKey().equals("<page title>") && label.equals("www.eglobalcentral.co.uk")) {
//                    System.out.println(label + " column "+ nameCol.getValue());
//                    System.out.println(value);
//                }
                final Attribute<?> attr = qidAttributeType.constructAttribute();
                attr.setValueFromString(value);
                r.addQidAttribute(globalCol, (QidAttribute<?>) attr);
            }
        }
        return r;
    }

    private SimilarityGraph<Record> readEdgeFile(SimilarityGraph<Record> similarityGraph, File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        while (br.ready()) {
            String csvLine = br.readLine();
            String[] tokens = csvLine.split(";");
            Record source = this.recordMap.get(tokens[2]);
            Record target = this.recordMap.get(tokens[3]);
            SimilarityVector sv = new SimilarityVector(Double.valueOf(tokens[5]));
            if (similarityGraph.containsSourceVertex(source) && similarityGraph.containsTargetVertex(target)) {
                similarityGraph.addEdge(source, target, sv, sv.getAggregatedValue());
            }else if (similarityGraph.containsSourceVertex(target) && similarityGraph.containsTargetVertex(source)){
                similarityGraph.addEdge(target, source, sv, sv.getAggregatedValue());
            }else  if (similarityGraph.containsSourceVertex(source) && similarityGraph.containsSourceVertex(target)) {
                similarityGraph.addEdge(source, target, sv, sv.getAggregatedValue());
            }else if (similarityGraph.containsTargetVertex(target) && similarityGraph.containsTargetVertex(source)){
                similarityGraph.addEdge(target, source, sv, sv.getAggregatedValue());
            }
        }
        return similarityGraph;
    }

    public List<Record> readDataset(String folder, String sourceName, String targetName) throws IOException {
        File f = new File(folder);

        List<Record> records = new ArrayList<>();
        System.out.println(RecordSchema.INSTANCE.getNameColumnMapping());
        if (recordMap.isEmpty()) {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                File metaFile = List.of(files).stream().filter(f2 -> f2.getName().equals("metadata.csv")).collect(Collectors.toList()).get(0);
                Map<String, Map<String, Integer>> metaMapping = this.parseMetadata(metaFile);
                if(RecordSchema.INSTANCE.getNameColumnMapping().isEmpty()) {
                    initSchemaInstance(metaMapping);
                }
                Arrays.sort(files, Collections.reverseOrder(Comparator.comparing(f1 -> f1.getName())));
                for (File objectFile : files) {
                    if (objectFile.isDirectory()) {
                        if (objectFile.getName().equals("vertices.csv")) {
                            for (File vertexFile : objectFile.listFiles()) {
                                records = readDataset(records, vertexFile, sourceName, targetName, metaMapping);
                            }
                        }
                    } else {
                        if (objectFile.getName().equals("vertices.csv")) {
                            records = readDataset(records, objectFile, sourceName, targetName, metaMapping);
                        }
                    }
                }
            }
        }else {
            for (Map.Entry<String, Record> e: recordMap.entrySet()) {
                records.add(e.getValue());
            }
        }
        return records;
    }

    public List<Record> readDataset(String folder) throws IOException {
        File f = new File(folder);

        List<Record> records = new ArrayList<>();
        System.out.println(RecordSchema.INSTANCE.getNameColumnMapping());
        if (recordMap.isEmpty()) {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                File metaFile = List.of(files).stream().filter(f2 -> f2.getName().equals("metadata.csv")).collect(Collectors.toList()).get(0);
                Map<String, Map<String, Integer>> metaMapping = this.parseMetadata(metaFile);
                if(RecordSchema.INSTANCE.getNameColumnMapping().isEmpty()) {
                    initSchemaInstance(metaMapping);
                }
                Arrays.sort(files, Collections.reverseOrder(Comparator.comparing(f1 -> f1.getName())));
                for (File objectFile : files) {
                    if (objectFile.isDirectory()) {
                        if (objectFile.getName().equals("vertices.csv")) {
                            for (File vertexFile : objectFile.listFiles()) {
                                records = readDataset(records, vertexFile, metaMapping);
                            }
                        }
                    } else {
                        if (objectFile.getName().equals("vertices.csv")) {
                            records = readDataset(records, objectFile, metaMapping);
                        }
                    }
                }
            }
        }else {
            for (Map.Entry<String, Record> e: recordMap.entrySet()) {
                records.add(e.getValue());
            }
        }
        return records;
    }

    private List<Record> readDataset(List<Record> records, File f,
                                     String sourceName, String targetName, Map<String,
            Map<String,Integer>> metaFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        while (br.ready()) {
            String csvLine = br.readLine();
            String[] tokens = StringEscaper.split(csvLine, ";", 4);
            String label = StringEscaper.unescape(tokens[2]);
            if (label.equals(sourceName) || label.equals(targetName)) {
                Record r = new Record();
                r.setIdAttribute(new IdAttribute(tokens[0]));
                r.setParty(new Party(label));
                r = parseProperties(r, label, tokens[3], metaFile);
                records.add(r);
            }
        }
        return records;
    }

    private List<Record> readDataset(List<Record> records, File f,
                                     Map<String,
            Map<String,Integer>> metaFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        while (br.ready()) {
            String csvLine = br.readLine();
            String[] tokens = StringEscaper.split(csvLine, ";", 4);
            String label = StringEscaper.unescape(tokens[2]);
            Record r = new Record();
            r.setIdAttribute(new IdAttribute(tokens[0]));
            r.setParty(new Party(label));
            r = parseProperties(r, label, tokens[3], metaFile);
            records.add(r);

        }
        return records;
    }

    public List<Record> filterDataset(List<Record> completeDataset, String s, String s1) {
        List<Record> filteredList = new ArrayList<>();
        for(Record r: completeDataset) {
            if(r.getParty().getName().equals(s) || r.getParty().getName().equals(s1)) {
                filteredList.add(r);
            }
        }
        return filteredList;
    }




}
