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
package de.uni_leipzig.dbs.pprl.primat.dataowner.preprocessing.splitting;

/**
 * 
 * @author mfranke
 *
 */
public class PositionSplitter implements Splitter {

	private final int position;
	private final static int PARTS = 2;

	public PositionSplitter(int position) {
		this.position = position;
	}

	@Override
	public String[] split(String string) {
		final String[] result = new String[2];

		if (position < string.length()) {
			result[0] = string.substring(0, this.position);
			result[1] = string.substring(this.position);
		}
		else {
			result[0] = string;
			result[1] = "";
		}
		return result;
	}

	@Override
	public int parts() {
		return PARTS;
	}
}