package de.uni_leipzig.dbs.pprl.primat.examples.setup;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram.BigramExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.qgram.SpecialTrigramExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.NamedRecordSchemaConfiguration;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.NonQidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;
import de.uni_leipzig.dbs.pprl.primat.common.utils.DatasetReader;
import de.uni_leipzig.dbs.pprl.primat.common.utils.RandomFactory;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.Encoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterEncoder;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.BloomFilterExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hardening.NoHardener;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.HashingMethod;
import de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing.RandomHashing;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.FieldNormalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.NormalizeDefinition;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.*;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.io.GradoopCSVReader;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.io.JSONReader;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DataSources {

    public static List<Record> getNCVR(String inputPath) throws IOException {
        final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
                .add(0, NonQidAttributeType.PARTY)
                .add(1, NonQidAttributeType.GLOBAL_ID)
                .add(2, NonQidAttributeType.ID)
                .add(3, QidAttributeType.STRING, "FN")
                .add(4, QidAttributeType.STRING, "MN")
                .add(5, QidAttributeType.STRING, "LN")
                .add(6, QidAttributeType.STRING, "YOB")
                .add(7, QidAttributeType.STRING, "BIRTH_PLACE")
                .add(9, QidAttributeType.STRING, "CITY")
                .build();

        final DatasetReader reader = new DatasetReader(inputPath, rsc);
        final List<Record> dataset = reader.read();
        return dataset;
    }

    public static List<Record> getAlmser(String inputPath, String nodePath) throws IOException {
        final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
                .add(0, NonQidAttributeType.PARTY)
                .add(1, NonQidAttributeType.ID)
                .add(2, NonQidAttributeType.GLOBAL_ID)
                .add(3, QidAttributeType.STRING, "title")
                .add(4, QidAttributeType.STRING, "description")
                .add(5, QidAttributeType.STRING, "brand")
                .add(6, QidAttributeType.STRING, "Part Number")
                .add(7, QidAttributeType.STRING, "Sub-Category")
                .add(8, QidAttributeType.STRING, "Category")
                .add(9, QidAttributeType.STRING, "Generation")
                .add(10, QidAttributeType.STRING, "Capacity")
                .add(11, QidAttributeType.STRING, "Spindle Speed")
                .add(12, QidAttributeType.STRING, "Manufacturer")
                .add(13, QidAttributeType.STRING, "price")
                .build();
        final DatasetReader reader = new DatasetReader(inputPath, rsc);
        final List<Record> dataset = reader.read();
        final List<Record> filterDataset = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(nodePath));
        Set<String> ids = new HashSet<>();
        while (br.ready()){
            String line = br.readLine();
            for(String id :line.split(",")) {
                ids.add(id);
            }
        }
        br.close();
        for(Record r: dataset) {
            if(ids.contains(r.getId())) {
                filterDataset.add(r);
            }
        }
        return filterDataset;

    }

    public static List<Record> getMusicBrainz(String inputFolder, String source, String target) throws IOException {
        final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
                .add(0, NonQidAttributeType.PARTY)
                .add(1, NonQidAttributeType.GLOBAL_ID)
                .add(2, NonQidAttributeType.ID)
                .add(3, QidAttributeType.STRING, "title")
                .add(4, QidAttributeType.STRING, "language")
                .add(5, QidAttributeType.STRING, "length")
                .add(6, QidAttributeType.STRING, "album")
                .add(7, QidAttributeType.STRING, "artist")
                .add(8, QidAttributeType.STRING, "number")
                .add(9, QidAttributeType.STRING, "year")
                .build();

        final JSONReader reader = new JSONReader();
        final List<Record> dataset = reader.readDataset(inputFolder, rsc, source, target);
        final NormalizerChain normChain = new NormalizerChain(
                List.of(new UmlautNormalizer(), new TrimNormalizer(), new LowerCaseNormalizer(), new AccentRemover(),
                        new SpecialCharacterRemover()));
        final NormalizerChain titleNormChain = new NormalizerChain(
                List.of(new UmlautNormalizer(), new TrimNormalizer(), new LowerCaseNormalizer(), new AccentRemover(),
                        new SpecialCharacterRemover(), new DigitRemover()));

        final NormalizerChain numberNorm = new NormalizerChain(new LetterLowerCaseToNumberNormalizer(),
                new LetterUpperCaseToNumberNormalizer());

        final NormalizeDefinition normDef = new NormalizeDefinition();
        for (Map.Entry<String, Integer> entry : RecordSchema.INSTANCE.getNameColumnMapping().entrySet()) {
            if (entry.getKey().equals("title"))
                normDef.setNormalizer(entry.getValue(), titleNormChain);
            else
                normDef.setNormalizer(entry.getValue(), normChain);

        }
        final FieldNormalizer fn = new FieldNormalizer(normDef);
        fn.preprocess(dataset);
        return dataset;
    }

    public static List<Record> getDexterData(String absolutePath, String source, String target) throws IOException {
        GradoopCSVReader csvReader = new GradoopCSVReader();
        List<Record> dataSet = csvReader.readDataset(absolutePath, source, target);
        return dataSet;
    }

    public static List<Record> getDexterData(String absolutePath) throws IOException {
        GradoopCSVReader csvReader = new GradoopCSVReader();
        List<Record> dataSet = csvReader.readDataset(absolutePath);
        return dataSet;
    }

    public static List<Record> getGeoData(String inputFolder, String source, String target) throws IOException {
        final NamedRecordSchemaConfiguration rsc = new NamedRecordSchemaConfiguration.Builder()
                .add(0, NonQidAttributeType.PARTY)
                .add(1, NonQidAttributeType.GLOBAL_ID)
                .add(2, NonQidAttributeType.ID)
                .add(3, QidAttributeType.STRING, "label")
                .add(4, QidAttributeType.STRING, "lon")
                .add(5, QidAttributeType.STRING, "lat")
                .build();

        final JSONReader reader = new JSONReader();
        final List<Record> dataset = reader.readDataset(inputFolder, rsc, source, target);
        final NormalizerChain normChain = new NormalizerChain(
                List.of(new UmlautNormalizer(), new TrimNormalizer(), new LowerCaseNormalizer(), new AccentRemover(),
                        new SpecialCharacterRemover()));
        final NormalizerChain titleNormChain = new NormalizerChain(
                List.of(new UmlautNormalizer(), new TrimNormalizer(), new LowerCaseNormalizer(), new AccentRemover(),
                        new SpecialCharacterRemover(), new DigitRemover()));

        final NormalizerChain numberNorm = new NormalizerChain(new LetterLowerCaseToNumberNormalizer(),
                new LetterUpperCaseToNumberNormalizer());

        final NormalizeDefinition normDef = new NormalizeDefinition();
        for (Map.Entry<String, Integer> entry : RecordSchema.INSTANCE.getNameColumnMapping().entrySet()) {
            if (entry.getKey().equals("label"))
                normDef.setNormalizer(entry.getValue(), titleNormChain);
            else
                normDef.setNormalizer(entry.getValue(), normChain);

        }
        final FieldNormalizer fn = new FieldNormalizer(normDef);
        fn.preprocess(dataset);
        return dataset;
    }

    public static Map<String, String> getNCVRAttributePairs() {
        Map<String, String> attributePairs = new LinkedHashMap<>();
        attributePairs.put("FN", "FN");
        attributePairs.put("MN", "MN");
        attributePairs.put("LN", "LN");
        attributePairs.put("YOB", "YOB");
        attributePairs.put("BIRTH_PLACE", "BIRTH_PLACE");
        return attributePairs;
    }

    public static Map<String, String> getMusicBrainzAttributePairs(){
        Map<String, String> attributePairs = new LinkedHashMap<>();
        attributePairs.put("title", "title");
        attributePairs.put("language", "language");
        attributePairs.put("length", "length");
        attributePairs.put("album", "album");
        attributePairs.put("artist", "artist");
        attributePairs.put("number", "number");
        attributePairs.put("year", "year");
        return attributePairs;
    }

    public static Map<String, String> getGeoAttributePairs(){
        Map<String, String> attributePairs = new LinkedHashMap<>();
        attributePairs.put("label", "label");
        attributePairs.put("lon", "lon");
        attributePairs.put("lat", "lat");
        return attributePairs;
    }

    public static Map<String, String> getDexterPairs(){
        Map<String, String> attributePairs = new LinkedHashMap<>();

        attributePairs.put("famer_product_name", "famer_product_name");
        attributePairs.put("famer_model_list", "famer_model_list");
        attributePairs.put("famer_model_no_list", "famer_model_no_list");
        attributePairs.put("famer_brand_list", "famer_brand_list");
        attributePairs.put("famer_keys", "famer_keys");
        attributePairs.put("<page title>", "<page title>");
        return attributePairs;
    }

    public static List<Record> getEncodedNCVR(List<Record> records) throws IOException {
        final boolean padding = false;
        final FeatureExtractor featEx = new BigramExtractor(padding);
        // TrigramExtractor(padding);
        final FeatureExtractor featExSpecial = new SpecialTrigramExtractor(padding);
        int k = 10;
        // NCVR
        // /*
        final BloomFilterExtractorDefinition exDefFN = new BloomFilterExtractorDefinition();
        exDefFN.setColumnsByName("FN");
        exDefFN.setExtractors(featEx);
        exDefFN.setNumberOfHashFunctions(12);
        exDefFN.setSalt("FN_");

        final BloomFilterExtractorDefinition exDefMN = new BloomFilterExtractorDefinition();
        exDefMN.setColumnsByName("MN");
        exDefMN.setExtractors(featEx);
        exDefMN.setNumberOfHashFunctions(3);
        exDefMN.setSalt("MN_");

        final BloomFilterExtractorDefinition exDefLN = new BloomFilterExtractorDefinition();
        exDefLN.setColumnsByName("LN");
        exDefLN.setExtractors(featEx);
        exDefLN.setNumberOfHashFunctions(13);
        exDefLN.setSalt("LN_");

        final BloomFilterExtractorDefinition exDefYob = new BloomFilterExtractorDefinition();
        exDefYob.setColumnsByName("YOB");
        exDefYob.setExtractors(featEx);
        exDefYob.setNumberOfHashFunctions(13);
        exDefYob.setSalt("YOB_");

        final BloomFilterExtractorDefinition exDefBP = new BloomFilterExtractorDefinition();
        exDefBP.setColumnsByName("BIRTH_PLACE");
        exDefBP.setExtractors(featEx);
        exDefBP.setNumberOfHashFunctions(3);
        exDefBP.setSalt("BIRTHPLACE_");

        final BloomFilterExtractorDefinition exDefCity = new BloomFilterExtractorDefinition();
        exDefCity.setColumnsByName("CITY");
        exDefCity.setExtractors(featEx);
        exDefCity.setNumberOfHashFunctions(2);
        exDefCity.setSalt("CITY_");
        final HashingMethod hashing = new RandomHashing(1024, RandomFactory.SECURE_RANDOM);

        final BloomFilterDefinition def1 = new BloomFilterDefinition();
        def1.setName("RBF");
        def1.setBfLength(1024);
        def1.setHashingMethod(hashing);
        def1.setFeatureExtractors(List.of(
                exDefFN, exDefMN, exDefLN, exDefYob, exDefBP, exDefCity));
        def1.setHardener(new NoHardener());
        final Encoder encoder = new BloomFilterEncoder(List.of(def1));
        // new TwoStepHashEncoder(List.of(tshdef));

        final List<Record> encodedRecords = encoder.encode(records);
        final Map<Integer, Set<String>> featurePositionMapping = def1.getHashingMethod().getFeaturePositionMapping();
        System.out.println("Feature_Position_Mapping:");
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (Map.Entry<Integer, Set<String>> entry : featurePositionMapping.entrySet()) {
            stats.addValue(entry.getValue().size());
        }
        DescriptiveStatistics lengthStat = new DescriptiveStatistics();

        for (final Record rec : encodedRecords) {
            final int card = rec.getBitSetAttributes().get(0).getValue().cardinality();
            lengthStat.addValue(card);
        }

        System.out.println(lengthStat);
        return encodedRecords;
    }


}
