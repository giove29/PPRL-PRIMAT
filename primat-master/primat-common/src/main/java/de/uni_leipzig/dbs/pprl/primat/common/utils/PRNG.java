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

public enum PRNG {

	NATIVE_PRNG("NativePRNG"), 
	NATIVE_PRNG_BLOCKING("NativePRNGBlocking"),
	NATIVE_PRNG_NON_BLOCKING("NativePRNGNonBlocking"), 
	PKCS11("PKCS11"), 
	DRBG("DRBG"), 
	SHA1_PRNG("SHA1PRNG"),
	WINDOWS_PRNG("HmacSHA512/224");

	private final String name;

	private PRNG(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}