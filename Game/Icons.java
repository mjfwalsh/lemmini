package Game;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import GameUtil.Sprite;
import Tools.ToolBox;

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
 * Handle the control icons.
 *
 * @author Volker Oth
 */
public class Icons {

	/** icon width in pixels */
	public final static int WIDTH = 32;
	/** icon height in pixels */
	public final static int HEIGHT = 40;

	/** Icon types */
	public static enum Type {
		/** minus icon */
		MINUS,
		/** plus icon */
		PLUS,
		/** climber icon */
		CLIMB,
		/** floater icon */
		FLOAT,
		/** bomber icon */
		BOMB,
		/** blocker icon */
		BLOCK,
		/** builder icon */
		BUILD,
		/** basher icon */
		BASH,
		/** miner icon */
		MINE,
		/** digger icon */
		DIG,
		/** pause icon */
		PAUSE,
		/** nuke icon */
		NUKE,
		/** fast forward icon */
		FFWD,
		/** an empty icon (not used) */
		EMPTY,
		/** invalid no such icon */
		INVALID;

		private static final Map<Integer,Type> lookup = new HashMap<Integer,Type>();

		static {
			for(Type s : EnumSet.allOf(Type.class))
				lookup.put(s.ordinal(), s);
		}

		/**
		 * Reverse lookup implemented via hashtable.
		 * @param val Ordinal value
		 * @return Parameter with ordinal value val
		 */
		public static Type get(final int val) {
			return lookup.get(val);
		}
	}

	/** 1st radio button */
	private final static int FIRST_RADIO = Type.CLIMB.ordinal();
	/** last radio button */
	private final static int LAST_RADIO = Type.DIG.ordinal();
	/** last icon to be drawn */
	private final static int LAST_DRAWN = Type.FFWD.ordinal();

	/** array of Sprites that contains the icons */
	private static Sprite icons[];
	/** buffered image that contains the whole icon bar in its current state */
	private static BufferedImage iconImg;
	/** graphics object used to draw on iconImg */
	private static Graphics2D iconGfx;


	/**
	 * Initialization.
	 * @param cmp parent component
	 * @throws ResourceException
	 */
	public static void init(final Component cmp) throws ResourceException {
		iconImg = ToolBox.createImage(WIDTH*(1+LAST_DRAWN),HEIGHT,Transparency.OPAQUE);
		iconGfx = iconImg.createGraphics();
		MediaTracker tracker = new MediaTracker(cmp);
		icons = new Sprite[15];
		for (int i=0; i<14; i++) {
			Image sourceImg = Core.loadImage(tracker, "misc/icon_"+i+".gif");
			try {
				tracker.waitForAll();
			} catch (InterruptedException ex) {}
			icons[i] = new Sprite(sourceImg, (i==Type.EMPTY.ordinal())?1:2);
			if (i<=LAST_DRAWN)
				iconGfx.drawImage(icons[i].getImage(),WIDTH*i,0,null);
		}
	}

	/**
	 * Get Icon type by x position.
	 * @param x x position inside bar in pixels
	 * @return Icon type
	 */
	public static Type getType(final int x) {
		if (x>=(LAST_DRAWN+1)*WIDTH)
			return Type.INVALID; // invalid
		return Type.get(x/WIDTH);
	}

	/**
	 * Get buffered image that contains the whole icon bar in its current state.
	 * @return image of icon bar
	 */
	public static BufferedImage getImg() {
		return iconImg;
	}

	/**
	 * Get pressed state of the given Icon
	 * @param type
	 * @return
	 */
	static boolean isPressed(Type type) {
		int idx = type.ordinal();
		if (idx > LAST_DRAWN)
			return false;
		return (icons[idx].getFrameIdx() == 1);
	}

	/**
	 * Press down icon.
	 * @param type Icon Type
	 */
	static void press(final Type type) {
		int idx = type.ordinal();
		switch (type) {
			case PAUSE:
			case FFWD:
				icons[idx].setFrameIdx((icons[idx].getFrameIdx()==0)?1:0); // toggle
				if (idx <= LAST_DRAWN)
					iconGfx.drawImage(icons[idx].getImage(),WIDTH*idx,0,null);
				break;
			case CLIMB:
			case FLOAT:
			case BOMB:
			case BLOCK:
			case BUILD:
			case BASH:
			case MINE:
			case DIG:
				for (int i=FIRST_RADIO; i<=LAST_RADIO; i++)
					if (i!=idx) {
						icons[i].setFrameIdx(0);
						iconGfx.drawImage(icons[i].getImage(),WIDTH*i,0,null);
					}
				//$FALL-THROUGH$
			case MINUS:
			case PLUS:
			case NUKE:
				icons[idx].setFrameIdx(1); // set "pressed" frame
				if (idx <= LAST_DRAWN)
					iconGfx.drawImage(icons[idx].getImage(),WIDTH*idx,0,null);
				break;
		}
	}

	/**
	 * Release Icon.
	 * @param type Icon Type
	 */
	static void release(final Type type) {
		int idx = type.ordinal();
		switch (type) {
			case MINUS:
			case PLUS:
			case NUKE:
				icons[idx].setFrameIdx(0); // set "released" frame
				if (idx <= LAST_DRAWN)
					iconGfx.drawImage(icons[idx].getImage(),WIDTH*idx,0,null);
				break;
		}
	}

	/**
	 * Reset Icon bar.
	 */
	static void reset() {
		for (int i=0; i<=LAST_DRAWN; i++) {
			icons[i].setFrameIdx(0);
			iconGfx.drawImage(icons[i].getImage(),WIDTH*i,0,null);
		}
	}
}
