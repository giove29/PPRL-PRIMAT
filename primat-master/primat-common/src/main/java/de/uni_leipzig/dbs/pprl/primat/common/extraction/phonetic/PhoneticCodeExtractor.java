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

package de.uni_leipzig.dbs.pprl.primat.common.extraction.phonetic;

import java.util.List;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;

import org.apache.commons.codec.language.*;
import org.apache.commons.codec.language.bm.BeiderMorseEncoder; 



import de.uni_leipzig.dbs.pprl.primat.common.extraction.FeatureExtractor;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;


/**
 * A phonetic encoding function is an algorithm for transforming
 * words into an phonetic representation based on their pronunciation.
 * As a consequence, two words producing the same phonetic code
 * will sound similar.
 * 
 * @author mfranke
 *
 */
public enum PhoneticCodeExtractor implements FeatureExtractor {

	SOUNDEX(new Soundex()),
	
	REFINED_SOUNDEX(new RefinedSoundex()),
	
	METAPHONE(new Metaphone()),
	
	DOUBLE_METAPHONE(new DoubleMetaphone()),
	
	COLOGNE_PHONETIC(new ColognePhonetic()),
	
	NYSIIS(new Nysiis()),
	
	BEIDER_MORSE(new BeiderMorseEncoder());
	
	private final StringEncoder codec;

	private PhoneticCodeExtractor(StringEncoder codec) {
		this.codec = codec;
	}
	
	public String get(String attrValue) {
		String encoding = "";

		try {
			encoding = this.codec.encode(attrValue);
		}
		catch (EncoderException e) {
			throw new RuntimeException();
		}
		
		return encoding; 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> extract(QidAttribute<?> attrValue) {
		return List.of(this.get(attrValue.getStringValue()));
	}
}