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

/**
 * 
 * @author mfranke
 *
 */
public final class StandardStringNormalizer implements Normalizer {

	private final NormalizerChain normalizer;

	public StandardStringNormalizer() {
		this.normalizer = new NormalizerChain();
		this.normalizer.add(new WhitespaceRemover());
		this.normalizer.add(new UmlautNormalizer());
		this.normalizer.add(new AccentRemover());
		this.normalizer.add(new PunctuationRemover());
		this.normalizer.add(new NumberToLetterLowerCaseNormalizer());
		this.normalizer.add(new LowerCaseNormalizer());
	}

	@Override
	public String normalize(String string) {
		return this.normalizer.normalize(string);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}