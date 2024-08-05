package Game;

import Tools.ToolBox;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * Copyright 2009 Volker Oth
 * (Minor changes by Michael J. Walsh Copyright 2018)
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
 * Handle the main bitmap font.
 *
 * @author Volker Oth
 */
public class LemmFont {

  /** Colors */
  public static enum Color {
    /** green color */
    GREEN,
    /** blue color */
    BLUE,
    /** red color */
    RED,
    /** brown/yellow color */
    BROWN,
    /** turquoise/cyan color */
    TURQUOISE,
    /** violet color */
    VIOLET
  }

  /** ascii chars from ! to ~ inclusive */
  private static final int NUM_FONT_CHARS = 94;

  /** width of one character in pixels */
  private static int charWidth;

  /** height of one character pixels */
  private static int charHeight;

  /** array of array of images [color,character] */
  private static HashMap<Color, ArrayList<BufferedImage>> img;

  /**
   * Initialization.
   *
   * @throws ResourceException
   */
  public static void init() throws ResourceException {
    BufferedImage sourceImg =
        ToolBox.ImageToBuffered(Core.loadImage("misc/lemmfontscaled.gif"), Transparency.BITMASK);

    int h = sourceImg.getHeight();
    int w = sourceImg.getWidth();

    charWidth = w;
    charHeight = h / NUM_FONT_CHARS;

    HashMap<Color, BufferedImage> sourceImges = new HashMap<>();
    sourceImges.put(Color.GREEN, sourceImg);

    Color[] otherColors = {Color.RED, Color.BLUE, Color.TURQUOISE, Color.BROWN, Color.VIOLET};
    for (Color c : otherColors) sourceImges.put(c, ToolBox.createImage(w, h, Transparency.BITMASK));

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        int col = sourceImg.getRGB(x, y); // A R G B
        int a = col & 0xff000000; // transparent part
        int r = (col >> 16) & 0xff;
        int g = (col >> 8) & 0xff;
        int b = col & 0xff;

        // patch image to red version by swapping red and green components
        col = a | (g << 16) | (r << 8) | b;
        sourceImges.get(Color.RED).setRGB(x, y, col);

        // patch image to blue version by swapping blue and green components
        col = a | (r << 16) | (b << 8) | g;
        sourceImges.get(Color.BLUE).setRGB(x, y, col);

        // patch image to turquoise version by setting blue component to value of green component
        col = a | (r << 16) | (g << 8) | g;
        sourceImges.get(Color.TURQUOISE).setRGB(x, y, col);

        // patch image to yellow version by setting red component to value of green component
        col = a | (g << 16) | (g << 8) | b;
        sourceImges.get(Color.BROWN).setRGB(x, y, col);

        // patch image to violet version by exchanging red and blue with green
        col = a | (g << 16) | (((r + b) << 7) & 0xff00) | g;
        sourceImges.get(Color.VIOLET).setRGB(x, y, col);
      }
    }

    img = new HashMap<Color, ArrayList<BufferedImage>>();
    for (Color c : Color.values())
      img.put(
          c,
          ToolBox.getAnimation(
              sourceImges.get(c), NUM_FONT_CHARS, Transparency.BITMASK, charWidth));
  }

  /**
   * Draw string into graphics object in given color.
   *
   * @param g graphics object to draw to.
   * @param s string to draw.
   * @param x x coordinate in pixels
   * @param y y coordinate in pixels
   * @param offX x coordinate in characters
   * @param offY y coordinate in characters
   * @param color Color
   */
  public static void strImage(
      final Graphics2D g, final String s, int x, int y, int offX, int offY, final Color color) {
    x += offX * charWidth;
    y += offY * (charHeight + 4);

    strImage(g, s, x, y, color);
  }

  /**
   * Draw string into graphics object in given color.
   *
   * @param g graphics object to draw to.
   * @param s string to draw.
   * @param sx x coordinate in pixels
   * @param sy y coordinate in pixels
   * @param color Color
   */
  public static void strImage(
      final Graphics2D g, final String s, final int sx, final int sy, final Color color) {
    ArrayList<BufferedImage> font = img.get(color);
    for (int i = 0, x = sx; i < s.length(); i++, x += charWidth) {
      int pos = s.codePointAt(i) - 33;
      if (pos > -1 && pos < NUM_FONT_CHARS) {
        g.drawImage(font.get(pos), x, sy, null);
      }
    }
  }

  /**
   * Draw string into graphics object in given color. (Right aligned)
   *
   * @param g graphics object to draw to.
   * @param s string to draw.
   * @param rMargin rightmost x coordinate in pixels
   */
  public static void strImageRight(final Graphics2D g, final String s, final int rMargin) {
    int alignOffset = rMargin - (s.length() * charWidth);
    strImage(g, s, alignOffset, 0, Color.GREEN);
  }

  /**
   * Draw string into graphics object in given color. (Left aligned)
   *
   * @param g graphics object to draw to.
   * @param s string to draw.
   * @param lMargin leftmost x coordinate in pixels
   */
  public static void strImageLeft(final Graphics2D g, final String s, final int lMargin) {
    strImage(g, s, lMargin, 0, Color.GREEN);
  }

  /**
   * Draw string into graphics object in given color.
   *
   * @param g graphics object to draw to.
   * @param s string to draw.
   * @param color Color
   */
  public static void strImage(final Graphics2D g, final String s, final Color color) {
    strImage(g, s, 0, 0, color);
  }

  /**
   * Create image of string in given color.
   *
   * @param s string to draw
   * @param color Color
   * @return a buffered image of the needed size that contains an image of the given string
   */
  public static BufferedImage strImage(final String s, final Color color) {
    BufferedImage image =
        ToolBox.createImage(charWidth * s.length(), charHeight, Transparency.BITMASK);
    strImage(image.createGraphics(), s, color);
    return image;
  }

  /**
   * Create image of string in default color (green).
   *
   * @param s string to draw
   * @return a buffered image of the needed size that contains an image of the given string
   */
  public static BufferedImage strImage(final String s) {
    return strImage(s, Color.GREEN);
  }

  /**
   * Draw string into graphics object in default color (green).
   *
   * @param g graphics object to draw to.
   * @param s string to draw.
   */
  public static void strImage(final Graphics2D g, final String s) {
    strImage(g, s, Color.GREEN);
  }

  /**
   * Get width of one character in pixels.
   *
   * @return width of one character in pixels
   */
  public static int getWidth() {
    return charWidth;
  }

  /**
   * Get height of one character in pixels.
   *
   * @return height of one character in pixels
   */
  public static int getHeight() {
    return charHeight;
  }
}
