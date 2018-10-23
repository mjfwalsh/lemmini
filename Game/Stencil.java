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
 * Defines the stencil which defines the properties of each pixel of the level.
 * @author Volker Oth
 */
public class Stencil {
	/* Each pixel is represented by a 32bit value in the stencil.
	 * The lower part is used for a bitmask that contains stencil properties.
	 * The higher part (val << ID_SHIFT_VAL) can contain an identifier for
	 * a special trap, exit etc.
	 * Note: the lowe part is a bitmask, mainly to save space, but also
	 * since some combinations of properties are possible.
	 * Yet of course not all combinations are possible or make sense.
	 */
	/** empty space - background is visible */
	public final static int MSK_EMPTY = 0;
	/** brick - can be destroyed, Lemmings can walk on it */
	public final static int MSK_BRICK = 1;
	/** steel - can't be destroyed, Lemmings can walk on it */
	public final static int MSK_STEEL = 2;
	/** Lemmings can either walk on steel or on brick */
	public final static int MSK_WALK_ON = MSK_BRICK|MSK_STEEL;
	/** stair build by a builder - note that this is just an additional attribute - brick is also needed to walk on it */
	public final static int MSK_STAIR = 4;
	/** right side of stopper mask - reflects to the right */
	public final static int MSK_STOPPER_RIGHT = 8;
	/** left side of stopper mask - reflects to the left */
	public final static int MSK_STOPPER_LEFT = 16;
	/** stopper mask (either left or right) */
	public final static int MSK_STOPPER = MSK_STOPPER_RIGHT|MSK_STOPPER_LEFT;
	/** arrow to the right - no digging to the left */
	public final static int MSK_NO_DIG_LEFT = 32;
	/** arrow to the left - no digging to the right */
	public final static int MSK_NO_DIG_RIGHT = 64;
	/** no digging - either left or right */
	public final static int MSK_NO_DIG = MSK_NO_DIG_LEFT|MSK_NO_DIG_RIGHT;

	/** mask used to erase stencil properties when a pixel is erased */
	public final static int MSK_ERASE = ~(Stencil.MSK_WALK_ON|Stencil.MSK_STAIR|Stencil.MSK_NO_DIG);

	/** a trap triggering the drowning animation - i.e. water */
	public final static int MSK_TRAP_DROWN = 128;
	/** a trap that replaces the Lemming with a special death animation */
	public final static int MSK_TRAP_REPLACE = 256;
	/** a trap that triggers the normal death animation */
	public final static int MSK_TRAP_DIE = 512;
	/** a trap (either DROWN, REPLACE or DIE) */
	public final static int MSK_TRAP = MSK_TRAP_DROWN|MSK_TRAP_REPLACE|MSK_TRAP_DIE;
	/** the level exit */
	public final static int MSK_EXIT = 1024;

	/** number of bits the identifier is shifter up (below is the bitmask part) */
	private final static int ID_SHIFT_VAL = 16;

	/** array which represents the stencil buffer */
	private int stencil[];
	/** width of stencil (=width of level) */
	private int width;
	/** height of stencil (=height of level) */
	private int height;


	/**
	 * Constructor.
	 * @param w width in pixels
	 * @param h height in pixels
	 */
	public Stencil(final int w, final int h) {
		width = w;
		height = h;
		stencil = new int[width * height];
	}

	/**
	 * Clear stencil (fill with MSK_EMPTY).
	 */
	public void clear() {
		int size = width*height;
		for (int idx=0; idx<size; idx++)
			stencil[idx] = MSK_EMPTY;
	}

	/**
	 * Set given value at given position.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @param val stencil value
	 */
	public void set(final int x, final int y, final int val) {
		stencil[x+y*width] = val;
	}

	/**
	 * Set given value at given position.
	 * @param pos position (x*width+y)
	 * @param val stencil value
	 */
	public void set(final int pos, final int val) {
		stencil[pos] = val;
	}

	/**
	 * Get stencil value at given position.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @return stencil value
	 */
	public int get(final int x, final int y) {
		return stencil[x+y*width];
	}

	/**
	 * Get stencil value at given position.
	 * @param pos position (x*width+y)
	 * @return stencil value
	 */
	public int get(final int pos) {
		return stencil[pos];
	}

	/**
	 * AND given value with existing value at given position.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @param val stencil value
	 */
	public void and(final int x, final int y, final int val) {
		int pos = x+y*width;
		stencil[pos] = (stencil[pos] & val);
	}

	/**
	 * AND given value with existing value at given position.
	 * @param pos position (x*width+y)
	 * @param val stencil value
	 */
	public void and(final int pos, final int val) {
		stencil[pos] = (stencil[pos] & val);
	}

	/**
	 * OR given value with existing value at given position.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @param val stencil value
	 */
	public void or(final int x, final int y, final int val) {
		int pos = x+y*width;
		stencil[pos] = (stencil[pos] | val);
	}

	/**
	 * OR given value with existing value at given position.
	 * @param pos position (x*width+y)
	 * @param val stencil value
	 */
	public void or(final int pos, final int val) {
		stencil[pos] = (stencil[pos] | val);
	}

	/**
	 * Set only the ID without changing the lower bitmask part.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @param id identifier (must not exceed 16bit)
	 */
	public void setID(final int x, final int y, final int id) {
		stencil[x+y*width] |= (id << ID_SHIFT_VAL);
	}

	/**
	 * Set only the ID without changing the lower bitmask part.
	 * @param pos position (x*width+y)
	 * @param id identifier (must not exceed 16bit)
	 */
	public void setID(final int pos, final int id) {
		stencil[pos] |= (id << ID_SHIFT_VAL);
	}

	/**
	 * Get the identifier from the stencil.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @return identifier
	 */
	public int getID(final int x, final int y) {
		return (stencil[x+y*width] >> ID_SHIFT_VAL);
	}

	/**
	 * Get the identifier from the stencil.
	 * @param pos position (x*width+y)
	 * @return identifier
	 */
	public int getID(final int pos) {
		return (stencil[pos] >> ID_SHIFT_VAL);
	}

	/**
	 * Get identifier (upper part) from full stencil value.
	 * @param sval stencil value
	 * @return identifier
	 */
	public static int getObjectID(final int sval) {
		return sval >> ID_SHIFT_VAL;
	}

	/**
	 * Create the numerical value used in the stencil from the identifier.
	 * @param id identifier
	 * @return numerical value of the identifier as used in the stencil
	 */
	static int createObjectID(final int id) {
		return id << ID_SHIFT_VAL;
	}

	/** Get width of stencil.
	 * @return width of stencil
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Get height of stencil.
	 * @return height of stencil
	 */
	public int getHeight() {
		return height;
	}
}
