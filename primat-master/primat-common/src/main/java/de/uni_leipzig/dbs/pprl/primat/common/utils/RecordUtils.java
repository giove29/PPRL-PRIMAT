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
package de.uni_leipzig.dbs.pprl.primat.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.uni_leipzig.dbs.pprl.primat.common.model.Record;
import de.uni_leipzig.dbs.pprl.primat.common.model.attributes.QidAttribute;

/**
 * 
 * @author mfranke
 *
 */
public final class RecordUtils {

	private RecordUtils() {
		throw new RuntimeException();
	}

//	public static Map<String, List<Record>> groupById(Collection<Record> records) {
//		return records.stream().collect(Collectors.groupingBy(r -> r.getId()));
//	}

	public static Map<String, List<Record>> groupByGlobalId(Collection<Record> records) {
		return records.stream().collect(Collectors.groupingBy(r -> r.getGlobalId()));
	}
	
	public static Map<String, List<Record>> groupByParty(Collection<Record> records) {
		return records.stream().collect(Collectors.groupingBy(r -> r.getParty().getName()));
	}
	
	 public static long numberOfSources(Collection<Record> records) {
		 return records.stream()
		      .map(r -> r.getParty())
		      .distinct()
		      .count();
	}
	 
	 public static Map<Integer, List<QidAttribute<?>>> groupByAttribute(Collection<Record> records) {
		 final Map<Integer, List<QidAttribute<?>>> result = new HashMap<>();
		 
		 for (final Record rec : records) {
			 final List<QidAttribute<?>> attrList = rec.getAttributes();
			 for (int i = 0; i < attrList.size(); i++) {
				 if (!result.containsKey(i)) {
					result.put(i, new ArrayList<>());
				 }
				 final QidAttribute<?> attr = attrList.get(i); 
				 result.get(i).add(attr);
			 }
		 }
		 
		 return result;
	 }


}
