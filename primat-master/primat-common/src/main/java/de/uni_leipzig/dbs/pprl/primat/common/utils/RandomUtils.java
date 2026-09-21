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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;


/**
 * 
 * @author mfranke
 *
 */
public final class RandomUtils {

	private RandomUtils() {
		throw new RuntimeException();
	}

	public static Random getRandom() {
		return new Random();
	}

	public static Random getRandom(long seed) {
		return new Random(seed);
	}

	public static SecureRandom getSecureRandom() {
		return getSecureRandom(PRNG.SHA1_PRNG);
	}

	public static SecureRandom getSecureRandom(long seed) {
		return getSecureRandom(PRNG.SHA1_PRNG, seed);		
	}

	public static SecureRandom getSecureRandom(PRNG prng) {
		try {
			return SecureRandom.getInstance(prng.getName());
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static SecureRandom getSecureRandom(PRNG prng, long seed) {
		final SecureRandom rnd = getSecureRandom(prng);
		rnd.setSeed(seed);
		return rnd;
	}

	public static RandomDataGenerator getRandomDataGenerator() {
		return new RandomDataGenerator();
	}

	public static RandomDataGenerator getRandomDataGenerator(RandomGenerator rnd) {
		return new RandomDataGenerator(rnd);
	}

	public static RandomGenerator getRandomGenerator(RNG rng) {
		return rng.getRandomGenerator();
	}

	public static RandomGenerator getRandomGenerator(RNG rng, long seed) {
		final RandomGenerator rnd = rng.getRandomGenerator();
		rnd.setSeed(seed);
		return rnd;
	}
	
	public static String getRandomStringAlphanumeric(int length, Random rnd) {
		return RandomStringUtils.random(length, 0, 0, true, true, null, rnd);
	}
	
	public static String getRandomStringAlphabetic(int length, Random rnd) {
		return RandomStringUtils.random(length, 0, 0, true, false, null, rnd);
	}
	
	public static String getRandomStringNumeric(int length, Random rnd) {
		return RandomStringUtils.random(length, 0, 0, false, true, null, rnd);
	}
}