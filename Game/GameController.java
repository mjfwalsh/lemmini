package Game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import GameUtil.Fader;
import GameUtil.KeyRepeat;
import GameUtil.Sound;
import GameUtil.Sprite;
import Tools.MicrosecondTimer;
import Tools.ToolBox;

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
 * Game controller. Contains all the game logic.
 * @author Volker Oth
 */
public class GameController {
	/** game state */
	public static enum State {
		/** init state */
		INIT,
		/** display intro screen */
		INTRO,
		/** display level briefing screen */
		BRIEFING,
		/** display level */
		LEVEL,
		/** display debriefing screen */
		DEBRIEFING,
		/** fade out after level was finished */
		LEVEL_END
	}

	/** Transition states */
	public static enum TransitionState {
		/** no fading */
		NONE,
		/** restart level: fade out, fade in briefing */
		RESTART_LEVEL,
		/** replay level: fade out, fade in briefing */
		REPLAY_LEVEL,
		/** load level: fade out, fade in briefing */
		LOAD_LEVEL,
		/** load replay: fade out, fade in briefing */
		LOAD_REPLAY,
		/** level finished: fade out */
		END_LEVEL,
		/** go to intro: fade in intro */
		TO_INTRO,
		/** go to briefing: fade in briefing */
		TO_BRIEFING,
		/** go to debriefing: fade in debriefing */
		TO_DEBRIEFING,
		/** go to level: fade in level */
		TO_LEVEL
	}

	/** key repeat bitmask for icons */
	public final static int KEYREPEAT_ICON = 1;
	/** key repeat bitmask for keys */
	public final static int KEYREPEAT_KEY = 2;

	/** bang sound */
	public final static int SND_BANG = 0;
	/** brick wheel trap sound */
	public final static int SND_CHAIN = 1;
	/** setting new skill sound */
	public final static int SND_CHANGE_OP = 2;
	/** only some builder steps left sound */
	public final static int SND_CHINK = 3;
	/** dieing sound */
	public final static int SND_DIE = 4;
	/** trap door opening sound */
	public final static int SND_DOOR = 5;
	/** electric sound */
	public final static int SND_ELECTRIC = 6;
	/** explode sound */
	public final static int SND_EXPLODE = 7;
	/** fire sound */
	public final static int SND_FIRE = 8;
	/** drowning sound */
	public final static int SND_GLUG = 9;
	/** start of level sound */
	public final static int SND_LETSGO = 10;
	/** bear/twiner trap sound */
	public final static int SND_MANTRAP = 11;
	/** mouse clicked sound */
	public final static int SND_MOUSEPRE = 12;
	/** nuke command sound */
	public final static int SND_OHNO = 13;
	/** leaving exit sound */
	public final static int SND_OING = 14;
	/** scrape sound */
	public final static int SND_SCRAPE = 15;
	/** slicer sound */
	public final static int SND_SLICER = 16;
	/** splash sound */
	public final static int SND_SPLASH = 17;
	/** faller splat sound */
	public final static int SND_SPLAT = 18;
	/** ten tons sound, also pipe sucking lemmings in */
	public final static int SND_TEN_TONS = 19;
	/** icycle, brick stamper sound */
	public final static int SND_THUD = 20;
	/** thunk sound */
	public final static int SND_THUNK = 21;
	/** ting sound */
	public final static int SND_TING = 22;
	/** yipee sound */
	public final static int SND_YIPEE = 23;

	/** updates 5 frames instead of 1 in fast forward mode */
	public final static int FAST_FWD_MULTI = 5;
	/** updates 3 frames instead of 1 in Superlemming mode */
	public final static int SUPERLEMM_MULTI = 3;
	/** time per frame in microseconds - this is the timing everything else is based on */
	public final static int MICROSEC_PER_FRAME = 30*1000;
	/** resync if time difference greater than that (in microseconds)*/
	public final static int MICROSEC_RESYNC = 5*30*1000;

	/** redraw animated level obejcts every 3rd frame (about 100ms) */
	private final static int MAX_ANIM_CTR = 100*1000/MICROSEC_PER_FRAME;
	/** open Entry after about 1.5 seconds */
	private final static int MAX_ENTRY_OPEN_CTR = 1500*1000/MICROSEC_PER_FRAME;
	/** one second is 33.33 ticks (integer would cause error) */
	private final static double MAX_SECOND_CTR = 1000.0*1000/MICROSEC_PER_FRAME;
	/** maximum release rate */
	private final static int MAX_RELEASE_RATE = 99;

	/** nuke icon: maximum time between two mouse clicks for double click detection (in microseconds) */
	private final static long MICROSEC_NUKE_DOUBLE_CLICK = 240*1000;
	/** +/- icons: maximum time between two mouse clicks for double click detection (in microseconds) */
	private final static long MICROSEC_RELEASE_DOUBLE_CLICK = 200*1000;
	/** +/- icons: time for key repeat to kick in */
	private final static long MICROSEC_KEYREPEAT_START = 250*1000;
	/** +/- icons: time for key repeat rate */
	private final static long MICROSEC_KEYREPEAT_REPEAT = 67*1000;

	/** sound object */
	public static Sound sound;

