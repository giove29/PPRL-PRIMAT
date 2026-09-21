/*******************************************************************************
 *  Copyright © 2017 - 2022 Leipzig University (Database Research Group)
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
package de.uni_leipzig.dbs.pprl.primat.lu.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Linkable;
import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.PartyPair;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.utils.Pair;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityEvaluator;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.QualityMetrics;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.IdEqualityTrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.evaluation.true_match_checker.TrueMatchChecker;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResult;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkageResultPartition;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.LinkedPair;
import de.uni_leipzig.dbs.pprl.primat.lu.linkage_result.matches.SimilarityGraphMatchStrategy;
import de.uni_leipzig.dbs.pprl.primat.lu.model.SimilarityGraph;
import de.uni_leipzig.dbs.pprl.primat.lu.postprocessing.best_match.MaxBothPostprocessor;
import de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector.SimilarityVector;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.numbers.NumberColumnFormatter;

/**
 * 
 * @author mfranke
 *
 */
public class StandardThresholdClassificationRefinement implements ThresholdClassificationRefinement{

	public static double getPseudoPrecision(long source, long links) {
		return source / (double) links;
	}

	public static double getPseudoPrecisionSum(long source, long target, long links) {
		return (source + target) / ((double) 2 * links);
	}

	public static double getPseudoPrecisionMin(long source, long target, long links) {
		return Math.min(source, target) / (double) links;
	}

	public static double getPseudoRecall(long links, long sourceAll, long targetAll) {
		return links / (double) Math.min(sourceAll, targetAll);
	}

	public static double getPseudoRecallAlt(long source, long target, long sourceAll, long targetAll) {
		return (source + target) / (double) (sourceAll + targetAll);
	}

	public static double getPseudoRecallAltMin(long source, long target, long sourceAll, long targetAll) {
		return (source + target) / (double) Math.min(sourceAll, targetAll);
	}

	public static double getHarmony(long maxBothLinks, long sourceAll, long targetAll) {
		return (maxBothLinks) / (double) Math.min(sourceAll, targetAll);
	}
	
