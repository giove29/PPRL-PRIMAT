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

import java.math.BigDecimal;


/**
 * Class for converting strings into other standard data types, e.g., integer,
 * float, double and so on.
 * 
 * @author mfranke
 *
 */
public final class StringToObjectConverter {

	private StringToObjectConverter() {
		throw new RuntimeException();
	}

	/**
	 * Parses string s into object of class t.
	 * 
	 * @param  s String to convert.
	 * @param  t Class to parse to, e.g., Integer.class.
	 * @return   Object of specified type.
	 */
	public static Object convert(String s, Class<?> t) {

		if (t.equals(int.class)) {
			return Integer.valueOf(s).intValue();
		}
		else if (t.equals(Integer.class)) {
			return Integer.valueOf(s);
		}
		else if (t.equals(Long.class)) {
			return Long.valueOf(s);
		}
		else if (t.equals(long.class)) {
			return Long.valueOf(s).longValue();
		}
		else if (t.equals(double.class)) {
			return Double.valueOf(s).doubleValue();
		}
		else if (t.equals(Double.class)) {
			return Double.valueOf(s);
		}
		else if (t.equals(float.class)) {
			return Float.parseFloat(s);
		}
		else if (t.equals(Float.class)) {
			return Float.parseFloat(s);
		}
		else if (t.equals(BigDecimal.class)) {
			return BigDecimal.valueOf(Double.parseDouble(s));
		}
		else if (t.equals(boolean.class)) {
			return Boolean.parseBoolean(s);
		}
		else if (t.equals(Boolean.class)) {
			return Boolean.parseBoolean(s);
		}
		else if (t.equals(String.class)) {
			return s;
		}
		else if (t.equals(char.class)) {
			return s.charAt(0);
		}
		else if (t.equals(Character.class)) {
			return s.charAt(0);
		}
		else {
			return null;
		}
	}
}