	/** the background stencil */
	private static Stencil stencil;
	/** the background image */
	private static BufferedImage bgImage;
	/** flag: play music */
	private static boolean musicOn;
	/** flag: play sounds */
	private static boolean soundOn;
	/** flag: use advanced mouse selection methods */
	private static boolean advancedSelect;
	/** graphics object for the background image */
	private static Graphics2D bgGfx;
	/** color used to erase the background (black) */
	private static Color blankColor = new Color(0xff,0,0,0);
	/** flag: fast forward mode is active */
	private static boolean fastForward;
	/** flag: Superlemming mode is active */
	private static boolean superLemming;
	/** game state */
	private static State gameState;
	/** transition (fading) state */
	private static TransitionState transitionState;
	/** skill to assign to lemming (skill icon) */
	private static Lemming.Type lemmSkill;
	/** flag: entry is openend */
	private static boolean entryOpened;
	/** flag: nuke was acticated */
	private static boolean nuke;
	/** flag: game is paused */
	private static boolean paused;
	/** flag: cheat/debug mode is activated */
	private static boolean cheat = false;
	/** flag: cheat mode was activated during play */
	private static boolean wasCheated = false;
	/** frame counter for handling opening of entries */
	private static int entryOpenCtr;
	/** frame counter for handling time */
	private static double secondCtr;
	/** frame counter used to handle release of new Lemmings */
	private static int releaseCtr;
	/** threshold to release a new Lemming */
	private static int releaseBase;
	/** frame counter used to update animated sprite objects */
	private static int animCtr;
	/** level object */
	private static Level level;
	/** index of current difficulty level */
	private static int curDiffLevel;
	/** index of current level pack */
	private static int curLevelPack;
	/** index of current level */
	private static int curLevelNumber;
	/** index of next difficulty level */
	private static int nextDiffLevel;
	/** index of next level pack */
	private static int nextLevelPack;
	/** index of next level */
	private static int nextLevelNumber;
	/** list of all active Lemmings in the Level */
	private static LinkedList<Lemming> lemmings;
	/** list of all active explosions */
	private static LinkedList<Explosion> explosions;
	/** list of all Lemmings under the mouse cursor */
	private static ArrayList<Lemming> lemmsUnderCursor;
	/** list of available level packs */
	private static ArrayList<LevelPack> levelPack;
	/** small preview version of level used in briefing screen */
	private static BufferedImage mapPreview;
	/** timer used for nuking */
	private static MicrosecondTimer timerNuke;
	/** key repeat object for plus key/icon */
	private static KeyRepeat plus;
	/** key repeat object for minus key/icon */
	private static KeyRepeat minus;
	/** Lemming for which skill change is requested */
	private static Lemming lemmSkillRequest;
	/** horizontal scrolling offset for level */
	private static int xPos;
	/** replay stream used for handling replays */
	private static ReplayStream replay;
	/** frame counter used for handling replays */
	private static int replayFrame;
	/** old value of release rate */
	private static int releaseRateOld;
	/** old value of nuke flag */
	private static boolean nukeOld;
	/** old value of horizontal scrolling position */
	private static int xPosOld;
	/** old value of selected skill */
	private static Lemming.Type lemmSkillOld;
	/** flag: replay mode is active */
	private static boolean replayMode;
	/** flag: replay mode should be stopped */
	private static boolean stopReplayMode;
	/** listener to inform GUI of player's progress */
	private static UpdateListener levelMenuUpdateListener;
	/** number of Lemmings which left the level */
	private static int numLeft;
	/** release rate 0..99 */
	private static int releaseRate;
	/** number of Lemmings available */
	private static int numLemmingsMax;
	/** number of Lemmings who entered the level */
	private static int numLemmingsOut;
	/** number of Lemmings which have to be rescued to finish the level */
	private static int numToRecue;
	/** time left in seconds */
	private static int time;
	/** number of climber skills left to be assigned */
	private static int numClimbers;
	/** number of floater skills left to be assigned */
	private static int numFloaters;
	/** number of bomber skills left to be assigned */
	private static int numBombers;
	/** number of blocker skills left to be assigned */
	private static int numBlockers;
	/** number of builder skills left to be assigned */
	private static int numBuilders;
	/** number of basher skills left to be assigned */
	private static int numBashers;
	/** number of miner skills left to be assigned */
	private static int numMiners;
	/** number of digger skills left to be assigned */
	private static int numDiggers;
	/** free running update counter */
	private static int updateCtr;
	/** gain for sound 0..1.0 */
	private static double soundGain = 1.0;
	/** gain for music 0..1.0 */
	private static double musicGain = 1.0;

	/**
	 * Initialization.
	 * @throws ResourceException
	 */
	public static void init() throws ResourceException {
		bgImage = ToolBox.createImage(Level.WIDTH, Level.HEIGHT, Transparency.BITMASK);
		bgGfx = bgImage.createGraphics();

		gameState = State.INIT;
		sound = new Sound(24,SND_MOUSEPRE);
		sound.setGain(soundGain);
		Icons.init(Core.getCmp());
		Explosion.init();
		Lemming.loadLemmings(Core.getCmp());
		lemmings = new LinkedList<Lemming>();
		explosions = new LinkedList<Explosion>();
		lemmsUnderCursor = new ArrayList<Lemming>(10);
		lemmSkillRequest = null;

		LemmFont.init();
		NumFont.init();
		LemmCursor.init();
		Music.init();
		Music.setGain(musicGain);
		MiscGfx.init();

		plus  = new KeyRepeat(MICROSEC_KEYREPEAT_START, MICROSEC_KEYREPEAT_REPEAT, MICROSEC_RELEASE_DOUBLE_CLICK);
		minus = new KeyRepeat(MICROSEC_KEYREPEAT_START, MICROSEC_KEYREPEAT_REPEAT, MICROSEC_RELEASE_DOUBLE_CLICK);
		timerNuke = new MicrosecondTimer();

		level = new Level();
		// read level packs

		File dir = new File(Core.resourcePath+"levels");
		File files[] = dir.listFiles();
		// now get the names of the directories
		ArrayList<String> dirs = new ArrayList<String>();
		for (int i=0; i<files.length; i++)
			if (files[i].isDirectory())
				dirs.add(files[i].getName());
		Collections.sort(dirs);

		//levelPack = new LevelPack[]; // dirs.size()+1];
		levelPack = new ArrayList<LevelPack>();

		levelPack.add(new LevelPack()); // dummy
		for (int i=0; i<dirs.size(); i++) {  // read levels
			String lvlName = dirs.get(i);

			LevelPack tlp = new LevelPack(Core.findResource("levels/"+lvlName+"/levelpack.ini"));

			if(tlp != null) {
				if(!tlp.getName().equals("empty")) levelPack.add(tlp);
			}
		}
		curDiffLevel = 0;
		curLevelPack = 1; // since 0 is dummy
		curLevelNumber = 0;

		replayFrame = 0;
		replay = new ReplayStream();
		replayMode = false;
		stopReplayMode = false;

		if (isCheat())
			wasCheated = true;
		else
			wasCheated = false;
	}

	/**
	 * Calculate absolute level number from diff level and relative level number
	 * @param lvlPack   level pack
	 * @param diffLevel difficulty level
	 * @param level     relative level number
	 * @return absolute level number (0..127)
	 */
	static int absLevelNum(final int lvlPack, final int diffLevel, final int level) {
		LevelPack lpack = levelPack.get(lvlPack);
		// calculate absolute level number
		int absLvl = level;
		for (int i=0; i<diffLevel; i++)
			absLvl += lpack.getLevels(i).length;
		return absLvl;
	}

