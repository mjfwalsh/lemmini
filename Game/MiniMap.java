package Game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

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
 * Handles the mini map.
 * @author Volker Oth
 */
public class MiniMap {

	/** color of Lemmings in mini map */
	final private static Color LEMM_COLOR = Color.RED;
	/** color of screen frame in mini map */
	final private static Color BORDER_COLOR = Color.YELLOW;

	/** image used for mini map */
	private static BufferedImage img;
	/** X position in main gfx */
	private static int xPos;
	/** Y position in main gfx */
	private static int yPos;
	/** X scale */
	private static int scaleX;
	/** Y scale */
	private static int scaleY;
	/** height of mini map */
	private static int height;
	/** width of mini map */
	private static int width;

	/**
	 * init
	 * @param x X position in main gfx used in drawLemming() and move()
	 * @param y Y position in main gfx used in drawLemming() and move()
	 * @param sx X Scale (2 -> 0.5)
	 * @param sy Y Scale (3 -> 0.333)
	 * @param tint true: apply greenish tint, false: use original colors
	 */
	public static void init(final int x, final int y, final int sx, final int sy, final boolean tint) {
		xPos = x;
		yPos = y;
		scaleX = sx;
		scaleY = sy;
		Level level = GameController.getLevel();
		BufferedImage bgImage = GameController.getBgImage();
		img = level.createMiniMap(img, bgImage, scaleX, scaleY, tint);
		width = img.getWidth();
		height = img.getHeight();
	}

	/**
	 * Draw mini map.
	 * @param g Graphics object to draw on
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @param xOfs horizontal level offset
	 */
	public static void draw(final Graphics2D g, final int x, final int y, final int xOfs) {
		int wWidth = Core.getDrawWidth();
		g.drawImage(img,x,y,null);
		g.setColor(BORDER_COLOR);
		g.drawRect(x+xOfs/scaleX,y,wWidth/scaleX,img.getHeight()-1);
	}

	/**
	 * Draw Lemming in mini map.
	 * @param g Graphics object to draw on
	 * @param lx original lemming x position in pixels
	 * @param ly original lemming y position in pixels
	 */
	public static void drawLemming(final Graphics2D g, final int lx, final int ly) {
		int x = xPos + (lx+scaleX/2)/scaleX;
		int y = yPos + (ly+scaleY/2)/scaleY;
		g.setColor(LEMM_COLOR);
		g.fillRect(x,y,2,2);
	}

	/**
	 * Return current image.
	 * @return current image.
	 */
	public static BufferedImage getImage() {
		return img;
	}

	/**
	 * Move screen frame via mini map.
	 * @param x cursor x position in original gfx.
	 * @param y cursor y position in original gfx.
	 * @param swidth screen width
	 * @return new horizontal screen offset
	 */
	public static int move(final int x, final int y, final int swidth) {
		if (y < yPos || y >= yPos+height || x < xPos || x >= xPos+width)
			return -1; // cursor outside the mini map
		int xOfs = (x-xPos)*scaleX-swidth/2;
		if (xOfs < 0)
			xOfs = 0;
		if (xOfs > Level.WIDTH - swidth)
			xOfs = Level.WIDTH - swidth-1;
		return xOfs;
	}
}
