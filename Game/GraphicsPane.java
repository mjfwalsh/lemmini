package Game;

import GameUtil.Fader;
import Tools.MicrosecondTimer;
import Tools.ToolBox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
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
  // step size in pixels for horizontal scrolling
  public static final int X_STEP = 4;
  // step size in pixels for fast horizontal scrolling
  public static final int X_STEP_FAST = 8;
  // size of auto scrolling range in pixels (from the left and right border)
  static final int AUTOSCROLL_RANGE = 20;
  // minimum sleep duration in milliseconds - values too small may cause system clock shift under
  // WinXP etc.
  static final int MIN_SLEEP = 10;
  // threshold for sleep - don't sleep if time to wait is shorter than this as sleep might return
  // too late
  static final int THR_SLEEP = 16;
  // y coordinate of score display in pixels
  static final int scoreY = Level.HEIGHT;
  // y coordinate of counter displays in pixels
  static final int counterY = scoreY + 40;
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
  // flag: Shift key is pressed
  private boolean shiftPressed;
  // flag: left mouse button is currently pressed
  private boolean leftMousePressed;
  // flag: debug draw is active
  private boolean draw;
  // image for information string display
  private BufferedImage outStrImg;
  // graphics object for information string display
  private Graphics2D outStrGfx;
  // array of offscreen images (one is active, one is passive)
  private BufferedImage offImage[];
  // graphics objects for the two offscreen images
  private Graphics2D offGraphics[];
  // index of the active buffer in the image buffer
  private int activeBuffer;
  // monitoring object used for synchronized painting
  private ReentrantLock drawLock;

  /** Zoom scale */
  private double m_scale = 1.0;

  /** internal draw width */
  private int internalWidth = 800;

  /** height of icon bar in pixels */
  private static final int WIN_OFS = 100;

  /** draw height */
  private static final int DRAWHEIGHT = Level.HEIGHT + WIN_OFS;

  /** max draw width */
  private static final int MAXDRAWWIDTH = 1600;

  private static final int MINDRAWWIDTH = 640;

  /** Constructor. */
  public GraphicsPane() {
    super();

    drawLock = new ReentrantLock();
    this.requestFocus();
    this.setCursor(LemmCursor.getCursor());
    this.addMouseListener(this);
    this.addMouseMotionListener(this);

    offImage = new BufferedImage[2];
    offGraphics = new Graphics2D[2];
    offImage[0] = ToolBox.createImage(MAXDRAWWIDTH, DRAWHEIGHT, Transparency.OPAQUE);
    offImage[1] = ToolBox.createImage(MAXDRAWWIDTH, DRAWHEIGHT, Transparency.OPAQUE);
    offGraphics[0] = offImage[0].createGraphics();
    offGraphics[1] = offImage[1].createGraphics();

    outStrImg = ToolBox.createImage(MAXDRAWWIDTH, LemmFont.getHeight(), Transparency.BITMASK);
    outStrGfx = outStrImg.createGraphics();
    outStrGfx.setBackground(new Color(0, 0, 0));

    TextScreen.init(MAXDRAWWIDTH, DRAWHEIGHT);
    shiftPressed = false;
  }

  /**
   * Change the drawWidth and scale based on the window size
   *
   * @return internal draw width
   */
  public void adjustSize(Dimension newSize) {
    if (!drawLock.tryLock()) return;

    try {
      double newScale = (float) newSize.height / DRAWHEIGHT;
      if (newScale < 1.0f) newScale = 1.0f;
      m_scale = newScale;

      double new_draw_width = (float) newSize.width / m_scale;
      if (new_draw_width < MINDRAWWIDTH) new_draw_width = MINDRAWWIDTH;
      else if (new_draw_width > MAXDRAWWIDTH) new_draw_width = MAXDRAWWIDTH;
      internalWidth = (int) new_draw_width;

      double scaledWidth = m_scale * (double) internalWidth;
      double scaledHeight = m_scale * (double) DRAWHEIGHT;
      this.setSize((int) scaledWidth, (int) scaledHeight);
    } finally {
      drawLock.unlock();
    }
  }

  /**
   * Get internal Draw Width
   *
   * @return internal draw width
   */
  public int getDrawWidth() {
    drawLock.lock();
    try {
      return internalWidth;
    } finally {
      drawLock.unlock();
    }
  }

  /**
   * Set internal Draw Width
   *
   * @param internal draw width
   */
  public void setDrawWidth(int w) {
    drawLock.lock();
    try {
      internalWidth = w;
    } finally {
      drawLock.unlock();
    }
  }

  /**
   * Get internal Draw Height
   *
   * @return internal draw width
   */
  public int getDrawHeight() {
    drawLock.lock();
    try {
      return DRAWHEIGHT;
    } finally {
      drawLock.unlock();
    }
  }

  /**
   * Get Zoom scale
   *
   * @return zoom scale
   */
  public double getScale() {
    drawLock.lock();
    try {
      return m_scale;
    } finally {
      drawLock.unlock();
    }
  }

  /**
   * Set zoom scale
   *
   * @param s zoom scale
   */
  public void setScale(double s) {
    drawLock.lock();
    try {
      m_scale = s;
    } finally {
      drawLock.unlock();
    }
  }

  /**
   * Set cursor type.
   *
   * @param c Cursor
   */
  public void setCursor(final LemmCursor.Type c) {
    LemmCursor.setType(c);
    this.setCursor(LemmCursor.getCursor());
  }

  /**
   * Show/hide Mouse cursor.
   *
   * @param en true to show the Mouse cursor, false to hide it
   */
  public void enableCursor(boolean en) {
    LemmCursor.setEnabled(en);
    this.setCursor(LemmCursor.getCursor());
  }

  /* (non-Javadoc)
   * @see javax.swing.JComponent#paint(java.awt.Graphics)
   */
  @Override
  public void paint(final Graphics g) {
    drawLock.lock();
    try {
      if (offImage != null) {
        int w = internalWidth;
        int scaledWidth = (int) (w * m_scale);
        int h = DRAWHEIGHT;
        int scaledHeight = (int) (h * m_scale);
        g.drawImage(offImage[activeBuffer], 0, 0, scaledWidth, scaledHeight, 0, 0, w, h, null);
      }
    } finally {
      drawLock.unlock();
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.JComponent#update(java.awt.Graphics)
   */
  @Override
  public void update(final Graphics g) {
    paint(g);
  }

  /** Delete offImage to avoid redraw and force init. */
  public void shutdown() {
    drawLock.lock();
    try {
      offImage = null;
    } finally {
      drawLock.unlock();
    }
  }

  /** redraw the offscreen image, then flip buffers and force repaint. */
  private void redraw() {
    drawLock.lock();
    try {
      int drawBuffer = (activeBuffer == 0) ? 1 : 0;
      Graphics2D offGfx = offGraphics[drawBuffer];

      switch (GameController.getGameState()) {
        case INTRO:
          TextScreen.setMode(TextScreen.Mode.INTRO, m_scale, internalWidth);
          TextScreen.update();
          offGfx.drawImage(TextScreen.getScreen(), 0, 0, null);
          break;

        case BRIEFING:
          TextScreen.setMode(TextScreen.Mode.BRIEFING, m_scale, internalWidth);
          TextScreen.update();
          offGfx.drawImage(TextScreen.getScreen(), 0, 0, null);
          break;

        case DEBRIEFING:
          TextScreen.setMode(TextScreen.Mode.DEBRIEFING, m_scale, internalWidth);
          TextScreen.update();
          TextScreen.getDialog()
              .handleMouseMove((int) (xMouseScreen / m_scale), (int) (yMouseScreen / m_scale));
          offGfx.drawImage(TextScreen.getScreen(), 0, 0, null);
          break;

        case LEVEL:
        case LEVEL_END:
          drawLevelEnd(offGfx);
          break;
      }

      // fader
      GameController.fade(offGfx, internalWidth, DRAWHEIGHT);
      // and all onto screen
      activeBuffer = drawBuffer;

      repaint();
    } finally {
      drawLock.unlock();
    }
  }

  /** draw the level end */
  private void drawLevelEnd(Graphics2D offGfx) {
    BufferedImage bgImage = GameController.getBgImage();
    if (bgImage == null) return;

    GameController.update();
    // mouse movement
    if (yMouseScreen > 40
        && yMouseScreen < scoreY * m_scale) { // avoid scrolling if menu is selected
      int xOfsTemp;
      if (xMouseScreen > this.getWidth() - AUTOSCROLL_RANGE * m_scale) {
        xOfsTemp = GameController.getxPos() + ((shiftPressed) ? X_STEP_FAST : X_STEP);
        if (xOfsTemp < Level.WIDTH - this.getWidth() / m_scale) GameController.setxPos(xOfsTemp);
        else GameController.setxPos((int) (Level.WIDTH - this.getWidth() / m_scale));
      } else if (xMouseScreen < AUTOSCROLL_RANGE * m_scale) {
        xOfsTemp = GameController.getxPos() - ((shiftPressed) ? X_STEP_FAST : X_STEP);
        if (xOfsTemp > 0) GameController.setxPos(xOfsTemp);
        else GameController.setxPos(0);
      }
    }
    // store local copy of xOfs to avoid sync problems with AWT threads
    // (scrolling by dragging changes xOfs as well)
    int xOfsTemp = GameController.getxPos();

    Level level = GameController.getLevel();
    if (level != null) {

      // clear screen
      offGfx.setBackground(level.getBgColor());
      offGfx.clearRect(0, 0, MAXDRAWWIDTH, level.HEIGHT);

      // draw "behind" objects
      GameController.getLevel().drawBehindObjects(offGfx, internalWidth, xOfsTemp);

      // draw background
      offGfx.drawImage(
          bgImage,
          0,
          0,
          internalWidth,
          level.HEIGHT,
          xOfsTemp,
          0,
          xOfsTemp + internalWidth,
          level.HEIGHT,
          this);

      // draw "in front" objects
      GameController.getLevel().drawInFrontObjects(offGfx, internalWidth, xOfsTemp);
    }

    // clear parts of the screen for menu etc.
    offGfx.setBackground(Color.BLACK);
    offGfx.clearRect(0, scoreY, MAXDRAWWIDTH, DRAWHEIGHT - scoreY);

    // draw icons, small level pic
    GameController.drawIcons(offGfx, 0, iconsY);
    int smallX = getSmallX();
    offGfx.drawImage(MiscGfx.getImage(MiscGfx.Index.BORDER), smallX - 4, smallY - 4, null);
    MiniMap.draw(offGfx, smallX, smallY, xOfsTemp, internalWidth);

    // draw counters
    GameController.drawCounters(offGfx, counterY);

    // draw lemmings
    GameController.getLemmsUnderCursor().clear();
    List<Lemming> lemmings = GameController.getLemmings();
    synchronized (GameController.getLemmings()) {
      Iterator<Lemming> it = lemmings.iterator();
      while (it.hasNext()) {
        Lemming l = it.next();
        int lx = l.screenX();
        int ly = l.screenY();
        int mx = l.midX() - 16;
        if (lx + l.width() > xOfsTemp && lx < xOfsTemp + internalWidth) {
          offGfx.drawImage(l.getImage(), lx - xOfsTemp, ly, null);
          if (LemmCursor.doesCollide(l, xOfsTemp)) {
            GameController.getLemmsUnderCursor().add(l);
          }
          BufferedImage cd = l.getCountdown();
          if (cd != null) offGfx.drawImage(cd, mx - xOfsTemp, ly - cd.getHeight(), null);

          BufferedImage sel = l.getSelectImg();
          if (sel != null) offGfx.drawImage(sel, mx - xOfsTemp, ly - sel.getHeight(), null);
        }
      }
      // draw pixels in mini map
      it = lemmings.iterator();
      while (it.hasNext()) {
        Lemming l = it.next();
        int lx = l.screenX();
        int ly = l.screenY();
        // draw pixel in mini map
        MiniMap.drawLemming(offGfx, lx, ly, getSmallX());
      }
    }
    Lemming lemmUnderCursor = GameController.lemmUnderCursor(LemmCursor.getType());

    // draw explosions
    GameController.drawExplosions(offGfx, internalWidth, Level.HEIGHT, xOfsTemp);

    // draw info string
    outStrGfx.clearRect(0, 0, MAXDRAWWIDTH, DRAWHEIGHT);
    if (GameController.isCheat()) {
      Stencil stencil = GameController.getStencil();
      if (stencil != null) {
        int pos = xMouse + yMouse * Level.WIDTH;
        pos = (int) (pos / m_scale);
        int stencilVal = stencil.get(pos);
        String test =
            "x: "
                + xMouse
                + ", y: "
                + yMouse
                + ", mask: "
                + (stencilVal & 0xffff)
                + " "
                + Stencil.getObjectID(stencilVal);
        LemmFont.strImage(outStrGfx, test);
        offGfx.drawImage(outStrImg, 4, Level.HEIGHT + 8, null);
      }
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append("OUT ");
      String s = Integer.toString(GameController.getLemmings().size());
      sb.append(s);
      if (s.length() == 1) sb.append(" ");
      sb.append("  IN ");
      s = Integer.toString(GameController.getNumLeft() * 100 / GameController.getNumLemmingsMax());
      if (s.length() == 1) sb.append(" ");
      sb.append(s);
      sb.append("%  TIME ").append(GameController.getTimeString());
      LemmFont.strImageRight(outStrGfx, sb.toString(), internalWidth - 4);

      if (lemmUnderCursor != null) {
        String n = lemmUnderCursor.getName();
        // display also the total number of lemmings under the cursor
        int num = GameController.getLemmsUnderCursor().size();
        if (num > 1) n = n + " " + Integer.toString(num);
        LemmFont.strImageLeft(outStrGfx, n, 4);
      }

      offGfx.drawImage(outStrImg, 0, Level.HEIGHT + 8, null);
    }
    // replay icon
    BufferedImage replayImage = GameController.getReplayImage();
    if (replayImage != null)
      offGfx.drawImage(
          replayImage, this.getWidth() - 2 * replayImage.getWidth(), replayImage.getHeight(), null);
    // draw cursor
    if (lemmUnderCursor != null) {
      int lx, ly;
      lx = (int) (xMouseScreen / m_scale);
      ly = (int) (yMouseScreen / m_scale);
      enableCursor(false);
      BufferedImage cursorImg = LemmCursor.getBoxImage();
      lx -= cursorImg.getWidth() / 2;
      ly -= cursorImg.getHeight() / 2;
      offGfx.drawImage(cursorImg, lx, ly, null);
    } else if (LemmCursor.getEnabled() == false) enableCursor(true);
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
          try {
            // determine time until next frame
            long diff = GameController.MICROSEC_PER_FRAME - timerRepaint.delta();
            if (diff > GameController.MICROSEC_RESYNC) {
              timerRepaint.update(); // resync to time base
              System.out.println("Resynced, diff was " + (diff / 1000) + " millis");
            } else if (diff > THR_SLEEP * 1000) Thread.sleep(MIN_SLEEP);
          } catch (InterruptedException ex) {
          }
        }
      }
    } catch (Exception ex) {
      ToolBox.showException(ex);
      System.exit(1);
    } catch (Error ex) {
      ToolBox.showException(ex);
      System.exit(1);
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseReleased(final MouseEvent mouseevent) {
    int x = (int) (mouseevent.getX() / m_scale);
    int y = (int) (mouseevent.getY() / m_scale);
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
  public void mouseClicked(final MouseEvent mouseevent) {}

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(final MouseEvent mouseevent) {
    int x = (int) (mouseevent.getX() / m_scale);
    int y = (int) (mouseevent.getY() / m_scale);
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
            String replayPath =
                ToolBox.getFileName(
                    Core.getCmp(), Core.resourcePath, Core.REPLAY_EXTENSIONS, false);
            if (replayPath != null) {
              try {
                String ext = ToolBox.getExtension(replayPath);
                if (ext == null) replayPath += ".rpl";
                if (GameController.saveReplay(replayPath)) return;
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
            Lemming l = GameController.lemmUnderCursor(LemmCursor.getType());
            if (l != null) GameController.requestSkill(l);
          }
          // check minimap mouse move
          int ofs = MiniMap.move(x, y, (int) (this.getWidth() / m_scale), getSmallX());
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
  private void debugDraw(final int x, final int y, final boolean doDraw) {
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
  public void mouseEntered(final MouseEvent mouseevent) {
    mouseDx = 0;
    mouseDy = 0;
    int x = (int) (mouseevent.getX() / m_scale);
    int y = (int) (mouseevent.getY() / m_scale);
    LemmCursor.setX(x);
    LemmCursor.setY(y);
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseExited(final MouseEvent mouseevent) {
    int x = xMouseScreen + mouseDx;
    switch (GameController.getGameState()) {
      case BRIEFING:
      case DEBRIEFING:
      case LEVEL:
        if (x >= this.getWidth()) x = this.getWidth() - 1;
        if (x < 0) x = 0;
        xMouseScreen = x;
        x += GameController.getxPos() * m_scale;
        if (x >= Level.WIDTH) x = Level.WIDTH - 1;
        xMouse = x;
        LemmCursor.setX((int) (xMouseScreen / m_scale));

        int y = yMouseScreen + mouseDy;
        if (y >= this.getHeight()) y = this.getHeight() - 1;
        if (y < 0) y = 0;
        yMouseScreen = y;

        y = yMouse + mouseDy;
        if (y >= Level.HEIGHT) y = Level.HEIGHT - 1;
        if (y < 0) y = 0;
        yMouse = y;
        LemmCursor.setY((int) (yMouseScreen / m_scale));
        mouseevent.consume();
        break;
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseDragged(final MouseEvent mouseevent) {
    mouseDx = 0;
    mouseDy = 0;
    // check minimap mouse move
    switch (GameController.getGameState()) {
      case LEVEL:
        int x = (int) (mouseevent.getX() / m_scale);
        int y = (int) (mouseevent.getY() / m_scale);
        if (leftMousePressed) {
          int ofs = MiniMap.move(x, y, (int) (this.getWidth() / m_scale), getSmallX());
          if (ofs != -1) GameController.setxPos(ofs);
        } else {
          int xOfsTemp = GameController.getxPos() + (x - mouseDragStartX);
          if (xOfsTemp < 0) xOfsTemp = 0;
          else if (xOfsTemp >= Level.WIDTH - this.getWidth() / m_scale)
            GameController.setxPos((int) (Level.WIDTH - this.getWidth() / m_scale));
          else GameController.setxPos(xOfsTemp);
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
  public void mouseMoved(final MouseEvent mouseevent) {
    int oldX = xMouse;
    int oldY = yMouse;

    int x = (int) ((mouseevent.getX() / m_scale + GameController.getxPos()));
    int y = (int) (mouseevent.getY() / m_scale);
    if (x >= Level.WIDTH) x = Level.WIDTH - 1;
    if (y >= Level.HEIGHT) y = Level.HEIGHT - 1;
    xMouse = (int) (x * m_scale);
    yMouse = (int) (y * m_scale);

    // LemmCursor
    xMouseScreen = mouseevent.getX();
    if (xMouseScreen >= this.getWidth()) xMouseScreen = this.getWidth();
    else if (xMouseScreen < 0) xMouseScreen = 0;
    yMouseScreen = mouseevent.getY();
    if (yMouseScreen >= this.getHeight()) yMouseScreen = this.getHeight();
    else if (yMouseScreen < 0) yMouseScreen = 0;
    LemmCursor.setX((int) (xMouseScreen / m_scale));
    LemmCursor.setY((int) (yMouseScreen / m_scale));

    if (!System.getProperty("os.name").equals("Mac OS X")) {
      if (Core.isFullScreen()) {
        getParent().getComponent(0).setVisible(y < 5);
      }
    }

    switch (GameController.getGameState()) {
      case INTRO:
      case BRIEFING:
      case DEBRIEFING:
        TextScreen.getDialog()
            .handleMouseMove((int) (xMouseScreen / m_scale), (int) (yMouseScreen / m_scale));
        // $FALL-THROUGH$
      case LEVEL:
        mouseDx = (xMouse - oldX);
        mouseDy = (yMouse - oldY);
        mouseDragStartX = (int) (mouseevent.getX() / m_scale);
        mouseevent.consume();
        break;
    }
  }

  /**
   * Get cursor x position in pixels.
   *
   * @return cursor x position in pixels
   */
  public int getCursorX() {
    return xMouse;
  }

  /**
   * Get cursor y position in pixels.
   *
   * @return cursor y position in pixels
   */
  public int getCursorY() {
    return yMouse;
  }

  /**
   * Get flag: Shift key is pressed?
   *
   * @return true if Shift key is pressed, false otherwise
   */
  public boolean isShiftPressed() {
    return shiftPressed;
  }

  /**
   * Set flag: Shift key is pressed.
   *
   * @param p true: Shift key is pressed, false otherwise
   */
  public void setShiftPressed(final boolean p) {
    shiftPressed = p;
  }

  /**
   * Toggle state of debug draw option.
   *
   * @return true: debug draw is active, false otherwise
   */
  public void toggleDebugDraw() {
    draw = !draw;
  }

  private int getSmallX() {
    return internalWidth - 208 - 4;
  }
}