	/**
	 * Calculate diffLevel and relative level number from absolute level number
	 * @param lvlPack level pack
	 * @param lvlAbs  absolute level number
	 * @return { difficulty level, relative level number }
	 */
	public static int[] relLevelNum(final int lvlPack, final int lvlAbs) {
		int retval[] = new int[2];
		LevelPack lpack = levelPack.get(lvlPack);
		int diffLevels = lpack.getDiffLevels().length;
		int lvl=0;
		int diffLvl=0;
		int maxLevels=30;
		for (int i=0, ls = 0; i<diffLevels; i++) {
			int ls_old = ls;
			// add number of levels existing in this diff level
			maxLevels = lpack.getLevels(i).length;
			ls += maxLevels;
			if (lvlAbs < ls) {
				diffLvl = i;
				lvl = lvlAbs - ls_old; // relative level mumber
				break;
			}
		}
		retval[0] = diffLvl;
		retval[1] = lvl;
		return retval;
	}

	/**
	 * Proceed to next level.
	 */
	public static synchronized void nextLevel() {
		if ( curLevelNumber < (levelPack.get(curLevelPack).getLevels(curDiffLevel).length - 1) ) {
			curLevelNumber++;
			GameController.requestChangeLevel(curLevelPack, curDiffLevel, curLevelNumber, false);
		}
	}

	/**
	 * Plays the highest available level in the remembered diff level
	 */
	public static synchronized void playCurDifLevel() {
		String p[] = Core.player.getCurDifLevel();

		if(p == null || p[0] == null) {
			GameController.requestChangeLevel(1, 0, 0, false);
			return;
		}

		// find the lowest completed level in this difficulty level
		int ln = Core.player.getCompletedLevelNum(p[0], p[1]);

		// search for the indexes for the pack and diff levels
		for(int i = 1;/* skip dummp */ i<levelPack.size(); i++ ) {
			if(p[0].equalsIgnoreCase(levelPack.get(i).getName())) {
				String diffs[] = levelPack.get(i).getDiffLevels();
				for(int j = 0; j<diffs.length; j++ ) {
					if(p[1].equalsIgnoreCase(diffs[j])) {
						GameController.requestChangeLevel(i, j, ln, false);
						break;
					}
				}
				break;
			}
		}
	}

	/**
	 * Determine the next level to play
	 * This function is called when a player successfully finishes a level.
	 * It increments the saved current difficulty level the user is on to
	 * the next pack or difficulty level found.
	 */
	public static synchronized void determineNextLevel() {
		int clp = curLevelPack;
		int cdl = curDiffLevel;

		if ( curLevelNumber >= (levelPack.get(clp).getLevels(cdl).length - 1) ) {
			return;
		}

		// current difficulty level has completed
		if( cdl < (levelPack.get(clp).getDiffLevels().length - 1) ) {
			cdl++;
		} else if( clp < (levelPack.size() - 1) ) {
			cdl = 0;
			clp++;
		} else {
			cdl = 0;
			clp = 1; // avoid dummy level pack
		}

		// remember pack and difficulty level for next time
		String pack = levelPack.get(clp).getName();
		String diff = levelPack.get(clp).getDiffLevels()[cdl];
		Core.player.setCurDifLevel(pack, diff);
	}

	/**
	 * Remember this difficulty level
	 */
	public static synchronized void rememberThisDifficultyLevel() {
		String pack = levelPack.get(curLevelPack).getName();
		String diff = levelPack.get(curLevelPack).getDiffLevels()[curDiffLevel];
		Core.player.setCurDifLevel(pack, diff);
	}

	/**
	 * Fade out at end of level.
	 */
	public static synchronized void endLevel() {
		transitionState = TransitionState.END_LEVEL;
		gameState = State.LEVEL_END;
		Fader.setState(Fader.State.OUT);
	}

	/**
	 * Level successfully finished, enter debriefing and tell GUI to enable next level.
	 */
	static synchronized void finishLevel() {
		Music.stop();
		setFastForward(false);
		setSuperLemming(false);
		replayMode = false;

		if (!wasLost()) {
				levelMenuUpdateListener.update();
			determineNextLevel();
		}
		gameState = State.DEBRIEFING;
	}

	/**
	 * Hook for GUI to get informed when a level was successfully finished.
	 * @param l UpdateListener
	 */
	public static void setLevelMenuUpdateListener(final UpdateListener l) {
		levelMenuUpdateListener = l;
	}

	/**
	 * Restart level.
	 * @param doReplay true: replay, false: play
	 */
	private static synchronized void restartLevel(final boolean doReplay) {
		initLevel();
		if (doReplay) {
			replayMode = true;
			replay.save(Core.resourcePath+"/replay.rpl");
			replay.rewind();
		} else {
			replayMode = false;
			replay.clear();
		}
	}

	/**
	 * Initialize a level after it was loaded.
	 */
	private static void initLevel() {
		Music.stop();

		setFastForward(false);
		setPaused(false);
		nuke = false;

		lemmSkillRequest = null;

		TextScreen.setMode(TextScreen.Mode.INIT);

		bgGfx.setBackground(blankColor);
		bgGfx.clearRect(0, 0, bgImage.getWidth(), bgImage.getHeight());

		stencil = getLevel().paintLevel(bgImage, Core.getCmp(), stencil);

		lemmings.clear();
		explosions.clear();
		Icons.reset();

		TrapDoor.reset(getLevel().getEntryNum());
		entryOpened = false;
		entryOpenCtr = 0;
		secondCtr = 0;
		releaseCtr = 0;
		lemmSkill = Lemming.Type.UNDEFINED;

		plus.init();
		minus.init();

		numLeft = 0;
		releaseRate = getLevel().getReleaseRate();
		numLemmingsMax = getLevel().getNumLemmings();
		numLemmingsOut = 0;
		numToRecue  = getLevel().getNumToRescue();
		time        = getLevel().getTimeLimitSeconds();
		numClimbers = getLevel().getNumClimbers();
		numFloaters = getLevel().getNumFloaters();
		numBombers = getLevel().getNumBombers();
		numBlockers = getLevel().getNumBlockers();
		numBuilders = getLevel().getNumBuilders();
		numBashers = getLevel().getNumBashers();
		numMiners = getLevel().getNumMiners();
		numDiggers = getLevel().getMumDiggers();
		setxPos(getLevel().getXpos());

		calcReleaseBase();

		mapPreview = getLevel().createMiniMap(mapPreview, bgImage, 4, 4, false);

		setSuperLemming(getLevel().isSuperLemming());

		replayFrame = 0;
		stopReplayMode = false;
		releaseRateOld = releaseRate;
		lemmSkillOld = lemmSkill;
		nukeOld = false;
		xPosOld = getLevel().getXpos();

		GameController.rememberThisDifficultyLevel();

		gameState = State.BRIEFING;
	}