	@Override
	public LinkageResult<Record> refine(LinkageResult<Record> linkRes) {
		
		final TrueMatchChecker trueMatchChecker = new IdEqualityTrueMatchChecker();
		final int matches = 80_000;
	
		
		final PartyPair partyPairAB = new PartyPair(new Party("A"), new Party("B"));

		final LinkageResultPartition<Record> linkResPart = linkRes.getPartition(partyPairAB);
		
		final SimilarityGraphMatchStrategy<Record> strategy = (SimilarityGraphMatchStrategy<Record>) linkResPart.getMatchStrategy();				
			
		final SimilarityGraph<Record> simGraph = strategy.getSimilarityGraph();
	
		
		final double[] thresholds = { 
			0.50d, 0.55d, 0.60d, 0.65d, 0.70d, 0.75d, 
			0.80d, 0.85d, 0.90d, 0.95d, 
			1.00d 
			};
		
		final int[] singleLinks = new int[thresholds.length];
		final int[] leftMultiLinks = new int[thresholds.length];
		final int[] rightMultiLinks = new int[thresholds.length];
		final int[] multiLinks = new int[thresholds.length];
		
		final double[] singleLinksFactors = new double[thresholds.length];
		final double[] multiLinksFactors = new double[thresholds.length];
		
		final double[] precision = new double[thresholds.length];
		final double[] recall = new double[thresholds.length];
		final double[] fmeasure = new double[thresholds.length];
		
		final long[] truePos = new long[thresholds.length];
		final long[] falsePos = new long[thresholds.length];
		final long[] cTruePos = new long[thresholds.length];
		final long[] cFalsePos = new long[thresholds.length];
		
		final double[] cPrecision = new double[thresholds.length];
		final double[] cRecall = new double[thresholds.length];
		final double[] cFmeasure = new double[thresholds.length];
			
		final double[] pPrecision1 = new double[thresholds.length];
		final double[] pPrecision2 = new double[thresholds.length];
		final double[] pPrecision3 = new double[thresholds.length];
		final double[] pRecall1 = new double[thresholds.length];
		final double[] pRecall2 = new double[thresholds.length];
		final double[] pRecall3 = new double[thresholds.length];
		
		final int[] source = new int[thresholds.length];
		final int[] target = new int[thresholds.length];
		final int[] sourceAll = new int[thresholds.length];
		final int[] targetAll = new int[thresholds.length];
		final int[] links = new int[thresholds.length];
		final int[] cLinks = new int[thresholds.length];
				
		final MaxBothPostprocessor<Record> mbp = new MaxBothPostprocessor<Record>();

		
//		/*		
		System.out.println("Graph: " 
			+ simGraph.edges() + " edges, " 
			+ simGraph.sourceVertices() + " sources, " 
			+ simGraph.targetVertices() + " targets, "
			+ simGraph.vertices() + " total."
		);
		
		final QualityEvaluator<Record> evaluator1 = new QualityEvaluator<>(trueMatchChecker);
		
		for (final SimilarityVector sv : simGraph.edgeSet()) {
			final Record sourceR = simGraph.getEdgeSource(sv);
			final Record targetR = simGraph.getEdgeTarget(sv);
			evaluator1.addMatch(new LinkedPair<Record>(sourceR, targetR, sv, sv.getAggregatedValue()));
		}

		
		System.out.println("TP:" + evaluator1.getTruePositives());
		System.out.println("FP:" + evaluator1.getFalsePositives());

		System.out.println("------------------------------");
	
		
		final SimilarityGraph<Record> sgc = simGraph.clone();
		mbp.clean(sgc);
		
		System.out.println("CleanGraph: " 
			+ sgc.edges() + " edges, " 
			+ sgc.sourceVertices() + " sources, " 
			+ sgc.targetVertices() + " targets, "
			+ sgc.vertices() + " total."
		);
		
		final QualityEvaluator<Record> evaluatorcc = new QualityEvaluator<>(trueMatchChecker);
		
		for (final SimilarityVector sv : sgc.edgeSet()) {
			final Record sourceR = sgc.getEdgeSource(sv);
			final Record targetR = sgc.getEdgeTarget(sv);
			evaluatorcc.addMatch(new LinkedPair<Record>(sourceR, targetR, sv, sv.getAggregatedValue()));
		}
		
		System.out.println("TP:" + evaluatorcc.getTruePositives());
		System.out.println("FP:" + evaluatorcc.getFalsePositives());
		
		System.out.println("############################");
//		*/
		
		for (int i = 0; i < thresholds.length; i++) {
			final double t = thresholds[i];
			
			System.out.println("Current threshold: " + t);
			
			final SimilarityGraph<Record> filteredGraph = simGraph.getSimilarityFilteredSubgraph(t);	
			
//			/*
			System.out.println("Graph: " 
				+ simGraph.edges() + " edges, " 
				+ simGraph.sourceVertices() + " sources, " 
				+ simGraph.targetVertices() + " targets "
				+ simGraph.vertices() + " total."
			);
			
			System.out.println("FilteredGraph: " 
				+ filteredGraph.edges() + " edges, " 
				+ filteredGraph.sourceVertices() + " sources, " 
				+ filteredGraph.targetVertices() + " targets "
				+ filteredGraph.vertices() + " total."
			);
//			*/
			
			final QualityEvaluator<Record> evaluator = new QualityEvaluator<>(trueMatchChecker);
			
			for (final SimilarityVector sv : filteredGraph.edgeSet()) {
				final Record sourceR = filteredGraph.getEdgeSource(sv);
				final Record targetR = filteredGraph.getEdgeTarget(sv);
				evaluator.addMatch(new LinkedPair<Record>(sourceR, targetR, sv, sv.getAggregatedValue()));
			}
			
			final SimilarityGraph<Record> cleanedFilteredGraph = filteredGraph.clone();	
			mbp.clean(cleanedFilteredGraph);			
			
			System.out.println("CleanFilteredGraph: " 
				+ cleanedFilteredGraph.edges() + " edges, " 
				+ cleanedFilteredGraph.sourceVertices() + " sources, " 
				+ cleanedFilteredGraph.targetVertices() + " targets "
				+ cleanedFilteredGraph.vertices() + " total."
			);
			
			final QualityEvaluator<Record> evaluatorCleanGraph = new QualityEvaluator<>(trueMatchChecker);

			for (final SimilarityVector sv : cleanedFilteredGraph.edgeSet()) {
				final Record sourceR = cleanedFilteredGraph.getEdgeSource(sv);
				final Record targetR = cleanedFilteredGraph.getEdgeTarget(sv);
				evaluatorCleanGraph.addMatch(new LinkedPair<Record>(sourceR, targetR, sv, sv.getAggregatedValue()));
			}		
			
			/*
			final List<Set<Linkable>> cc = filteredGraph.getConnectedSets();
			
			int tp = 0;
			int fp = 0;
			
			
			for (final Set<Linkable> component : cc) {		
				
				int sourceV = 0;
				int targetV = 0;
				
				for (final Linkable vertex : component) {
					try {
						if (filteredGraph.getSource().contains(vertex)) {
							sourceV++;
						}
					} catch (ClassCastException e) {};
					
					try {
						if (filteredGraph.getTarget().contains(vertex)) {
							targetV++;
						}
					} catch (ClassCastException e) {};
				}
				
				if (sourceV == 1 && targetV == 1) {
					tp++;
				}
				else if (sourceV > 0 && targetV > 0){
					tp += Math.min(sourceV, targetV);
					fp += Math.max(sourceV, targetV) - Math.min(sourceV, targetV);
//					System.out.println(Math.max(sourceV, targetV) + " vs " + Math.min(sourceV, targetV));
				}
				else {
					// one side is 0
				}
			}
			
			System.out.println("tp: " + tp + ", fp:" + fp);
			System.out.println(QualityMetrics.getPrecision(tp, tp + fp));		
//			*/
			
			singleLinks[i] = filteredGraph.singleLinks();
			multiLinks[i] = filteredGraph.multiLinks();
			leftMultiLinks[i] = filteredGraph.leftMultiLinks();
			rightMultiLinks[i] = filteredGraph.rightMultiLinks();
			links[i] = filteredGraph.links();
			cLinks[i] = cleanedFilteredGraph.links();

			if (i == 0) {
				singleLinksFactors[i] = 0d;
				multiLinksFactors[i] = 0d;
			}
			else {
				singleLinksFactors[i] = (double) singleLinks[i] / singleLinks[i-1];
				multiLinksFactors[i] = (double) multiLinks[i] / multiLinks[i-1];
				
			}
			
			truePos[i] = evaluator.getTruePositives();
			falsePos[i] = evaluator.getFalsePositives();
			
			cTruePos[i] = evaluatorCleanGraph.getTruePositives();
			cFalsePos[i] = evaluatorCleanGraph.getFalsePositives();
			
	
			recall[i] = QualityMetrics.getRecall(truePos[i], matches);
			precision[i] = QualityMetrics.getPrecision(truePos[i], truePos[i] + falsePos[i]);
			fmeasure[i] = QualityMetrics.getFMeasure(recall[i], precision[i]);	
			
			cRecall[i] = QualityMetrics.getRecall(cTruePos[i], matches);
			cPrecision[i] = QualityMetrics.getPrecision(cTruePos[i], cTruePos[i] + cFalsePos[i]);
			cFmeasure[i] = QualityMetrics.getFMeasure(cRecall[i], cPrecision[i]);		
			
			source[i] = filteredGraph.mappedSourceVertices();
			target[i] = filteredGraph.mappedTargetVertices();
			sourceAll[i] = simGraph.sourceVertices();
			targetAll[i] =  simGraph.targetVertices();
			
			pPrecision1[i] = getPseudoPrecisionMin(source[i], target[i], links[i]);
			pPrecision2[i] = getPseudoPrecisionSum(source[i], target[i], links[i]);
			pPrecision3[i] = getPseudoPrecision(source[i], links[i]);
			
			pRecall1[i] = getPseudoRecall(links[i], source[i], target[i]);
			pRecall2[i] = getPseudoRecallAlt(source[i], target[i], sourceAll[i], targetAll[i]);
			pRecall3[i] = getPseudoRecallAltMin(source[i], target[i], sourceAll[i], targetAll[i]);
			
			System.out.println("-------------------------");
		}

		System.out.println("#SingleLinks:" + Arrays.toString(singleLinks));
		System.out.println("#SingleLinkFactors:" + Arrays.toString(singleLinksFactors));
		
		System.out.println("#MultiLinks:" + Arrays.toString(multiLinks));
		System.out.println("#MultiLinkFactors:" + Arrays.toString(multiLinksFactors));
		
		System.out.println(Arrays.toString(source));
		System.out.println(Arrays.toString(target));
		System.out.println(Arrays.toString(sourceAll));
		System.out.println(Arrays.toString(targetAll));

		
		
		final int maxSingle = 
			Arrays.stream(singleLinks).max().getAsInt();
//			simGraph.edges();
//			Math.min(source, target);
			
		final int maxMulti = 
			Arrays.stream(multiLinks).max().getAsInt();
//			simGraph.edges();
		
	
		
		System.out.println(maxSingle);
		System.out.println(maxMulti);
		
		final List<Double> eRec = Arrays.stream(singleLinks).boxed().map(i -> (double) i / maxSingle).collect(Collectors.toList());
		final List<Double> ePrec = Arrays.stream(multiLinks).boxed().map(i -> 1 - ((double) i / maxMulti)).collect(Collectors.toList());
		final List<Double> eFmeasure = new ArrayList<>();
		for (int i = 0; i < eRec.size(); i++) {
			final double r = eRec.get(i);
			final double p = ePrec.get(i);
			final double f1 = (2 * r * p) / (r + p);
			eFmeasure.add(f1);
		}
		
		Locale.setDefault(Locale.US);
		
		final DoubleColumn colThreshold = DoubleColumn.create("Threshold", thresholds);
		colThreshold.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(2));
		
