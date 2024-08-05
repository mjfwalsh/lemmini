package Game;

import Tools.ToolBox;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

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
 * Class to create text screens which can be navigated with the mouse. Uses {@link LemmFont} as
 * bitmap font.
 *
 * @author Volker Oth
 */
public class TextDialog {
  /** list of buttons */
  private ArrayList<Button> buttons;

  /** image used as screen buffer */
  private BufferedImage screenBuffer;

  /** graphics object to draw in screen buffer */
  private Graphics2D gScreen;

  /** image used as 2nd screen buffer for offscreen drawing */
  private BufferedImage backBuffer;

  /** graphics object to draw in 2nd (offscreen) screen buffer */
  private Graphics2D gBack;

  /** width of max draw width in pixels */
  private int wholeWidth;

  /** width of visible drawing area in pixels */
  private int clipWidth;

  /** height of screen in pixels */
  private int height;

  /** horizontal center of the screen in pixels */
  private int centerX;

  /** vertical center of the screen in pixels */
  private int centerY;

  /**
   * Create dialog text screen.
   *
   * @param w Width of screen to create
   * @param h Height of screen to create
   */
  public TextDialog(final int w, final int h) {
    clipWidth = wholeWidth = w;
    height = h;
    centerX = wholeWidth / 2;
    centerY = height / 2;
    screenBuffer = ToolBox.createImage(w, h, Transparency.OPAQUE);
    gScreen = screenBuffer.createGraphics();
    backBuffer = ToolBox.createImage(w, h, Transparency.OPAQUE);
    gBack = backBuffer.createGraphics();
    buttons = new ArrayList<Button>();
  }

  /** Initialize/reset the text screen. */
  public void init(final int w) {
    clipWidth = w;
    centerX = w / 2;
    buttons.clear();
    gScreen.setBackground(Color.BLACK);
    gScreen.clearRect(0, 0, wholeWidth, height);
  }

  /**
   * Get image containing current (on screen) screenbuffer.
   *
   * @return image containing current (on screen) screenbuffer
   */
  public BufferedImage getScreen() {
    return screenBuffer;
  }

  /**
   * Fill brackground with tiles.
   *
   * @param tile Image used as tile
   */
  public void fillBackground(final BufferedImage tile) {
    for (int x = 0; x < wholeWidth; x += tile.getWidth()) {
      for (int y = 0; y < wholeWidth; y += tile.getHeight()) gBack.drawImage(tile, x, y, null);
    }
    gScreen.drawImage(backBuffer, 0, 0, null);
  }

  /** Copy back buffer to front buffer. */
  public void copyToBackBuffer() {
    gBack.drawImage(screenBuffer, 0, 0, null);
  }

  /**
   * Set Image as background. The image will appear centered.
   *
   * @param image Image to use as background
   */
  public void setBackground(final BufferedImage image) {
    int x = (wholeWidth - image.getWidth()) / 2;
    int y = (height - image.getHeight()) / 2;
    gBack.setBackground(Color.BLACK);
    gBack.clearRect(0, 0, wholeWidth, height);
    gBack.drawImage(image, x, y, null);
    gScreen.drawImage(backBuffer, 0, 0, null);
  }

  /** Restore whole background from back buffer. */
  public void restore() {
    gScreen.drawImage(backBuffer, 0, 0, null);
  }

  /**
   * Draw string.
   *
   * @param s String
   * @param x0 X position relative to center expressed in character widths
   * @param y0 Y position relative to center expressed in character heights
   * @param col LemmFont color
   */
  public void print(final String s, final int x0, final int y0, final LemmFont.Color col) {
    LemmFont.strImage(gScreen, s, centerX, centerY, x0, y0, col);
  }

  /**
   * Find how many characters can fit in a line of print
   *
   * @return number of characters
   */
  public int getLineWidth() {
    return (int) Math.floor(clipWidth / LemmFont.getWidth());
  }

  /**
   * Draw string.
   *
   * @param s String
   * @param x X position relative to center expressed in character widths
   * @param y Y position relative to center expressed in character heights
   */
  public void print(final String s, final int x, final int y) {
    print(s, x, y, LemmFont.Color.GREEN);
  }

  /**
   * Draw string horizontally centered.
   *
   * @param s String
   * @param y0 Y position relative to center expressed in character heights
   * @param col LemmFont color
   */
  public void printCentered(final String s, final int y0, final LemmFont.Color col) {
    LemmFont.strImage(gScreen, s, centerX, centerY, -s.length() / 2, y0, col);
  }

  /**
   * Draw string horizontally centered.
   *
   * @param s String
   * @param y Y position relative to center expressed in character heights
   */
  public void printCentered(final String s, final int y) {
    printCentered(s, y, LemmFont.Color.GREEN);
  }

  /**
   * Draw Image.
   *
   * @param img Image
   * @param x X position relative to center
   * @param y Y position relative to center
   */
  public void drawImage(final BufferedImage img, final int x, final int y) {
    gScreen.drawImage(img, centerX + x, centerY + y, null);
  }

  /**
   * Draw Image horizontally centered.
   *
   * @param img Image
   * @param y Y position relative to center
   */
  public void drawImage(final BufferedImage img, final int y) {
    int x = centerX - img.getWidth() / 2;
    gScreen.drawImage(img, x, centerY + y, null);
  }

