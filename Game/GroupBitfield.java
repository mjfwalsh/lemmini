package Game;

import java.math.BigInteger;

/*
 * Copyright 2009 Volker Oth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Wrapper class which uses BigInteger to implement a large bitfield
 * in which single bits can be set via testBit and setBit.
 */
public class GroupBitfield extends BigInteger {

	/** define "1" to avoid multiple instances */
	final static GroupBitfield ONE = new GroupBitfield("1");

	private final static long serialVersionUID = 1;

	/**
	 * Constructor with numerical value as string
	 * @param s numerical value as string
	 */
	public GroupBitfield(final String s) {
		super(s);
	}

	/**
	 * Constructor with numerical value as BigInteger
	 * @param i numerical value as BigInteger
	 */
	public GroupBitfield(final BigInteger i) {
		super(i.toString());
	}
}
