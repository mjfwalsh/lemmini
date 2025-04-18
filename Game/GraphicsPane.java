package Game;

import GameUtil.Fader;
import Tools.MicrosecondTimer;
import Tools.ToolBox;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
 * A graphics panel in which the actual game contents is displayed.
 *
 * @author Volker Oth
 */
public class GraphicsPane extends JPanel implements Runnable, MouseListener, MouseMotionListener {
  // size of auto scrolling range in pixels (from the left and right border)
  static final int AUTOSCROLL_RANGE = 20;

  // minimum sleep duration in milliseconds - values too small may cause system clock shift under
  // WinXP etc.
  static final int MIN_SLEEP = 10;

  // threshold for sleep - don't sleep if time to wait is shorter than this as sleep might return
  // too late
  static final int THR_SLEEP = 16;

  // y coordinate of counter displays in pixels
  static final int counterY = Level.HEIGHT + 40;

  // y coordinate of icons in pixels
  static final int iconsY = counterY + 14;

  // y coordinate of minimap in pixels
  static final int smallY = iconsY;

  private static final long serialVersionUID = 0x01;

  // start position of mouse drag (for mouse scrolling)
  private int mouseDragStartX;

  // x position of cursor in level
  private int xMouse;

  // x position of cursor on screen
  private int xMouseScreen;

  // y position of cursor in level
  private int yMouse;

  // y position of cursor on screen
  private int yMouseScreen;

  // mouse drag length in x direction (pixels)
  private int mouseDx;

  // mouse drag length in y direction (pixels)
  private int mouseDy;

  // cursor object
  private LemmCursor lemmCursor;

  // flag: left mouse button is currently pressed
  private boolean leftMousePressed;

  // flag: debug draw is active
  private boolean draw;

  // array of offscreen images (one is active, one is passive)
  private BufferedImage offImage[];

  // graphics objects for the two offscreen images
  private Graphics2D offGraphics[];

  // index of the active buffer in the image buffer
  private int activeBuffer;

  /** Zoom scale */
  private double scale = 1.0;

  /** internal draw width */
  private int internalWidth = 800;

  /** screen changed */
  private boolean forceRedraw = true;

  /** height of icon bar in pixels */
  private static final int WIN_OFS = 100;

  /** draw height */
  public static final int DRAWHEIGHT = Level.HEIGHT + WIN_OFS;

  /** max draw width */
  public static final int MAXDRAWWIDTH = 1600;

  /** min draw width */
  private static final int MINDRAWWIDTH = 640;

  /** fullscreen boolean */
  private boolean fullScreen = false;

  /** Constructor. */
  public GraphicsPane(Dimension screen) throws ResourceException {
    requestFocus();
    lemmCursor = new LemmCursor();
    addMouseListener(this);
    addMouseMotionListener(this);

    offImage = new BufferedImage[2];
    offGraphics = new Graphics2D[2];
    offImage[0] = ToolBox.createImage(MAXDRAWWIDTH, DRAWHEIGHT, Transparency.OPAQUE);
    offImage[1] = ToolBox.createImage(MAXDRAWWIDTH, DRAWHEIGHT, Transparency.OPAQUE);
    offGraphics[0] = offImage[0].createGraphics();
    offGraphics[1] = offImage[1].createGraphics();

    TextScreen.init(MAXDRAWWIDTH, DRAWHEIGHT);

    setBackground(Color.BLACK);
    setDoubleBuffered(false);

    // adjust game so height is about a quarter the size of the screen
    scale = (float) screen.height / (2 * (float) DRAWHEIGHT);

    // set graphics pane dimensions
    int width = (int) ((float) internalWidth * scale);
    int height = (int) ((float) DRAWHEIGHT * scale);
    setPreferredSize(new Dimension(width, height));
  }

  /**
   * Change the width and scale based on the window size
   *
   * @return internal draw width
   */
  public synchronized void adjustSize(Dimension newSize) {
    forceRedraw = true;

    double newScale = (float) newSize.height / DRAWHEIGHT;
    if (newScale < 1.0f) newScale = 1.0f;
    scale = newScale;
    lemmCursor.scaleCursor(scale);

    double new_draw_width = (float) newSize.width / scale;
    if (new_draw_width < MINDRAWWIDTH) new_draw_width = MINDRAWWIDTH;
    else if (new_draw_width > MAXDRAWWIDTH) new_draw_width = MAXDRAWWIDTH;
    internalWidth = (int) new_draw_width;

    double scaledWidth = scale * (double) internalWidth;
    double scaledHeight = scale * (double) DRAWHEIGHT;
    setSize((int) scaledWidth, (int) scaledHeight);
  }

  /**
   * Set cursor type.
   *
   * @param c Cursor
   */
  public synchronized void setCursor(final LemmCursor.Type c) {
    lemmCursor.setType(c);
  }

