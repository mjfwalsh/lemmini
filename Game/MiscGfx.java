package Game;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

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
 * Wrapper class for additional images which don't fit anywhere else.
 *
 * @author Volker Oth
 */
public class MiscGfx {

	/** Index of images */
	public static enum Index {
		/** border for the mini map */
		BORDER,
		/** Lemmin i logo */
		LEMMINI,
		/** green background tile */
		TILE_GREEN,
		/** brows background tile */
		TILE_BROWN,
		/** replay sign 1 */
		REPLAY_1,
		/** replay sign 2 */
		REPLAY_2,
		/** selection marker for replay */
		SELECT
	}

	/** array of images */
	private static BufferedImage image[];

	/**
	 * Initialization.
	 * @throws ResourceException
	 */
	public static void init() throws ResourceException {
		ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
		BufferedImage img;
		/* 0: BORDER */
		img = ToolBox.ImageToBuffered(Core.loadImage("misc/border.gif"),Transparency.OPAQUE);
		images.add(img);
		/* 1: LEMMINI */
		img = ToolBox.ImageToBuffered(Core.loadImageJar("lemmini.png"), Transparency.TRANSLUCENT);
		images.add(img);
		/* 2: TILE_GREEN */
		img = ToolBox.ImageToBuffered(Core.loadImageJar("background.gif"), Transparency.OPAQUE);
		images.add(img);
		/* 3: TILE_BROWN */
		//patch brown version of tile
		BufferedImage brownImg = ToolBox.createImage(img.getWidth(), img.getHeight(),Transparency.BITMASK);
		for (int xp=0; xp<img.getWidth(null); xp++)
			for (int yp=0; yp<img.getHeight(null); yp++) {
				int col = img.getRGB(xp, yp); // A R G B
				int a = col & 0xff000000; // transparent part
				int r = (col >> 16) & 0xff;
				int g = (col >> 8) & 0xff;
				int b = col & 0xff;
				// patch image to brown version by manipulating red component
				r = (r*2)&0xff;
				b = b/2;
				col = a | (g<<16) | (r<<8) | b;
				brownImg.setRGB(xp, yp, col);
			}
		images.add(brownImg);
		/* 4: REPLAY_1 */
		BufferedImage anim[] = ToolBox.getAnimation(Core.loadImage("misc/replay.gif"),2,Transparency.BITMASK);
		images.add(anim[0]);
		/* 5: REPLAY_2 */
		images.add(anim[1]);
		/* 6: SELECT */
		img = ToolBox.ImageToBuffered(Core.loadImage("misc/select.gif"),Transparency.BITMASK);
		images.add(img);

		image = new BufferedImage[images.size()];
		image = images.toArray(image);
	}

	/**
	 * Get image.
	 * @param idx Index
	 * @return image of the given index
	 */
	public static BufferedImage getImage(Index idx) {
		return image[idx.ordinal()];
	}
}
