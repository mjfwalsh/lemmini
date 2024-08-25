package Game;

import Tools.ToolBox;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
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
 * Implementation of the Lemmini selection cursor.
 *
 * @author Volker Oth
 */
public class LemmCursor {
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

  /** current cursor type */
  private Type type;

  /** array of images - one for each cursor type */
  private ArrayList<BufferedImage> img;

  /** a blank cursor to make it hide it */
  private final Cursor invisibleCursor;

  /** normal crosshair, appropriately scaled */
  private Cursor defaultCursor;

  /**
   * Initialization.
   *
   * @throws ResourceException
   */
  public LemmCursor() throws ResourceException {
    img = ToolBox.getAnimation(Core.loadImage("misc/cursor.gif"), 8, Transparency.BITMASK);
    invisibleCursor =
        Toolkit.getDefaultToolkit()
            .createCustomCursor(
                new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "");
    scaleCursor(1.0);
    type = Type.NORMAL;
  }

  /**
   * Get image for a certain cursor type.
   *
   * @param t cursor type
   * @return image for the given cursor type
   */
  public BufferedImage getImage(final Type t) {
    return img.get(t.ordinal() - 1);
  }

  /**
   * Get image for current cursor type.
   *
   * @return image for current cursor type
   */
  public BufferedImage getImage() {
    return getImage(type);
  }

  /**
   * Get boxed version of image for the current cursor type.
   *
   * @return boxed version of image for the current cursor type
   */
  public BufferedImage getBoxImage() {
    Type t =
        switch (type) {
          case NORMAL -> Type.BOX_NORMAL;
          case LEFT -> Type.BOX_LEFT;
          case RIGHT -> Type.BOX_RIGHT;
          case WALKER -> Type.BOX_WALKER;
          default -> type; // should never happen
        };
    return getImage(t);
  }

  /**
   * Get the default current cursor as AWT cursor object.
   *
   * @return default cursor
   */
  public Cursor getCursor(Type c) {
    if (c == Type.NORMAL) return defaultCursor;
    else return invisibleCursor;
  }

  /**
   * Scale the default cursor
   *
   * @param scale scaling factor
   */
  public void scaleCursor(double scale) {
    Image img = getImage(Type.NORMAL);
    if (scale == 1.0) {
      int w = getImage(Type.NORMAL).getWidth();
      int h = getImage(Type.NORMAL).getHeight();
      defaultCursor =
          Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(w / 2, h / 2), "");
    } else {
      int w = (int) ((double) getImage(Type.NORMAL).getWidth() * scale);
      int h = (int) ((double) getImage(Type.NORMAL).getHeight() * scale);
      Image scaledImg = img.getScaledInstance(w, h, 0);
      defaultCursor =
          Toolkit.getDefaultToolkit().createCustomCursor(scaledImg, new Point(w / 2, h / 2), "");
    }
  }

  /**
   * Get current cursor type.
   *
   * @return current cursor type
   */
  public Type getType() {
    return type;
  }

  /**
   * Set current cursor type.
   *
   * @param t cursor type
   */
  public void setType(final Type t) {
    type = t;
  }
}