  /**
   * Set cursor type.
   *
   * @param c Cursor
   */
  public synchronized void setCursorIf(final LemmCursor.Type cond) {
    if (lemmCursor.getType() == cond) lemmCursor.setType(LemmCursor.Type.NORMAL);
  }

  /* (non-Javadoc)
   * @see javax.swing.JComponent#paint(java.awt.Graphics)
   */
  @Override
  public synchronized void paint(final Graphics g) {
    if (offImage != null) {
      int scaledWidth = (int) (internalWidth * scale);
      int scaledHeight = (int) (DRAWHEIGHT * scale);
      g.drawImage(
          offImage[activeBuffer],
          0,
          0,
          scaledWidth,
          scaledHeight,
          0,
          0,
          internalWidth,
          DRAWHEIGHT,
          null);
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.JComponent#update(java.awt.Graphics)
   */
  @Override
  public synchronized void update(final Graphics g) {
    paint(g);
  }

  private void showCursor(final LemmCursor.Type c) {
    Cursor nc;
    synchronized (this) {
      nc = lemmCursor.getCursor(c);
    }

    if (getCursor() != nc) setCursor(nc);
  }

  /** redraw the offscreen image, then flip buffers and force repaint. */
  private void redraw() {
    LemmCursor.Type cursor = LemmCursor.Type.NORMAL;
    synchronized (this) {
      int drawBuffer = (activeBuffer == 0) ? 1 : 0;
      Graphics2D offGfx = offGraphics[drawBuffer];

      switch (GameController.getGameState()) {
        case INTRO:
          TextScreen.drawIntro(internalWidth, forceRedraw);
          forceRedraw = false;
          offGfx.drawImage(TextScreen.getScreen(), 0, 0, null);
          break;

        case START_BRIEFING:
          forceRedraw = true;
          GameController.setGameState(GameController.State.BRIEFING);
          // fall through

        case BRIEFING:
          TextScreen.drawBriefing(internalWidth, forceRedraw);
          forceRedraw = false;
          offGfx.drawImage(TextScreen.getScreen(), 0, 0, null);
          break;

        case DEBRIEFING:
          TextScreen.drawDebriefing(internalWidth, forceRedraw);
          forceRedraw = false;

          TextScreen.getDialog().handleMouseMove(xMouseScreen, yMouseScreen);
          offGfx.drawImage(TextScreen.getScreen(), 0, 0, null);
          break;

        case LEVEL:
        case LEVEL_END:
          GameController.drawLevel(
              offGfx,
              internalWidth,
              xMouseScreen,
              yMouseScreen,
              xMouse,
              yMouse,
              lemmCursor.getType());

          // draw cursor
          if (xMouseScreen > 0
              && xMouseScreen < internalWidth
              && yMouseScreen > 0
              && yMouseScreen < DRAWHEIGHT) {
            Lemming lemmUnderCursor = GameController.lemmUnderCursor(lemmCursor.getType());
            BufferedImage cursorImg =
                lemmUnderCursor != null ? lemmCursor.getBoxImage() : lemmCursor.getImage();

            int lx = xMouseScreen - cursorImg.getWidth() / 2;
            int ly = yMouseScreen - cursorImg.getHeight() / 2;
            offGfx.drawImage(cursorImg, lx, ly, null);
            cursor = LemmCursor.Type.HIDDEN;
          }
          break;
      }

      // fader
      GameController.fade(offGfx, internalWidth, DRAWHEIGHT);
      // and all onto screen
      activeBuffer = drawBuffer;

      repaint();
    }
    showCursor(cursor);
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
    MicrosecondTimer timerRepaint = new MicrosecondTimer();
    try {
      while (true) {
        GameController.State gameState = GameController.getGameState();
        // Try to keep the Amiga timing. Note that no frames are skipped
        // If one frame is too late, the next one will be a little earlier
        // to compensate. No frames are skipped though!
        // On a slow CPU this might slow down gameplay...
        if (timerRepaint.timePassedAdd(GameController.MICROSEC_PER_FRAME)) {
          // time passed -> redraw necessary
          redraw();
          // special handling for fast forward or super lemming mode only during real gameplay
          if (gameState == GameController.State.LEVEL) {
            // in fast forward or super lemming modes, update the game mechanics
            // multiple times per (drawn) frame
            if (GameController.isFastForward())
              for (int f = 0; f < GameController.FAST_FWD_MULTI - 1; f++) GameController.update();
            else if (GameController.isSuperLemming())
              for (int f = 0; f < GameController.SUPERLEMM_MULTI - 1; f++) GameController.update();
          }
        } else {
          // determine time until next frame
          long diff = GameController.MICROSEC_PER_FRAME - timerRepaint.delta();
          if (diff > GameController.MICROSEC_RESYNC) {
            timerRepaint.update(); // resync to time base
            System.out.println("Resynced, diff was " + (diff / 1000) + " millis");
          } else if (diff > THR_SLEEP * 1000) Thread.sleep(MIN_SLEEP);
        }
      }
    } catch (Exception | Error ex) {
      ToolBox.showException(ex);
      System.exit(1);
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
   */
  @Override
  public synchronized void mouseReleased(final MouseEvent mouseevent) {
    int x = (int) (mouseevent.getX() / scale);
    int y = (int) (mouseevent.getY() / scale);
    mouseDx = 0;
    mouseDy = 0;
    if (mouseevent.getButton() == MouseEvent.BUTTON1) leftMousePressed = false;

    switch (GameController.getGameState()) {
      case LEVEL:
        if (y > iconsY && y < iconsY + Icons.HEIGHT) {
          Icons.Type type = GameController.getIconType(x);
          if (type != Icons.Type.INVALID) GameController.releaseIcon(type);
        }
        // always release icons which don't stay pressed
        // this is to avoid the icons get stuck when they're pressed,
        // the the mouse is dragged out and released outside
        GameController.releasePlus(GameController.KEYREPEAT_ICON);
        GameController.releaseMinus(GameController.KEYREPEAT_ICON);
        GameController.releaseIcon(Icons.Type.MINUS);
        GameController.releaseIcon(Icons.Type.PLUS);
        GameController.releaseIcon(Icons.Type.NUKE);
        mouseevent.consume();
        break;
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  @Override
  public synchronized void mouseClicked(final MouseEvent mouseevent) {}

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public synchronized void mousePressed(final MouseEvent mouseevent) {
    int x = (int) (mouseevent.getX() / scale);
    int y = (int) (mouseevent.getY() / scale);
    mouseDx = 0;
    mouseDy = 0;
    if (mouseevent.getButton() == MouseEvent.BUTTON1) leftMousePressed = true;

    if (Fader.getState() != Fader.State.OFF) return;

    switch (GameController.getGameState()) {
      case INTRO:
        GameController.playCurDifLevel();
        break;
      case BRIEFING:
        MiniMap.init(smallY, 16, 8, true);
        GameController.setTransition(GameController.TransitionState.TO_LEVEL);
        Fader.setState(Fader.State.OUT);
        mouseevent.consume();
        break;
      case DEBRIEFING:
        int button = TextScreen.getDialog().handleLeftClick(x, y);
        switch (button) {
          case TextScreen.BUTTON_CONTINUE:
            GameController.nextLevel(); // continue to next level
            break;
          case TextScreen.BUTTON_RESTART:
            GameController.requestRestartLevel(false);
            break;
          case TextScreen.BUTTON_REPLAY:
            GameController.requestRestartLevel(true);
            break;
          case TextScreen.BUTTON_SAVEREPLAY:
            File replayFile = Core.promptForReplayFile(false);

            if (replayFile != null) {
              try {
                String ext = ToolBox.getExtension(replayFile);
                if (ext == null) replayFile = new File(replayFile.getAbsolutePath() + ".rpl");

                if (GameController.saveReplay(replayFile)) return;
                // else: no success
                JOptionPane.showMessageDialog(
                    Core.getCmp(),
                    "Error!",
                    "Saving replay failed",
                    JOptionPane.INFORMATION_MESSAGE);
              } catch (Exception ex) {
                ToolBox.showException(ex);
              }
            }
            break;
        }
        mouseevent.consume();
        break;
      case LEVEL:
        // debug drawing
        debugDraw(x, y, leftMousePressed);
        if (leftMousePressed) {
          if (y > iconsY && y < iconsY + Icons.HEIGHT) {
            Icons.Type type = GameController.getIconType(x);
            if (type != Icons.Type.INVALID) {
              GameController.handleIconButton(type);
            }
          } else {
            Lemming l = GameController.lemmUnderCursor(lemmCursor.getType());
            if (l != null) GameController.requestSkill(l);
          }
          // check minimap mouse move
          int ofs = MiniMap.move(x, y, internalWidth, getSmallX());
          if (ofs != -1) GameController.setxPos(ofs);
          mouseevent.consume();
        }
    }
  }

  /**
   * Debug routine to draw terrain pixels in stencil and background image.
   *
   * @param x x position in pixels
   * @param y y position in pixels
   * @param doDraw true: draw, false: erase
   */
  private synchronized void debugDraw(final int x, final int y, final boolean doDraw) {
    if (draw && GameController.isCheat()) {
      int rgbVal = (doDraw) ? 0xffffffff : 0x0;
      int maskVal = (doDraw) ? Stencil.MSK_BRICK : Stencil.MSK_EMPTY;
      int xOfs = GameController.getxPos();
      if (x + xOfs > 0 && x + xOfs < Level.WIDTH - 1 && y > 0 && y < Level.HEIGHT - 1) {
        GameController.getBgImage().setRGB(x + xOfs, y, rgbVal);
        GameController.getStencil().set(x + xOfs, y, maskVal);
        GameController.getBgImage().setRGB(x + xOfs + 1, y, rgbVal);
        GameController.getStencil().set(x + xOfs + 1, y, maskVal);
        GameController.getBgImage().setRGB(x + xOfs, y + 1, rgbVal);
        GameController.getStencil().set(x + xOfs, y + 1, maskVal);
        GameController.getBgImage().setRGB(x + xOfs + 1, y + 1, rgbVal);
        GameController.getStencil().set(x + xOfs + 1, y + 1, maskVal);
      }
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
   */
  @Override
  public synchronized void mouseEntered(final MouseEvent mouseevent) {
    mouseDx = 0;
    mouseDy = 0;
    xMouseScreen = (int) (mouseevent.getX() / scale);
    yMouseScreen = (int) (mouseevent.getY() / scale);
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
   */
  @Override
  public synchronized void mouseExited(final MouseEvent mouseevent) {
    switch (GameController.getGameState()) {
      case BRIEFING:
      case DEBRIEFING:
      case LEVEL:
        xMouseScreen += (int) (mouseDx / scale);
        xMouse = GameController.getxPos() + clamp(xMouseScreen, internalWidth);

        yMouseScreen += (int) (mouseDy / scale);
        yMouse = clamp(yMouseScreen, Level.HEIGHT);

        // hide the cursor but preserve scroll
        yMouseScreen = -1;

        if (xMouseScreen < AUTOSCROLL_RANGE) xMouseScreen = -1;
        else if (xMouseScreen > internalWidth - AUTOSCROLL_RANGE) xMouseScreen = internalWidth + 1;

        mouseevent.consume();
        break;
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
   */
  @Override
  public synchronized void mouseDragged(final MouseEvent mouseevent) {
    mouseDx = 0;
    mouseDy = 0;
    // check minimap mouse move
    switch (GameController.getGameState()) {
      case LEVEL:
        int x = (int) (mouseevent.getX() / scale);
        int y = (int) (mouseevent.getY() / scale);

        if (leftMousePressed) {
          int ofs = MiniMap.move(x, y, internalWidth, getSmallX());
          if (ofs != -1) GameController.setxPos(ofs);
        } else {
          int xOfsTemp = GameController.getxPos() + (x - mouseDragStartX);

          if (xOfsTemp < 0) xOfsTemp = 0;
          else if (xOfsTemp >= Level.WIDTH - internalWidth) xOfsTemp = Level.WIDTH - internalWidth;

          GameController.setxPos(xOfsTemp);
        }
        // debug drawing
        debugDraw(x, y, leftMousePressed);
        mouseMoved(mouseevent);
        mouseevent.consume();
        break;
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
   */
  @Override
  public synchronized void mouseMoved(final MouseEvent mouseevent) {
    int oldX = xMouse;
    int oldY = yMouse;

    // LemmCursor
    xMouseScreen = (int) (mouseevent.getX() / scale);
    xMouse = GameController.getxPos() + clamp(xMouseScreen, internalWidth);

    yMouseScreen = (int) (mouseevent.getY() / scale);
    yMouse = clamp(yMouseScreen, Level.HEIGHT);

    if (fullScreen) {
      getParent().getComponent(0).setVisible(yMouseScreen < 5);
    }

    switch (GameController.getGameState()) {
      case INTRO:
      case BRIEFING:
      case DEBRIEFING:
        TextScreen.getDialog().handleMouseMove(xMouseScreen, yMouseScreen);
        // $FALL-THROUGH$
      case LEVEL:
        mouseDx = (xMouse - oldX);
        mouseDy = (yMouse - oldY);
        mouseDragStartX = (int) (mouseevent.getX() / scale);
        mouseevent.consume();
        break;
    }
  }

  /**
   * Create new lemming at cursor position.
   *
   * @return lemming
   */
  public synchronized Lemming createLemmingAtCursorPosition() {
    return new Lemming(xMouse, yMouse);
  }

  /**
   * Toggle state of debug draw option.
   *
   * @return true: debug draw is active, false otherwise
   */
  public synchronized void toggleDebugDraw() {
    draw = !draw;
  }

  /**
   * Is fullscreen
   *
   * @return boolean fullScreen
   */
  public synchronized boolean isFullScreen() {
    return fullScreen;
  }

  /**
   * Record fullscreen
   *
   * @param b boolean fullScreen
   */
  public synchronized void setFullScreen(boolean b) {
    fullScreen = b;
  }

  private synchronized int getSmallX() {
    return internalWidth - 208 - 4;
  }

  private static int clamp(int value, int max) {
    if (value < 0) return 0;
    if (value > max) return max;
    return value;
  }
}
