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

import de.uni_leipzig.dbs.pprl.primat.common.utils.MessageDigestAlgorithm;

class StandardHashingTest {

	@Test
	void test() {
		final HashingMethod hm = 
				new StandardHashing(1024, MessageDigestAlgorithm.MD_2, MessageDigestAlgorithm.MD_5);
		final Set<Integer> s1 = hm.hash("a", 2);
		final Set<Integer> s2 = hm.hash("a", 2);
		
		assertEquals(s1, s2);
	}
	
	@Test
	void test2() {
		final HashingMethod hm = 
				new StandardHashing(1024, MessageDigestAlgorithm.MD_2, MessageDigestAlgorithm.MD_5);
		
		assertThrows(RuntimeException.class, () -> {
			hm.hash("a", 4);
		});	
	}
}