	/**
	 * Request the restart of this level.
	 * @param doReplay
	 */
	public static synchronized void requestRestartLevel(final boolean doReplay) {
		if (doReplay)
			transitionState = TransitionState.REPLAY_LEVEL;
		else
			transitionState = TransitionState.RESTART_LEVEL;
		Fader.setState(Fader.State.OUT);
	}

	/**
	 * Request a new level.
	 * @param lPack index of level pack
	 * @param dLevel index of difficulty level
	 * @param lNum level number
	 * @param doReplay true: replay, false: play
	 */
	public static synchronized void requestChangeLevel(final int lPack, final int dLevel, final int lNum, final boolean doReplay) {
		nextLevelPack = lPack;
		nextDiffLevel = dLevel;
		nextLevelNumber = lNum;

		if (doReplay)
			transitionState = TransitionState.LOAD_REPLAY;
		else
			transitionState = TransitionState.LOAD_LEVEL;
		Fader.setState(Fader.State.OUT);
	}

	/**
	 * Start a new level.
	 * @param lPack index of level pack
	 * @param dLevel index of difficulty level
	 * @param lNum level number
	 * @param doReplay true: replay, false: play
	 */
	static private synchronized Level changeLevel(final int lPack, final int dLevel, final int lNum, final boolean doReplay) throws ResourceException, LemmException {
		//gameState = GAME_ST_INIT;
		curLevelPack = lPack;
		curDiffLevel = dLevel;
		curLevelNumber = lNum;

		String lvlPath = levelPack.get(curLevelPack).getInfo(curDiffLevel, curLevelNumber).getFileName();
		// lemmings need to be reloaded to contain pink color
		Lemming.loadLemmings(Core.getCmp());
		// loading the level will patch pink lemmings pixels to correct color
		getLevel().loadLevel(lvlPath);

		// if with and height would be stored inside the level, the bgImage etc. would have to
		// be recreated here
		// bgImage = gc.createCompatibleImage(Level.width, Level.height, Transparency.BITMASK);
		// bgGfx = bgImage.createGraphics();

		initLevel();

		if (doReplay) {
			replayMode = true;
			replay.rewind();
		} else {
			replayMode = false;
			replay.clear();
		}

		return getLevel();
	}

	/**
	 * Get level lost state.
	 * @return true if level was lost, false otherwise
	 */
	static synchronized boolean wasLost() {
		if (gameState != State.LEVEL && numLeft >= numToRecue)
			return false;
		return true;
	}

	/**
	 * Get current replay image.
	 * @return current replay image
	 */
	public synchronized static BufferedImage getReplayImage() {
		if (!replayMode)
			return null;
		if ( (replayFrame & 0x3f) > 0x20)
			return MiscGfx.getImage(MiscGfx.Index.REPLAY_1);
		else
			return MiscGfx.getImage(MiscGfx.Index.REPLAY_2);
	}

	/**
	 * Get a Lemming under the selection cursor.
	 * @param type cursor type
	 * @return fitting Lemming or null if none found
	 */
	public static synchronized Lemming lemmUnderCursor(final LemmCursor.Type type) {
		// search for level without the skill
		for (int i=0; i<getLemmsUnderCursor().size(); i++) {
			Lemming l = getLemmsUnderCursor().get(i);
			// Walker only cursor: ignore non-walkers
			if (type == LemmCursor.Type.WALKER && l.getSkill()!=Lemming.Type.WALKER)
				continue;
			if (type == LemmCursor.Type.LEFT && l.getDirection() != Lemming.Direction.LEFT)
				continue;
			if (type == LemmCursor.Type.RIGHT && l.getDirection() != Lemming.Direction.RIGHT)
				continue;
			switch (lemmSkill) {
				case CLIMBER:
					if (!l.canClimb())
						return l;
					break;
				case FLOATER:
					if (!l.canFloat())
						return l;
					break;
				default:
					if (l.canChangeSkill() && l.getSkill() != lemmSkill && l.getName().length() > 0) {
						//System.out.println(l.getName());
						return l;
					}
			}
			break;
		}
		if (type == LemmCursor.Type.NORMAL && getLemmsUnderCursor().size() > 0) {
			Lemming l = getLemmsUnderCursor().get(0);
			if (l.getName().length() == 0)
				return null;
			//System.out.println(((Lemming)lemmsUnderCursor.get(0)).getName());
			return l;
		}
		return null;
	}

	/**
	 * Lemming has left the Level.
	 */
	static synchronized void increaseLeft() {
		numLeft += 1;
	}

	/**
	 * Stop replay.
	 */
	private static void stopReplayMode() {
		if (replayMode)
			stopReplayMode = true;
	}

	/**
	 * Return time as String "minutes-seconds"
	 * @return time as String "minutes-seconds"
	 */
	public synchronized static String getTimeString() {
		String t1 = Integer.toString(time/60);
		String t2 = Integer.toString(time%60);
		if (t2.length() < 2)
			t2 = "0"+t2;
		return t1 + "-" + t2;
	}

