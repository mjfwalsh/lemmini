import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;

//START-MAC
import com.apple.eawt.FullScreenListener;
import com.apple.eawt.AppEvent;
import com.apple.eawt.QuitStrategy;
//END-MAC

import GUI.GainDialog;
import GUI.LevelCodeDialog;
import GUI.PlayerDialog;
import Game.Core;
import Game.GameController;
import Game.GroupBitfield;
import Game.Icons;
import Game.LemmCursor;
import Game.LemmException;
import Game.LemmFont;
import Game.Lemming;
import Game.Level;
import Game.LevelCode;
import Game.LevelPack;
import Game.MiniMap;
import Game.MiscGfx;
import Game.Music;
import Game.Player;
import Game.ReplayLevelInfo;
import Game.ResourceException;
import Game.Stencil;
import Game.TextScreen;
import Game.UpdateListener;
import GameUtil.Fader;
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
 * Lemmini - a game engine for Lemmings.<br>
 * This is the main window including input handling. The game logic is located in
 * {@link GameController}, some core components are in {@link Core}.<br>
 * <br>
 * Note: this was developed for JRE1.4 and only ported to JRE1.5 after it was finished.
 * Also the design evolved during two years of development and thus isn't nearly as clean
 * as it should be. During the porting to 1.5, I cleaned up some things here and there,
 * but didn't want to redesign the whole thing from scratch.
 *
 * @author Volker Oth
 */

public class Lemmini extends JFrame implements KeyListener {
	/** minimum sleep duration in milliseconds - values too small may cause system clock shift under WinXP etc. */
	final static int MIN_SLEEP = 10;
	/** threshold for sleep - don't sleep if time to wait is shorter than this as sleep might return too late */
	final static int THR_SLEEP = 16;

	private final static long serialVersionUID = 0x01;

	/** self reference */
	static JFrame thisFrame;

	/** path for loading single level files */
	private String lvlPath;
	/** HashMap to store menu items for difficulty levels */
	private HashMap<String,ArrayList<LvlMenuItem>> diffLevelMenus;
	/** panel for the game graphics */
	private GraphicsPane gp;

	/** size of frame margins */
	private int xMargin;
	private int yMargin;

	// Swing stuff
	private JMenuBar jMenuBar = null;
	private JMenu jMenuLevel = null;
	private JMenuItem jMenuItemRestart = null;
	private JMenuItem jMenuItemLevelCode = null;
	private JMenuItem jMenuSelect = null;
	private JMenuItem jMenuItemFullscreen = null;
	private JMenu jMenuPlayer = null;
	private JMenu jMenuSelectPlayer = null;
	private JMenu jMenuSound = null;
	private JMenu jMenuSFX = null;
	private JMenuItem jMenuItemVolume = null;
	private JMenu jMenuOptions = null;
	private JMenuItem jMenuItemCursor = null;
	private JMenuItem jMenuItemClassicalCursor = null;
	private JMenuItem jMenuItemExit = null;
	private JMenuItem jMenuItemLoad = null;
	private JMenuItem jMenuItemReplay = null;
	private JCheckBoxMenuItem jMenuItemMusic = null;
	private JCheckBoxMenuItem jMenuItemSound = null;
	private ButtonGroup playerGroup = null;
	private ButtonGroup zoomGroup = null;

	// action listener for levels
	private java.awt.event.ActionListener lvlListener;

	/**
	 * Constructor of the main frame.
	 */
	Lemmini() {
		try {
			Core.init(this);      // initialize Core object
			GameController.init();
			GameController.setLevelMenuUpdateListener(new LevelMenuUpdateListener());
		} catch (ResourceException ex) {
			Core.resourceError(ex.getMessage());
		} catch (LemmException ex) {
			JOptionPane.showMessageDialog( null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
			System.exit(1);
		} catch (Exception ex) {
			ToolBox.showException(ex);
			System.exit(1);
		} catch (Error ex) {
			ToolBox.showException(ex);
			System.exit(1);
		}

		// gather screen dimension, window height and scale
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int screenWidth = gd.getDisplayMode().getWidth();
		int screenHeight = gd.getDisplayMode().getHeight();
		int drawHeight = Core.getDrawHeight();
		double scale = Core.getScale();

		// adjust game width to fit screen dimensions
		float hRatio = (float)drawHeight / screenHeight;
		int drawWidth = (int)Math.round((float)screenWidth * hRatio);
		if(drawWidth < 672) drawWidth = 672;
		else if(drawWidth > 850) drawWidth = 850;
		Core.setDrawWidth(drawWidth);

		// set internal window dimensions
		int width = (int)Math.round((float)drawWidth * scale);
		int height = (int)Math.round((float)drawHeight * scale);

		// Unfortunately JFrame provides very little control here
		this.setResizable(true);

		//START-MAC this enables the green button on mac
		com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(this,true);
		com.apple.eawt.Application.getApplication().requestToggleFullScreen(this);
		com.apple.eawt.Application.getApplication().setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
		//END-MAC

		// set component pane
		gp = new GraphicsPane();
		gp.setBackground(Color.BLACK);
		gp.setDoubleBuffered(false);

		// get dimensions and set width and height
		gp.setPreferredSize(new Dimension(width, height));

		// create an intermediate panel which allow us to add a border
		final JPanel intermezzo = new JPanel();
		intermezzo.setDoubleBuffered(false);
		intermezzo.setBorder(new MatteBorder( 0, 0, 0, 0, Color.BLACK ) );
		intermezzo.setLayout(new GridLayout(0, 1));
		intermezzo.add(gp);

		// finish window
		this.setContentPane(intermezzo);
		this.pack();
		this.validate(); // force redraw
		this.setTitle("Lemmini");

		// calculate frame size and set min size
		Dimension wholeWindow = this.getSize();
		xMargin = wholeWindow.width - width;
		yMargin = wholeWindow.height - height;
		this.setMinimumSize(new Dimension(drawWidth + xMargin, drawHeight + yMargin));

		// set coords - default to centre of screen
		int posX = Core.programProps.get("framePosX", (screenWidth/2) - (wholeWindow.width/2));
		int posY = Core.programProps.get("framePosY", (screenHeight/2) - (wholeWindow.height/2));
		posX = Math.max(posX, 0);
		posY = Math.max(posY, 0);
		this.setLocation(posX, posY);

		// Add menu bar
		jMenuBar = new JMenuBar();

		// Create File Menu
		if(!System.getProperty("os.name").equals("Mac OS X")) {
			JMenu jMenuFile = new JMenu("File");

			// Exit menu
			jMenuItemExit = new JMenuItem("Exit");
			jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					exit();
				}
			});
			jMenuFile.add(jMenuItemExit);

