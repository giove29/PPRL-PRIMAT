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

/**
 * 
 * ANSI codes that can be used to color console output.
 * 
 * @author mfranke
 *
 */
public enum ANSICode {

	RESET("\u001B[0m"), BLACK("\u001B[30m"), RED("\u001B[31m"), GREEN("\u001B[32m"), YELLOW("\u001B[33m"),
	BLUE("\u001B[34m"), PURPLE("\u001B[35m"), CYAN("\u001B[36m"), WHITE("\u001B[37m"), YELLOW_BACKGROUND("\u001B[43m"),
	BLUE_BACKGROUND("\u001B[44m"), PURPLE_BACKGROUND("\u001B[45m"), CYAN_BACKGROUND("\u001B[46m"),
	WHITE_BACKGROUND("\u001B[47m");

	private final String code;

	private ANSICode(String code) {
		this.code = code;
	}

	/**
	 * Color a String to be print on console.
	 * 
	 * @param  string the string to print.
	 * @param  code   the color code to use.
	 * @return        the modified string.
	 */
	public static String color(String string, ANSICode code) {
		return code.getCode() + string + ANSICode.RESET.getCode();
	}

	/**
	 * @return ANSI color code.
	 */
	private String getCode() {
		return this.code;
	}
}