  /**
   * Add Button.
   *
   * @param x X position relative to center in pixels
   * @param y Y position relative to center in pixels
   * @param img Button image
   * @param imgSelected Button selected image
   * @param id Button ID
   */
  public void addButton(
      final int x,
      final int y,
      final BufferedImage img,
      final BufferedImage imgSelected,
      final int id) {
    Button b = new Button(centerX + x, centerY + y, id);
    b.SetImage(img);
    b.SetImageSelected(imgSelected);
    buttons.add(b);
  }

  /**
   * Add text button.
   *
   * @param x0 X position relative to center (in characters)
   * @param y0 Y position relative to center (in characters)
   * @param id Button ID
   * @param t Button text
   * @param ts Button selected text
   * @param col Button text color
   * @param cols Button selected text color
   */
  public void addTextButton(
      final int x0,
      final int y0,
      final int id,
      final String t,
      final String ts,
      final LemmFont.Color col,
      final LemmFont.Color cols) {
    int x = x0 * LemmFont.getWidth();
    int y = y0 * (LemmFont.getHeight() + 4);
    TextButton b = new TextButton(centerX + x, centerY + y, id);
    b.setText(t, col);
    b.setTextSelected(ts, cols);
    buttons.add(b);
  }

  /**
   * React on left click.
   *
   * @param x Absolute x position in pixels
   * @param y Absolute y position in pixels
   * @return Button ID if button clicked, else -1
   */
  public int handleLeftClick(final int x, final int y) {
    for (Button b : buttons) {
      if (b.inside(x, y)) return b.id;
    }
    return -1;
  }

  /**
   * React on mouse hover.
   *
   * @param x Absolute x position
   * @param y Absolute y position
   */
  public void handleMouseMove(final int x, final int y) {
    for (Button b : buttons) {
      b.selected = b.inside(x, y);
    }
  }

  /** Draw buttons on screen. */
  public void drawButtons() {
    for (Button b : buttons) b.draw(gScreen);
  }

  /**
   * React on right click.
   *
   * @param x Absolute x position
   * @param y Absolute y position
   * @return Button ID if button clicked, else -1
   */
  public int handleRightClick(final int x, final int y) {
    for (Button b : buttons) {
      if (b.inside(x, y)) return b.id;
    }
    return -1;
  }

  /** Draw Scroller */
  public void drawScroller(
      int charWidth, int h, int scrollY, BufferedImage scrollerImg, int scrollPixCtr) {
    int w = charWidth * LemmFont.getWidth();
    int dx = (clipWidth - w) / 2;
    int dy = (height / 2) + scrollY;
    screenBuffer
        .createGraphics()
        .drawImage(
            scrollerImg, dx, dy, dx + w, dy + h, scrollPixCtr, 0, scrollPixCtr + w, h / 2, null);
  }
}

/**
 * Button class for TextDialog.
 *
 * @author Volker Oth
 */
class Button {
  /** x coordinate in pixels */
  private int x;

  /** y coordinate in pixels */
  private int y;

  /** width in pixels */
  protected int width;

  /** height in pixels */
  protected int height;

  /** button identifier */
  protected int id;

  /** true if button is selected */
  protected boolean selected;

  /** normal button image */
  protected BufferedImage image;

  /** selected button image */
  protected BufferedImage imgSelected;

  /**
   * Constructor
   *
   * @param xi x position in pixels
   * @param yi y position in pixels
   * @param idi identifier
   */
  Button(final int xi, final int yi, final int idi) {
    x = xi;
    y = yi;
    id = idi;
  }

  /**
   * Set normal button image.
   *
   * @param img image
   */
  void SetImage(final BufferedImage img) {
    image = img;
    if (image.getHeight() != height) height = image.getHeight();
    if (image.getWidth() != width) width = image.getWidth();
  }

  /**
   * Set selected button image.
   *
   * @param img image
   */
  void SetImageSelected(final BufferedImage img) {
    imgSelected = img;
    if (imgSelected.getHeight() != height) height = imgSelected.getHeight();
    if (imgSelected.getWidth() != width) width = imgSelected.getWidth();
  }

  /**
   * Draw the button.
   *
   * @param g graphics object to draw on
   */
  void draw(final Graphics2D g) {
    g.drawImage(selected ? imgSelected : image, x, y, null);
  }

  /**
   * Check if a (mouse) position is inside this button.
   *
   * @param xi
   * @param yi
   * @return true if the coordinates are inside this button, false if not
   */
  boolean inside(final int xi, final int yi) {
    return (xi >= x && xi < x + width && yi >= y && yi < y + height);
  }
}

/**
 * Button class for TextDialog.
 *
 * @author Volker Oth
 */
class TextButton extends Button {
  /**
   * Constructor
   *
   * @param xi x position in pixels
   * @param yi y position in pixels
   * @param idi identifier
   */
  TextButton(final int xi, final int yi, final int idi) {
    super(xi, yi, idi);
  }

  /**
   * Set text which is used as button.
   *
   * @param s String which contains the button text
   * @param color Color of the button (LemmFont color!)
   */
  void setText(final String s, final LemmFont.Color color) {
    image = LemmFont.strImage(s, color);
    if (image.getHeight() != height) height = image.getHeight();
    if (image.getWidth() != width) width = image.getWidth();
  }

  /**
   * Set text for selected button.
   *
   * @param s String which contains the selected button text
   * @param color Color of the button (LemmFont color!)
   */
  void setTextSelected(final String s, final LemmFont.Color color) {
    imgSelected = LemmFont.strImage(s, color);
    if (imgSelected.getHeight() != height) height = imgSelected.getHeight();
    if (imgSelected.getWidth() != width) width = imgSelected.getWidth();
  }
}
