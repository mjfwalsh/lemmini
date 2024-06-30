package Game;

import Tools.ToolBox;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Transparency;
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
 * Implementation of the Lemmini selection cursor.
 *
 * @author Volker Oth
 */
public class LemmCursor {

  /** distance from center of cursor to be used to detect Lemmings under the cursor */
  private static final int HIT_DISTANCE = 12;

  /** cursor type */
  public enum Type {
    /** empty image to hide cursor */
    HIDDEN,
    /** normal cursor */
    NORMAL,
    /** select left cursor */
    LEFT,
    /** select right cursor */
    RIGHT,
    /** select walkers cursor */
    WALKER,
    /** normal cursor with selection box */
    BOX_NORMAL,
    /** select left cursor with selection box */
    BOX_LEFT,
    /** select right cursor with selection box */
    BOX_RIGHT,
    /** select walkers cursor with selection box */
    BOX_WALKER,
  }

  /** x position in pixels */
  private static int x;

  /** y position in pixels */
  private static int y;

  /** current cursor type */
  private static Type type;

  /** array of images - one for each cursor type */
  private static BufferedImage img[];

  /** array of AWT cursor Objects */
  private static Cursor cursor[];

  /** is Mouse cursor hidden? */
  private static boolean enabled;

  /**
   * Initialization.
   *
   * @throws ResourceException
   */
  public static void init() throws ResourceException {
    img = ToolBox.getAnimation(Core.loadImage("misc/cursor.gif"), 8, Transparency.BITMASK);
    cursor = new Cursor[5];
    int w = getImage(Type.NORMAL).getWidth() / 2;
    int h = getImage(Type.NORMAL).getHeight() / 2;
    cursor[Type.HIDDEN.ordinal()] =
        Toolkit.getDefaultToolkit()
            .createCustomCursor(
                new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "");
    cursor[Type.NORMAL.ordinal()] =
        Toolkit.getDefaultToolkit()
            .createCustomCursor(LemmCursor.getImage(Type.NORMAL), new Point(w, h), "");
    cursor[Type.LEFT.ordinal()] =
        Toolkit.getDefaultToolkit()
            .createCustomCursor(LemmCursor.getImage(Type.LEFT), new Point(w, h), "");
    cursor[Type.RIGHT.ordinal()] =
        Toolkit.getDefaultToolkit()
            .createCustomCursor(LemmCursor.getImage(Type.RIGHT), new Point(w, h), "");
    cursor[Type.WALKER.ordinal()] =
        Toolkit.getDefaultToolkit()
            .createCustomCursor(LemmCursor.getImage(Type.WALKER), new Point(w, h), "");
    type = Type.NORMAL;
    setX(0);
    setY(0);
    enabled = true;
  }

  /**
   * Set enable state for Mouse cursor.
   *
   * @param en true to show, false to hide
   */
  public static void setEnabled(boolean en) {
    enabled = en;
  }

  /**
   * Get enable state.
   *
   * @return true if shows, false if hidden
   */
  public static boolean getEnabled() {
    return enabled;
  }

  /**
   * Get image for a certain cursor type.
   *
   * @param t cursor type
   * @return image for the given cursor type
   */
  public static BufferedImage getImage(final Type t) {
    return img[t.ordinal() - 1];
  }

  /**
   * Get image for current cursor type.
   *
   * @return image for current cursor type
   */
  public static BufferedImage getImage() {
    return getImage(type);
  }

  /**
   * Get boxed version of image for the current cursor type.
   *
   * @return boxed version of image for the current cursor type
   */
  public static BufferedImage getBoxImage() {
    Type t;
    switch (type) {
      case NORMAL:
        t = Type.BOX_NORMAL;
        break;
      case LEFT:
        t = Type.BOX_LEFT;
        break;
      case RIGHT:
        t = Type.BOX_RIGHT;
        break;
      case WALKER:
        t = Type.BOX_WALKER;
        break;
      default:
        t = type; // should never happen
    }
    return getImage(t);
  }

  /**
   * Get current cursor as AWT cursor object.
   *
   * @return current cursor as AWT cursor object
   */
  public static Cursor getCursor() {
    if (enabled) return cursor[type.ordinal()];
    else return cursor[Type.HIDDEN.ordinal()];
  }

  /**
   * Get current cursor type.
   *
   * @return current cursor type
   */
  public static Type getType() {
    return type;
  }

  /**
   * Set current cursor type.
   *
   * @param t cursor type
   */
  public static void setType(final Type t) {
    type = t;
  }

  /**
   * Check if a Lemming is under the cursor.
   *
   * @param l Lemming to check
   * @param xOfs screen x offset
   * @return true if the Lemming is under the Cursor, else false.
   */
  public static boolean doesCollide(final Lemming l, final int xOfs) {
    // get center of lemming
    int lx = l.midX() - xOfs;
    int ly = l.midY();

    // calculate center of cursor
    int cx = getX();
    int cy = getY();

    // calculate distance
    int dx = Math.abs(lx - cx);
    int dy = Math.abs(ly - cy);

    if (dx <= HIT_DISTANCE && dy <= HIT_DISTANCE) return true;
    else return false;
  }

  /**
   * Set x position in pixels.
   *
   * @param x x position in pixels.
   */
  public static void setX(final int x) {
    LemmCursor.x = x;
  }

  /**
   * Get x position in pixels.
   *
   * @return x position in pixels
   */
  public static int getX() {
    return x;
  }

  /**
   * Set y position in pixels.
   *
   * @param y y position in pixels
   */
  public static void setY(final int y) {
    LemmCursor.y = y;
  }

  /**
   * Get y position in pixels.
   *
   * @return y position in pixels
   */
  public static int getY() {
    return y;
  }
}