	/**
	 * Update the whole game state by one frame.
	 */
	public static synchronized void update() {
		if (gameState != State.LEVEL)
			return;

		updateCtr++;

		if (!replayMode)
			assignSkill(false); // first try to assign skill

		// check +/- buttons also if paused
		KeyRepeat.Event fired = plus.fired();
		if (fired != KeyRepeat.Event.NONE) {
			if (releaseRate < MAX_RELEASE_RATE) {
				if (fired == KeyRepeat.Event.DOUBLE_CLICK)
					releaseRate = MAX_RELEASE_RATE;
				else
					releaseRate += 1;
				calcReleaseBase();
				sound.playPitched(releaseRate);
			} else sound.play(SND_TING);
		}

		fired = minus.fired();
		if (fired != KeyRepeat.Event.NONE) {
			if (releaseRate > getLevel().getReleaseRate()) {
				if (fired == KeyRepeat.Event.DOUBLE_CLICK)
					releaseRate = getLevel().getReleaseRate();
				else
					releaseRate -= 1;
				calcReleaseBase();
				sound.playPitched(releaseRate);
			} else sound.play(SND_TING);
		}


		if (isPaused())
			return;

		// test for end of replay mode
		if ( replayMode && stopReplayMode ) {
			replay.clearFrom(replayFrame);
			replayMode = false;
			stopReplayMode = false;
		}

		if (!replayMode) {
			if (!wasCheated) {
				// replay: release rate changed?
				if (releaseRate != releaseRateOld) {
					replay.addReleaseRateEvent(replayFrame, releaseRate);
					releaseRateOld = releaseRate;
				}
				// replay: nuked?
				if (nuke != nukeOld) {
					replay.addNukeEvent(replayFrame);
					nukeOld = nuke;
				}
				// replay: xPos changed?
				if (getxPos() != xPosOld) {
					replay.addXPosEvent(replayFrame, getxPos());
					xPosOld = getxPos();
				}
				// skill changed
				if (lemmSkill != lemmSkillOld) {
					replay.addSelectSkillEvent(replayFrame, lemmSkill);
					lemmSkillOld = lemmSkill;
				}
			} else replay.clear();
		} else {
			// replay mode
			ReplayEvent r;
			while ( (r=replay.getNext(replayFrame)) != null) {
				switch (r.type) {
					case ReplayStream.ASSIGN_SKILL: {
						ReplayAssignSkillEvent rs = (ReplayAssignSkillEvent)r;
						synchronized (lemmings) {
							Lemming l = lemmings.get(rs.lemming);
							l.setSkill(rs.skill);
							l.setSelected();
						}
						switch (rs.skill) {
							case FLOATER:
								numFloaters -= 1;
								break;
							case CLIMBER:
								numClimbers -= 1;
								break;
							case BOMBER:
								numBombers -= 1;
								break;
							case DIGGER:
								numDiggers -= 1;
								break;
							case BASHER:
								numBashers -= 1;
								break;
							case BUILDER:
								numBuilders -= 1;
								break;
							case MINER:
								numMiners -= 1;
								break;
							case STOPPER:
								numBlockers -= 1;
								break;
						}
						sound.play(SND_CHANGE_OP);
						break;
					}
					case ReplayStream.SET_RELEASE_RATE:
						ReplayReleaseRateEvent rr = (ReplayReleaseRateEvent)r;
						releaseRate = rr.releaseRate;
						calcReleaseBase();
						sound.playPitched(releaseRate);
						break;
					case ReplayStream.NUKE:
						nuke = true;
						break;
					case ReplayStream.MOVE_XPOS: {
						ReplayMoveXPosEvent rx = (ReplayMoveXPosEvent)r;
						setxPos(rx.xPos);
						break;
					}
					case ReplayStream.SELECT_SKILL: {
						ReplaySelectSkillEvent rs = (ReplaySelectSkillEvent)r;
						lemmSkill = rs.skill;
						switch (lemmSkill) {
							case FLOATER:
								Icons.press(Icons.Type.FLOAT);
								break;
							case CLIMBER:
								Icons.press(Icons.Type.CLIMB);
								break;
							case BOMBER:
								Icons.press(Icons.Type.BOMB);
								break;
							case DIGGER:
								Icons.press(Icons.Type.DIG);
								break;
							case BASHER:
								Icons.press(Icons.Type.BASH);
								break;
							case BUILDER:
								Icons.press(Icons.Type.BUILD);
								break;
							case MINER:
								Icons.press(Icons.Type.MINE);
								break;
							case STOPPER:
								Icons.press(Icons.Type.BLOCK);
								break;
						}
						break;
					}
				}
			}
		}

		// replay: xpos changed

		// store locally to avoid it's overwritten amidst function
		boolean nukeTemp = nuke;

		// time
		secondCtr+=1.0;
		if (secondCtr > MAX_SECOND_CTR) {
			// one second passed
			secondCtr -= MAX_SECOND_CTR;
			time--;
			if (!isCheat() && time == 0) {
				// level failed
				endLevel();
			}
		}
		// release
		if (entryOpened && !nukeTemp && !isPaused() && numLemmingsOut < getNumLemmingsMax() && ++releaseCtr >= releaseBase) {
			releaseCtr = 0;
			//LemmingResource ls = Lemming.getResource(Lemming.TYPE_FALLER);
			try {
				if (getLevel().getEntryNum() != 0) {
					Entry e = getLevel().getEntry(TrapDoor.getNext());
					Lemming l = new Lemming(e.xPos+2, e.yPos+20);
					synchronized (lemmings) {
						lemmings.add(l);
					}
					numLemmingsOut++;
				}
			} catch (ArrayIndexOutOfBoundsException ex) {}
		}
		// nuking
		if (nukeTemp && ((updateCtr&1)==1)) {
			synchronized (lemmings) {
				Iterator<Lemming> it = lemmings.iterator();
				while (it.hasNext()) {
					Lemming l = it.next();
					if (!l.nuke() && !l.hasDied() && !l.hasLeft()) {
						l.setSkill(Lemming.Type.NUKE);
						//System.out.println("nuked!");
						break;
					}
				}
			}
		}
		// open trap doors ?
		if (!entryOpened) {
			if (++entryOpenCtr == MAX_ENTRY_OPEN_CTR) {
				for (int i=0; i<getLevel().getEntryNum(); i++)
					getLevel().getSprObject(getLevel().getEntry(i).id).setAnimMode(Sprite.Animation.ONCE);
				sound.play(SND_DOOR);
			} else if (entryOpenCtr == MAX_ENTRY_OPEN_CTR+10*MAX_ANIM_CTR) {
				//System.out.println("opened");
				entryOpened = true;
				releaseCtr = releaseBase; // first lemming to enter at once
				if (musicOn)
					Music.play();
			}
		}
		// end of game conditions
		if ((nukeTemp || numLemmingsOut == getNumLemmingsMax()) && explosions.size()==0 && lemmings.size()==0) {
			endLevel();
		}

		synchronized (lemmings) {
			Iterator<Lemming> it = lemmings.iterator();
			while (it.hasNext()) {
				Lemming l = it.next();
				if (l.hasDied() || l.hasLeft()) {
					it.remove();
					continue;
				}
				l.animate();
			}
		}

		synchronized (explosions) {
			Iterator<Explosion> it = explosions.iterator();
			while (it.hasNext()) {
				Explosion e = it.next();
				if (e.isFinished())
					it.remove();
				else
					e.update();
			}
		}

		// animate level objects
		if (++animCtr > MAX_ANIM_CTR) {
			animCtr -= MAX_ANIM_CTR;
			for (int n=0; n<getLevel().getSprObjectNum(); n++) {
				SpriteObject spr = getLevel().getSprObject(n);
				spr.getImageAnim(); // just to animate
			}
		}

		if (!replayMode)
			assignSkill(true); // 2nd try to assign skill

		replayFrame ++;
	}

