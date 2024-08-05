package Game;

import Tools.ToolBox;
import java.awt.Cursor;
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

  /** array of AWT cursor Objects */
  private Cursor cursor[];

  /** is Mouse cursor hidden? */
  private boolean enabled;

  /**
   * Initialization.
   *
   * @throws ResourceException
   */
  public LemmCursor() throws ResourceException {
    img = ToolBox.getAnimation(Core.loadImage("misc/cursor.gif"), 8, Transparency.BITMASK);
    cursor = new Cursor[5];
    int w = getImage(Type.NORMAL).getWidth() / 2;
    int h = getImage(Type.NORMAL).getHeight() / 2;
    cursor[Type.HIDDEN.ordinal()] =
        Toolkit.getDefaultToolkit()
            .createCustomCursor(
                new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "");
    cursor[Type.NORMAL.ordinal()] =
        Toolkit.getDefaultToolkit().createCustomCursor(getImage(Type.NORMAL), new Point(w, h), "");
    cursor[Type.LEFT.ordinal()] =
        Toolkit.getDefaultToolkit().createCustomCursor(getImage(Type.LEFT), new Point(w, h), "");
    cursor[Type.RIGHT.ordinal()] =
        Toolkit.getDefaultToolkit().createCustomCursor(getImage(Type.RIGHT), new Point(w, h), "");
    cursor[Type.WALKER.ordinal()] =
        Toolkit.getDefaultToolkit().createCustomCursor(getImage(Type.WALKER), new Point(w, h), "");
    type = Type.NORMAL;
    enabled = true;
  }

  /**
   * Set enable state for Mouse cursor.
   *
   * @param en true to show, false to hide
   */
  public void setEnabled(boolean en) {
    enabled = en;
  }

  /**
   * Get enable state.
   *
   * @return true if shows, false if hidden
   */
  public boolean getEnabled() {
    return enabled;
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
   * Get current cursor as AWT cursor object.
   *
   * @return current cursor as AWT cursor object
   */
  public Cursor getCursor() {
    if (enabled) return cursor[type.ordinal()];
    else return cursor[Type.HIDDEN.ordinal()];
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
