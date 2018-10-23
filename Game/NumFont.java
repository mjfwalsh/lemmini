package Game;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

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
 * Handle small number font.
 * Meant to print out values between 0 and 99.
 *
 * @author Volker Oth
 */
public class NumFont {

	/** width in pixels */
	private static int width;
	/** height in pixels */
	private static int height;
	/** array of images - one for each cipher 0..9 */
	private static BufferedImage numImg[];

	/**
	 * Load and initialize the font.
	 * @throws ResourceException
	 */
	public static void init() throws ResourceException {
		Image sourceImg = Core.loadImage("misc/numfont.gif");
		BufferedImage img[] = ToolBox.getAnimation(sourceImg,10,Transparency.OPAQUE);
		width = sourceImg.getWidth(null);
		height = sourceImg.getHeight(null)/10;
		numImg = new BufferedImage[100];
		for (int i=0; i<100; i++) {
			numImg[i] = ToolBox.createImage(width*2, height, Transparency.OPAQUE);
			Graphics2D g = numImg[i].createGraphics();
			g.drawImage(img[i/10], 0, 0, null);
			g.drawImage(img[i%10], width, 0, null);
			g.dispose();
		}
	}

	/**
	 * Get an image for a number between 0 and 99
	 * @param n number (0..99)
	 * @return image of the number
	 */
	public static BufferedImage numImage(final int n) {
		int num;
		if (n>99)
			num = 99;
		else if (n<0)
			num = 0;
		else num = n;
		return numImg[num];
	}
}