		final int fractionDigits = 6;
		
		final DoubleColumn colRecall = DoubleColumn.create("Recall", recall);
		colRecall.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colPrecision = DoubleColumn.create("Precision", precision);
		colPrecision.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colFMeasure = DoubleColumn.create("F-Measure", fmeasure);
		colFMeasure.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));
		
		final DoubleColumn colCRecall = DoubleColumn.create("C_Recall", cRecall);
		colCRecall.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colCPrecision = DoubleColumn.create("C_Precision", cPrecision);
		colCPrecision.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));
		
		final DoubleColumn colCFMeasure = DoubleColumn.create("C_F-Measure", cFmeasure);
		colCFMeasure.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colERecall = DoubleColumn.create("E_Recall", eRec);
		colERecall.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colEPrecision = DoubleColumn.create("E_Precision", ePrec);
		colEPrecision.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colEFMeasure = DoubleColumn.create("E_F-Measure", eFmeasure);
		colEFMeasure.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));
	
		final DoubleColumn colPPrecision1 = DoubleColumn.create("P_Precision1", pPrecision1);
		colPPrecision1.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colPPrecision2 = DoubleColumn.create("P_Precision2", pPrecision2);
		colPPrecision2.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colPPrecision3 = DoubleColumn.create("P_Precision3", pPrecision3);
		colPPrecision3.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));
		
		final DoubleColumn colPRecall1 = DoubleColumn.create("P_Recall1", pRecall1);
		colPRecall1.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colPRecall2 = DoubleColumn.create("P_Recall2", pRecall2);
		colPRecall2.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		final DoubleColumn colPRecall3 = DoubleColumn.create("P_Recall3", pRecall3);	
		colPRecall3.setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(fractionDigits));

		
		final Table res = Table.create("Analysis Results").addColumns(
			colThreshold,
			IntColumn.create("1:1-Links", singleLinks),
			IntColumn.create("N:1-Links", leftMultiLinks),
			IntColumn.create("1:N-Links", rightMultiLinks),
			IntColumn.create("N:M-Links", multiLinks),
			IntColumn.create("Links", links),
			IntColumn.create("Source", source),
			IntColumn.create("SourceAll", sourceAll),
			IntColumn.create("Target", target),
			IntColumn.create("TargetAll", targetAll),
			colRecall, 
			colPrecision, 
			colFMeasure,
			colCRecall, 
			colCPrecision, 
			colCFMeasure,
			LongColumn.create("TP", truePos),
			LongColumn.create("FP",falsePos),
			LongColumn.create("cTP",cTruePos),
			LongColumn.create("cFP",cFalsePos),
			colERecall, 
			colEPrecision, 
			colEFMeasure, 
			colPPrecision1,
			colPPrecision2,
			colPPrecision3, 
			colPRecall1, 
			colPRecall2, 
			colPRecall3
		);
		
		System.out.println(res);
		
		return linkRes;
	}
}