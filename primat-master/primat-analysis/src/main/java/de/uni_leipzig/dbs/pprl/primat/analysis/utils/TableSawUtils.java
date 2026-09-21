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
package de.uni_leipzig.dbs.pprl.primat.analysis.utils;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.stream.Collectors;


public class TableSawUtils {

	public static Table convertAllTextColumnToStringColumn(Table in) {
		List<String> textColumnNames = in.columnsOfType(ColumnType.TEXT).stream().map(c -> c.name())
			.collect(Collectors.toList());

		for (String textColumnName : textColumnNames) {
			in = convertTextColumnToStringColumn(in, textColumnName);
		}
		return in;
	}

	public static Table convertTextColumnToStringColumn(Table in, String colName) {
		return in.replaceColumn(colName,
			in.textColumn(colName).mapInto(t -> t, StringColumn.create(colName, in.rowCount())));
	}
}
