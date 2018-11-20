package Game;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import Tools.ToolBox;
import static Game.LemmFont.Color.*;

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
 * Class to print text screens which can be navigated with the mouse.
 * Uses {@link TextDialog}
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
	public final static int BUTTON_CONTINUE = 0;
	/** Button: restart level */
	public final static int BUTTON_RESTART = 1;
	/** Button: back to menu */
	public final static int BUTTON_MENU = 2;
	/** Button: replay level */
	public final static int BUTTON_REPLAY = 3;
	/** Button: save replay */
	public final static int BUTTON_SAVEREPLAY = 4;

	/** y position of scroll text - pixels relative to center */
	private final static int SCROLL_Y = 140;
	/** width of scroll text in characters */
	private final static int SCROLL_WIDTH = 39;
	/** height of scroll text in pixels */
	private final static int SCROLL_HEIGHT = LemmFont.getHeight()*2;
	/** step width of scroll text in pixels */
	private final static int SCROLL_STEP = 2;
	/** scroll text */
	private final static String SCROLL_TEXT =
		"                                           "+
		"Lemmini - a game engine for Lemmings (tm) in Java. "+
		"Thanks to Martin Cameron for his MicroMod Library, "+
		"Jef Poskanzer for his GifEncoder Library, "+
		"Mindless for his MOD conversions of the original Amiga Lemmings tunes, "+
		"the guys of DMA Design for writing the original Lemmings, "+
		"ccexplore and the other nice folks at the Lemmingswelt Forum for discussion and advice "+
		"and to Sun for maintaining Java and providing the community with a free development environment.";

	/** TextDialog used as base component */
	private static TextDialog textScreen;
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

	private static double oldScale = Core.getScale();

	/**
	 * Set mode.
	 * @param m mode.
	 */
	public static void setMode(final Mode m) {
		synchronized (monitor) {
			double scale = Core.getScale();
			if (mode != m || oldScale != scale) {
				switch (m) {
					case INTRO:
						textScreen.init();
						textScreen.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_BROWN));
						textScreen.printCentered("A game engine for Lemmings(tm) in Java", 0, RED);
						textScreen.printCentered("Release 0.87 08/2017", 1, BLUE);
						textScreen.printCentered("Coded by Volker Oth 2005-2017", 2, VIOLET);
						textScreen.printCentered("www.lemmini.de", 3, GREEN);
						textScreen.copyToBackBuffer();
						//textScreen.addTextButton(-4, 3, 1, "  Start ", "Let's go", BLUE, RED);
						break;
					case BRIEFING:
						initBriefing();
						break;
					case DEBRIEFING:
						initDebriefing();
						break;
				}
			}
			mode = m;
			oldScale = scale;
		}
	}

	/**
	 * Initialize the briefing dialog.
	 */
	static void initBriefing() {
		textScreen.init();
		textScreen.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_GREEN));
		Level level = GameController.getLevel();
		//LevelInfo li;
		textScreen.restore();
		//li = GameController.levelPack[GameController.curLevelPack].getInfo(GameController.curDiffLevel, GameController.curLevelNumber);
		String rating = GameController.getCurLevelPack().getDiffLevels()[GameController.getCurDiffLevel()];
		textScreen.drawImage(GameController.getMapPreview(), -170);

		String levelLabel = "Level " + (GameController.getCurLevelNumber()+1) + " ";

		textScreen.print(levelLabel + level.getLevelName(), (-9 - levelLabel.length()), -2, RED);
		textScreen.print("Number of Lemmings "+level.getNumLemmings(), -9, 0, BLUE);
		textScreen.print(""+(level.getNumToRescue()*100/level.getNumLemmings())+"% to be saved", -9, 1, GREEN);
		textScreen.print("Release Rate "+level.getReleaseRate(), -9, 2, BROWN);
		int minutes = level.getTimeLimitSeconds() / 60;
		int seconds = level.getTimeLimitSeconds() % 60;
		if (seconds == 0)
			textScreen.print("Time         "+minutes+" Minutes", -9, 3, TURQUOISE);
		else
			textScreen.print("Time         "+minutes+"-"+seconds+" Minutes", -9, 3, TURQUOISE);
		textScreen.print("Rating       "+rating, -9, 4, VIOLET);
		textScreen.copyToBackBuffer(); // though not really needed
	}

	/**
	 * Initialize the debriefing dialog.
	 */
	static void initDebriefing() {
		textScreen.init();
		textScreen.fillBackground(MiscGfx.getImage(MiscGfx.Index.TILE_GREEN));
		int toRescue = GameController.getNumToRecue()*100/GameController.getNumLemmingsMax(); // % to rescue of total number
		int rescued =  GameController.getNumLeft()*100/GameController.getNumLemmingsMax();    // % rescued of total number
		int rescuedOfToRescue = GameController.getNumLeft()*100/GameController.getNumToRecue(); // % rescued of no. to rescue
		textScreen.restore();
		if (GameController.getTime()==0)
			textScreen.printCentered("Time is up.", -6, TURQUOISE);
		else
			textScreen.printCentered("All lemmings accounted for.", -6, TURQUOISE);
		textScreen.print("You needed:  "+Integer.toString(toRescue)+"%", -7, -4, VIOLET);
		textScreen.print("You rescued: "+Integer.toString(rescued)+"%", -7, -3, VIOLET);
		if (GameController.wasLost()) {
			if (rescued == 0) {
				textScreen.printCentered("ROCK BOTTOM! I hope for your sake", -1, RED);
				textScreen.printCentered("that you nuked that level", 0, RED);
			} else if (rescuedOfToRescue < 50){
				textScreen.printCentered("Better rethink your strategy before", -1, RED);
				textScreen.printCentered("you try this level again!", 0, RED);
			}  else if (rescuedOfToRescue < 95){
				textScreen.printCentered("A little more practice on this level", -1, RED);
				textScreen.printCentered("is definitely recommended.", 0, RED);
			} else {
				textScreen.printCentered("You got pretty close that time.", -1, RED);
				textScreen.printCentered("Now try again for that few % extra.", 0, RED);
			}
			textScreen.addTextButton(-2, 6, BUTTON_RESTART, "Retry", "Retry", BLUE, BROWN);
		} else {
			if (rescued == 100) {
				textScreen.printCentered("Superb! You rescued every lemming on", -1, RED);
				textScreen.printCentered("that level. Can you do it again....?", 0, RED);
			} else if (rescued > toRescue) {
				textScreen.printCentered("You totally stormed that level!", -1, RED);
				textScreen.printCentered("Let's see if you can storm the next...", 0, RED);
			} else if (rescued == toRescue) {
				textScreen.printCentered("SPOT ON. You can't get much closer", -1, RED);
				textScreen.printCentered("than that. Let's try the next....", 0, RED);
			} else {
				textScreen.printCentered("That level seemed no problem to you on", -1, RED);
				textScreen.printCentered("that attempt. Onto the next....       ", 0, RED);
			}
			LevelPack lp = GameController.getCurLevelPack();
			int ln = GameController.getCurLevelNumber();
			if (lp.getLevels(GameController.getCurDiffLevel()).length > ln+1) {
				int absLevel = GameController.absLevelNum(GameController.getCurLevelPackIdx(), GameController.getCurDiffLevel(), ln+1);
				String code = LevelCode.create(lp.getCodeSeed(), absLevel, rescued, 0,lp.getCodeOffset());

				textScreen.printCentered("Your access code for level "+(ln+2), 2, BROWN);
				textScreen.printCentered("is "+code, 3, BROWN);
				textScreen.addTextButton(-1, 6, BUTTON_CONTINUE, "Next", "Next", BLUE, BROWN);
			} else {
				textScreen.printCentered("Congratulations!", 2, BROWN);
				textScreen.printCentered("You finished all the "+lp.getDiffLevels()[GameController.getCurDiffLevel()]+" levels!", 3, GREEN);
			}
		}
		textScreen.copyToBackBuffer(); // though not really needed
		textScreen.addTextButton(-12, 5, BUTTON_REPLAY, "Replay", "Replay", BLUE, BROWN);
		if (GameController.getCurLevelPackIdx() != 0) // not for single levels started via "load level"
			textScreen.addTextButton( -4, 5, BUTTON_SAVEREPLAY, "Save Replay", "Save Replay", BLUE, BROWN);
		textScreen.addTextButton( 9, 5, BUTTON_MENU, "Menu", "Menu", BLUE, BROWN);
	}

	/**
	 * Get text dialog.
	 * @return text dialog.
	 */
	public static TextDialog getDialog() {
		synchronized (monitor) {
			return textScreen;
		}
	}

	/**
	 * Initialize text screen.
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
			rotCtr = 0 ;
			flipCtr = 0;
			imgTrg = ToolBox.createImage(imgSrc.getWidth(),imgSrc.getHeight(), Transparency.TRANSLUCENT);
			imgGfx = imgTrg.createGraphics();
			imgGfx.setBackground(new Color(0,0,0,0)); // invisible
			scrollCharCtr = 0;
			scrollPixCtr = 0;

			scrollerImg = ToolBox.createImage(LemmFont.getWidth()*(1+SCROLL_WIDTH),SCROLL_HEIGHT, Transparency.BITMASK);
			scrollerGfx = scrollerImg.createGraphics();
			scrollerGfx.setBackground(new Color(0,0,0,0));

			textScreen  = new TextDialog(width, height);
		}
	}

	/**
	 * Update the text screen (for animations)
	 */
	public static void update() {
		synchronized (monitor) {
			textScreen.restore();
			switch (mode) {
				case INTRO:
					update_intro();
					break;
				case BRIEFING:
					update_briefing();
					break;
				case DEBRIEFING:
					update_debriefing();
					break;
			}
		}
	}

	/**
	 * Update the into screen.
	 */
	private static void update_intro() {
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
				if (++flipCtr > 1)
					rotCtr = 0;
			}
			if (flip) {
				at.setToScale(1, -rotFact);
				at.translate(1,-imgSrc.getHeight());
			} else at.setToScale(1, rotFact);
			AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			imgGfx.clearRect(0, 0, imgTrg.getWidth(), imgTrg.getHeight());
			op.filter(imgSrc, imgTrg);
			textScreen.drawImage(imgTrg, -120 - (int)(imgSrc.getHeight()/2*Math.abs(rotFact)+0.5));
		} else {
			// display original image
			flipCtr=0;
			textScreen.drawImage(imgSrc, -120 - imgSrc.getHeight()/2);
		}
		// manage scroller
		String out;
		boolean wrapAround = false;
		int endIdx = scrollCharCtr+SCROLL_WIDTH+1;
		if (endIdx > SCROLL_TEXT.length()) {
			endIdx = SCROLL_TEXT.length();
			wrapAround = true;
		}
		out = SCROLL_TEXT.substring(scrollCharCtr, endIdx);
		if (wrapAround)
			out += SCROLL_TEXT.substring(0,scrollCharCtr+SCROLL_WIDTH+1-SCROLL_TEXT.length());
		scrollerGfx.clearRect(0, 0, scrollerImg.getWidth(), scrollerImg.getHeight());
		LemmFont.strImage(scrollerGfx, out, BLUE);
		int w = SCROLL_WIDTH*LemmFont.getWidth();
		int dx = (textScreen.getScreen().getWidth()-w)/2;
		int dy = (textScreen.getScreen().getHeight()/2)+SCROLL_Y;
		textScreen.getScreen().createGraphics().drawImage(
				scrollerImg, dx, dy, dx+w, dy+SCROLL_HEIGHT, scrollPixCtr,0,scrollPixCtr+w,SCROLL_HEIGHT/2, null
		);

		scrollPixCtr+=SCROLL_STEP;
		if (scrollPixCtr >= LemmFont.getWidth()) {
			scrollCharCtr++;
			scrollPixCtr = 0;
			if (scrollCharCtr >= SCROLL_TEXT.length())
				scrollCharCtr = 0;
		}
	}

	/**
	 * Update the briefing screen.
	 */
	private static void update_briefing() {
	}

	/**
	 * Update the debriefing screen.
	 */
	private static void update_debriefing() {
		textScreen.drawButtons();
	}

	/**
	 * Get image of text screen
	 * @return image of text screen
	 */
	public static BufferedImage getScreen() {
		synchronized (monitor) {
			return textScreen.getScreen();
		}
	}
}
