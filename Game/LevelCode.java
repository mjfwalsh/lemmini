package Game;

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
 * @author Volker Oth
 * Create and evaluate lemmings level codes
 * Based on the documentation and Basic code of Herman Perk (LemGen)
 */
public class LevelCode {
	//  nibble 0 | nibble 1 | nibble 2 | nibble 3  | nibble 4 | nibble 5 | nibble 6
	// ----------|----------|----------|-----------|----------|----------|---------
	//  3  2  1 0| 3  2 1  0|3  2  1  0| 3  2  1  0| 3 2  1  0|3  2  1  0|3  2 1  0
	// ----------|----------|----------|-----------|----------|----------|---------
	// L0 %0 S0 0|S1 L1 0 %1|0 L2 %2 S2|S4 S3 %3 L3|%4 0 L4 S5|0 %5 S6 L5|0 L6 0 %6

	/* magic: S*/
	private final static int MMASK[]   = { 1, 2, 4, 24, 32, 64, 0};
	private final static int MSHIFTL[] = { 1, 2, 0,  0,  0,  0, 0};
	private final static int MSHIFTR[] = { 0, 0, 2,  1,  5,  5, 0};
	/* level: L */
	private final static int LMASK[]   = { 1, 2, 4,  8, 16, 32, 64};
	private final static int LSHIFTL[] = { 3, 1, 0,  0,  0,  0,  0};
	private final static int LSHIFTR[] = { 0, 0, 0,  3,  3,  5,  4};
	/* percent: % */
	private final static int PMASK[]   = { 1, 2, 4,  8, 16, 32, 64};
	private final static int PSHIFTL[] = { 2, 0, 0,  0,  0,  0, 0};
	private final static int PSHIFTR[] = { 0, 1, 1,  2,  1,  3, 6};

	private final static int MAX_LVL_NUM = 127;


	/**
	 * Create a level code from the given parameters
	 * @param seed The seed string used as base for the level code
	 * @param lvl The level number (0..127)
	 * @param percent Percentage of levels saved in the level won to get this code
	 * @param magic A "magic" number with more or less unknown sense
	 * @param offset Used to get a higher code for the first level
	 * @return String containing level code
	 */
	public static String create(final String seed, int lvl, final int percent, final int magic, final int offset) {
		if (lvl > MAX_LVL_NUM || percent > 127 || magic > 127 || seed==null || seed.length() != 10)
			return null;
		byte bi[] = seed.getBytes();
		byte bo[] = new byte[bi.length];

		// add offset and wrap around
		int level = lvl + offset;
		level %= (MAX_LVL_NUM+1);

		// create first 7 bytes
		int sum = 0;
		for (int i=0; i<7; i++) {
			bi[i] += (byte)(((magic & MMASK[i]) << MSHIFTL[i]) >>> MSHIFTR[i]);
			bi[i] += (byte)(((level & LMASK[i]) << LSHIFTL[i]) >>> LSHIFTR[i]);
			bi[i] += (byte)(((percent & PMASK[i]) << PSHIFTL[i]) >>> PSHIFTR[i]);
			bo[(i + 8 - (level % 8)) % 7] = bi[i]; // rotate
			sum += bi[i] & 0xff; // checksum
		}
		// create bytes 8th and 9th byte (level)
		bo[7] = (byte)(bi[7] + (level & 0xf));
		bo[8] = (byte)(bi[8] + ((level & 0xf0) >> 4));
		sum += (bo[7] + bo[8]) & 0xff;
		// create 10th byte (checksum)
		bo[9] = (byte)(bi[9] + (sum & 0x0f));
		return new String(bo);
	}

	/**
	 * Extract the level number from the level code and seed
	 * @param seed The seed string used as base for the level code
	 * @param code Code that contains the level number (amongst other things)
	 * @param offset Used to get a higher code for the first level
	 * @return Level number extracted from the level code (-1 in case of error)
	 */
	public static int getLevel(final String seed, final String code, final int offset) {
		byte bs[] = seed.getBytes();
		byte bi[] = code.getBytes();
		byte bo[] = new byte[bi.length];

		if (seed.length() != 10 || code.length() != 10)
			return -1;

		int level = ((bi[7]-bs[7]) & 0xf) + (((bi[8]-bs[8])& 0xf)<<4);
		// unrotate
		for (int j=0; j<7; j++)
			bo[(j + 6 + (level % 8)) % 7] = bi[j];
		//decode
		int level_ = 0;
		int percent = 0;
		for (int i=0; i<7; i++) {
			int nibble = (bo[i] - bs[i]) & 0xff; // reconstruct nibble stored
			level_ += ((nibble << LSHIFTR[i]) >> LSHIFTL[i]) & LMASK[i];
			percent += ((nibble << PSHIFTR[i]) >> PSHIFTL[i]) & PMASK[i];
		}
		if (level != level_ || percent > 100)
			return -1;

		level -= offset;
		while (level < 0)
			level += MAX_LVL_NUM;

		return level;
	}
}
