package Game;

import static Game.LemmFont.Color.*;

import Tools.ToolBox;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

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
 * Class to print text screens which can be navigated with the mouse. Uses {@link TextDialog}
 *
 * @author Volker Oth
 */
public class TextScreen {

  /** Mode (type of screen to present) */
  public static enum Mode {
    /** initial state */
    INIT,
    /** main introduction screen */
    INTRO,
    /** level briefing screen */
    BRIEFING,
    /** level debriefing screen */
    DEBRIEFING
  }

  /** Button: continue */
  public static final int BUTTON_CONTINUE = 0;

  /** Button: restart level */
  public static final int BUTTON_RESTART = 1;

  /** Button: replay level */
  public static final int BUTTON_REPLAY = 3;

  /** Button: save replay */
  public static final int BUTTON_SAVEREPLAY = 4;

  /** y position of scroll text - pixels relative to center */
  private static final int SCROLL_Y = 140;

  /** width of scroll text in characters */
  private static final int SCROLL_WIDTH = 39;

  /** height of scroll text in pixels */
  private static final int SCROLL_HEIGHT = LemmFont.getHeight() * 2;

  /** step width of scroll text in pixels */
  private static final int SCROLL_STEP = 2;

  /** scroll text */
  private static final String SCROLL_TEXT =
      "                                           Lemmini - a game engine for Lemmings (tm) in"
          + " Java. Thanks to Martin Cameron for his MicroMod Library, Jef Poskanzer for his"
          + " GifEncoder Library, Mindless for his MOD conversions of the original Amiga Lemmings"
          + " tunes, the guys of DMA Design for writing the original Lemmings, ccexplore and the"
          + " other nice folks at the Lemmingswelt Forum for discussion and advice and to Sun for"
          + " maintaining Java and providing the community with a free development environment.";

  /** TextDialog used as base component */
  private static TextDialog textDialog;

  /** factor used for the rotation animation */
  private static double rotFact = 1.0;

  /** delta used for the rotation animation */
  private static double rotDelta;

  /** source image for rotation animation */
  private static BufferedImage imgSrc;

  /** target image for rotation animation */
  private static BufferedImage imgTrg;

  /** graphics for rotation animation */
  private static Graphics2D imgGfx;

  /** flip state for rotation: true - image is flipped in Y direction */
  private static boolean flip;

  /** affine transformation used for rotation animation */
  private static AffineTransform at;

  /** counter used to trigger the rotation animation (in animation update frames) */
  private static int rotCtr;

  /** counter threshold used to trigger the rotation animation (in animation update frames) */
  private static final int maxRotCtr = 99;

  /** used to stop the rotation only after it was flipped twice -> original direction */
  private static int flipCtr;

  /** counter for scrolled characters */
  private static int scrollCharCtr;

  /** counter for scrolled pixels */
  private static int scrollPixCtr;

  /** image used for scroller */
  private static BufferedImage scrollerImg;

  /** graphics used for scroller */
  private static Graphics2D scrollerGfx;

  /** screen type to display */
  private static Mode mode;

  /** synchronization monitor */
  private static Object monitor = new Object();

  private static double oldScale = -1.0f;
  private static int oldWidth = -1;

