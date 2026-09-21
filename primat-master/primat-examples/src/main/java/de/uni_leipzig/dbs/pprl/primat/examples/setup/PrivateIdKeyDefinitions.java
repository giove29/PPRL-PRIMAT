package de.uni_leipzig.dbs.pprl.primat.examples.setup;

import de.uni_leipzig.dbs.pprl.primat.analysis.AnalysisResult;
import de.uni_leipzig.dbs.pprl.primat.analysis.DataSetAnalyzer;
import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeAvailability;
import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeLength;
import de.uni_leipzig.dbs.pprl.primat.analysis.attribute.AttributeMostFrequent;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.Result;
import de.uni_leipzig.dbs.pprl.primat.analysis.results.ResultSet;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.*;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.phonetic.PhoneticCodeExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.Attribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttributeType;

import java.math.BigDecimal;
import java.util.*;

public class PrivateIdKeyDefinitions {

    public static List<ExtractorDefinition> getSoundexFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("SN_FN");
        exDef1.setColumnsByName("FN");
        exDef1.setExtractors(PhoneticCodeExtractor.SOUNDEX);
        ExtractorDefinition exDef2 = new ExtractorDefinition("SN_LN");
        exDef2.setColumnsByName("LN");
        exDef2.setExtractors(PhoneticCodeExtractor.SOUNDEX);
        ExtractorDefinition exDef3 = new ExtractorDefinition("YOB");
        exDef3.setColumnsByName("YOB");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef2, exDef3);
    }


    public static List<ExtractorDefinition> get3FirstLettersFNFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3FN");
        exDef1.setColumnsByName("FN");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef2 = new ExtractorDefinition("3LN");
        exDef2.setColumnsByName("LN");
        exDef2.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef3 = new ExtractorDefinition("YOB");
        exDef3.setColumnsByName("YOB");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1);
    }


    public static List<ExtractorDefinition> get3FirstLettersFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3FN");
        exDef1.setColumnsByName("FN");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef2 = new ExtractorDefinition("3LN");
        exDef2.setColumnsByName("LN");
        exDef2.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef3 = new ExtractorDefinition("YOB");
        exDef3.setColumnsByName("YOB");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef2, exDef3);
    }

    public static List<ExtractorDefinition> get2FirstLettersFunction(){
        ExtractorDefinition exDef1 = new ExtractorDefinition("2FN");
        exDef1.setColumnsByName("FN");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,2));
        ExtractorDefinition exDef2 = new ExtractorDefinition("2LN");
        exDef2.setColumnsByName("LN");
        exDef2.setExtractors(new SubstringByPositionExtractor(0,2));
        ExtractorDefinition exDef3 = new ExtractorDefinition("YOB");
        exDef3.setColumnsByName("YOB");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef2, exDef3);
    }

    public static List<ExtractorDefinition> getMBSoundexFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("SN_title");
        exDef1.setColumnsByName("title");
        exDef1.setExtractors(PhoneticCodeExtractor.SOUNDEX);
        ExtractorDefinition exDef2 = new ExtractorDefinition("artist");
        exDef2.setColumnsByName("artist");
        exDef2.setExtractors(PhoneticCodeExtractor.SOUNDEX);
        ExtractorDefinition exDef3 = new ExtractorDefinition("year");
        exDef3.setColumnsByName("year");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef3);
    }

    public static List<ExtractorDefinition> getMB3FirstLettersAlbumFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3album");
        exDef1.setColumnsByName("album");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef3 = new ExtractorDefinition("year");
        exDef3.setColumnsByName("year");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef3);
    }


    public static List<ExtractorDefinition> getMB3FirstLettersFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3title");
        exDef1.setColumnsByName("title");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef3 = new ExtractorDefinition("year");
        exDef3.setColumnsByName("year");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef3);
    }

    public static List<ExtractorDefinition> getMB3FirstLettersTitleArtistFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3title");
        exDef1.setColumnsByName("title");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        final ExtractorDefinition exDef2 = new ExtractorDefinition("3artist");
        exDef2.setColumnsByName("artist");
        exDef2.setExtractors(new SubstringByPositionExtractor(0,3));
        return Arrays.asList(exDef1, exDef2);
    }

    public static List<ExtractorDefinition> getMB3FirstLettersTitleFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3title");
        exDef1.setColumnsByName("title");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        return Arrays.asList(exDef1);
    }

    public static List<ExtractorDefinition> getMB2FirstLettersTitleAlbumFunction(){
        ExtractorDefinition exDef1 = new ExtractorDefinition("2title");
        exDef1.setColumnsByName("title");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,2));
        ExtractorDefinition exDef2 = new ExtractorDefinition("2album");
        exDef2.setColumnsByName("album");
        exDef2.setExtractors(new SubstringByPositionExtractor(0,2));
        ExtractorDefinition exDef3 = new ExtractorDefinition("year");
        exDef3.setColumnsByName("year");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef2, exDef3);
    }

    public static List<ExtractorDefinition> getMB2FirstLettersTitleFunction(){
        ExtractorDefinition exDef1 = new ExtractorDefinition("2title");
        exDef1.setColumnsByName("title");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,2));
        ExtractorDefinition exDef3 = new ExtractorDefinition("year");
        exDef3.setColumnsByName("year");
        exDef3.setExtractors(new IdentityExtractor());
        return Arrays.asList(exDef1, exDef3);
    }

    public static List<ExtractorDefinition> getGeo3FirstLettersLabelFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3label");
        exDef1.setColumnsByName("label");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        return Arrays.asList(exDef1);
    }

    public static List<ExtractorDefinition> getGeo3FirstLettersLabelLonLatFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3label");
        exDef1.setColumnsByName("label");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef2 = new ExtractorDefinition("1lon");
        exDef2.setColumnsByName("lon");
        exDef2.setExtractors(new SubstringByPositionExtractor(0,1));
        ExtractorDefinition exDef3 = new ExtractorDefinition("1lat");
        exDef3.setColumnsByName("lat");
        exDef3.setExtractors(new SubstringByPositionExtractor(0,1));
        return Arrays.asList(exDef1, exDef2, exDef3);
    }

    public static List<ExtractorDefinition> getDexter3FirstLetterNameBrandFunction(){
        final ExtractorDefinition exDef1 = new ExtractorDefinition("3name");
        exDef1.setColumnsByName("famer_product_name");
        exDef1.setExtractors(new SubstringByPositionExtractor(0,3));
        ExtractorDefinition exDef2 = new ExtractorDefinition("3brand");
        exDef2.setColumnsByName("famer_brand_list");
        exDef2.setExtractors(new SubstringByPositionExtractor(0,3));
        return Arrays.asList(exDef1, exDef2);
    }

    private static void addKeyFeatures(List<Record> dataSet, FeatureExtractor extractor, Map<String, String> compAtts) {
        Set<String> usedAttrs = new HashSet<>(compAtts.keySet());
        usedAttrs.addAll(compAtts.values());
        for (Record r : dataSet) {
            int col = 0;
            List<QidAttribute<?>> keyAtts = new ArrayList<>();
            for (QidAttribute<?> attribute : r.getAttributes()) {
                if(RecordSchema.INSTANCE.getAttributeName(col)!= null) {
                    if (!RecordSchema.INSTANCE.getAttributeName(col).contains("key_") &&
                            usedAttrs.contains(RecordSchema.INSTANCE.getAttributeName(col))) {

                        ExtractorDefinition exDef = new ExtractorDefinition("key_" + RecordSchema.INSTANCE.getAttributeName(col));
                        exDef.setExtractors(extractor);
                        exDef.setColumnsByName(RecordSchema.INSTANCE.getAttributeName(col));
                        final FeatureExtraction featEx = new FeatureExtraction(exDef);
                        List<String> values = featEx.getFeatures(r);
                        final Attribute<?> attr = attribute.getType().constructAttribute();
                        attr.setValueFromString(values.get(0));
                        keyAtts.add((QidAttribute<?>) attr);
                        if (!RecordSchema.INSTANCE.getNameAttributeTypeMapping()
                                .containsKey("key_" + RecordSchema.INSTANCE.getAttributeName(col))) {
                            RecordSchema.INSTANCE.put("key_" + RecordSchema.INSTANCE.getAttributeName(col), (QidAttributeType) attr.getType(),
                                    RecordSchema.INSTANCE.getNameColumnMapping().size());
                        }
                    }
                }
                col++;
            }
            for (QidAttribute<?> a : keyAtts) {
                r.addQidAttribute(a);
            }
        }
    }

    private static Map<String, String> filterInvalidAttributePairs(ResultSet resultSetSource, ResultSet resultSetTarget,
        Map<String, String> attributePairs, double validRatio, double validDifference) {
        int i = 0;
        Map<String, BigDecimal> srcAttributeMetric = new HashMap<>();
        Map<String, BigDecimal> targetAttributeMetric = new HashMap<>();
        for(Result r : resultSetSource.getResults()) {
            srcAttributeMetric.put(r.getParams().get(AttributeAvailability.HEADER_ATTRIBUTE),
                    r.getMetrics().get(AttributeAvailability.VALID));
        }
        for(Result r : resultSetTarget.getResults()) {
            targetAttributeMetric.put(r.getParams().get(AttributeAvailability.HEADER_ATTRIBUTE),
                    r.getMetrics().get(AttributeAvailability.VALID));
        }
        Map<String, String> filteredPairs = new HashMap<>();
        for(Map.Entry<String, String> e : attributePairs.entrySet()) {
            double srcValue = srcAttributeMetric.get(e.getKey()).doubleValue();
            double targetValue = targetAttributeMetric.get(e.getValue()).doubleValue();
            if (srcValue >= validRatio && targetValue >= validRatio &&
            Math.abs(srcValue-targetValue) <= validDifference) {
                filteredPairs.put(e.getKey(), e.getValue());
            }
        }
        return filteredPairs;
    }

    public List<List<ExtractorDefinition>> getKeyDefinitions (List<Record> dataSet, String source, String target,
                                                               FeatureExtractor extractor, Map<String, String> attributePairs,
                                                              double validRatioThreshold, double uniqueDistrThreshold){

        DataSetAnalyzer analyzer = new DataSetAnalyzer();
        addKeyFeatures(dataSet, extractor, attributePairs);
        analyzer.addAnalyzer(new AttributeAvailability());
        analyzer.addAnalyzer(new AttributeMostFrequent());
        analyzer.addAnalyzer(new AttributeLength());
        analyzer.setRunPerSource(true);
        AnalysisResult result = analyzer.run(dataSet);
        List<ResultSet> attributeResults = result.getResults().get(source);
        Map<String, Double> lengthDistribution = new HashMap<>();
        for (int i = 0; i < attributeResults.size(); i++) {
            if (i == attributeResults.size()-1) {
                for(Result r : attributeResults.get(i).getResults()){
                    lengthDistribution.put(r.getParams().get(AttributeAvailability.HEADER_ATTRIBUTE),
                            r.getMetrics().get("median").doubleValue());
                }
            }
        }

        Map<String, String> filtered = filterInvalidAttributePairs(result.getResults().get(source).get(0),
                result.getResults().get(target).get(0), attributePairs,validRatioThreshold, 0.05);
        int topK = 0;
        List<AttributeListCombination> bestCombination = combineKeyFeatures(dataSet, source, target, extractor, filtered, lengthDistribution, uniqueDistrThreshold);
        List<List<ExtractorDefinition>> definitions = new ArrayList<>();
        for(AttributeListCombination acComb : bestCombination) {
            List<ExtractorDefinition> extractorDefinitions = new ArrayList<>();
            for(String property : acComb.attributeCombinationA) {
                if (lengthDistribution.get(property)<5) {
                    ExtractorDefinition exDef = new ExtractorDefinition("id" + property);
                    exDef.setColumnsByName(property);
                    exDef.setExtractors(new IdentityExtractor());
                    extractorDefinitions.add(exDef);
                }else {
                    ExtractorDefinition exDef = new ExtractorDefinition("ex" + property);
                    exDef.setColumnsByName(property);
                    exDef.setExtractors(extractor);
                    extractorDefinitions.add(exDef);
                }
            }
            definitions.add(extractorDefinitions);
        }

        return definitions;
    }

    private boolean isInvalid(String value) {
        Set<String> invalidValue = new HashSet<>(Arrays.asList("null", "NULL", "Null", "NaN"));
        return value == null || invalidValue.contains(value);
    }


    private List<AttributeListCombination> combineKeyFeatures(List<Record> dataSet, String source, String target,
                                                   FeatureExtractor extractor, Map<String, String> filtered,
                                                              Map<String, Double> lengthDistribution, double impurityThreshold) {
        List<AttributeListCombination> combinations = new ArrayList<>();
        for (Map.Entry<String, String> e : filtered.entrySet()) {
            AttributeListCombination comb = new AttributeListCombination();
            comb.attributeCombinationA.add(e.getKey());
            comb.attributeCombinationB.add(e.getValue());
            combinations.add(comb);
        }

        Set<AttributeListCombination> result = new HashSet<>();
//        Map<AttributeListCombination, Set<AttributeListCombination>> newCombs = getCombinations(combinations);
        Map<AttributeListCombination, Set<AttributeListCombination>> newCombs = new HashMap<>();
        for(AttributeListCombination comb: combinations) {
            newCombs.put(comb, new HashSet<>());
        }
        Map<AttributeListCombination, Set<AttributeListCombination>> filteredCombination = new HashMap<>();
        do {
            filteredCombination.clear();
            for (Map.Entry<AttributeListCombination, Set<AttributeListCombination>> ac : newCombs.entrySet()) {
                ExtractorDefinition exDef = new ExtractorDefinition("keyCombA");
                exDef.setExtractors(new IdentityExtractor());
                exDef.setColumnsByName(ac.getKey().attributeCombinationA.toArray(new String[]{}));
                ExtractorDefinition exDef2 = new ExtractorDefinition("keyCombB");
                exDef2.setExtractors(new IdentityExtractor());
                exDef2.setColumnsByName(ac.getKey().attributeCombinationB.toArray(new String[]{}));
                final FeatureExtraction featEx = new FeatureExtraction(exDef);
                final FeatureExtraction featEx2 = new FeatureExtraction(exDef2);
                List<String> sourceValueComb = new ArrayList<>();
                List<String> targetValueComb = new ArrayList<>();
                for (Record r : dataSet) {
                    if (r.getParty().getName().equals(source)) {
                        List<String> values = featEx.getFeatures(r);
                        String valueComb = "";
                        boolean isInvalidValue = false;
                        for (String v : values) {
                            if(!isInvalid(v) && !v.isEmpty()) {
                                valueComb += v;
                            }else{
                                isInvalidValue = true;
                                break;
                            }
                        }
                        if(!isInvalidValue)
                            sourceValueComb.add(valueComb);
                    } else if (r.getParty().getName().equals(target)) {
                        List<String> values = featEx2.getFeatures(r);
                        String valueComb = "";
                        boolean isInvalidValue = false;
                        for (String v : values) {
                            if(!isInvalid(v) && !v.isEmpty()) {
                                valueComb += v;
                            } else{
                                isInvalidValue = true;
                                break;
                            }
                        }
                        if(!isInvalidValue)
                            targetValueComb.add(valueComb);
                    }
                }
                double uniquenessA = computeUniqueness(sourceValueComb);
                double uniquenessB = computeUniqueness(targetValueComb);
                double simA = computeDistributionDistance(sourceValueComb);
                double simB = computeDistributionDistance(targetValueComb);
                ac.getKey().scoreA = 2*(simA * uniquenessA)/(simA + uniquenessA);
                ac.getKey().scoreB = 2*(simB * uniquenessB)/(simB + uniquenessB);
                if ((ac.getKey().scoreA + ac.getKey().scoreB)/2 > impurityThreshold) {
                    result.add(ac.getKey());
                    filteredCombination.put(ac.getKey(), ac.getValue());

                    if (ac.getValue().size()>0) {
                        for(AttributeListCombination acOld : ac.getValue()) {
                            result.remove(acOld);
                        }
                    }
                }
            }
            newCombs = getCombinations(new ArrayList<>(filteredCombination.keySet()));
        }while(newCombs.size()>0);
        return new ArrayList<>(result);
    }

    private double computeUniqueness(List<String> values) {
        Set<String> uniqueness = new HashSet<>();
        uniqueness.addAll(values);
        return uniqueness.size()/(double)(values.size());
    }

    private double computeDistributionDistance(List<String> values) {
        Map<String, Integer> frequencies = new HashMap<>();
        for(String v : values) {
            Integer count = frequencies.get(v);
            if (frequencies.containsKey(v)) {
                frequencies.put(v, count + 1);
            } else {
                frequencies.put(v, 1);
            }

        }
        List<Integer> orderedFrequencies = new ArrayList<>(frequencies.values());
        Collections.sort(orderedFrequencies, Collections.reverseOrder());
        double avgSize = values.size()/(double)orderedFrequencies.size();
        double distance = 0;

        double variance = 0;
        for(int i = 0; i < orderedFrequencies.size(); i ++) {
            distance += Math.min(orderedFrequencies.get(i), avgSize);
            variance += Math.pow(orderedFrequencies.get(i)-avgSize, 2);
        }
        double uniqueWeight = orderedFrequencies.size()/(double)values.size();
        uniqueWeight = (uniqueWeight * (1-uniqueWeight))/0.25;
//        System.out.println(uniqueWeight);
        return (distance/(double) values.size())*uniqueWeight;
    }



    private double computeEntropy(List<String> values) {
        Map<String, Integer> frequencies = new HashMap<>();
        for(String v : values) {
            Integer count = frequencies.get(v);
            if(frequencies.containsKey(v)) {
                frequencies.put(v, count+1);
            }else {
                frequencies.put(v, 1);
            }
        }
        int totalSum = values.size();
        double entropy = 0;
        for (Integer f: frequencies.values()) {
            double prob = f/(double)totalSum;
            entropy += prob*(Math.log(prob));
        }
        double normalizedEntropy = -entropy/Math.log(frequencies.size());
//        System.out.println(normalizedEntropy);
        return normalizedEntropy;
    }

    private double computeGiniImpurity(List<String> values) {
        Map<String, Integer> frequencies = new HashMap<>();

        for(String v : values) {
            Integer count = frequencies.get(v);
            if(frequencies.containsKey(v)) {
                frequencies.put(v, count+1);
            }else {
                frequencies.put(v, 1);
            }
        }
        double impurity = 0;
        for(Map.Entry<String, Integer> e : frequencies.entrySet()) {
            impurity += (e.getValue()/(double)values.size() * (1 - (double)e.getValue()/(double)values.size()));
        }
        return impurity;
    }

    /**
     * generate new combinations of attributes using apriori strategy
     * @param oldCombs
     * @return
     */
    private Map<AttributeListCombination, Set<AttributeListCombination>> getCombinations(List<AttributeListCombination> oldCombs) {
        Map<AttributeListCombination, Set<AttributeListCombination>> newCombinations = new HashMap<>();
        for (int i = 0; i < oldCombs.size(); i++) {
            for (int k = i +1; k < oldCombs.size(); k++) {
                if (oldCombs.get(i).attributeCombinationA.size()==1 && oldCombs.get(k).attributeCombinationA.size() ==1) {
                    AttributeListCombination newComb = new AttributeListCombination();
                    newComb.attributeCombinationA.addAll(oldCombs.get(i).attributeCombinationA);
                    newComb.attributeCombinationA.addAll(oldCombs.get(k).attributeCombinationA);
                    newComb.attributeCombinationB.addAll(oldCombs.get(i).attributeCombinationB);
                    newComb.attributeCombinationB.addAll(oldCombs.get(k).attributeCombinationB);
                    Set<AttributeListCombination> set = new HashSet<>(Arrays.asList(oldCombs.get(i),oldCombs.get(k)));
                    newCombinations.put(newComb, set);
                } else {
                    AttributeListCombination newComb = new AttributeListCombination();
                    Set<String> attA1 = new HashSet<>(oldCombs.get(i).attributeCombinationA);
                    Set<String> attA2 = new HashSet<>(oldCombs.get(k).attributeCombinationA);
                    Set<String> attB1 = new HashSet<>(oldCombs.get(i).attributeCombinationB);
                    Set<String> attB2 = new HashSet<>(oldCombs.get(k).attributeCombinationB);
                    Set<String> intersect = new HashSet<>(attA1);
                    Set<String> intersectB = new HashSet<>(attB1);
                    intersect.retainAll(attA2);
                    intersectB.retainAll(attB2);
                    Set<String> difference = new HashSet<>(attA2);
                    Set<String> differenceB = new HashSet<>(attB2);
                    difference.removeAll(intersect);
                    differenceB.removeAll(intersectB);
                    if (intersect.size() == oldCombs.get(i).attributeCombinationA.size()-1 &&
                            intersect.size() == oldCombs.get(k).attributeCombinationA.size()-1) {
                        newComb.attributeCombinationA.addAll(oldCombs.get(i).attributeCombinationA);
                        newComb.attributeCombinationB.addAll(oldCombs.get(i).attributeCombinationB);
                        newComb.attributeCombinationA.addAll(difference);
                        newComb.attributeCombinationB.addAll(differenceB);
                        Set<AttributeListCombination> oldCombinations = newCombinations.get(newComb);
                        if(oldCombinations == null) {
                            oldCombinations = new HashSet<>();
                            newCombinations.put(newComb, oldCombinations);
                        }
                        oldCombinations.add(oldCombs.get(i));
                        oldCombinations.add(oldCombs.get(k));
                    }
                }
            }
        }
        return newCombinations;
    }

    private static void clearKeyAtts(List<Record> dataSet) {
        List<Integer> remAtts = new ArrayList<>();
        for(Map.Entry<String, Integer> e : RecordSchema.INSTANCE.getNameColumnMapping().entrySet()) {
            if(e.getKey().contains("key_")) {
                remAtts.add(e.getValue());
            }
        }
        for (Record r : dataSet){
            for(int remIndex: remAtts) {
                r.removeQidAttribute(remIndex);
            }
        }
        List<String> remAttsNames = new ArrayList<>();
        for(Map.Entry<String, Integer> e : RecordSchema.INSTANCE.getNameColumnMapping().entrySet()) {
            if(e.getKey().contains("key_")) {
                remAttsNames.add(e.getKey());
            }
        }
        for (String name : remAttsNames) {
            RecordSchema.INSTANCE.remove(name);
        }
    }

    private class AttributeListCombination implements Comparable<AttributeListCombination>{
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AttributeListCombination that = (AttributeListCombination) o;
            return Objects.equals(attributeCombinationA, that.attributeCombinationA) &&
                    Objects.equals(attributeCombinationB, that.attributeCombinationB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attributeCombinationA, attributeCombinationB);
        }

        Set<String> attributeCombinationA;
        Set<String> attributeCombinationB;
        double scoreA;
        double scoreB;
        public AttributeListCombination(){
            attributeCombinationA = new HashSet<>();
            attributeCombinationB = new HashSet<>();
            scoreA = 0;
            scoreB = 0;
        }

        @Override
        public String toString() {
            return "AttributeListCombination{" +
                    "attributeCombinationA=" + attributeCombinationA +
                    ", attributeCombinationB=" + attributeCombinationB +
                    ", scoreA=" + scoreA +
                    ", scoreB=" + scoreB +
                    '}';
        }

        @Override
        public int compareTo(AttributeListCombination o) {
            return -1 * Double.compare((scoreA+scoreB)/2, (o.scoreA + o.scoreB)/2);
        }
    }
}
