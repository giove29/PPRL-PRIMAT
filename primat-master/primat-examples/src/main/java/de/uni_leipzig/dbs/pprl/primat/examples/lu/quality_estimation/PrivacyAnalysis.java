package de.uni_leipzig.dbs.pprl.primat.examples.lu.quality_estimation;

import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.model.CountingBloomFilter;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.DataSources;
import de.uni_leipzig.dbs.pprl.primat.examples.setup.PrivateIdKeyDefinitions;
import de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall.OverlapEstimation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PrivacyAnalysis {


    /**
     * Computes the information gain also known as Kullback-Leibler divergence being also used in "S. Joshua Swamidass,
     * Matthew Matlock, Leon Rozenblit:Securely Measuring the Overlap between Private Datasets with Cryptosets"
     * for the privacy analysis
     * @return
     */
    public static double computeInformationGainBySwami(CountingBloomFilter cbf, int l) {
        int A = Arrays.stream(cbf.getData()).sum();
        double sum = 0;
        for(int i = 0; i<cbf.getData().length; i++) {
            int A_i = cbf.getData()[i];

            double log = 0;
            if(A_i != 0) {
                log = Math.log(((double) (A_i*l) / (double) A));
            }
            sum += (A_i/(double)A)*
                    (log/Math.log(2));
        }
        return sum;
    }

    public static double computeInformationGain(CountingBloomFilter cbf, int l) {
        int A = Arrays.stream(cbf.getData()).sum();
        double sum = 0;
        for(int i = 0; i<cbf.getData().length; i++) {
            int A_i = cbf.getData()[i];

            double log = 0;
            if(A_i != 0) {
                log = Math.log(((double) (A_i) / (double) A));
            }
            sum += (A_i/(double)A)*
                    (log/Math.log(2));
        }
        double gain = -Math.log(1/(double)l)/Math.log(2) + (sum);
        return gain;
    }


    public static void main(String[] args) throws IOException {
        String[] data_sets = new String[]{
                "E:/data/bloomfilter_primat/normal/normal/ncvr_160k_160k_40k.csv",
                "E:/data/bloomfilter_primat/normal/normal/ncvr_120k_120k_80k.csv",
                "E:/data/bloomfilter_primat/normal/normal/ncvr_100k_100k_100k.csv",
                "E:/data/bloomfilter_primat/normal/normal/ncvr_80k_80k_120k.csv",
                "E:/data/bloomfilter_primat/normal/normal/ncvr_40k_40k_160k.csv"     
        };
        long totalRuntime = 0;
        for(String dataSet : data_sets) {
            final String inputPath = dataSet;
            OverlapEstimation overlapEstimation = new OverlapEstimation(true);
            List<Record> dataset = DataSources.getNCVR(inputPath);

            List<List<ExtractorDefinition>> privateKeyExtractions = Arrays.asList(
                    PrivateIdKeyDefinitions.get2FirstLettersFunction(),
                    PrivateIdKeyDefinitions.get3FirstLettersFunction(),
                    PrivateIdKeyDefinitions.getSoundexFunction()
            );
            Random rnd = new Random(42);
            long runtime = System.currentTimeMillis();
            Map<String, DescriptiveStatistics> manualEstimations = overlapEstimation.estimateOverlap(dataset, privateKeyExtractions, rnd,
                    1, "A", "B");
            System.out.println(System.currentTimeMillis()-runtime);
            totalRuntime +=(System.currentTimeMillis()-runtime);
            System.out.println(dataSet);
            for(Map.Entry<String, DescriptiveStatistics> e: manualEstimations.entrySet()) {
                System.out.println(e.getKey()+"\t"+ e.getValue().getMean());
            }
            String dataRes = "";
            for (int i = 0; i < privateKeyExtractions.size(); i++) {
                CountingBloomFilter cbfA = overlapEstimation.getCbfA().get(i);
                CountingBloomFilter cbfB = overlapEstimation.getCbfB().get(i);
                double gainA = computeInformationGain(cbfA, (int) Math.pow(2, 13));
                double gainB = computeInformationGain(cbfB, (int) Math.pow(2, 13));
                double gainASwami = computeInformationGainBySwami(cbfA, (int) Math.pow(2, 13));
                double gainBSwami = computeInformationGainBySwami(cbfB, (int) Math.pow(2, 13));
                DescriptiveStatistics statA = new DescriptiveStatistics(Arrays.stream(cbfA.getData()).asDoubleStream().toArray());
                DescriptiveStatistics statB = new DescriptiveStatistics(Arrays.stream(cbfB.getData()).asDoubleStream().toArray());
                dataRes +=
                privateKeyExtractions.get(i).toString() + "\t" + (statA.getMean() + statB.getMean()) / 2 + "\t" +
                        (statA.getStandardDeviation() + statB.getStandardDeviation()) / 2 + "\t" + (gainA + gainB) / 2 +
                        "\t" + (gainASwami + gainBSwami) / 2 + System.getProperty("line.separator");
            }
            System.out.println(dataSet);
            System.out.println(dataRes);
        }
        System.out.println(totalRuntime);
    }



}