	/**
	 * Request a skill change for a Lemming (currently selected skill).
	 * @param lemm Lemming
	 */
	public synchronized static void requestSkill(final Lemming lemm) {
		if (lemmSkill != Lemming.Type.UNDEFINED)
			lemmSkillRequest = lemm;
		stopReplayMode();
	}

	/**
	 * Assign the selected skill to the selected Lemming.
	 * @param delete flag: reset the current skill request
	 */
	private synchronized static void assignSkill(final boolean delete) {
		if (lemmSkillRequest == null || lemmSkill == Lemming.Type.UNDEFINED)
			return;

		Lemming lemm = lemmSkillRequest;
		if (delete)
			lemmSkillRequest = null;

		boolean canSet = false;
		stopReplayMode();

		if (isCheat()) {
			canSet = lemm.setSkill(lemmSkill);
		} else {
			switch (lemmSkill) {
				case BASHER:
					if (numBashers > 0 && lemm.setSkill(lemmSkill)) {
						numBashers -= 1;
						canSet = true;
					}
					break;
				case BOMBER:
					if (numBombers > 0 && lemm.setSkill(lemmSkill)) {
						numBombers -= 1;
						canSet = true;
					}
					break;
				case BUILDER:
					if (numBuilders > 0 && lemm.setSkill(lemmSkill)) {
						numBuilders -= 1;
						canSet = true;
					}
					break;
				case CLIMBER:
					if (numClimbers > 0 && lemm.setSkill(lemmSkill)) {
						numClimbers -= 1;
						canSet = true;
					}
					break;
				case DIGGER:
					if (numDiggers > 0 && lemm.setSkill(lemmSkill)) {
						numDiggers -= 1;
						canSet = true;
					}
					break;
				case FLOATER:
					if (numFloaters > 0 && lemm.setSkill(lemmSkill)) {
						numFloaters -= 1;
						canSet = true;
					}
					break;
				case MINER:
					if (numMiners > 0 && lemm.setSkill(lemmSkill)) {
						numMiners -= 1;
						canSet = true;
					}
					break;
				case STOPPER:
					if (numBlockers > 0 && lemm.setSkill(lemmSkill)) {
						numBlockers -= 1;
						canSet = true;
					}
					break;
			}
		}
		if (canSet) {
			lemmSkillRequest = null; // erase request
			sound.play(SND_MOUSEPRE);
			if (isPaused()) {
				setPaused(false);
				Icons.press(Icons.Type.PAUSE);
			}
			// add to replay stream
			if (!wasCheated)
				synchronized (lemmings) {
					for (int i=0; i<lemmings.size(); i++)
						if (lemmings.get(i) == lemm) // if 2nd try (delete==true) assign to next frame
							replay.addAssignSkillEvent(replayFrame+((delete) ? 1:0), lemmSkill, i);
				}
		} else if (delete)
			sound.play(SND_TING);
	}

	/**
	 * Calculate the counter threshold for releasing a new Lemmings.
	 */
	static private void calcReleaseBase() {
		// the original formula is: release lemming every 4+(99-speed)/2 time steps
		// where one step is 60ms (3s/50) or 66ms (4s/60).
		// Lemmini runs at 30ms/33ms, so the term has to be multiplied by 2
		// 8+(99-releaseRate) should be correct
		releaseBase = 8 + (99 - releaseRate);
	}

	/**
	 * Handle pressing of an icon button.
	 * @param type icon type
	 */
	public static synchronized void handleIconButton(final Icons.Type type) {
		Lemming.Type lemmSkillOld = lemmSkill;
		boolean ok = false;
		switch (type) {
			case FLOAT:
				if (isCheat() || numFloaters>0)
					lemmSkill = Lemming.Type.FLOATER;
				stopReplayMode();
				break;
			case CLIMB:
				if (isCheat() || numClimbers>0)
					lemmSkill = Lemming.Type.CLIMBER;
				stopReplayMode();
				break;
			case BOMB:
				if (isCheat() || numBombers>0)
					lemmSkill = Lemming.Type.BOMBER;
				stopReplayMode();
				break;
			case DIG:
				if (isCheat() || numDiggers>0)
					lemmSkill = Lemming.Type.DIGGER;
				stopReplayMode();
				break;
			case BASH:
				if (isCheat() || numBashers>0)
					lemmSkill = Lemming.Type.BASHER;
				stopReplayMode();
				break;
			case BUILD:
				if (isCheat() || numBuilders>0)
					lemmSkill = Lemming.Type.BUILDER;
				stopReplayMode();
				break;
			case MINE:
				if (isCheat() || numMiners>0)
					lemmSkill = Lemming.Type.MINER;
				stopReplayMode();
				break;
			case BLOCK:
				if (isCheat() || numBlockers>0)
					lemmSkill = Lemming.Type.STOPPER;
				stopReplayMode();
				break;
			case NUKE: {
				ok = true;
				stopReplayMode();
				if (timerNuke.delta() < MICROSEC_NUKE_DOUBLE_CLICK) {
					if (!nuke) {
						nuke = true;
						sound.play(SND_OHNO);
					}
				} else timerNuke.deltaUpdate();
				break;}
			case PAUSE:
				setPaused(!isPaused());
				ok = true;
				break;
			case FFWD:
				setFastForward(!isFastForward());
				ok = true;
				break;
			case PLUS:
				ok = true; // supress sound
				plus.pressed(KEYREPEAT_ICON);
				stopReplayMode();
				break;
			case MINUS:
				ok = true; // supress sound
				minus.pressed(KEYREPEAT_ICON);
				stopReplayMode();
				break;
		}
		if (ok || lemmSkill != lemmSkillOld) {
			switch (type) {
				case PLUS:
				case MINUS:
					break; // supress sound
				default:
					sound.play(SND_CHANGE_OP);
			}
			Icons.press(type);
		} else
			sound.play(SND_TING);
	}

