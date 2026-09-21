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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Pretty print a table Based on:
 * https://www.logicbig.com/how-to/code-snippets/jcode-java-cmd-command-line-table.html
 */
public class CommandLineTable {
	private static final String HORIZONTAL_SEP = "-";
	private String verticalSep;
	private String joinSep;
	private String[] headers;
	private List<String[]> rows = new ArrayList<>();
	private boolean rightAlign;

	public CommandLineTable() {
		setShowVerticalLines(false);
	}

	public void setRightAlign(boolean rightAlign) {
		this.rightAlign = rightAlign;
	}

	public void setShowVerticalLines(boolean showVerticalLines) {
		verticalSep = showVerticalLines ? "|" : "";
		joinSep = showVerticalLines ? "+" : " ";
	}

	public void setHeaders(List<String> headers) {
		setHeaders(headers.toArray(new String[0]));
	}

	public void setHeaders(String... headers) {
		this.headers = headers;
	}

	public void addRow(List<String> cells) {
		addRow(cells.toArray(new String[0]));
	}

	public void addRow(String... cells) {
		rows.add(cells);
	}

	public String build() {
		StringBuilder sb = new StringBuilder();
		int[] maxWidths = headers != null ? Arrays.stream(headers).mapToInt(String::length).toArray() : null;

		for (String[] cells : rows) {

			if (maxWidths == null) {
				maxWidths = new int[cells.length];
			}

			if (cells.length != maxWidths.length) {
				throw new IllegalArgumentException("Number of row-cells and headers should be consistent");
			}

			for (int i = 0; i < cells.length; i++) {
				maxWidths[i] = Math.max(maxWidths[i], cells[i].length());
			}
		}

		if (headers != null) {
			sb.append(buildLine(maxWidths));
			sb.append(buildRow(headers, maxWidths));
			sb.append(buildLine(maxWidths));
		}

		for (String[] cells : rows) {
			sb.append(buildRow(cells, maxWidths));
		}

		if (headers != null) {
			sb.append(buildLine(maxWidths));
		}
		return sb.toString();
	}

	private String buildLine(int[] columnWidths) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < columnWidths.length; i++) {
			String line = String.join("",
				Collections.nCopies(columnWidths[i] + verticalSep.length() + 1, HORIZONTAL_SEP));
			sb.append(joinSep + line + (i == columnWidths.length - 1 ? joinSep : ""));
		}
		sb.append("\n");
		return sb.toString();
	}

	private String buildRow(String[] cells, int[] maxWidths) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < cells.length; i++) {
			String s = cells[i];
			String verStrTemp = i == cells.length - 1 ? verticalSep : "";

			if (rightAlign) {
				sb.append(String.format("%s %" + maxWidths[i] + "s %s", verticalSep, s, verStrTemp));
			}
			else {
				sb.append(String.format("%s %-" + maxWidths[i] + "s %s", verticalSep, s, verStrTemp));
			}
		}
		sb.append("\n");
		return sb.toString();
	}
}