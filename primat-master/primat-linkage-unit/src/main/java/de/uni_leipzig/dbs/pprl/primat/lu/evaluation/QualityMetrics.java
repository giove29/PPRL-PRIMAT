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
package de.uni_leipzig.dbs.pprl.primat.lu.evaluation;

/**
 * Class for calculation quality metrics, in particular recall, precision and
 * f-measure, in order to evaluate the linkage process.
 * 
 * @author mfranke
 *
 */
public final class QualityMetrics {

	private QualityMetrics() {
		throw new RuntimeException();
	}

	/**
	 * Fraction of the total amount of matches that were actually found.
	 * 
	 * @param  truePositives   number of true matches that were found by the
	 *                         linkage process.
	 * @param  expectedMatches number of matches that occur in a dataset.
	 * @return                 recall a double value ranging from 0 to 1. The
	 *                         higher the recall the lower the number of
	 *                         false-negatives.
	 */
	public static double getRecall(long truePositives, long expectedMatches) {
		return truePositives / (double) expectedMatches;
	}

	/**
	 * Fraction of true matches among all classified matches.
	 * 
	 * @param  truePositives number of true matches that were found by the
	 *                       linkage process.
	 * @param  foundMatches  number of all matches (true + false matches) that
	 *                       were found by the linkage process.
	 * @return               precision a double value ranging from 0 to 1. The
	 *                       higher the precision the lower the number of
	 *                       false-positives.
	 */
	public static double getPrecision(long truePositives, long foundMatches) {
		return truePositives / (double) foundMatches;
	}

	/**
	 * Harmonic mean of precision and recall.
	 * 
	 * @param  recall
	 * @param  precision
	 * @return           f-measure a double value ranging from 0 to 1. The
	 *                   higher the f-measure the higher is the linkage quality.
	 */
	public static double getFMeasure(double recall, double precision) {
		final double denom = recall + precision;

		if (denom <= 0) {
			return 0;
		}
		else {
			return (2 * recall * precision) / denom;
		}
	}
	
	/**
	 * The pseudo precision measure is similar to precision but it is used
	 * whenever no gold standard (true match status) is available. This variant
	 * only considers the number of source records.
	 * 
	 * @param  source number of source records that have at least one link to a
	 *                target record.
	 * @param  links  total number of links.
	 * @return        pseudo precision a double value ranging from 0 to 1.
	 */
	public static double getPseudoPrecision(long source, long links) {
		return source / (double) links;
	}

	/**
	 * The pseudo precision measure is similar to precision but it is used
	 * whenever no gold standard (true match status) is available. This variant
	 * considers the sum of source and target records.
	 * 
	 * @param  source number of source records that have at least one link to a
	 *                target record.
	 * @param  target number of target records that have at least one link to a
	 *                source record.
	 * @param  links  total number of links.
	 * @return        pseudo precision a double value ranging from 0 to 1.
	 */
	public static double getPseudoPrecisionSum(long source, long target, long links) {
		return (source + target) / ((double) 2 * links);
	}

	/**
	 * The pseudo precision measure is similar to precision but it is used
	 * whenever no gold standard (true match status) is available. This variant
	 * considers the minimum of the source and target records.
	 * 
	 * @param  source number of source records that have at least one link to a
	 *                target record.
	 * @param  target number of target records that have at least one link to a
	 *                source record.
	 * @param  links  total number of links.
	 * @return        pseudo precision a double value ranging from 0 to 1.
	 */
	public static double getPseudoPrecisionMin(long source, long target, long links) {
		return Math.min(source, target) / (double) links;
	}

	/**
	 * The pseudo recall measure is similar to recall but it is used whenever no
	 * gold standard (true match status) is available.
	 * 
	 * @param  links     the total number of links.
	 * @param  sourceAll the total number of source records before any blocking
	 *                   or linkage is applied.
	 * @param  targetAll the total number of target records before any blocking
	 *                   or linkage is applied.
	 * @return           pseudo recall a double value ranging from 0 to 1.
	 */
	public static double getPseudoRecall(long links, long sourceAll, long targetAll) {
		return links / (double) Math.min(sourceAll, targetAll);
	}

	/**
	 * The pseudo recall measure is similar to recall but it is used whenever no
	 * gold standard (true match status) is available.
	 * 
	 * @param  source    number of source records that have at least one link to
	 *                   a target record.
	 * @param  target    target number of target records that have at least one
	 *                   link to a source record.
	 * @param  sourceAll the total number of source records before any blocking
	 *                   or linkage is applied.
	 * @param  targetAll the total number of target records before any blocking
	 *                   or linkage is applied.
	 * @return           pseudo recall a double value ranging from 0 to 1.
	 */
	public static double getPseudoRecallAlt(long source, long target, long sourceAll, long targetAll) {
		return (source + target) / (double) (sourceAll + targetAll);
	}

	/**
	 * The pseudo recall measure is similar to recall but it is used whenever no
	 * gold standard (true match status) is available.
	 * 
	 * @param  source    number of source records that have at least one link to
	 *                   a target record.
	 * @param  target    target number of target records that have at least one
	 *                   link to a source record.
	 * @param  sourceAll the total number of source records before any blocking
	 *                   or linkage is applied.
	 * @param  targetAll the total number of target records before any blocking
	 *                   or linkage is applied.
	 * @return           pseudo recall a double value ranging from 0 to 1.
	 */
	public static double getPseudoRecallAltMin(long source, long target, long sourceAll, long targetAll) {
		return (source + target) / (2*(double) Math.min(sourceAll, targetAll));
	}

	/**
	 * Fraction of links that carry the maximum similarity for the source
	 * element and the maximum similarity for the target element and the minimum
	 * of the total number of source and target records.
	 * 
	 * @param  maxBothLinks number of links with maximum similarity for record
	 *                      and target element.
	 * @param  sourceAll    the total number of source records before any
	 *                      blocking or linkage is applied. (?)
	 * @param  targetAll    the total number of target records before any
	 *                      blocking or linkge is apllied. (?)
	 * @return              a double value. The higher the harmony the better
	 *                      expected quality.
	 */
	public static double getHarmony(long maxBothLinks, long sourceAll, long targetAll) {
		return (maxBothLinks) / (double) Math.min(sourceAll, targetAll);
	}
}