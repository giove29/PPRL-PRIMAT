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
package de.uni_leipzig.dbs.pprl.primat.dataowner.encoding.bloomfilter.hashing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.uni_leipzig.dbs.pprl.primat.common.utils.RandomFactory;


class RandomHashingTest {

	@Test
	void secureRandomTest() {
		final HashingMethod rh = new RandomHashing(1024, RandomFactory.SECURE_RANDOM);		
		final Set<Integer> res1 = rh.hash("test", 10);
		final Set<Integer> res2 = rh.hash("test", 10);
		
		assertEquals(res1, res2);
	}
	
	@Test
	void standardRandomTest() {
		final HashingMethod rh = new RandomHashing(1024, RandomFactory.RANDOM);	
		final Set<Integer> res1 = rh.hash("test", 10);
		final Set<Integer> res2 = rh.hash("test", 10);
		
		assertEquals(res1, res2);
	}
}