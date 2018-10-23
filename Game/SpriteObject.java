package Game;

import java.awt.Image;
import java.awt.image.PixelGrabber;

import GameUtil.Sprite;

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
 * Extension of {@link Sprite} to define animated level objects as exits etc.
 * @author Volker Oth
 */
public class SpriteObject extends Sprite {

	/** Type of level object */
	public static enum Type {
		/** no influence on gameplay */
		PASSIVE,
		/** right arrows - no digging to the left */
		NO_DIG_LEFT,
		/** left arrows - no digging to the right */
		NO_DIG_RIGHT,
		/** trap triggering drowning animation */
		TRAP_DROWN,
		/** trap triggering a replacement with special death animation */
		TRAP_REPLACE,
		/** trap triggering default death animation */
		TRAP_DIE,
		/** level exit (active part!) */
		EXIT,
		/** level entry */
		ENTRY
	}

	/** x position in pixels */
	private int x;
	/** y position in pixels */
	private int y;
	/** Type of level object */
	private Type type;
	/** collision mask - only this part is copied into the stencil */
	private int mask[];

	/**
	 * Get Type depending on integer value from INI.
	 * @param t integer type
	 * @return Type
	 */
	public static Type getType(final int t) {
		switch (t) {
			case 3:
				return Type.NO_DIG_LEFT;
			case 4:
				return Type.NO_DIG_RIGHT;
			case 5:
				return Type.TRAP_DROWN;
			case 6:
				return Type.TRAP_REPLACE;
			case 7:
				return Type.TRAP_DIE;
			case 8:
				return Type.EXIT;
			case 32:
				return Type.ENTRY;
			default:
				return Type.PASSIVE;
		}
	}

	/**
	 * Constructor.
	 * @param sourceImg Image containing animation frames one above each other.
	 * @param animFrames number of frames.
	 */
	public SpriteObject(final Image sourceImg, final int animFrames) {
		super(sourceImg, animFrames);
		type = Type.PASSIVE;
		setX(0);
		setY(0);
	}

	/**
	 * Constructor. Create Sprite from other Sprite.
	 * @param src Sprite to clone.
	 */
	public SpriteObject(final SpriteObject src) {
		super(src);
		setX(src.getX());
		setY(src.getY());
		type = src.type;
		mask = src.mask; // flat copy - no deep copy needed!
	}

	/**
	 * Set the collision mask.
	 * @param imgMask image containing the collision mask.
	 */
	void setMask(final Image imgMask) {
		int w = imgMask.getWidth(null);
		int h = imgMask.getHeight(null);
		mask = new int[w*h];
		PixelGrabber grab = new PixelGrabber(imgMask,0,0,w,h,mask,0,w);
		try {
			grab.grabPixels();
		} catch (InterruptedException interruptedexception) {}
	}

	/**
	 * Get type of level object.
	 * @return mask as used in Stencil
	 */
	int getMaskType() {
		switch (type) {
			case NO_DIG_LEFT:
				return Stencil.MSK_NO_DIG_LEFT;
			case NO_DIG_RIGHT:
				return Stencil.MSK_NO_DIG_RIGHT;
			case TRAP_DROWN:
				return Stencil.MSK_TRAP_DROWN;
			case TRAP_REPLACE:
				return Stencil.MSK_TRAP_REPLACE;
			case TRAP_DIE:
				return Stencil.MSK_TRAP_DIE;
			case EXIT:
				return Stencil.MSK_EXIT;
		}
		return -1;
	}

	/**
	 * Set x position.
	 * @param xi x position in pixels
	 */
	public void setX(final int xi) {
		x = xi;
	}

	/**
	 * Get x position.
	 * @return x position in pixels
	 */
	public int getX() {
		return x;
	}

	/**
	 * Set y position.
	 * @param yi y position in pixels
	 */
	public void setY(final int yi) {
		y = yi;
	}

	/**
	 * Get y position.
	 * @return y position in pixels
	 */
	public int getY() {
		return y;
	}

	/**
	 * Set type of level object.
	 * @param t type of level object
	 */
	public void setType(final Type t) {
		type = t;
	}

	/**
	 * Get type of level object.
	 * @return type of level object
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Get mask value.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @return mask value (ARGB).
	 */
	public int getMask(final int x, final int y) {
		return mask[y*width+x];
	}
}