  /**
   * Set mode.
   *
   * @param m mode.
   */
  public static void setMode(final Mode m, final double scale, final int width) {
    synchronized (monitor) {
      if (mode != m || oldScale != scale || oldWidth != width) {
        switch (m) {
          case INTRO:
            textDialog.init(width);
            textDialog.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_BROWN));
            textDialog.printCentered("A game engine for Lemmings(tm) in Java", -1, RED);
            textDialog.printCentered("Created by Volker Oth", 0, VIOLET);
            textDialog.printCentered("www.lemmini.de", 1, GREEN);
            textDialog.printCentered("This release by Michael J. Walsh", 3, BLUE);
            textDialog.printCentered("github.com/mjfwalsh/lemmini", 4, GREEN);
            textDialog.copyToBackBuffer();
            break;
          case BRIEFING:
            textDialog.init(width);
            initBriefing();
            break;
          case DEBRIEFING:
            textDialog.init(width);
            initDebriefing();
            break;
        }
        mode = m;
        oldScale = scale;
        oldWidth = width;
      }
    }
  }

  /** Initialize the briefing dialog. */
  static void initBriefing() {
    textDialog.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_GREEN));
    Level level = GameController.getLevel();
    textDialog.restore();

    textDialog.drawImage(GameController.getMapPreview(), -185);

    textDialog.print("Level Pack  " + GameController.getCurLevelPack().getName(), -19, -3);

    textDialog.print(
        "Difficulty  "
            + GameController.getCurLevelPack().getDiffLevels()[GameController.getCurDiffLevel()],
        -19,
        -2,
        VIOLET);

    textDialog.print("Level  " + (GameController.getCurLevelNumber() + 1), -14, -1, RED);

    // primitive text wrap for long level names
    String levelLabel = "Title  " + level.getLevelName();
    int lineRef = 0;
    int limit = (textDialog.getLineWidth() / 2) + 13;
    if (levelLabel.length() > limit) {
      int index = levelLabel.lastIndexOf(" ", limit);

      textDialog.print(levelLabel.substring(0, index), -14, 0, BROWN);
      textDialog.print(levelLabel.substring(index + 1), -7, 1, BROWN);
      lineRef++;
    } else {
      textDialog.print(levelLabel, -14, 0, BROWN);
    }

    textDialog.print("Lemmings  " + level.getNumLemmings(), -17, lineRef + 2, BLUE);
    textDialog.print(
        "Target  "
            + level.getNumToRescue()
            + " ("
            + (level.getNumToRescue() * 100 / level.getNumLemmings())
            + "%)",
        -15,
        lineRef + 3,
        GREEN);
    textDialog.print("Release Rate  " + level.getReleaseRate(), -21, lineRef + 4, BROWN);
    int minutes = level.getTimeLimitSeconds() / 60;
    int seconds = level.getTimeLimitSeconds() % 60;

    String time_text = "Time  " + minutes;
    time_text += minutes == 1 ? " minute" : " minutes";

    if (seconds > 0) {
      time_text += ", " + seconds;
      time_text += seconds == 1 ? " second" : " seconds";
    }

    textDialog.print(time_text, -13, lineRef + 5, TURQUOISE);

    textDialog.copyToBackBuffer(); // though not really needed
  }

  /** Initialize the debriefing dialog. */
  static void initDebriefing() {
    textDialog.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_GREEN));
    int toRescue =
        GameController.getNumToRecue()
            * 100
            / GameController.getNumLemmingsMax(); // % to rescue of total number
    int rescued =
        GameController.getNumLeft()
            * 100
            / GameController.getNumLemmingsMax(); // % rescued of total number
    int rescuedOfToRescue =
        GameController.getNumLeft()
            * 100
            / GameController.getNumToRecue(); // % rescued of no. to rescue
    textDialog.restore();
    if (GameController.getTime() == 0) textDialog.printCentered("Time is up.", -6, TURQUOISE);
    else textDialog.printCentered("All lemmings accounted for.", -6, TURQUOISE);
    textDialog.print("You needed:  " + Integer.toString(toRescue) + "%", -7, -4, VIOLET);
    textDialog.print("You rescued: " + Integer.toString(rescued) + "%", -7, -3, VIOLET);
    if (GameController.wasLost()) {
      if (rescued == 0) {
        textDialog.printCentered("ROCK BOTTOM! I hope for your sake", -1, RED);
        textDialog.printCentered("that you nuked that level", 0, RED);
      } else if (rescuedOfToRescue < 50) {
        textDialog.printCentered("Better rethink your strategy before", -1, RED);
        textDialog.printCentered("you try this level again!", 0, RED);
      } else if (rescuedOfToRescue < 95) {
        textDialog.printCentered("A little more practice on this level", -1, RED);
        textDialog.printCentered("is definitely recommended.", 0, RED);
      } else {
        textDialog.printCentered("You got pretty close that time.", -1, RED);
        textDialog.printCentered("Now try again for that few % extra.", 0, RED);
      }
      textDialog.addTextButton(9, 5, BUTTON_RESTART, "Retry", "Retry", BLUE, BROWN);
    } else {
      if (rescued == 100) {
        textDialog.printCentered("Superb! You rescued every lemming on", -1, RED);
        textDialog.printCentered("that level. Can you do it again....?", 0, RED);
      } else if (rescued > toRescue) {
        textDialog.printCentered("You totally stormed that level!", -1, RED);
        textDialog.printCentered("Let's see if you can storm the next...", 0, RED);
      } else if (rescued == toRescue) {
        textDialog.printCentered("SPOT ON. You can't get much closer", -1, RED);
        textDialog.printCentered("than that. Let's try the next....", 0, RED);
      } else {
        textDialog.printCentered("That level seemed no problem to you on", -1, RED);
        textDialog.printCentered("that attempt. Onto the next....       ", 0, RED);
      }
      LevelPack lp = GameController.getCurLevelPack();
      int ln = GameController.getCurLevelNumber();
      if (lp.getLevels(GameController.getCurDiffLevel()).length > ln + 1) {
        int absLevel =
            GameController.absLevelNum(
                GameController.getCurLevelPackIdx(), GameController.getCurDiffLevel(), ln + 1);
        String code = LevelCode.create(lp.getCodeSeed(), absLevel, rescued, 0, lp.getCodeOffset());

        textDialog.printCentered("Your access code for level " + (ln + 2), 2, BROWN);
        textDialog.printCentered("is " + code, 3, BROWN);
        textDialog.addTextButton(9, 5, BUTTON_CONTINUE, "Next", "Next", BLUE, BROWN);
      } else {
        textDialog.printCentered("Congratulations!", 2, BROWN);
        textDialog.printCentered(
            "You finished all the "
                + lp.getDiffLevels()[GameController.getCurDiffLevel()]
                + " levels!",
            3,
            GREEN);
      }
    }
    textDialog.copyToBackBuffer(); // though not really needed
    textDialog.addTextButton(-12, 5, BUTTON_REPLAY, "Replay", "Replay", BLUE, BROWN);
    textDialog.addTextButton(-4, 5, BUTTON_SAVEREPLAY, "Save Replay", "Save Replay", BLUE, BROWN);
  }

  /**
   * Get text dialog.
   *
   * @return text dialog.
   */
  public static TextDialog getDialog() {
    synchronized (monitor) {
      return textDialog;
    }
  }

  /**
   * Initialize text screen.
   *
   * @param width width in pixels
   * @param height height in pixels
   */
  public static void init(final int width, final int height) {
    synchronized (monitor) {
      rotFact = 1.0;
      rotDelta = -0.1;
      imgSrc = MiscGfx.getImage(MiscGfx.Index.LEMMINI);
      at = new AffineTransform();
      flip = false;
      rotCtr = 0;
      flipCtr = 0;
      imgTrg = ToolBox.createImage(imgSrc.getWidth(), imgSrc.getHeight(), Transparency.TRANSLUCENT);
      imgGfx = imgTrg.createGraphics();
      imgGfx.setBackground(new Color(0, 0, 0, 0)); // invisible
      scrollCharCtr = 0;
      scrollPixCtr = 0;

      scrollerImg =
          ToolBox.createImage(
              LemmFont.getWidth() * (1 + SCROLL_WIDTH), SCROLL_HEIGHT, Transparency.BITMASK);
      scrollerGfx = scrollerImg.createGraphics();
      scrollerGfx.setBackground(new Color(0, 0, 0, 0));
      textDialog = new TextDialog(width, height);
    }
  }

  /** Update the text screen (for animations) */
  public static void update() {
    synchronized (monitor) {
      textDialog.restore();
      switch (mode) {
        case INTRO:
          updateIntro();
          break;
        case BRIEFING:
          break;
        case DEBRIEFING:
          textDialog.drawButtons();
          break;
      }
    }
  }

  /** Update the into screen. */
  private static void updateIntro() {
    // manage logo rotation
    if (++rotCtr > maxRotCtr) {
      // animate
      rotFact += rotDelta;
      if (rotFact <= 0.0) {
        // minimum size reached -> flip and increase again
        rotFact = 0.1;
        rotDelta = -rotDelta;
        flip = !flip;
      } else if (rotFact > 1.0) {
        // maximum size reached -> decrease again
        rotFact = 1.0;
        rotDelta = -rotDelta;
        // reset only after two rounds (flipped back)
        if (++flipCtr > 1) rotCtr = 0;
      }
      if (flip) {
        at.setToScale(1, -rotFact);
        at.translate(1, -imgSrc.getHeight());
      } else at.setToScale(1, rotFact);
      AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
      imgGfx.clearRect(0, 0, imgTrg.getWidth(), imgTrg.getHeight());
      op.filter(imgSrc, imgTrg);
      textDialog.drawImage(imgTrg, -120 - (int) (imgSrc.getHeight() / 2 * Math.abs(rotFact) + 0.5));
    } else {
      // display original image
      flipCtr = 0;
      textDialog.drawImage(imgSrc, -120 - imgSrc.getHeight() / 2);
    }
    // manage scroller
    boolean wrapAround = false;
    int endIdx = scrollCharCtr + SCROLL_WIDTH + 1;
    if (endIdx > SCROLL_TEXT.length()) {
      endIdx = SCROLL_TEXT.length();
      wrapAround = true;
    }

    String out = SCROLL_TEXT.substring(scrollCharCtr, endIdx);
    if (wrapAround)
      out += SCROLL_TEXT.substring(0, scrollCharCtr + SCROLL_WIDTH + 1 - SCROLL_TEXT.length());
    scrollerGfx.clearRect(0, 0, scrollerImg.getWidth(), scrollerImg.getHeight());
    LemmFont.strImage(scrollerGfx, out, BLUE);
    int w = SCROLL_WIDTH * LemmFont.getWidth();

    textDialog.drawScroller(w, SCROLL_HEIGHT, SCROLL_Y, scrollerImg, scrollPixCtr);

    scrollPixCtr += SCROLL_STEP;
    if (scrollPixCtr >= LemmFont.getWidth()) {
      scrollCharCtr++;
      scrollPixCtr = 0;
      if (scrollCharCtr >= SCROLL_TEXT.length()) scrollCharCtr = 0;
    }
  }

  /**
   * Get image of text screen
   *
   * @return image of text screen
   */
  public static BufferedImage getScreen() {
    synchronized (monitor) {
      return textDialog.getScreen();
    }
  }
}
