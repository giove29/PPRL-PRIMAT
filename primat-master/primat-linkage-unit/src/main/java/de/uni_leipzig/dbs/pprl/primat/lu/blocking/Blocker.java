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
package de.uni_leipzig.dbs.pprl.primat.lu.blocking;

import java.util.Collection;
import java.util.Map;

import de.uni_leipzig.dbs.pprl.primat.common.model.Party;
import de.uni_leipzig.dbs.pprl.primat.common.model.Record;


/**
 * A Blocker implements a specific blocking approach. Usually one or more
 * blocking keys are defined and used to group records into blocks.
 * Consequently, only records within the same block need to be compared.
 * 
 * @author mfranke
 *
 */
public interface Blocker {

	// TODO: Class dataset as input??
	public Collection<Block> getBlocks(Map<Party, Collection<Record>> records);

}