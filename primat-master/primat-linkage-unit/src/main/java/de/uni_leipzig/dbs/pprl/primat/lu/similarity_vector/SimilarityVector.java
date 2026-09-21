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
package de.uni_leipzig.dbs.pprl.primat.lu.similarity_vector;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jgrapht.graph.DefaultWeightedEdge;


/**
 * 
 * @author mfranke
 *
 */
public class SimilarityVector 
extends DefaultWeightedEdge 
implements Comparable<SimilarityVector> {
	
	private static final long serialVersionUID = -3238667838113920492L;
	private Map<Integer, List<Double>> similarityVector;
	private Double aggregatedValue;

	public SimilarityVector() {
		this.similarityVector = new TreeMap<>();
		this.aggregatedValue = null;
	}
	
	public SimilarityVector(double aggregatedValue) {
		this.similarityVector = new TreeMap<>();
		this.aggregatedValue = aggregatedValue;
	}
	
	public SimilarityVector(SimilarityVector other) {
		this.similarityVector = new TreeMap<>(other.getSimilarities());
		this.aggregatedValue = other.aggregatedValue;
	}

	public void setSimilarities(int pos, List<Double> similarities) {
		this.similarityVector.put(pos, similarities);
	}

	public List<Double> getSimilarities(int pos) {
		return this.similarityVector.get(pos);
	}
	
	public Map<Integer, List<Double>> getSimilarities(){
		return this.similarityVector;
	}

	public int getDimension() {
		return this.similarityVector.keySet().size();
	}

	public boolean hasAggregatedValue() {
		return this.aggregatedValue == null;
	}

	public void setAggregatedValue(double value) {
		this.aggregatedValue = value;
	}

	public Double getAggregatedValue() {
		return this.aggregatedValue;
	}

	
	@Override
	public String toString() {
		return 
			"(" 
//			+ super.getSource() 
//			+ ") -- [" 
//			+ similarityVector.toString() 
//			+ "; " 
			+ aggregatedValue 
//			+ "] -- ("
//			+ super.getTarget() 
			+ ")";
	}
	

	@Override
	public int compareTo(SimilarityVector o) {
		return Double.compare(this.aggregatedValue, o.getAggregatedValue());
	}

}