/*******************************************************************************
 *  Copyright Â© 2017 - 2022 Leipzig University (Database Research Group)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation.recall;

import java.util.*;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.blocking.Blocker;
import de.uni_leipzig.dbs.pprl.primat.common.blocking.BlockingKeyDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.extraction.ExtractorDefinition;
import de.uni_leipzig.dbs.pprl.primat.common.utils.*;
import javassist.runtime.Desc;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.uni_leipzig.dbs.pprl.primat.common.model.CountingBloomFilter;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 *
 * @author mfranke
 *
 */
public class OverlapEstimation {

    private int cbfSize;

    private boolean isPrivate;


    private List<CountingBloomFilter> cbfBs;
    private List<CountingBloomFilter> cbfAs;

    public OverlapEstimation(final int cbfSize) {
        this.cbfSize = cbfSize;
        this.isPrivate = true;
    }

    public OverlapEstimation(boolean isPrivate) {
        this.isPrivate = isPrivate;
        this.cbfSize = (int) Math.pow(2,13);
    }

    public OverlapEstimation(final int cbfSize, final boolean isPrivate) {
        this.cbfSize = cbfSize;
        this.isPrivate = isPrivate;
    }



    public OverlapEstimation(){
        this((int)Math.pow(2,13), true);

    }

    private List<Record> addPrivateKeys(List<Record> records, List<ExtractorDefinition> keyFunctions) {
        final BlockingKeyDefinition bk1 = new BlockingKeyDefinition(keyFunctions,
                StringListAggregator.CONCAT);
        final Blocker blocker = new Blocker();
        blocker.addBlockingKeyDefinition(bk1);
        blocker.addBlockingKeys(records);
        return records;
    }

    public Map<String, DescriptiveStatistics> estimateOverlap(List<Record> dataset,
                               List<List<ExtractorDefinition>> privateIDFunctions,
                               Random rnd, int iterations,
                               String sourceParty, String targetParty) {
        cbfAs = new ArrayList<>();
        cbfBs = new ArrayList<>();
        Map<String, DescriptiveStatistics> estimations = new LinkedHashMap<>();
        final DescriptiveStatistics stat = new DescriptiveStatistics();
        for(List<ExtractorDefinition> edf : privateIDFunctions) {
            DescriptiveStatistics statPerKey = new DescriptiveStatistics();
            for (int i = 0; i < iterations; i++) {
                final String key = RandomUtils.getRandomStringAlphanumeric(32, rnd);
                double estOverlap = this.estimateOverlap(dataset, edf, key, sourceParty, targetParty);
                stat.addValue(estOverlap);
                estOverlap = Math.max(estOverlap, 0);
                statPerKey.addValue(estOverlap);
            }
            for (Record r: dataset) {
                r.clearBlockingKeys();
            }
            String name = edf.stream().map(e -> e.getName()).collect(Collectors.toList()).toString();
            estimations.put(name, statPerKey);
        }
        if(privateIDFunctions.size()==0) {
            DescriptiveStatistics zeroStat = new DescriptiveStatistics();
            zeroStat.addValue(1e-6);
            estimations.put("mean", zeroStat);
        }
        return estimations;
    }

    private double estimateOverlap(List<Record> dataset, List<ExtractorDefinition> privateIDFunctions, String key,
                                   String sourceParty, String targetParty) {
        dataset = this.addPrivateKeys(dataset, privateIDFunctions);

        CountingBloomFilter cbfA, cbfB;

        if(!isPrivate) {
            cbfSize = dataset.size();
        }
        cbfA = new CountingBloomFilter(cbfSize);
        cbfB = new CountingBloomFilter(cbfSize);
        Map<String, Integer> idMap = new HashMap<>();
        for (final Record record : dataset) {
            final String privateId =
                    record.getBlockingKeys().iterator().next().getBlockingKeyId().getBlockingKeyValue();
            final int publicId;
            if(isPrivate) {
                final byte[] digest =
                        HashUtils.getHmac(HMacAlgorithm.HMAC_SHA_512, privateId, key);
                publicId = HashUtils.toPositiveIntHash(digest, cbfSize);
            }else {
                if(!idMap.containsKey(privateId)) {
                    idMap.put(privateId, idMap.size());
                }
                publicId = idMap.get(privateId);
            }
            if (record.getParty().getName().equals(sourceParty)) {
                cbfA.increment(publicId);
            } else if (record.getParty().getName().equals(targetParty)) {
                cbfB.increment(publicId);
            }
        }
        cbfAs.add(cbfA);
        cbfBs.add(cbfB);
        return estimateOverlap(cbfA, cbfB);
    }

    private double estimateOverlap(CountingBloomFilter csA, CountingBloomFilter csB) {
        final double[] countsA = ArrayUtils.intToDouble(csA.getData());
        final double[] countsB = ArrayUtils.intToDouble(csB.getData());
        double pc = new PearsonsCorrelation().correlation(countsA, countsB);
        long sumA = csA.cardinality();
        long sumB = csB.cardinality();
        long maxAB = Math.max(sumA, sumB);
        long minAB = Math.min(sumA, sumB);
        double overlapRelative = pc * Math.sqrt((double) maxAB / (double) minAB);
        double overlap = overlapRelative * minAB;
        return overlap;
    }

    public List<CountingBloomFilter> getCbfB() {
        return cbfBs;
    }

    public List<CountingBloomFilter>  getCbfA() {
        return cbfAs;
    }

}
