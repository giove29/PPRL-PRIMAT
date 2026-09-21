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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uni_leipzig.dbs.pprl.primat.common.model.RecordSchema;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.Normalizer;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.NormalizerChain;
import de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.normalizing.SimpleNormalizeDefinition;


/**
 * 
 * @author mfranke
 *
 */
public class NormalizeDefinition {

	private String name;

	private Map<Integer, Normalizer> columnNormalizerMapping;

	public NormalizeDefinition() {
		this("");
	}

	public NormalizeDefinition(String name) {
		this.name = name;
		this.columnNormalizerMapping = new HashMap<>();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<Normalizer> getAllNormalizer() {
		return this.columnNormalizerMapping.values();
	}

	public Set<Integer> getAffectedColumns() {
		return this.columnNormalizerMapping.keySet();
	}
	
	public Normalizer setNormalizer(String columnToNormalize, Normalizer normalizer) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(columnToNormalize);
		return this.setNormalizer(col, normalizer);
	}

	public Normalizer setNormalizer(int columnToNormalize, Normalizer normalizer) {
		return this.columnNormalizerMapping.put(columnToNormalize, normalizer);
	}

	public static NormalizeDefinition from(SimpleNormalizeDefinition simpleDef) {
		final NormalizeDefinition normDef = new NormalizeDefinition(simpleDef.getName());
		final Set<Integer> columns = simpleDef.getColumns();
		final List<Normalizer> normalizer = simpleDef.getNormalizer();

		for (final Integer col : columns) {
			final NormalizerChain normChain = new NormalizerChain(normalizer);
			normDef.setNormalizer(col, normChain);
		}
		return normDef;
	}

	public Normalizer getNormalizer(int columnToNormalize) {
		return this.columnNormalizerMapping.get(columnToNormalize);
	}
	
	public Normalizer getNormalizer(String columnToNormalize) {
		final Integer col = RecordSchema.INSTANCE.getAttributeColumn(columnToNormalize);
		return this.getNormalizer(col);
	}

	public Map<Integer, Normalizer> getColumnToNormalizerMapping() {
		return this.columnNormalizerMapping;
	}

	public void merge(NormalizeDefinition normDef) {
		final Map<Integer, Normalizer> normMapping = normDef.getColumnToNormalizerMapping();

		normMapping.forEach((columnId, normalizer) -> {

			if (this.columnNormalizerMapping.containsKey(columnId)) {
				final Normalizer currentNormalizer = this.columnNormalizerMapping.get(columnId);

				if (currentNormalizer instanceof NormalizerChain) {
					final NormalizerChain currentNormChain = (NormalizerChain) currentNormalizer;
					currentNormChain.add(normalizer);
				}
				else {
					final NormalizerChain normChain = new NormalizerChain();
					normChain.add(currentNormalizer);
					normChain.add(normalizer);
				}
			}
			else {
				this.columnNormalizerMapping.put(columnId, normalizer);
			}
		});
	}

	@Override
	public String toString() {
		return this.getName();
	}
}