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

import org.apache.commons.math3.random.RandomGenerator;

import org.apache.commons.math3.random.*;


/**
 * 
 * @author mfranke
 *
 */
public enum RNG {

	ISAAC_RANDOM(new ISAACRandom()), JDK_RANDOM_GENERATOR(new ISAACRandom()), MERSENNE_TWISTER(new ISAACRandom()),
	WELL_1024a(new Well1024a()), WELL_19937a(new Well19937a()), WELL_19937c(new Well19937c()),
	WELL_44497a(new Well44497a()), WELL_44497b(new Well44497b()), WELL_512a(new Well512a());

	private final RandomGenerator rnd;

	private RNG(RandomGenerator rnd) {
		this.rnd = rnd;
	}

	public RandomGenerator getRandomGenerator() {
		return rnd;
	}
}