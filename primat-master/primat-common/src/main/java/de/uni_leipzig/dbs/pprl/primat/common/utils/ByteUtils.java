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

import java.math.BigInteger;
import java.nio.ByteBuffer;


/**
 * 
 * @author mfranke
 *
 */
public final class ByteUtils {

	private ByteUtils() {
		throw new RuntimeException();
	}

	private static ByteBuffer intByteBuffer = ByteBuffer.allocate(Integer.BYTES);
	private static ByteBuffer longByteBuffer = ByteBuffer.allocate(Long.BYTES);

	public static int bytesToInt(byte[] data) {
		return ByteBuffer.wrap(data).getInt();
	}

	public static int bytesToIntBigInt(byte[] data) {
		return new BigInteger(data).intValue();
	}

	public static byte[] intToBytes(int x) {
		intByteBuffer.putInt(x);
		return intByteBuffer.array();
	}

	public static byte[] longToBytes(long x) {
		longByteBuffer.putLong(0, x);
		return longByteBuffer.array();
	}

	public static long bytesToLong(byte[] bytes) {
		longByteBuffer.put(bytes, 0, bytes.length);
		longByteBuffer.flip();
		return longByteBuffer.getLong();
	}

}