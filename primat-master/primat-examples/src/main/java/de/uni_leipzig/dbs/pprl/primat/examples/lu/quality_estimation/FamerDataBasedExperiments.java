package de.uni_leipzig.dbs.pprl.primat.examples.lu.quality_estimation;


import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.SubstringByPositionExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.DataSources;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.PrivateIdKeyDefinitions;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.io.GradoopCSVReader;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.io.JSONReader;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartitionFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.MatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.IgnoreNonMatchesStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.non_matches.NonMatchStrategyFactory;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.QualityEstimator;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.precision.*;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall.*;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FamerDataBasedExperiments {

    private static int getMatchesWithinTheSame(List<Record> dataSet, PartyPair partyPair) {
        Map<String, Integer> duplicates = new HashMap<>();
        System.out.println(dataSet.size());
        for(Record r: dataSet){
            if (r.getParty().getName().equals(partyPair.getLeftParty().getName()) ||
                    r.getParty().getName().equals(partyPair.getRightParty().getName())) {
                Integer c = duplicates.putIfAbsent(r.getGlobalIdentifier(), 1);
                if (c != null) {
                    duplicates.put(r.getGlobalId(), 1 + c);
                }
            }
        }
        int matches = duplicates.values().stream().filter(c -> c>1).mapToInt(i ->(i*(i-1))/2).sum();
        return matches;
    }

    private static int getMatches(List<Record> dataSet, PartyPair partyPair) {
        Map<String, Integer> srcDuplicates = new HashMap<>();
        Map<String, Integer> targetDuplicates = new HashMap<>();
        System.out.println(dataSet.size());
        for(Record r: dataSet){
            if (r.getParty().getName().equals(partyPair.getLeftParty().getName()) ) {
                Integer c = srcDuplicates.putIfAbsent(r.getGlobalIdentifier(), 1);
                if (c != null) {
                    srcDuplicates.put(r.getGlobalId(), 1 + c);
                }
            }else if(r.getParty().getName().equals(partyPair.getRightParty().getName())){
                Integer c = targetDuplicates.putIfAbsent(r.getGlobalIdentifier(), 1);
                if (c != null) {
                    targetDuplicates.put(r.getGlobalId(), 1 + c);
                }
            }
        }
        int matches = 0;
        for (Map.Entry<String, Integer> e : srcDuplicates.entrySet()) {
            if (targetDuplicates.containsKey(e.getKey())) {
                matches += e.getValue() * targetDuplicates.get(e.getKey());
            }

        }
        return matches;
    }

    public static void main(String[] args) throws IOException {
        Logger log = Logger.getLogger("eval_logger");
        FileHandler fh = new FileHandler("quality_eval.log");
        log.addHandler(fh);
        final MatchStrategyFactory<Record> matchFactory = new SimilarityGraphMatchStrategyFactory<>();
        final NonMatchStrategyFactory<Record> nonMatchFactory = new IgnoreNonMatchesStrategyFactory<>();
        final LinkageResultPartitionFactory<Record> linkResFac = new LinkageResultPartitionFactory<>(matchFactory,
                nonMatchFactory);
        List<String> sources = null;
        SimilarityGraph<Record> completeGraph = null;
        List<Record> completeDataset = null;
        if (args[0].equals("Music")) {
            sources = new ArrayList<>(Arrays.asList(
                    "1", "2", "3", "4", "5"));
        } else if (args[0].equals("Geo")) {
            sources = new ArrayList<>(Arrays.asList(
                    "http://sws.geonames.org/", "http://data.nytimes.com/", "http://dbpedia.org/", "http://rdf.freebase.com/"));
        } else if (args[0].equals("Dexter")) {
            sources = new ArrayList<>(Arrays.asList(
                    "www.flipkart.com", "www.eglobalcentral.co.uk", "www.price-hunt.com", "www.wexphotographic.com",
                    "www.garricks.com.au", "www.ebay.com", "www.buzzillions.com",
                    "cammarkt.com", "www.canon-europe.com", "www.shopbot.com.au", "buy.net",
                    "www.pcconnection.com", "www.pricedekho.com", "www.henrys.com",
                    "www.walmart.com", "www.ilgs.net", "www.cambuy.com.au",
                    "www.gosale.com", "www.camerafarm.com.au", "www.shopmania.in",
                    "www.ukdigitalcameras.co.uk", "www.priceme.co.nz", "www.mypriceindia.com"));
        }
        List<Double> macroPrecs = new ArrayList<>();
        List<Double> macroRecs = new ArrayList<>();
        List<Double> macroF1 = new ArrayList<>();
        List<Double> thresholds = new ArrayList<>();
        Map<String, List<Double>> resultPseudoMeasures = new LinkedHashMap<>();
        File dataSetFolder = new File(args[1]);
        if (!dataSetFolder.isDirectory()) {
            System.exit(1);
        }
        Pattern thresholdP = Pattern.compile("((?<=threshold_)|(?<=SW_))[0-9]\\.[0-9]{1,2}");
        PrivateIdKeyDefinitions autoKeyDefinition = new PrivateIdKeyDefinitions();
        List<Double> probTps =new ArrayList<>();
        List<Double> goldTps = new ArrayList<>();
        for (File threshFolder : dataSetFolder.listFiles()) {
            double totalMatches = 0;
            DescriptiveStatistics macroPrec = new DescriptiveStatistics();
            DescriptiveStatistics macroRec = new DescriptiveStatistics();
            double threshold = 0;
            if (threshFolder.isDirectory()) {
                if (threshFolder.getName().contains("threshold") || threshFolder.getName().contains("SW")) {
                    log.info(threshFolder.getName());
                    Matcher m = thresholdP.matcher(threshFolder.getName());
                    String thresholdString = "0";
                    if (m.find())
                        thresholdString = m.group();
                    threshold = Double.parseDouble(thresholdString);
                    thresholds.add(threshold);
                }
                Map<String, DescriptiveStatistics> statsOverallSources = new LinkedHashMap<>();

                double probTp = 0;
                double goldTp = 0;
                for (int i = 0; i < sources.size(); i++) {
                    for (int k = i + 1; k < sources.size(); k++) {
                        RecordSchema.INSTANCE.clear();
                        OverlapEstimation overlapEstimation = new OverlapEstimation();
                        log.info("pair" + sources.get(i) + "-" + sources.get(k));
                        SimilarityGraph<Record> similarityGraph = null;
                        List<Record> dataSet = null;
                        List<List<ExtractorDefinition>> manualPrivateKeyExtractions = null;
                        List<List<ExtractorDefinition>> autoExtraction = null;
                        Map<String, String> comparingAtts = null;
                        JSONReader jsonReader = new JSONReader();
                        if (args[0].equals("Music")) {
                            log.info(threshFolder.getAbsolutePath());
                            similarityGraph = jsonReader.readGradoopGraph(threshFolder.getAbsolutePath(), "" + sources.get(i), "" + sources.get(k));
                            dataSet = DataSources.getMusicBrainz(threshFolder.getAbsolutePath(), "" + sources.get(i), "" + sources.get(k));
                            comparingAtts = DataSources.getMusicBrainzAttributePairs();
                            FeatureExtractor extractor = new SubstringByPositionExtractor(0, 2);
                            FeatureExtractor extractor3 = new SubstringByPositionExtractor(0, 3);
                            autoExtraction = autoKeyDefinition.getKeyDefinitions(dataSet, "" + sources.get(i),
                                    "" + sources.get(k), extractor3, comparingAtts, 0.7, 0.3);
                            manualPrivateKeyExtractions = new ArrayList<>(Arrays.asList(
                                    PrivateIdKeyDefinitions.getMB2FirstLettersTitleAlbumFunction(),
                                    PrivateIdKeyDefinitions.getMB2FirstLettersTitleFunction(),
                                    PrivateIdKeyDefinitions.getMB3FirstLettersFunction(),
                                    PrivateIdKeyDefinitions.getMB3FirstLettersTitleFunction(),
                                    PrivateIdKeyDefinitions.getMB3FirstLettersTitleArtistFunction()));
                        } else if (args[0].equals("Geo")) {
                            similarityGraph = jsonReader.readGradoopGraph(threshFolder.getAbsolutePath(), "" + sources.get(i), "" + sources.get(k));
                            dataSet = DataSources.getGeoData(threshFolder.getAbsolutePath(), "" + sources.get(i), "" + sources.get(k));
                            comparingAtts = DataSources.getGeoAttributePairs();
                            FeatureExtractor extractor = new SubstringByPositionExtractor(0, 2);
                            FeatureExtractor extractor3 = new SubstringByPositionExtractor(0, 3);
                            autoExtraction = autoKeyDefinition.getKeyDefinitions(dataSet, "" + sources.get(i),
                                    "" + sources.get(k), extractor3, comparingAtts,
                                    0.7, 0.2);
                            manualPrivateKeyExtractions = new ArrayList<>(Arrays.asList(
                                    PrivateIdKeyDefinitions.getGeo3FirstLettersLabelFunction(),
                                    PrivateIdKeyDefinitions.getGeo3FirstLettersLabelLonLatFunction()));
                        } else if (args[0].equals("Dexter")) {
                            GradoopCSVReader csvReader = new GradoopCSVReader();
                            if(completeGraph == null) {
                                completeGraph = csvReader.readGradoopGraph(threshFolder.getAbsolutePath());
                                completeDataset = DataSources.getDexterData(threshFolder.getAbsolutePath());
                            } else {
                                csvReader.readSchema(threshFolder.getAbsolutePath());
                            }
                            similarityGraph = csvReader.filterGraph(completeGraph, sources.get(i), sources.get(k));
                            dataSet = csvReader.filterDataset(completeDataset, sources.get(i), sources.get(k));
                            comparingAtts = DataSources.getDexterPairs();
                            FeatureExtractor extractor3 = new SubstringByPositionExtractor(0, 3);
                            autoExtraction = autoKeyDefinition.getKeyDefinitions(dataSet, "" + sources.get(i),
                                    "" + sources.get(k), extractor3, comparingAtts, 0.7, 0.1);
                            manualPrivateKeyExtractions = new ArrayList<>(Arrays.asList(
                                    PrivateIdKeyDefinitions.getDexter3FirstLetterNameBrandFunction()));
                        }
                        log.info("|V| = " + similarityGraph.vertices() +" |E| = " + similarityGraph.edges());
                        Map<String, DescriptiveStatistics> manualEstimations = overlapEstimation.estimateOverlap(dataSet,
                                manualPrivateKeyExtractions, new Random(42), 10,
                                "" + sources.get(i), "" + sources.get(k));
                        Map<String, DescriptiveStatistics> autoEstimations = overlapEstimation.estimateOverlap(dataSet,
                                autoExtraction, new Random(42), 10,
                                "" + sources.get(i), "" + sources.get(k));

                        List<PrecisionEstimation> precisionEstimations = new ArrayList<>();
                        PrecisionEstimation probEstimation;
                        if(!args[0].equals("Dexter")) {
                            probEstimation = new PrecisionProbabilityDeduplicated(similarityGraph.edges());
                        } else {
                            probEstimation = new PrecisionPPBasedTPEstimation(similarityGraph.edges());
                        }
                        precisionEstimations.addAll(Arrays.asList(new PrecisionOneToOneBase(similarityGraph.edges()),
                                new PrecisionStructureOneToOne(similarityGraph.edges()),
                                new PrecisionStructureOneToN(similarityGraph.edges()),
                                probEstimation
                                )
                        );
                        QualityEstimator estimator = new QualityEstimator(null);
                        estimator.computePrecision(similarityGraph, precisionEstimations);
                        probTp += probEstimation.getTps();
                        List<RecallEstimation> recallEstimations = new ArrayList<>();
                        recallEstimations.addAll(Arrays.asList(
                                new RecallStructureNToM(),
                                new RecallStructureOneToOne(),
                                new RecallStructureOneToN()
                        ));

                        for (Map.Entry<String, DescriptiveStatistics> e : manualEstimations.entrySet()) {
                            String name = e.getKey();
                            recallEstimations.add(new RecallCryptoSetOneToN(name, e.getValue()));
                            recallEstimations.add(new RecallCryptoSetOneToOne(name, e.getValue()));
                            recallEstimations.add(new RecallCryptoSetProb(name, probEstimation.getTps(), e.getValue()));
                        }
                        int row = 0;
                        if (autoEstimations.size()>0) {
                            for (Map.Entry<String, DescriptiveStatistics> e : autoEstimations.entrySet()) {
                                String name = e.getKey();
                                if (row != autoEstimations.size()-1) {
                                    name = "auto_"+row;
                                }else {
                                    name = "auto_" +e.getKey();
                                }

                                if (!name.equals("auto_" + row)){
                                    recallEstimations.add(new RecallCryptoSetOneToN(name, e.getValue()));
                                    recallEstimations.add(new RecallCryptoSetOneToOne(name, e.getValue()));
                                    recallEstimations.add(new RecallCryptoSetProb(name, probEstimation.getTps(), e.getValue()));
                                }
                                row++;
                            }
                        } else {
                            DescriptiveStatistics statistics = new DescriptiveStatistics();
                            statistics.addValue(0);
                            recallEstimations.add(new RecallCryptoSetOneToN("r_cs_auto", statistics));
                            recallEstimations.add(new RecallCryptoSetOneToOne("r_cs_auto", statistics));
                            recallEstimations.add(new RecallCryptoSetProb("r_cs_auto", probEstimation.getTps(), statistics));
                        }
                        estimator.computeRecall(similarityGraph, recallEstimations);

                        LinkageResultPartition<Record> resultPartition = linkResFac.createLinkageResultPartition(
                                new PartyPair(new Party(sources.get(i)), new Party(sources.get(k))));
                        for (SimilarityVector e : similarityGraph.edgeSet())
                            if(!similarityGraph.getEdgeSource(e).getParty().equals(similarityGraph.getEdgeTarget(e).getParty()))
                                resultPartition.addMatch(similarityGraph.getEdgeSource(e), similarityGraph.getEdgeTarget(e), e, e.getAggregatedValue());
                        final TrueMatchChecker trueMatchChecker = new IdEqualityTrueMatchChecker();
                        final QualityEvaluator<Record> evaluator = new QualityEvaluator<>(trueMatchChecker);
                        for (Map.Entry<String, Double> e : estimator.getEstimationResult().entrySet()) {
                            DescriptiveStatistics stats = statsOverallSources.putIfAbsent(e.getKey(), new DescriptiveStatistics());
                            if (stats == null) {
                                stats = statsOverallSources.get(e.getKey());
                            }
                            if(!Double.isNaN(e.getValue()))
                                stats.addValue(e.getValue());
                        }

                        final PartyPair partyPairAB = new PartyPair(new Party(sources.get(i)), new Party(sources.get(k)));
                        long matches = getMatches(dataSet, partyPairAB);
                        totalMatches += matches;
                        evaluator.addMatches(resultPartition.getMatchStrategy());
                        final long truePos = evaluator.getTruePositives();
                        goldTp += truePos;

                        final long falsePos = evaluator.getFalsePositives();
                        final long falseNeg = matches - truePos;
                        final double recall = QualityMetrics.getRecall(truePos, falseNeg + truePos);
                        final double precision = QualityMetrics.getPrecision(truePos, truePos + falsePos);
                        System.out.println(precision +"\t"+recall);
                        if (matches != 0) {
                            macroRec.addValue(recall);
                        }
                        if(truePos + falsePos !=0) {
                            macroPrec.addValue(precision);
                        }
                    }
                }
                probTps.add(probTp/(double) sources.size()*(sources.size()-1)/2d);
                goldTps.add(goldTp/(double) sources.size()*(sources.size()-1)/2d);

                log.info(""+ threshold);
                System.out.println(totalMatches);
                for (Map.Entry<String, DescriptiveStatistics> e : statsOverallSources.entrySet()) {
                    List<Double> list = resultPseudoMeasures.putIfAbsent(e.getKey(), new ArrayList<>());
                    if (list == null) {
                        list = resultPseudoMeasures.get(e.getKey());
                    }
                    if(!Double.isNaN(e.getValue().getMean())) {
                        double result = Math.round(e.getValue().getMean() * 1000) / 1000d;
                        list.add(result);
                    }
                }
                double prec = Math.round(macroPrec.getMean()*1000)/1000d;
                double rec = Math.round(macroRec.getMean()*1000)/1000d;
                log.info("p: " +prec + " r: "+ rec);
                macroPrecs.add(prec);
                macroRecs.add(rec);
                log.info("recall list:" + macroRecs.size());
                log.info("threshold list:" + thresholds.size());
                double f1 = 2 * macroPrec.getMean() * macroRec.getMean() /
                        (macroPrec.getMean() + macroRec.getMean());
                f1 = Math.round(f1*1000)/1000d;
                macroF1.add(f1);
            }
            completeGraph = null;
            if(completeGraph != null)
                completeDataset.clear();
        }
        Table qualityResults =
                Table
                        .create("QualityResults")
                        .addColumns(
                                DoubleColumn.create("Threshold", thresholds),
                                DoubleColumn.create("Recall", macroRecs),
                                DoubleColumn.create("Precision", macroPrecs),
                                DoubleColumn.create("F-Measure", macroF1)
                        );
        for (Map.Entry<String, List<Double>> e : resultPseudoMeasures.entrySet()) {
            log.info(e.getKey() +" size:"+e.getValue().size());
            qualityResults.addColumns(DoubleColumn.create(e.getKey(), e.getValue().toArray(new Double[]{})));
        }
        qualityResults.addColumns(DoubleColumn.create("prob_tps", probTps.toArray(new Double[]{})));
        qualityResults.addColumns(DoubleColumn.create("gold_tps", goldTps.toArray(new Double[]{})));
        System.out.println(qualityResults.toString());
    }

}
