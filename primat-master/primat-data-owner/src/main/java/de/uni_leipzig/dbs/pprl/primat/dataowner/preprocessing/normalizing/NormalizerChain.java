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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * Build a ordered chain of normalizer functions.
 * 
 * @author mfranke
 *
 */
public class NormalizerChain implements Normalizer {

	private List<Normalizer> normalizer;

	public NormalizerChain() {
		this.normalizer = new ArrayList<>();
	}

	@SafeVarargs
	public NormalizerChain(Normalizer... normalizer) {
		this.normalizer = Arrays.asList(normalizer);
	}

	public NormalizerChain(Collection<Normalizer> normalizer) {
		this.normalizer = new ArrayList<>(normalizer);
	}

	public void add(Normalizer normalizer) {
		this.normalizer.add(normalizer);
	}

	public void addAll(Collection<Normalizer> normalizer) {
		this.normalizer.addAll(normalizer);
	}

	public List<Normalizer> getNormalizer() {
		return this.normalizer;
	}

	@Override
	public String normalize(String string) {
		String result = string;

		for (final Normalizer normalizer : this.normalizer) {
			result = normalizer.normalize(result);
		}
		return result;
	}
}