			// Fullscreen
			jMenuItemFullscreen = new JMenuItem("Fullscreen");
			jMenuItemFullscreen.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					setExtendedState(JFrame.MAXIMIZED_BOTH);
					setUndecorated(true);
				}
			});
			jMenuFile.add(jMenuItemFullscreen);

			jMenuBar.add(jMenuFile);
		}

		// Create Player Menu
		jMenuPlayer = new JMenu("Player");
		JMenuItem jMenuItemManagePlayer = new JMenuItem("Manage Players");
		jMenuItemManagePlayer.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Core.player.store(); // save player in case it is changed
				PlayerDialog d = new PlayerDialog((JFrame)Core.getCmp(), true);
				d.setVisible(true);
				// blocked until dialog returns
				List<String> players = d.getPlayers();
				if (players != null) {
					String player = Core.player.getName(); // old player
					int playerIdx = d.getSelection();
					if (playerIdx != -1)
						player = players.get(playerIdx); // remember selected player
					// check for players to delete
					for (int i=0; i<Core.getPlayerNum(); i++) {
						String p = Core.getPlayer(i);
						if (!players.contains(p)) {
							File f = new File(Core.resourcePath+"players/"+p+".ini");
							f.delete();
							if (p.equals(player))
								player = "default";
						}
					}
					// rebuild players list
					Core.clearPlayers();
					// add default player if missing
					if (!players.contains("default"))
						players.add("default");
					// now copy all player and create properties
					for (int i=0; i<players.size(); i++) {
						Core.addPlayer(players.get(i));
					}

					// select new default player
					Core.player = new Player(player);

					// rebuild players menu
					playerGroup = new ButtonGroup();
					jMenuSelectPlayer.removeAll();
					for (int idx=0; idx < Core.getPlayerNum(); idx++) {
						JCheckBoxMenuItem item = addPlayerItem(Core.getPlayer(idx));
						if ( Core.player.getName().equals(Core.getPlayer(idx)) )
							item.setSelected(true);
					}
					updateLevelMenus();
				}
			}
		});
		jMenuPlayer.add(jMenuItemManagePlayer);
		jMenuSelectPlayer = new JMenu("Select Player");
		playerGroup = new ButtonGroup();
		for (int idx=0; idx < Core.getPlayerNum(); idx++) {
			JCheckBoxMenuItem item = addPlayerItem(Core.getPlayer(idx));
			if ( Core.player.getName().equals(Core.getPlayer(idx)) )
				item.setSelected(true);
		}
		jMenuPlayer.add(jMenuSelectPlayer);
		jMenuBar.add(jMenuPlayer);

		// Create Level Menu
		jMenuLevel = new JMenu("Level");

		// Create action listener
		lvlListener = new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				LvlMenuItem item = (LvlMenuItem)e.getSource();
				GameController.requestChangeLevel(item.levelPack, item.diffLevel, item.level, false);
			}
		};

		// Menu for each item
		diffLevelMenus = new HashMap<String,ArrayList<LvlMenuItem>>(); // store menus to access them later
		for (int lp=1; lp<GameController.getLevelPackNum(); lp++) { // skip dummy level pack
			JMenu jMenuPack = makeLevelPackMenu(lp);
			jMenuLevel.add(jMenuPack);
		}
		jMenuLevel.addSeparator();

		// Restart sub-menu
		jMenuItemRestart = new JMenuItem();
		jMenuItemRestart.setText("Restart Level");
		jMenuItemRestart.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if (!GameController.getLevel().isReady())
					GameController.requestChangeLevel(GameController.getCurLevelPackIdx(), GameController.getCurDiffLevel(), GameController.getCurLevelNumber(), false);
				else
					GameController.requestRestartLevel(false);
			}
		});
		jMenuLevel.add(jMenuItemRestart);

		jMenuItemLoad = new JMenuItem();
		jMenuItemLoad.setText("Add Level Pack");
		jMenuItemLoad.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				addLevelPack();
			}
		});
		jMenuLevel.add(jMenuItemLoad);

		jMenuItemReplay = new JMenuItem();
		jMenuItemReplay.setText("Load Replay");
		jMenuItemReplay.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				String replayPath = ToolBox.getFileName(thisFrame,Core.resourcePath,Core.REPLAY_EXTENSIONS,true);
				if (replayPath != null) {
					try {
						if (ToolBox.getExtension(replayPath).equalsIgnoreCase("rpl")) {
							ReplayLevelInfo rli = GameController.loadReplay(replayPath);
							if (rli != null) {
								int lpn = -1;
								for (int i=0; i<GameController.getLevelPackNum(); i++)
									if (GameController.getLevelPack(i).getName().equals(rli.getLevelPack()))
										lpn = i;
								if (lpn > -1) {
									GameController.requestChangeLevel(lpn,rli.getDiffLevel(), rli.getLvlNumber(), true);
									return; // success
								}
							}
						}
						// else: no success
						JOptionPane.showMessageDialog(Core.getCmp(), "Wrong format!", "Loading replay failed", JOptionPane.INFORMATION_MESSAGE);
					} catch (Exception ex) {
						ToolBox.showException(ex);
					}
				}
			}
		});
		jMenuLevel.add(jMenuItemReplay);

		jMenuItemLevelCode = new JMenuItem("Enter Level Code");
		jMenuItemLevelCode.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				LevelCodeDialog lcd = new LevelCodeDialog((JFrame)Core.getCmp(), true);
				lcd.setVisible(true);
				String levelCode = lcd.getCode();
				int lvlPack = lcd.getLevelPack();
				if (levelCode != null && levelCode.length() != 0 && lvlPack > 0) {

					levelCode = levelCode.trim();
					// cheat mode
					if (levelCode.equals("0xdeadbeef")) {
						JOptionPane.showMessageDialog(Core.getCmp(), "All levels and debug mode enabled", "Cheater!", JOptionPane.INFORMATION_MESSAGE);
						Core.player.enableCheatMode();
						updateLevelMenus();
						return;
					}

					// real level code -> get absolute level
					levelCode = levelCode.toUpperCase();
					LevelPack lpack = GameController.getLevelPack(lvlPack);
					int lvlAbs = LevelCode.getLevel(lpack.getCodeSeed(), levelCode, lpack.getCodeOffset());
					if (lvlAbs != -1) {
						// calculate level pack and relative levelnumber from absolute number
						int l[] = GameController.relLevelNum(lvlPack, lvlAbs);
						int diffLvl = l[0];
						int lvlRel = l[1];
						Core.player.setAvailable(lpack.getName(), lpack.getDiffLevels()[diffLvl], lvlRel);
						GameController.requestChangeLevel(lvlPack, diffLvl, lvlRel, false);
						updateLevelMenus();
						return;
					}
				}
				// not found
				JOptionPane.showMessageDialog(Core.getCmp(), "Invalid Level Code", "Error", JOptionPane.WARNING_MESSAGE);
			}
		});
		jMenuLevel.add(jMenuItemLevelCode);

		// Finish level menu
		jMenuBar.add(jMenuLevel);

		// Create Sound Menu
		jMenuSound = new JMenu();
		jMenuSound.setText("Sound");


		jMenuItemMusic = new JCheckBoxMenuItem("Music", false);
		jMenuItemMusic.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				boolean selected = jMenuItemMusic.isSelected();
				//jMenuItemMusic.setSelected(selected);
				if (selected)
					GameController.setMusicOn(true);
				else
					GameController.setMusicOn(false);
				Core.programProps.set("music", GameController.isMusicOn());
				if (GameController.getLevel() != null) // to be improved: level is running (game state)
					if (GameController.isMusicOn())
						Music.play();
					else
						Music.stop();
			}
		});
		jMenuItemMusic.setSelected(GameController.isMusicOn());
		jMenuSound.add(jMenuItemMusic);

		jMenuItemSound = new JCheckBoxMenuItem("Sound", false);
		jMenuItemSound.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				boolean selected = jMenuItemSound.isSelected();
				if (selected)
					GameController.setSoundOn(true);
				else
					GameController.setSoundOn(false);
				Core.programProps.set("sound", GameController.isSoundOn());
			}
		});
		jMenuItemSound.setSelected(GameController.isSoundOn());
		jMenuSound.add(jMenuItemSound);

		jMenuSFX = new JMenu("SFX Mixer");
		String mixerNames[] = GameController.sound.getMixers();
		ButtonGroup mixerGroup = new ButtonGroup();
		String lastMixerName = Core.programProps.get("mixerName", "Java Sound Audio Engine");

		// special handling of mixer from INI that doesn't exist (any more)
		boolean found = false;
		for (int i=0; i<mixerNames.length; i++) {
			if (mixerNames[i].equals(lastMixerName)) {
				found = true;
				break;
			}
		}
		if (!found)
			lastMixerName = "Java Sound Audio Engine";

		for (int i=0; i<mixerNames.length; i++) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem();
			item.setText(mixerNames[i]);
			item.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					String mixerNames[] = GameController.sound.getMixers();
					String mixerName = e.getActionCommand();
					for (int i=0; i<mixerNames.length; i++) {
						if (mixerNames[i].equals(mixerName)) {
							GameController.sound.setMixer(i);
							Core.programProps.set("mixerName", mixerName);
							break;
						}
					}
				}
			});
			if (mixerNames[i].equals(lastMixerName)) { // default setting
				item.setState(true);
				GameController.sound.setMixer(i);
			}

			jMenuSFX.add(item);
			mixerGroup.add(item);
		}
		jMenuSound.add(jMenuSFX);

		jMenuItemVolume = new JMenuItem("Volume Control");
		jMenuItemVolume.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				GainDialog v = new GainDialog((JFrame)Core.getCmp(), true);
				v.setVisible(true);
			}
		});
		jMenuSound.add(jMenuItemVolume);

		// Finished Sound Menu
		jMenuBar.add(jMenuSound);

		// Create Options Menu
		jMenuOptions = new JMenu();
		jMenuOptions.setText("Options");

		jMenuItemCursor = new JCheckBoxMenuItem("Advanced select", false);
		jMenuItemCursor.addActionListener(new java.awt.event.ActionListener() {
			/* (non-Javadoc)
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				boolean selected = jMenuItemCursor.isSelected();
				if (selected)
					GameController.setAdvancedSelect(true);
				else {
					GameController.setAdvancedSelect(false);
					gp.setCursor(LemmCursor.Type.NORMAL);
				}
				Core.programProps.set("advancedSelect", GameController.isAdvancedSelect());
			}
		});
		jMenuItemCursor.setSelected(GameController.isAdvancedSelect());
		jMenuOptions.add(jMenuItemCursor);

		jMenuItemClassicalCursor = new JCheckBoxMenuItem("Classical Cursor", false);
		jMenuItemClassicalCursor.addActionListener(new java.awt.event.ActionListener() {
			/* (non-Javadoc)
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				boolean selected = jMenuItemClassicalCursor.isSelected();
				if (selected)
					GameController.setClassicalCursor(true);
				else {
					GameController.setClassicalCursor(false);
				}
				Core.programProps.set("classicalCursor", GameController.isClassicalCursor());
			}
		});
		jMenuItemClassicalCursor.setSelected(GameController.isClassicalCursor());
		jMenuOptions.add(jMenuItemClassicalCursor);


		// Finish Options Menu
		jMenuBar.add(jMenuOptions);

		// Finalise menu bar
		this.setJMenuBar(jMenuBar);

		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				exit();
			}

			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				exit();
			}
		});

		this.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent evt) {
				Dimension cur = getContentPane().getSize();

				float scale = (float)cur.width / Core.getDrawWidth();

				Core.setScale(scale);

				int newHeight = (int)Math.round((float)scale * Core.getDrawHeight());

				int margin = 0;
				if(Core.isFullScreen()) {
					margin = Math.max(cur.height - newHeight, 0);
					margin = Math.round(margin / 2);
				} else {
					setSize(new Dimension(cur.width + xMargin, newHeight + yMargin));
				}

				intermezzo.setBorder(new MatteBorder( margin, 0, margin, 0, Color.black ) );
			}
        });

		// all four methods need to be defined even if we're not using them
		//START-MAC
		com.apple.eawt.FullScreenUtilities.addFullScreenListenerTo(this, new FullScreenListener() {
			@Override
			public void windowEnteringFullScreen(AppEvent.FullScreenEvent fse) {
				Lemmini.this.saveProgramProps();
				Core.setFullScreen(true);
			}

			@Override
			public void windowEnteredFullScreen(AppEvent.FullScreenEvent fse) {}

			@Override
			public void windowExitingFullScreen(AppEvent.FullScreenEvent fse) {}

			@Override
			public void windowExitedFullScreen(AppEvent.FullScreenEvent fse) {
				Core.setFullScreen(false);
			}
		});
		//END-MAC

		this.setVisible(true);
		gp.init();
		GameController.setGameState(GameController.State.INTRO);
		GameController.setTransition(GameController.TransitionState.NONE);
		Fader.setBounds(Core.getDrawWidth(), Core.getDrawHeight());
		Fader.setState(Fader.State.IN);
		Thread t = new Thread(gp);

		lvlPath = ".";

		addKeyListener(this);

		t.start();
	}

	/**
	 * Add a menu item for a player.
	 * @param name player name
	 * @return JCheckBoxMenuItem
	 */
	private JCheckBoxMenuItem addPlayerItem(final String name) {
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
		item.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Core.player.store(); // save player in case it is changed
				JMenuItem item = (JMenuItem)e.getSource();
				String player = item.getText();
				Player p = new Player(player);
				Core.player = p; // default player
				item.setSelected(true);
				updateLevelMenus();
			}
		});
		playerGroup.add(item);
		jMenuSelectPlayer.add(item);
		return item;
	}

	/**
	 * Convert String to int.
	 * @param s String with decimal integer value
	 * @return integer value (0 if no valid number)
	 */
	private static int getInt(final String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/**
	 * The main function. Entry point of the program.
	 * @param args
	 */
	public static void main(final String[] args) {
		/*
		 * Set "Look and Feel" to system default
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { /* don't care */}
		/*
		 * Apple menu bar for MacOS
		 */
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		/*
		 * Check JVM version
		 */
		String jreStr = System.getProperty("java.version");
		jreStr = jreStr.replaceFirst("^1\\.", "");
		jreStr = jreStr.replaceFirst("^([0-9]+).*$", "$1");
		int jreVer = Integer.parseInt(jreStr);

		if(jreVer < 5) {
			JOptionPane.showMessageDialog(null,"Run this with JVM >= 1.5","Error",JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		// check free memory
		long free = Runtime.getRuntime().maxMemory();
		if (free < 60*1024*1024) { // 64MB doesn't seem to work even if set with -Xmx64M
			JOptionPane.showMessageDialog(null,"You need at least 64MB of heap","Error",JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		// workaround to adjust time base to 1ms under XP
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6435126
		new Thread() {
			{ this.setDaemon(true); this.start(); }
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(Integer.MAX_VALUE);
					}
					catch(InterruptedException ex) {
					}
				}
			}
		};

		Toolkit.getDefaultToolkit().setDynamicLayout(true);
		thisFrame = new Lemmini();
	}

	/**
	 * Update the level menus according to the progress of the current player.
	 */
	private void updateLevelMenus() {
		// update level menus
		for (int lp=1; lp<GameController.getLevelPackNum(); lp++) { // skip dummy level pack
			LevelPack lPack = GameController.getLevelPack(lp);
			String difficulties[] = lPack.getDiffLevels();
			for (int i=0; i<difficulties.length; i++) {
				// get activated levels for this group
				GroupBitfield bf = Core.player.getBitField(lPack.getName(), difficulties[i]);
				updateLevelMenu(lPack.getName(), difficulties[i], bf);
			}
		}
	}

	/**
	 * Update the level menus according to the given progress information.
	 * @param pack name of level pack
	 * @param diff name of difficulty level
	 * @param bf bitmap containing availability flags for each level
	 */
	private void updateLevelMenu(final String pack, final String diff, final GroupBitfield bf) {
		ArrayList<LvlMenuItem> menuItems = diffLevelMenus.get(LevelPack.getID(pack, diff));
		for (int k=0; k<menuItems.size(); k++) {
			// select level, e.g. "All fall down"
			JMenuItem level = menuItems.get(k);
			if (k == 0 || Core.player.isAvailable(bf, k))
				level.setEnabled(true);
			else
				level.setEnabled(false);
		}

	}


	/**
	 * Development function: patch current level x offset in the level configuration file.
	 * Works only in cheat mode.
	 * @param lvlPath path of level configuration files
	 */
	private void patchLevel(final String lvlPath) {
		try {
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader r = new BufferedReader(new FileReader(lvlPath));
			String l;
			while ( (l = r.readLine()) != null)
				lines.add(l);
			r.close();
			FileWriter sw = new FileWriter(lvlPath);
			for (int i=0; i<lines.size(); i++) {
				String s = lines.get(i);
				if (s.startsWith("xPos =")) {
					sw.write("xPos = "+Integer.toString(GameController.getxPos())+"\n");
				} else
					sw.write(s+"\n");
			}
			sw.close();
		} catch (FileNotFoundException ex) {}
		catch (IOException ex) {}
	}

	/**
	 * Add a level pack taken from a folder
	 */
	private void addLevelPack() {
		// Run a popup for user to give directory
		Path sourceDirectory = ToolBox.getFolderName(this);
		if(sourceDirectory == null) return;

		// Have we added this before?
		Path name = sourceDirectory.getName(sourceDirectory.getNameCount()-1);
		Path targetDirectory = Paths.get(Core.resourcePath + "levels/" + name.toString());
		if(Files.exists(targetDirectory)) {
			JOptionPane.showMessageDialog(Core.getCmp(), "Error!", "Target directory already exists", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// Add the level pack to the live application
		try {
			GameController.addLevelPack(sourceDirectory.toString());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(Core.getCmp(), "Error!", "Failed to load level pack. Bad formatting?", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		// Add to menu
		int lp = GameController.getLevelPackNum() - 1;
		jMenuLevel.insert(makeLevelPackMenu(lp), lp-1);

		//copy source to target using Files Class
		try {
			ToolBox.copyFolder(sourceDirectory, targetDirectory);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(Core.getCmp(), "Error!", "Failed to copy pack to resource directory", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyPressed(final KeyEvent keyevent) {
		int code = keyevent.getKeyCode();
		if (GameController.getGameState() == GameController.State.LEVEL) {
			switch (code) {
				case KeyEvent.VK_1:
				case KeyEvent.VK_F3:
					GameController.handleIconButton(Icons.Type.CLIMB);
					break;
				case KeyEvent.VK_2:
				case KeyEvent.VK_F4:
					GameController.handleIconButton(Icons.Type.FLOAT);
					break;
				case KeyEvent.VK_3:
				case KeyEvent.VK_F5:
					GameController.handleIconButton(Icons.Type.BOMB);
					break;
				case KeyEvent.VK_4:
				case KeyEvent.VK_F6:
					GameController.handleIconButton(Icons.Type.BLOCK);
					break;
				case KeyEvent.VK_5:
				case KeyEvent.VK_F7:
					GameController.handleIconButton(Icons.Type.BUILD);
					break;
				case KeyEvent.VK_6:
				case KeyEvent.VK_F8:
					GameController.handleIconButton(Icons.Type.BASH);
					break;
				case KeyEvent.VK_7:
				case KeyEvent.VK_F9:
					GameController.handleIconButton(Icons.Type.MINE);
					break;
				case KeyEvent.VK_8:
				case KeyEvent.VK_F10:
					GameController.handleIconButton(Icons.Type.DIG);
					break;
				case KeyEvent.VK_D:
					if (GameController.isCheat())
						gp.setDebugDraw(!gp.getDebugDraw());
					break;
				case KeyEvent.VK_W:
					if (GameController.isCheat()) {
						GameController.setNumLeft(GameController.getNumLemmingsMax());
						GameController.endLevel();
					}
					break;
				case KeyEvent.VK_L: // print current level on the console
					if (GameController.isCheat())
						System.out.println(GameController.getLevelPack(GameController.getCurLevelPackIdx()).getInfo(GameController.getCurDiffLevel(), GameController.getCurLevelNumber()).getFileName());
					break;
				case KeyEvent.VK_S: // superlemming on/off
					if (GameController.isCheat())
						GameController.setSuperLemming(!GameController.isSuperLemming());
					else {
						try{
							File file = new File(Core.resourcePath+"/level.png");
							BufferedImage tmp = GameController.getLevel().createMiniMap(null,GameController.getBgImage(), 1, 1, false);
							ImageIO.write(tmp, "png", file);
						} catch (Exception ex) {}
					}
					break;
				case KeyEvent.VK_C:
					if (Core.player.isCheat()) {
						GameController.setCheat(!GameController.isCheat());
						if (GameController.isCheat())
							GameController.setWasCheated(true);
					}
					else
						GameController.setCheat(false);
					break;
				case KeyEvent.VK_F11:
				case KeyEvent.VK_P:
					GameController.setPaused(!GameController.isPaused());
					GameController.pressIcon(Icons.Type.PAUSE);
					break;
				case KeyEvent.VK_F:
				case KeyEvent.VK_ENTER:
					GameController.setFastForward(!GameController.isFastForward());
					GameController.pressIcon(Icons.Type.FFWD);
					break;
				case KeyEvent.VK_X:
					if (GameController.isCheat())
						patchLevel(GameController.getLevelPack(GameController.getCurLevelPackIdx()).getInfo(GameController.getCurDiffLevel(), GameController.getCurLevelNumber()).getFileName());
					break;
				case KeyEvent.VK_RIGHT /*39*/: {
					if (GameController.isAdvancedSelect())
						gp.setCursor(LemmCursor.Type.RIGHT);
					else {
						int xOfsTemp = GameController.getxPos() + ((gp.isShiftPressed()) ? GraphicsPane.X_STEP_FAST : GraphicsPane.X_STEP);
						if (xOfsTemp < Level.WIDTH-this.getWidth())
							GameController.setxPos(xOfsTemp);
						else
							GameController.setxPos(Level.WIDTH-this.getWidth());
					}
					break; }
				case KeyEvent.VK_LEFT /*37*/: {
					if (GameController.isAdvancedSelect())
						gp.setCursor(LemmCursor.Type.LEFT);
					else {
						int xOfsTemp = GameController.getxPos() - ((gp.isShiftPressed()) ? GraphicsPane.X_STEP_FAST : GraphicsPane.X_STEP);
						if (xOfsTemp > 0)
							GameController.setxPos(xOfsTemp);
						else
							GameController.setxPos(0);
					}
					break; }
				case KeyEvent.VK_UP: {
					gp.setCursor(LemmCursor.Type.WALKER);
					break;}
				case KeyEvent.VK_SHIFT:
					gp.setShiftPressed(true);
					break;
				case KeyEvent.VK_SPACE:
					if (GameController.isCheat()) {
						Lemming l = new Lemming(gp.getCursorX(), gp.getCursorY());
						synchronized (GameController.getLemmings()) {
							GameController.getLemmings().add(l);
						}
					}
					break;
				case KeyEvent.VK_PLUS:
				case KeyEvent.VK_ADD:
				case KeyEvent.VK_F2:
					GameController.pressPlus(GameController.KEYREPEAT_KEY);
					break;
				case KeyEvent.VK_MINUS:
				case KeyEvent.VK_SUBTRACT:
				case KeyEvent.VK_F1:
					GameController.pressMinus(GameController.KEYREPEAT_KEY);
					break;
				case KeyEvent.VK_F12:
					GameController.handleIconButton(Icons.Type.NUKE);
					break;
				case KeyEvent.VK_ESCAPE:
					if (!GameController.getLevel().isReady())
						GameController.requestChangeLevel(GameController.getCurLevelPackIdx(), GameController.getCurDiffLevel(), GameController.getCurLevelNumber(), false);
					else
						GameController.requestRestartLevel(false);
					break;

			}
			keyevent.consume();
		}
		//System.out.println(keyevent.getKeyCode());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyReleased(final KeyEvent keyevent) {
		int code = keyevent.getKeyCode();
		if (GameController.getGameState() == GameController.State.LEVEL) {
			switch (code) {
				case KeyEvent.VK_SHIFT:
					gp.setShiftPressed(false);
					break;
				case KeyEvent.VK_PLUS:
				case KeyEvent.VK_ADD:
				case KeyEvent.VK_F2:
					GameController.releasePlus(GameController.KEYREPEAT_KEY);
					break;
				case KeyEvent.VK_MINUS:
				case KeyEvent.VK_SUBTRACT:
				case KeyEvent.VK_F1:
					GameController.releaseMinus(GameController.KEYREPEAT_KEY);
					break;
				case KeyEvent.VK_F12:
					GameController.releaseIcon(Icons.Type.NUKE);
					break;
				case KeyEvent.VK_LEFT:
					if (LemmCursor.getType() == LemmCursor.Type.LEFT)
						gp.setCursor(LemmCursor.Type.NORMAL);
					break;
				case KeyEvent.VK_RIGHT:
					if (LemmCursor.getType() == LemmCursor.Type.RIGHT)
						gp.setCursor(LemmCursor.Type.NORMAL);
					break;
				case KeyEvent.VK_UP:
					if (LemmCursor.getType() == LemmCursor.Type.WALKER)
						gp.setCursor(LemmCursor.Type.NORMAL);
					break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyTyped(final KeyEvent keyevent) {
	}

	/**
	 * Store program properties.
	 */
	public void saveProgramProps() {
		// store frame pos
		Point p = this.getLocation();
		Core.programProps.set("framePosX", p.x);
		Core.programProps.set("framePosY", p.y);

		// scale
		double s = Core.getScale();
		Core.programProps.set("scale", s);

		Core.programProps.save();
	}

	/**
	 * Common exit method to use in exit events.
	 */
	private void exit() {
		if(!Core.isFullScreen()) {
			// don't record window position when in full screen
			this.saveProgramProps();
		}
		// remember  player status
		Core.savePlayerProps();

		// exit
		System.exit(0);
	}

	/**
	 * Make the Level Pack Menu
	 */
	private JMenu makeLevelPackMenu(int lp) {
		LevelPack lPack = GameController.getLevelPack(lp);
		JMenu jMenuPack = new JMenu(lPack.getName());
		String difficulties[] = lPack.getDiffLevels();
		for (int i=0; i<difficulties.length; i++) {
			// get activated levels for this group
			GroupBitfield bf = Core.player.getBitField(lPack.getName(), difficulties[i]);
			String names[] = lPack.getLevels(i);
			JMenu jMenuDiff = new JMenu(difficulties[i]);
			// store menus to access them later
			ArrayList<LvlMenuItem> menuItems = new ArrayList<LvlMenuItem>();
			for (int n=0; n<names.length; n++) {
				LvlMenuItem jMenuLvl = new LvlMenuItem(names[n],lp,i,n);
				jMenuLvl.addActionListener(lvlListener);
				if (Core.player.isAvailable(bf, n))
					jMenuLvl.setEnabled(true);
				else
					jMenuLvl.setEnabled(false);
				jMenuDiff.add(jMenuLvl);
				menuItems.add(jMenuLvl);
			}
			jMenuPack.add(jMenuDiff);
			// store menus to access them later
			diffLevelMenus.put(LevelPack.getID(lPack.getName(), difficulties[i]), menuItems);
		}
		return jMenuPack;
	}

	/**
	 * Listener to inform the GUI of the player's progress.
	 * @author Volker Oth
	 */
	class LevelMenuUpdateListener implements UpdateListener {
		/* (non-Javadoc)
		 * @see Game.UpdateListener#update()
		 */
		@Override
		public void update() {
			if (GameController.getCurLevelPackIdx()!=0) { // 0 is the dummy pack
				LevelPack lvlPack = GameController.getLevelPack(GameController.getCurLevelPackIdx());
				String pack = lvlPack.getName();
				String diff = lvlPack.getDiffLevels()[GameController.getCurDiffLevel()];
				// get next level
				int num = GameController.getCurLevelNumber() + 1;
				if ( num >= lvlPack.getLevels(GameController.getCurDiffLevel()).length )
					num = GameController.getCurLevelNumber();
				// set next level as available
				GroupBitfield bf = Core.player.setAvailable(pack, diff, num);
				// update the menu
				updateLevelMenu(pack, diff, bf);
			}
		}
	}


	/**
	 * Specialized menu item for level selection menus.
	 * @author Volker Oth
	 */
	class LvlMenuItem extends JMenuItem {
		private final static long serialVersionUID = 0x01;

		/** index of level pack */
		int levelPack;
		/** index of difficulty level */
		int diffLevel;
		/** level number */
		int level;

		/**
		 * Constructor
		 * @param text level name
		 * @param pack index level pack
		 * @param diff index of difficulty level
		 * @param lvl level number
		 */
		LvlMenuItem(final String text, final int pack, final int diff, final int lvl) {
			super(text);
			levelPack = pack;
			diffLevel = diff;
			level = lvl;
		}
	}
}

/**
 * A graphics panel in which the actual game contents is displayed.
 * @author Volker Oth
 */
class GraphicsPane extends JPanel implements Runnable, MouseListener, MouseMotionListener {
	/** step size in pixels for horizontal scrolling */
	final static int X_STEP = 4;
	/** step size in pixels for fast horizontal scrolling */
	final static int X_STEP_FAST = 8;
	/** size of auto scrolling range in pixels (from the left and right border) */
	final static int AUTOSCROLL_RANGE = 20;
	/** y coordinate of score display in pixels */
	final static int scoreY = Level.HEIGHT;
	/** y coordinate of counter displays in pixels */
	final static int counterY = scoreY+40;
	/** y coordinate of icons in pixels */
	final static int iconsY = counterY+14;
	/** x coordinate of minimap in pixels */
	final static int smallX = Core.getDrawWidth() - 208 - 4;
	/** y coordinate of minimap in pixels */
	final static int smallY = iconsY;

	private final static long serialVersionUID = 0x01;

	/** start position of mouse drag (for mouse scrolling) */
	private int mouseDragStartX;
	/** x position of cursor in level */
	private int xMouse;
	/** x position of cursor on screen */
	private int xMouseScreen;
	/** y position of cursor in level */
	private int yMouse;
	/** y position of cursor on screen */
	private int yMouseScreen;
	/** mouse drag length in x direction (pixels) */
	private int mouseDx;
	/** mouse drag length in y direction (pixels) */
	private int mouseDy;
	/** flag: Shift key is pressed */
	private boolean shiftPressed;
	/** flag: left mouse button is currently pressed */
	private boolean leftMousePressed;
	/** flag: debug draw is active */
	private boolean draw;
	/** image for information string display */
	private BufferedImage outStrImg;
	/** graphics object for information string display */
	private Graphics2D outStrGfx;
	/** array of offscreen images (one is active, one is passive) */
	private BufferedImage offImage[];
	/** graphics objects for the two offscreen images */
	private Graphics2D offGraphics[];
	/** index of the active buffer in the image buffer */
	private int activeBuffer;
	/** monitoring object used for synchronized painting */
	private Object paintSemaphore;

	/**
	 * Constructor.
	 */
	public GraphicsPane() {
		super();

		paintSemaphore = new Object();
		this.requestFocus();
		this.setCursor(LemmCursor.getCursor());
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
	}

	/**
	 * Set cursor type.
	 * @param c Cursor
	 */
	public void setCursor(final LemmCursor.Type c) {
		LemmCursor.setType(c);
		this.setCursor(LemmCursor.getCursor());
	}

	/**
	 * Show/hide Mouse cursor.
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
		//super.paint(iconGfx);
		synchronized (paintSemaphore) {
			if (offImage != null) {
				int w = Core.getDrawWidth();
				int h = Core.getDrawHeight();
				double scale = Core.getScale();
				//g.drawImage(offImage[activeBuffer],0,0,null);
				g.drawImage(offImage[activeBuffer],0,0,(int)Math.round(w*scale),(int)Math.round(h*scale),0,0,w,h,null);
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#update(java.awt.Graphics)
	 */
	@Override
	public void update(final Graphics g) {
		//super.update(iconGfx);
		synchronized (paintSemaphore) {
			if (offImage != null) {
				int w = Core.getDrawWidth();
				int h = Core.getDrawHeight();
				double scale = Core.getScale();
				//g.drawImage(offImage[activeBuffer],0,0,null);
				g.drawImage(offImage[activeBuffer],0,0,(int)Math.round(w*scale),(int)Math.round(h*scale),0,0,w,h,null);
			}
		}
	}

	/**
	 * Initialization.
	 */
	public void init() {
		synchronized (paintSemaphore) {
			int w = Core.getDrawWidth();
			int h = Core.getDrawHeight();
			double scale = Core.getScale();

			this.setSize((int)Math.round(scale*w), (int)Math.round(scale*h));

			offImage = new BufferedImage[2];
			offGraphics = new Graphics2D[2];
			offImage[0] = ToolBox.createImage(w, h, Transparency.OPAQUE);
			offImage[1] = ToolBox.createImage(w, h, Transparency.OPAQUE);
			offGraphics[0] = offImage[0].createGraphics();
			offGraphics[1] = offImage[1].createGraphics();

			outStrImg = ToolBox.createImage(w, LemmFont.getHeight(), Transparency.BITMASK);
			outStrGfx = outStrImg.createGraphics();
			outStrGfx.setBackground(new Color(0,0,0));
			outStrGfx.setClip(0, 0, w, LemmFont.getHeight());

			TextScreen.init(w, (int)Math.round(this.getHeight()/scale));
			shiftPressed = false;
		}
	}

	/**
	 * Delete offImage to avoid redraw and force init.
	 */
	public void shutdown() {
		synchronized (paintSemaphore) {
			offImage = null;
		}
	}

	/**
	 * redraw the offscreen image, then flip buffers and force repaint.
	 */
	private void redraw() {
		double scale = Core.getScale();
		int drawBuffer;
		Graphics2D offGfx;

		synchronized (paintSemaphore) {
			//if (offImage == null)
			//	init();
			drawBuffer = (activeBuffer == 0) ? 1:0;
			offGfx = offGraphics[drawBuffer];

			BufferedImage bgImage = GameController.getBgImage();
			switch (GameController.getGameState()) {
				case INTRO:
					TextScreen.setMode(TextScreen.Mode.INTRO);
					TextScreen.update();
					offGfx.drawImage(TextScreen.getScreen(), 0,0,null);
					//offGfx.drawImage(LemmCursor.getImage(LemmCursor.TYPE_NORMAL), LemmCursor.x, LemmCursor.y, null);
					break;
				case BRIEFING:
					TextScreen.setMode(TextScreen.Mode.BRIEFING);
					TextScreen.update();
					offGfx.drawImage(TextScreen.getScreen(), 0,0,null);
					//offGfx.drawImage(LemmCursor.getImage(LemmCursor.TYPE_NORMAL), LemmCursor.x, LemmCursor.y, null);
					break;
				case DEBRIEFING:
					TextScreen.setMode(TextScreen.Mode.DEBRIEFING);
					TextScreen.update();
					offGfx.drawImage(TextScreen.getScreen(), 0,0,null);
					TextScreen.getDialog().handleMouseMove((int)Math.round(xMouseScreen/scale), (int)Math.round(yMouseScreen/scale));
					//offGfx.drawImage(LemmCursor.getImage(LemmCursor.TYPE_NORMAL), LemmCursor.x, LemmCursor.y, null);
					break;
				case LEVEL:
				case LEVEL_END:
					if (bgImage != null) {
						GameController.update();
						// mouse movement
						if (yMouseScreen > 40 && yMouseScreen < scoreY*scale) { // avoid scrolling if menu is selected
							int xOfsTemp;
							if (xMouseScreen > this.getWidth() - AUTOSCROLL_RANGE*scale) {
								xOfsTemp = GameController.getxPos() + ((shiftPressed) ? X_STEP_FAST : X_STEP);
								if (xOfsTemp < Level.WIDTH-this.getWidth()/scale)
									GameController.setxPos(xOfsTemp);
								else
									GameController.setxPos((int)Math.round(Level.WIDTH-this.getWidth()/scale));
							} else if (xMouseScreen < AUTOSCROLL_RANGE*scale) {
								xOfsTemp = GameController.getxPos() - ((shiftPressed) ? X_STEP_FAST : X_STEP);
								if (xOfsTemp > 0)
									GameController.setxPos(xOfsTemp);
								else
									GameController.setxPos(0);
							}
						}
						// store local copy of xOfs to avoid sync problems with AWT threads
						// (scrolling by dragging changes xOfs as well)
						int xOfsTemp = GameController.getxPos();

						//timeBaseRedraw +=GameController.timePerFrame;
						int w = this.getWidth();
						int h = Level.HEIGHT;
						if (h>this.getHeight())
							h = this.getHeight();

						Level level = GameController.getLevel();
						if (level != null) {

							// clear screen
							offGfx.setClip(0,0,w,h);
							offGfx.setBackground(level.getBgColor());
							offGfx.clearRect(0, 0, w, h);

							// draw "behind" objects
							GameController.getLevel().drawBehindObjects(offGfx, w, xOfsTemp);

							// draw background
							offGfx.drawImage(bgImage, 0, 0, w, h, xOfsTemp, 0, xOfsTemp+w, h, this);

							// draw "in front" objects
							GameController.getLevel().drawInFrontObjects(offGfx, w, xOfsTemp);
						}
						// clear parts of the screen for menu etc.
						offGfx.setClip(0,Level.HEIGHT,w,this.getHeight());
						offGfx.setBackground(Color.BLACK);
						offGfx.clearRect(0,scoreY,w,this.getHeight());
						// draw counter, icons, small level pic
						// draw menu
						//Icons icons = GameController.getIcons();
						GameController.drawIcons(offGfx, 0, iconsY);
						offGfx.drawImage(MiscGfx.getImage(MiscGfx.Index.BORDER), smallX-4, smallY-4, null);
						MiniMap.draw(offGfx, smallX, smallY, xOfsTemp);
						// draw counters
						GameController.drawCounters(offGfx,counterY);

						// draw lemmings
						offGfx.setClip(0,0,w,h);
						GameController.getLemmsUnderCursor().clear();
						List<Lemming> lemmings = GameController.getLemmings();
						synchronized (GameController.getLemmings()) {
							Iterator<Lemming> it = lemmings.iterator();
							while (it.hasNext()) {
								Lemming l = it.next();
								int lx = l.screenX();
								int ly = l.screenY();
								int mx = l.midX()-16;
								if (lx+l.width() > xOfsTemp && lx < xOfsTemp+w) {
									offGfx.drawImage(l.getImage(),lx-xOfsTemp,ly,null);
									if (LemmCursor.doesCollide(l, xOfsTemp)) {
										GameController.getLemmsUnderCursor().add(l);
									}
									BufferedImage cd = l.getCountdown();
									if (cd!=null)
										offGfx.drawImage(cd,mx-xOfsTemp,ly-cd.getHeight(),null);

									BufferedImage sel = l.getSelectImg();
									if (sel!=null)
										offGfx.drawImage(sel,mx-xOfsTemp,ly-sel.getHeight(),null);

								}
							}
							// draw pixels in mini map
							offGfx.setClip(0,0,w,this.getHeight());
							it = lemmings.iterator();
							while (it.hasNext()) {
								Lemming l = it.next();
								int lx = l.screenX();
								int ly = l.screenY();
								// draw pixel in mini map
								MiniMap.drawLemming(offGfx,  lx, ly);
							}
						}
						Lemming lemmUnderCursor = GameController.lemmUnderCursor(LemmCursor.getType());
						offGfx.setClip(0,0,w,h);
						// draw explosions
						GameController.drawExplosions(offGfx,offImage[0].getWidth(), Level.HEIGHT, xOfsTemp);
						offGfx.setClip(0,0,w,this.getHeight());

						// draw info string
						outStrGfx.clearRect(0, 0, outStrImg.getWidth(), outStrImg.getHeight());
						if (GameController.isCheat()) {
							Stencil stencil = GameController.getStencil();
							if (stencil != null) {
								int stencilVal = stencil.get(xMouse+yMouse*Level.WIDTH);
								String test = "x: "+xMouse+", y: "+yMouse+", mask: "+(stencilVal&0xffff)+" "+Stencil.getObjectID(stencilVal);
								LemmFont.strImage(outStrGfx, test);
								offGfx.drawImage(outStrImg,4,Level.HEIGHT+8,null);
							}
						} else {
							StringBuffer sb = new StringBuffer();
							sb.append("OUT ");
							String s = Integer.toString(GameController.getLemmings().size());
							sb.append(s);
							if (s.length()==1)
								sb.append(" ");
							sb.append("  IN ");
							s = Integer.toString(GameController.getNumLeft()*100/GameController.getNumLemmingsMax());
							if (s.length()==1)
								sb.append(" ");
							sb.append(s);
							sb.append("%  TIME ").append(GameController.getTimeString());
							LemmFont.strImageRight(outStrGfx, sb.toString(), 4);

							if (lemmUnderCursor != null) {
								String n = lemmUnderCursor.getName();
								// display also the total number of lemmings under the cursor
								int num = GameController.getLemmsUnderCursor().size();
								if (num > 1)
									n = n + " " + Integer.toString(num);
								LemmFont.strImageLeft(outStrGfx, n, 4);
							}

							offGfx.drawImage(outStrImg,0,Level.HEIGHT+8,null);
						}
						// replay icon
						BufferedImage replayImage = GameController.getReplayImage();
						if (replayImage != null)
							offGfx.drawImage(replayImage,this.getWidth()-2*replayImage.getWidth(),replayImage.getHeight(),null);
						// draw cursor
						if (lemmUnderCursor != null) {
							int lx,ly;
							if (GameController.isClassicalCursor()) {
	                            lx = (int)Math.round(xMouseScreen/scale);
	                            ly = (int)Math.round(yMouseScreen/scale);
	                            enableCursor(false);
							} else {
								lx = lemmUnderCursor.midX()-xOfsTemp;
								ly = lemmUnderCursor.midY();
							}
							BufferedImage cursorImg = LemmCursor.getBoxImage();
							lx -= cursorImg.getWidth()/2;
							ly -= cursorImg.getHeight()/2;
							offGfx.drawImage(cursorImg,lx,ly,null);
						} else if (LemmCursor.getEnabled() == false)
							enableCursor(true);
					}
			}

			// fader
			GameController.fade(offGfx);
			// and all onto screen
			activeBuffer = drawBuffer;

			repaint();
		}

	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY+1);
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
							for (int f=0; f<GameController.FAST_FWD_MULTI-1;f++)
								GameController.update();
						else if (GameController.isSuperLemming())
							for (int f=0; f<GameController.SUPERLEMM_MULTI-1;f++)
								GameController.update();
					}
				} else {
					try {
						// determine time until next frame
						long diff = GameController.MICROSEC_PER_FRAME - timerRepaint.delta();
						if (diff > GameController.MICROSEC_RESYNC) {
							timerRepaint.update(); // resync to time base
							System.out.println("Resynced, diff was "+(diff/1000)+" millis");
						} else if (diff > Lemmini.THR_SLEEP*1000)
							Thread.sleep(Lemmini.MIN_SLEEP);
					} catch (InterruptedException ex) {}
				}
			}
		} catch (Exception ex) {
			ToolBox.showException(ex);
			System.exit(1);
		}  catch (Error ex) {
			ToolBox.showException(ex);
			System.exit(1);
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased(final MouseEvent mouseevent) {
		double scale = Core.getScale();
		int x = (int)Math.round(mouseevent.getX()/scale);
		int y = (int)Math.round(mouseevent.getY()/scale);
		mouseDx = 0;
		mouseDy = 0;
		if (mouseevent.getButton() == MouseEvent.BUTTON1)
			leftMousePressed = false;

		switch (GameController.getGameState()) {
			case LEVEL:
				if (y > iconsY && y < iconsY+Icons.HEIGHT) {
					Icons.Type type = GameController.getIconType(x);
					if (type != Icons.Type.INVALID)
						GameController.releaseIcon(type);
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
	public void mouseClicked(final MouseEvent mouseevent) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed(final MouseEvent mouseevent) {
		double scale = Core.getScale();
		int x = (int)Math.round(mouseevent.getX()/scale);
		int y = (int)Math.round(mouseevent.getY()/scale);
		mouseDx = 0;
		mouseDy = 0;
		if (mouseevent.getButton() == MouseEvent.BUTTON1)
			leftMousePressed = true;

		if (Fader.getState() != Fader.State.OFF)
			return;

		switch (GameController.getGameState()) {
			case BRIEFING:
				MiniMap.init(smallX, smallY, 16, 8, true);
				GameController.setTransition(GameController.TransitionState.TO_LEVEL);
				Fader.setState(Fader.State.OUT);
				mouseevent.consume();
				break;
			case DEBRIEFING:
				int button = TextScreen.getDialog().handleLeftClick(x,y);
				switch (button) {
					case TextScreen.BUTTON_CONTINUE:
						GameController.nextLevel(); // continue to next level
						GameController.requestChangeLevel(GameController.getCurLevelPackIdx(), GameController.getCurDiffLevel(),
								GameController.getCurLevelNumber(), false);
						break;
					case TextScreen.BUTTON_RESTART:
						GameController.requestRestartLevel(false);
						break;
					case TextScreen.BUTTON_MENU:
						GameController.setTransition(GameController.TransitionState.TO_INTRO);
						Fader.setState(Fader.State.OUT);
						((JFrame)Core.getCmp()).setTitle("Lemmini");
						break;
					case TextScreen.BUTTON_REPLAY:
						GameController.requestRestartLevel(true);
						break;
					case TextScreen.BUTTON_SAVEREPLAY:
						String replayPath = ToolBox.getFileName(Lemmini.thisFrame,Core.resourcePath,Core.REPLAY_EXTENSIONS,false);
						if (replayPath != null) {
							try {
								String ext = ToolBox.getExtension(replayPath);
								if (ext == null)
									replayPath += ".rpl";
								if (GameController.saveReplay(replayPath))
									return;
								// else: no success
								JOptionPane.showMessageDialog(Core.getCmp(), "Error!", "Saving replay failed", JOptionPane.INFORMATION_MESSAGE);
							} catch (Exception ex) {
								ToolBox.showException(ex);
							}
						}
						break;
				}
				mouseevent.consume();
				break;
			case LEVEL:
				//  debug drawing
				debugDraw(x,y,leftMousePressed);
				if (leftMousePressed) {
					if (y > iconsY && y < iconsY+Icons.HEIGHT) {
						Icons.Type type = GameController.getIconType(x);
						if (type != Icons.Type.INVALID) {
							GameController.handleIconButton(type);
						}
					} else {
						Lemming l = GameController.lemmUnderCursor(LemmCursor.getType());
						if (l != null)
							GameController.requestSkill(l);
					}
					// check minimap mouse move
					int ofs = MiniMap.move(x,y,(int)Math.round(this.getWidth()/scale));
					if (ofs != -1)
						GameController.setxPos(ofs);
					mouseevent.consume();
				}
		}
	}

	/**
	 * Debug routine to draw terrain pixels in stencil and background image.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 * @param doDraw true: draw, false: erase
	 */
	private void debugDraw(final int x, final int y, final boolean doDraw) {
		if (draw && GameController.isCheat()) {
			int rgbVal = (doDraw) ? 0xffffffff : 0x0;
			int maskVal = (doDraw) ? Stencil.MSK_BRICK : Stencil.MSK_EMPTY;
			int xOfs = GameController.getxPos();
			if (x+xOfs > 0 && x+xOfs<Level.WIDTH-1 && y >0 && y < Level.HEIGHT-1) {
				GameController.getBgImage().setRGB(x+xOfs, y, rgbVal);
				GameController.getStencil().set(x+xOfs, y, maskVal);
				GameController.getBgImage().setRGB(x+xOfs+1, y, rgbVal);
				GameController.getStencil().set(x+xOfs+1, y, maskVal);
				GameController.getBgImage().setRGB(x+xOfs, y+1, rgbVal);
				GameController.getStencil().set(x+xOfs, y+1, maskVal);
				GameController.getBgImage().setRGB(x+xOfs+1, y+1, rgbVal);
				GameController.getStencil().set(x+xOfs+1, y+1, maskVal);
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseEntered(final MouseEvent mouseevent) {
		double scale = Core.getScale();
		mouseDx = 0;
		mouseDy = 0;
		int x = (int)Math.round(mouseevent.getX()/scale/*-LemmCursor.width/2*/);
		int y = (int)Math.round(mouseevent.getY()/scale/*-LemmCursor.height/2*/);
		LemmCursor.setX(x/*-LemmCursor.width/2*/);
		LemmCursor.setY(y/*-LemmCursor.height/2*/);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseExited(final MouseEvent mouseevent) {
		double scale = Core.getScale();
		int x = xMouseScreen + mouseDx;
		switch (GameController.getGameState()) {
			case BRIEFING:
			case DEBRIEFING:
			case LEVEL:
				if (x>=this.getWidth())
					x = this.getWidth()-1;
				if (x<0)
					x = 0;
				xMouseScreen = x;
				x += GameController.getxPos()*scale;
				if (x>=Level.WIDTH)
					x = Level.WIDTH-1;
				xMouse = x;
				LemmCursor.setX((int)Math.round(xMouseScreen/scale/*-LemmCursor.width/2*/));

				int y = yMouseScreen + mouseDy;
				if (y >= this.getHeight())
					y = this.getHeight()-1;
				if (y<0)
					y = 0;
				yMouseScreen = y;

				y = yMouse + mouseDy;
				if (y >= Level.HEIGHT)
					y = Level.HEIGHT-1;
				if (y<0)
					y = 0;
				yMouse = y;
				LemmCursor.setY((int)Math.round(yMouseScreen/scale/*-LemmCursor.height/2*/));
				mouseevent.consume();
				break;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseDragged(final MouseEvent mouseevent) {
		double scale = Core.getScale();
		mouseDx = 0;
		mouseDy = 0;
		// check minimap mouse move
		switch (GameController.getGameState()) {
			case LEVEL:
				int x = (int)Math.round(mouseevent.getX()/scale);
				int y = (int)Math.round(mouseevent.getY()/scale);
				if (leftMousePressed) {
					int ofs = MiniMap.move(x,y,(int)Math.round(this.getWidth()/scale));
					if (ofs != -1)
						GameController.setxPos(ofs);
				} else {
					int xOfsTemp = GameController.getxPos() + (x-mouseDragStartX);
					if (xOfsTemp < 0)
						xOfsTemp = 0;
					else if (xOfsTemp >= Level.WIDTH-this.getWidth()/scale)
						GameController.setxPos((int)Math.round(Level.WIDTH-this.getWidth()/scale));
					else GameController.setxPos(xOfsTemp);
				}
				// debug drawing
				debugDraw(x,y,leftMousePressed);
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
		//long t = System.currentTimeMillis();
		double scale = Core.getScale();
		int x,y;
		int oldX = xMouse;
		int oldY = yMouse;

		x = (int)Math.round((mouseevent.getX()/scale + GameController.getxPos()));
		y = (int)Math.round(mouseevent.getY()/scale);
		if (x>=Level.WIDTH)
			x = Level.WIDTH-1;
		if (y >= Level.HEIGHT)
			y = Level.HEIGHT-1;
		xMouse = (int)Math.round(x*scale);
		yMouse = (int)Math.round(y*scale);
		// LemmCursor
		xMouseScreen = mouseevent.getX();
		if (xMouseScreen>=this.getWidth())
			xMouseScreen = this.getWidth();
		else if (xMouseScreen <0)
			xMouseScreen = 0;
		yMouseScreen = mouseevent.getY();
		if (yMouseScreen>=this.getHeight())
			yMouseScreen = this.getHeight();
		else if (yMouseScreen <0)
			yMouseScreen = 0;
		LemmCursor.setX((int)Math.round(xMouseScreen/scale/*-LemmCursor.width/2*/));
		LemmCursor.setY((int)Math.round(yMouseScreen/scale/*-LemmCursor.height/2*/));

		switch (GameController.getGameState()) {
			case INTRO:
			case BRIEFING:
			case DEBRIEFING:
				TextScreen.getDialog().handleMouseMove((int)Math.round(xMouseScreen/scale), (int)Math.round(yMouseScreen/scale));
				//$FALL-THROUGH$
			case LEVEL:
				mouseDx = (xMouse - oldX);
				mouseDy = (yMouse - oldY);
				mouseDragStartX = (int)Math.round(mouseevent.getX()/scale);
				mouseevent.consume();
				break;
		}
	}

	/**
	 * Get cursor x position in pixels.
	 * @return cursor x position in pixels
	 */
	int getCursorX() {
		return xMouse;
	}

	/**
	 * Get cursor y position in pixels.
	 * @return cursor y position in pixels
	 */
	int getCursorY() {
		return yMouse;
	}

	/**
	 * Get flag: Shift key is pressed?
	 * @return true if Shift key is pressed, false otherwise
	 */
	boolean isShiftPressed() {
		return shiftPressed;
	}

	/**
	 * Set flag: Shift key is pressed.
	 * @param p true: Shift key is pressed,false otherwise
	 */
	void setShiftPressed(final boolean p) {
		shiftPressed = p;
	}

	/**
	 * Get state of debug draw option.
	 * @return true: debug draw is active, false otherwise
	 */
	boolean getDebugDraw() {
		return draw;
	}

	/**
	 * Set state of debug draw option.
	 * @param d true: debug draw is active, false otherwise
	 */
	void setDebugDraw(final boolean d) {
		draw = d;
	}
}