	/**
	 * Fade in/out.
	 * @param g graphics object
	 */
	public static void fade(final Graphics g) {
		if (Fader.getState() == Fader.State.OFF && transitionState != TransitionState.NONE) {
			switch (transitionState) {
				case END_LEVEL:
					finishLevel();
					break;
				case TO_BRIEFING:
					gameState = State.BRIEFING;
					break;
				case TO_DEBRIEFING:
					gameState = State.DEBRIEFING;
					break;
				case TO_INTRO:
					gameState = State.INTRO;
					break;
				case TO_LEVEL:
					GameController.sound.play(SND_LETSGO);
					try {
						Music.load("music/"+GameController.levelPack.get(GameController.curLevelPack).getInfo(GameController.curDiffLevel,
								GameController.curLevelNumber).getMusic());
					} catch (ResourceException ex) {
						Core.resourceError(ex.getMessage());
					} catch (LemmException ex) {
						JOptionPane.showMessageDialog( null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
						System.exit(1);
					}
					gameState = State.LEVEL;
					break;
				case RESTART_LEVEL:
				case REPLAY_LEVEL:
					restartLevel(transitionState == TransitionState.REPLAY_LEVEL);
					break;
				case LOAD_LEVEL:
				case LOAD_REPLAY:
					try {
						changeLevel(nextLevelPack, nextDiffLevel, nextLevelNumber, transitionState == TransitionState.LOAD_REPLAY);
						/*IF-NOT-MAC
						((JFrame)Core.getCmp()).setTitle("Lemmini - "+getLevel().getLevelName());
						//END-NOT-MAC*/
					} catch (ResourceException ex) {
						Core.resourceError(ex.getMessage());
					} catch (LemmException ex) {
						JOptionPane.showMessageDialog( null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
						System.exit(1);
					}
					break;
			}
			Fader.setState(Fader.State.IN);
			transitionState = TransitionState.NONE;
		}
		Fader.fade(g);
	}

	/**
	 * Draw the explosions
	 * @param g graphics object
	 * @param width width of screen in pixels
	 * @param height height of screen in pixels
	 * @param xOfs horizontal level offset in pixels
	 */
	public static void drawExplosions(final Graphics2D g, final int width, final int height, final int xOfs) {
		synchronized (explosions) {
			Iterator<Explosion> it = explosions.iterator();
			while (it.hasNext()) {
				Explosion e = it.next();
				e.draw(g, width, height, xOfs);
			}
		}
	}

	/**
	 * Add a new explosion.
	 * @param x x coordinate in pixels.
	 * @param y y coordinate in pixels.
	 */
	public static void addExplosion(final int x, final int y) {
		// create particle explosion
		synchronized (GameController.explosions) {
			GameController.explosions.add(new Explosion(x, y));
		}
	}

	/**
	 * Draw icon bar.
	 * @param g graphics object
	 * @param x x coordinate in pixels
	 * @param y y coordinate in pixels
	 */
	public static void drawIcons(final Graphics2D g, final int x, final int y) {
		g.drawImage(Icons.getImg(),x,y,null);
	}

	/**
	 * Draw the skill/release rate values
	 * @param g graphics object
	 * @param y y offset in pixels
	 */
	public static void drawCounters(final Graphics2D g, final int y) {
		// draw counters
		int[] val = new int[]{level.getReleaseRate(), releaseRate,
			numClimbers, numFloaters, numBombers, numBlockers, numBuilders,
			numBashers, numMiners, numDiggers};

		for (int i=0; i<10;i++) {
			g.drawImage(NumFont.numImage(val[i]),Icons.WIDTH*i+8,y,null);
		}
	}

	/**
	 * Get index of current level pack.
	 * @return index of current level pack
	 */
	public static int getCurLevelPackIdx() {
		return curLevelPack;
	}

	/**
	 * Get current level pack.
	 * @return current level pack
	 */
	public static LevelPack getCurLevelPack() {
		return levelPack.get(curLevelPack);
	}

	/**
	 * get number of level packs
	 * @return number of level packs
	 */
	public static int getLevelPackNum() {
		return levelPack.size();
	}

	/**
	 * Get level pack via index.
	 * @param i index of level pack
	 * @return LevelPack
	 */
	public static LevelPack getLevelPack(final int i) {
		return levelPack.get(i);
	}

	/**
	 * Get index of current difficulty level.
	 * @return index of current difficulty level
	 */
	public static int getCurDiffLevel() {
		return curDiffLevel;
	}

	/**
	 * Get number of current level.
	 * @return number of current leve
	 */
	public static int getCurLevelNumber() {
		return curLevelNumber;
	}


	/**
	 * Set horizontal scrolling offset.
	 * @param x horizontal scrolling offset in pixels
	 */
	public static void setxPos(final int x) {
		xPos = x;
	}

	/**
	 * Get horizontal scrolling offset.
	 * @return horizontal scrolling offset in pixels
	 */
	public static int getxPos() {
		return xPos;
	}

	/**
	 * Set game state.
	 * @param s new game state
	 */
	public static void setGameState(final State s) {
		gameState = s;
	}

	/**
	 * Get game state.
	 * @return game state
	 */
	public static State getGameState() {
		return gameState;
	}

	/**
	 * Enable/disable cheat mode.
	 * @param c true: enable, false: disable
	 */
	public static void setCheat(final boolean c) {
		cheat = c;
	}

	/**
	 * Get state of cheat mode.
	 * @return true if cheat mode enabled, false otherwise
	 */
	public static boolean isCheat() {
		return cheat;
	}

	/**
	 * Set transition state.
	 * @param ts TransitionState
	 */
	public static void setTransition(final TransitionState ts) {
		transitionState = ts;
	}


	/**
	 * Load a replay.
	 * @param fn file name
	 * @return replay level info object
	 */
	public static ReplayLevelInfo loadReplay(final String fn) {
		return replay.load(fn);
	}

	/**
	 * Save a replay.
	 * @param fn file name
	 * @return true if saved successfully, false otherwise
	 */
	public static boolean saveReplay(final String fn) {
		return replay.save(fn);
	}

	/**
	 * Activate/deactivate Superlemming mode.
	 * @param sl true: activate, false: deactivate
	 */
	public static void setSuperLemming(final boolean sl) {
		superLemming = sl;
	}

	/**
	 * Get Superlemming state.
	 * @return true is Superlemming mode is active, false otherwise
	 */
	public static boolean isSuperLemming() {
		return superLemming;
	}

	/**
	 * Set cheated detection.
	 * @param c true: cheat mode was activated, false otherwise
	 */
	public static void setWasCheated(final boolean c) {
		wasCheated = c;
	}

	/**
	 * Enable pause mode.
	 * @param p true: pause is active, false otherwise
	 */
	public static void setPaused(final boolean p) {
		paused = p;
	}

	/**
	 * Get pause state.
	 * @return true if pause is active, false otherwise
	 */
	public static boolean isPaused() {
		return paused;
	}

	/**
	 * Enable fast forward mode.
	 * @param ff true: fast forward is active, false otherwise
	 */
	public static void setFastForward(final boolean ff) {
		fastForward = ff;
	}

	/**
	 * Get fast forward state.
	 * @return true if fast forward is active, false otherwise
	 */
	public static boolean isFastForward() {
		return fastForward;
	}

	/** get number of lemmings left in the game
	 * @return number of lemmings left in the game
	 */
	public static int getNumLeft() {
		return numLeft;
	}

	/**
	 * Set number of Lemmings left in the game.
	 * @param n number of Lemmings left in the game
	 */
	public static void setNumLeft(final int n) {
		numLeft = n;
	}

	/**
	 * Get level object.
	 * @return level object
	 */
	public static Level getLevel() {
		return level;
	}

	/**
	 * Get maximum number of Lemmings for this level.
	 * @return maximum number of Lemmings for this level
	 */
	public static int getNumLemmingsMax() {
		return numLemmingsMax;
	}

	/**
	 * Get icon type from x position.
	 * @param x x position in pixels
	 * @return icon type
	 */
	public static Icons.Type getIconType(final int x) {
		return Icons.getType(x);
	}

	/**
	 * Icon was pressed.
	 * @param t icon type
	 */
	public static void pressIcon(final Icons.Type t) {
		Icons.press(t);
	}

	/**
	 * Icon was released.
	 * @param t icon type
	 */
	public static void releaseIcon(final Icons.Type t) {
		Icons.release(t);
	}

	/**
	 * Plus was pressed.
	 * @param d bitmask: key or icon
	 */
	public static void pressPlus(final int d) {
		plus.pressed(d);
	}

	/**
	 * Plus was released.
	 * @param d bitmask: key or icon
	 */
	public static void releasePlus(final int d) {
		plus.released(d);
	}

	/**
	 * Minus was pressed.
	 * @param d bitmask: key or icon
	 */
	public static void pressMinus(final int d) {
		minus.pressed(d);
	}

	/**
	 * Minus was released.
	 * @param d bitmask: key or icon
	 */
	public static void releaseMinus(final int d) {
		minus.released(d);
	}

	/**
	 * Get list of all Lemmings under the mouse cursor.
	 * @return list of all Lemmings under the mouse cursor
	 */
	public static ArrayList<Lemming> getLemmsUnderCursor() {
		return lemmsUnderCursor;
	}

	/**
	 * Get list of all Lemmings in this level.
	 * @return list of all Lemmings in this level
	 */
	public static LinkedList<Lemming> getLemmings() {
		return lemmings;
	}

	/**
	 * Set sound gain.
	 * @param g gain (0..1.0)
	 */
	public static void setSoundGain(final double g) {
		soundGain = g;
		if (sound != null)
			sound.setGain(soundGain);
	}

	/**
	 * Set music gain.
	 * @param g gain (0..1.0)
	 */
	public static void setMusicGain(final double g) {
		musicGain = g;
		if (Music.getType() != null)
			Music.setGain(musicGain);
	}

	/**
	 * Set advanced mouse selection mode.
	 * @param sel true: advanced selection mode active, false otherwise
	 */
	public static void setAdvancedSelect(final boolean sel) {
		advancedSelect = sel;
	}

	/**
	 * Get state of advanced mouse selection mode.
	 * @return true if advanced selection mode activated, false otherwise
	 */
	public static boolean isAdvancedSelect() {
		return advancedSelect;
	}

	/**
	 * Get background image of level.
	 * @return background image of level
	 */
	public static BufferedImage getBgImage() {
		return bgImage;
	}

	/**
	 * Get background stencil of level.
	 * @return background stencil of level
	 */
	public static Stencil getStencil() {
		return stencil;
	}

	/**
	 * Enable music.
	 * @param on true: music on, false otherwise
	 */
	public static void setMusicOn(final boolean on) {
		musicOn = on;
	}

	/**
	 * Get music enable state.
	 * @return true: music is on, false otherwise
	 */
	public static boolean isMusicOn() {
		return musicOn;
	}

	/**
	 * Enable sound.
	 * @param on true: sound on, false otherwise
	 */
	public static void setSoundOn(final boolean on) {
		soundOn = on;
	}

	/**
	 * Get sound enable state.
	 * @return true: sound is on, false otherwise
	 */
	public static boolean isSoundOn() {
		return soundOn;
	}

	/**
	 * Get small preview image of level.
	 * @return small preview image of level
	 */
	public static BufferedImage getMapPreview() {
		return mapPreview;
	}

	/**
	 * Get number of Lemmings to rescue.
	 * @return number of Lemmings to rescue
	 */
	public static int  getNumToRecue() {
		return numToRecue;
	}

	/**
	 * Get time left in seconds.
	 * @return time left in seconds
	 */
	public static int getTime() {
		return time;
	}

	/**
	 * Add level pack.
	 * @param absolute path of level pack folder
	 */
	public static void addLevelPack(String folder) throws ResourceException {
		levelPack.add(new LevelPack(folder + "/levelpack.ini"));
	}
}


/**
 * Trapdoor/Entry class
 * Trapdoor logic: for numbers >1, just take the next door for each lemming and wrap around to 1 when
 * the last one is reached.
 * Special rule for 3 trapdoors: the order is 1, 2, 3, 2 (loop), not 1, 2, 3 (loop)
 *
 * @author Volker Oth
 */
class TrapDoor {
	/** pattern for three entries */
	private final static int[] PATTERN3 = {0,1,2,1};

	/** number of entries */
	private static int entries;
	/** entry counter */
	private static int counter;

	/**
	 * Reset to new number of entries.
	 * @param e number of entries
	 */
	static void reset(final int e) {
		entries = e;
		counter = 0;
	}

	/**
	 * Get index of next entry.
	 * @return index of next entry
	 */
	static int getNext() {
		int retVal = counter;
		counter++;
		if (entries != 3) {
			if (counter >= entries)
				counter = 0;
			return retVal;
		}
		// special case: 3
		if (counter >= 4)
			counter = 0;
		return PATTERN3[retVal];
	}

}