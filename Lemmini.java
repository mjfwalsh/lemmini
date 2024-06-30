import GUI.GainDialog;
import GUI.LevelCodeDialog;
import GUI.PlayerDialog;
import Game.Core;
import Game.GameController;
import Game.GraphicsPane;
import Game.GroupBitfield;
import Game.Icons;
import Game.LemmCursor;
import Game.LemmException;
import Game.Lemming;
import Game.Level;
import Game.LevelCode;
import Game.LevelPack;
import Game.Music;
import Game.Player;
import Game.ReplayLevelInfo;
import Game.ResourceException;
import Game.UpdateListener;
import GameUtil.Fader;
import Tools.ToolBox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;

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
 * Lemmini - a game engine for Lemmings. This is the main window including input handling. The game
 * logic is located in {@link GameController}, some core components are in {@link Core}.
 *
 * <p>Note: this was developed for JRE1.4 and only ported to JRE1.5 after it was finished. Also the
 * design evolved during two years of development and thus isn't nearly as clean as it should be.
 * During the porting to 1.5, I cleaned up some things here and there, but didn't want to redesign
 * the whole thing from scratch.
 *
 * @author Volker Oth
 */

/**
 * I've been working with Jave 1.7 and have increased the minimum version accordingly.
 *
 * <p>Michael J. Walsh
 */
public class Lemmini extends JFrame implements KeyListener {
  private static final long serialVersionUID = 0x01;

  // self reference
  static Lemmini thisFrame;

  // HashMap to store menu items for difficulty levels
  private HashMap<String, ArrayList<LvlMenuItem>> diffLevelMenus;
  // panel for the game graphics
  private GraphicsPane gp;
  // graphics device
  private final GraphicsDevice gd;

  // store some lengths
  private int screenWidth;
  private int screenHeight;
  private int xMargin;
  private int yMargin;
  private float maxScale;
  private Dimension oldSize;
  private double windowRatio;

  // Menu stuff
  private JMenuBar jMenuBar;
  private JMenu jMenuLevel;
  private JMenu jMenuSelectPlayer;
  private ButtonGroup playerGroup;
  private JCheckBoxMenuItem jMenuItemMusic;
  private JCheckBoxMenuItem jMenuItemSound;
  private JCheckBoxMenuItem jMenuItemCursor;
  private JMenu jMenuZoom;
  private JMenuItem jMenuItemFullscreen;
  private JMenuItem jMenuItemManagePlayer;
  private JMenuItem jMenuItemLoad;
  private JMenuItem jMenuItemReplay;
  private JMenuItem jMenuItemLevelCode;
  private JMenuItem jMenuItemVolume;

  // action listener for levels
  private java.awt.event.ActionListener lvlListener;

  /** Constructor of the main frame. */
  Lemmini() {
    try {
      Core.init(this); // initialize Core object
      GameController.init();
      GameController.setLevelMenuUpdateListener(new LevelMenuUpdateListener());
    } catch (ResourceException ex) {
      Core.resourceError(ex.getMessage());
    } catch (LemmException ex) {
      JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    } catch (Exception ex) {
      ToolBox.showException(ex);
      System.exit(1);
    } catch (Error ex) {
      ToolBox.showException(ex);
      System.exit(1);
    }

    // gather screen dimension, window height and scale
    gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    screenWidth = gd.getDisplayMode().getWidth();
    screenHeight = gd.getDisplayMode().getHeight();
    int drawHeight = Core.getDrawHeight();
    double scale = Core.getScale();

    // adjust game width to fit screen dimensions
    float hRatio = (float) drawHeight / screenHeight;
    int drawWidth = (int) ((float) screenWidth * hRatio);
    if (drawWidth < 672) drawWidth = 672;
    else if (drawWidth > 850) drawWidth = 850;
    Core.setDrawWidth(drawWidth);

    // Calculate max scale
    float maxXScale = (float) screenWidth / drawWidth;
    float maxYScale = (float) screenHeight / drawHeight;
    maxScale = Math.min(maxXScale, maxYScale);
    if (scale > maxScale) {
      scale = maxScale;
      Core.setScale(scale);
    }
    setMaximumSize(new Dimension(screenWidth, screenHeight));

    // Unfortunately JFrame provides very little control here
    setResizable(false);

    // set graphics pane
    gp = new GraphicsPane();
    gp.setBackground(Color.BLACK);
    gp.setDoubleBuffered(false);

    // set dimensions
    int width = (int) ((float) drawWidth * scale);
    int height = (int) ((float) drawHeight * scale);
    gp.setPreferredSize(new Dimension(width, height));
    windowRatio = (double) drawWidth / drawHeight;

    // Add menu bar
    buildMenuBar();

    // finish window
    setContentPane(gp);
    pack();
    validate(); // force redraw
    setTitle("Lemmini");

    // calculate frame size and set min size
    Dimension wholeWindow = getSize();
    xMargin = wholeWindow.width - width;
    yMargin = wholeWindow.height - height;
    setMinimumSize(new Dimension(drawWidth + xMargin, drawHeight + yMargin));
    System.out.println("Margins: " + xMargin + " - " + yMargin);

    // store content pane size
    oldSize = new Dimension(width, height);

    // set coords - default to centre of screen
    int posX = Core.programProps.get("framePosX", (screenWidth / 2) - (wholeWindow.width / 2));
    int posY = Core.programProps.get("framePosY", (screenHeight / 2) - (wholeWindow.height / 2));
    posX = Math.max(posX, 0);
    posY = Math.max(posY, 0);
    setLocation(posX, posY);

    // Exit the app when the user closes the window
    addWindowListener(
        new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosing(java.awt.event.WindowEvent e) {
            exit();
          }
        });

    // Add a listener to react when the window changes size
    addComponentListener(
        new java.awt.event.ComponentAdapter() {
          @Override
          public void componentResized(java.awt.event.ComponentEvent evt) {
            Dimension newSize = getContentPane().getSize();

            float scaleX = (float) newSize.width / Core.getDrawWidth();
            float scaleY = (float) newSize.height / Core.getDrawHeight();

            float scale = Math.min(scaleX, scaleY);

            Core.setScale(scale);
          }
        });

    try { // It doesn't matter if this fails
      setIconImage(Core.loadImageJar("LemminiIcon.png"));
    } catch (ResourceException ex) {
    }

    setVisible(true);
    GameController.setGameState(GameController.State.INTRO);
    GameController.setTransition(GameController.TransitionState.NONE);
    Fader.setBounds(Core.getDrawWidth(), Core.getDrawHeight());
    Fader.setState(Fader.State.IN);
    Thread t = new Thread(gp);

    addKeyListener(this);

    t.start();
  }

  /** Build the menu bar */
  private void buildMenuBar() {
    // Add menu bar
    jMenuBar = new JMenuBar();

    // Create File Menu
    if (!System.getProperty("os.name").equals("Mac OS X")) {
      JMenu jMenuFile = new JMenu("File");

      // Exit menu
      JMenuItem jMenuItemAbout = new JMenuItem("About");
      jMenuItemAbout.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
              showInitScreen();
            }
          });
      jMenuFile.add(jMenuItemAbout);

      // Exit menu
      JMenuItem jMenuItemExit = new JMenuItem("Exit");
      jMenuItemExit.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
              exit();
            }
          });
      jMenuFile.add(jMenuItemExit);

      jMenuBar.add(jMenuFile);
    }

    // Create Player Menu
    JMenu jMenuPlayer = new JMenu("Player");
    jMenuItemManagePlayer = new JMenuItem("Manage Players");
    jMenuItemManagePlayer.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            Core.player.store(); // save player in case it is changed
            PlayerDialog d = new PlayerDialog(thisFrame, true);
            d.setVisible(true);
            // blocked until dialog returns
            List<String> players = d.getPlayers();
            if (players != null) {
              String player = Core.player.getName(); // old player
              int playerIdx = d.getSelection();
              if (playerIdx != -1) player = players.get(playerIdx); // remember selected player
              // check for players to delete
              for (int i = 0; i < Core.getPlayerNum(); i++) {
                String p = Core.getPlayer(i);
                if (!players.contains(p)) {
                  File f = new File(Core.resourcePath + "players/" + p + ".ini");
                  f.delete();
                  if (p.equals(player)) player = "default";
                }
              }
              // rebuild players list
              Core.clearPlayers();
              // add default player if missing
              if (!players.contains("default")) players.add("default");
              // now copy all player and create properties
              for (int i = 0; i < players.size(); i++) {
                Core.addPlayer(players.get(i));
              }

              // select new default player
              Core.player = new Player(player);

              // rebuild players menu
              playerGroup = new ButtonGroup();
              jMenuSelectPlayer.removeAll();
              for (int idx = 0; idx < Core.getPlayerNum(); idx++) {
                JCheckBoxMenuItem item = addPlayerItem(Core.getPlayer(idx));
                if (Core.player.getName().equals(Core.getPlayer(idx))) item.setSelected(true);
              }
              updateLevelMenus();
            }
          }
        });
    jMenuPlayer.add(jMenuItemManagePlayer);

    jMenuSelectPlayer = new JMenu("Select Player");
    playerGroup = new ButtonGroup();
    for (int idx = 0; idx < Core.getPlayerNum(); idx++) {
      JCheckBoxMenuItem item = addPlayerItem(Core.getPlayer(idx));
      if (Core.player.getName().equals(Core.getPlayer(idx))) item.setSelected(true);
    }
    jMenuPlayer.add(jMenuSelectPlayer);
    jMenuBar.add(jMenuPlayer);

    // Create Level Menu
    jMenuLevel = new JMenu("Level");

    // Create action listener
    lvlListener =
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            LvlMenuItem item = (LvlMenuItem) e.getSource();
            GameController.requestChangeLevel(item.levelPack, item.diffLevel, item.level, false);
          }
        };

    // Menu for each item
    diffLevelMenus =
        new HashMap<String, ArrayList<LvlMenuItem>>(); // store menus to access them later
    for (int lp = 1; lp < GameController.getLevelPackNum(); lp++) { // skip dummy level pack
      JMenu jMenuPack = makeLevelPackMenu(lp);
      jMenuLevel.add(jMenuPack);
    }
    jMenuLevel.addSeparator();

    // Restart sub-menu
    JMenuItem jMenuItemRestart = new JMenuItem();
    jMenuItemRestart.setText("Restart Level");
    jMenuItemRestart.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            if (!GameController.getLevel().isReady())
              GameController.requestChangeLevel(
                  GameController.getCurLevelPackIdx(),
                  GameController.getCurDiffLevel(),
                  GameController.getCurLevelNumber(),
                  false);
            else GameController.requestRestartLevel(false);
          }
        });
    jMenuLevel.add(jMenuItemRestart);

    jMenuItemLoad = new JMenuItem();
    jMenuItemLoad.setText("Add Level Pack");
    jMenuItemLoad.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            addLevelPack();
          }
        });
    jMenuLevel.add(jMenuItemLoad);

    jMenuItemReplay = new JMenuItem();
    jMenuItemReplay.setText("Load Replay");
    jMenuItemReplay.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            String replayPath =
                ToolBox.getFileName(thisFrame, Core.resourcePath, Core.REPLAY_EXTENSIONS, true);
            if (replayPath != null) {
              try {
                if (ToolBox.getExtension(replayPath).equalsIgnoreCase("rpl")) {
                  ReplayLevelInfo rli = GameController.loadReplay(replayPath);
                  if (rli != null) {
                    int lpn = -1;
                    for (int i = 0; i < GameController.getLevelPackNum(); i++)
                      if (GameController.getLevelPack(i).getName().equals(rli.getLevelPack()))
                        lpn = i;
                    if (lpn > -1) {
                      GameController.requestChangeLevel(
                          lpn, rli.getDiffLevel(), rli.getLvlNumber(), true);
                      return; // success
                    }
                  }
                }
                // else: no success
                JOptionPane.showMessageDialog(
                    thisFrame,
                    "Wrong format!",
                    "Loading replay failed",
                    JOptionPane.INFORMATION_MESSAGE);
              } catch (Exception ex) {
                ToolBox.showException(ex);
              }
            }
          }
        });
    jMenuLevel.add(jMenuItemReplay);

    jMenuItemLevelCode = new JMenuItem("Enter Level Code");
    jMenuItemLevelCode.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            LevelCodeDialog lcd = new LevelCodeDialog(thisFrame, true);
            lcd.setVisible(true);
            String levelCode = lcd.getCode();
            int lvlPack = lcd.getLevelPack();
            if (levelCode != null && levelCode.length() != 0 && lvlPack > 0) {

              levelCode = levelCode.trim();
              // cheat mode
              if (levelCode.equals("0xdeadbeef")) {
                JOptionPane.showMessageDialog(
                    thisFrame,
                    "All levels and debug mode enabled",
                    "Cheater!",
                    JOptionPane.INFORMATION_MESSAGE);
                Core.player.enableCheatMode();
                updateLevelMenus();
                return;
              }

              // real level code -> get absolute level
              levelCode = levelCode.toUpperCase();
              LevelPack lpack = GameController.getLevelPack(lvlPack);
              int lvlAbs =
                  LevelCode.getLevel(lpack.getCodeSeed(), levelCode, lpack.getCodeOffset());
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
              JOptionPane.showMessageDialog(
                  thisFrame, "Invalid Level Code", "Error", JOptionPane.WARNING_MESSAGE);
            }
          }
        });
    jMenuLevel.add(jMenuItemLevelCode);

    // Finish level menu
    jMenuBar.add(jMenuLevel);

    // Create Sound Menu
    JMenu jMenuSound = new JMenu();
    jMenuSound.setText("Sound");

    jMenuItemMusic = new JCheckBoxMenuItem("Music", false);
    jMenuItemMusic.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            boolean selected = jMenuItemMusic.isSelected();
            GameController.setMusicOn(selected);

            if (GameController.getLevel() != null) // to be improved: level is running (game state)
            if (GameController.isMusicOn()) Music.play();
              else Music.stop();
          }
        });
    jMenuItemMusic.setSelected(GameController.isMusicOn());
    jMenuSound.add(jMenuItemMusic);

    jMenuItemSound = new JCheckBoxMenuItem("Sound", false);
    jMenuItemSound.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            boolean selected = jMenuItemSound.isSelected();
            GameController.setSoundOn(selected);
          }
        });
    jMenuItemSound.setSelected(GameController.isSoundOn());
    jMenuSound.add(jMenuItemSound);

    JMenu jMenuSFX = new JMenu("SFX Mixer");
    String mixerNames[] = GameController.sound.getMixers();
    ButtonGroup mixerGroup = new ButtonGroup();
    String lastMixerName = Core.programProps.get("mixerName", "Java Sound Audio Engine");

    // special handling of mixer from INI that doesn't exist (any more)
    boolean found = false;
    for (int i = 0; i < mixerNames.length; i++) {
      if (mixerNames[i].equals(lastMixerName)) {
        found = true;
        break;
      }
    }
    if (!found) lastMixerName = "Java Sound Audio Engine";

    for (int i = 0; i < mixerNames.length; i++) {
      JCheckBoxMenuItem item = new JCheckBoxMenuItem();
      item.setText(mixerNames[i]);
      item.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
              String mixerNames[] = GameController.sound.getMixers();
              String mixerName = e.getActionCommand();
              for (int i = 0; i < mixerNames.length; i++) {
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
    jMenuItemVolume.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            GainDialog v = new GainDialog(thisFrame, true);
            v.addWindowListener(
                new java.awt.event.WindowAdapter() {
                  @Override
                  public void windowClosed(java.awt.event.WindowEvent e) {
                    updateSoundAndMusicMenuItems();
                  }

                  @Override
                  public void windowClosing(java.awt.event.WindowEvent e) {
                    updateSoundAndMusicMenuItems();
                  }
                });

            v.setVisible(true);
          }
        });
    jMenuSound.add(jMenuItemVolume);

    // Finished Sound Menu
    jMenuBar.add(jMenuSound);

    // Create Options Menu
    JMenu jMenuOptions = new JMenu();
    jMenuOptions.setText("Options");

    jMenuItemCursor = new JCheckBoxMenuItem("Advanced select", false);
    jMenuItemCursor.addActionListener(
        new java.awt.event.ActionListener() {
          /* (non-Javadoc)
           * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
           */
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            boolean selected = jMenuItemCursor.isSelected();
            if (selected) GameController.setAdvancedSelect(true);
            else {
              GameController.setAdvancedSelect(false);
              gp.setCursor(LemmCursor.Type.NORMAL);
            }
            Core.programProps.set("advancedSelect", GameController.isAdvancedSelect());
          }
        });
    jMenuItemCursor.setSelected(GameController.isAdvancedSelect());
    jMenuOptions.add(jMenuItemCursor);

    // Zoom Menu
    jMenuZoom = new JMenu("Zoom");
    ButtonGroup zoomGroup = new ButtonGroup();
    double realMaxScale = Math.pow(maxScale, 2);
    double realCurrentScale = ((int) Math.pow(Core.getScale(), 2) * 1000) / 1000;

    for (double zoom = 1; zoom < realMaxScale; zoom += 0.25) {
      JRadioButtonMenuItem zoomMenuItem = new JRadioButtonMenuItem("x" + zoom);
      final double z = Math.sqrt(zoom);
      zoomMenuItem.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
              setScale(z);
            }
          });
      jMenuZoom.add(zoomMenuItem);
      zoomGroup.add(zoomMenuItem);

      if (zoom == realCurrentScale) {
        zoomMenuItem.setSelected(true);
      }
    }

    jMenuOptions.add(jMenuZoom);

    // Fullscreen
    jMenuItemFullscreen = new JMenuItem("Fullscreen");
    jMenuItemFullscreen.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            toggleFullScreen();
          }
        });
    jMenuOptions.add(jMenuItemFullscreen);

    // Finish Options Menu
    jMenuBar.add(jMenuOptions);

    // Finalise menu bar
    setJMenuBar(jMenuBar);
  }

  /**
   * Add a menu item for a player.
   *
   * @param name player name
   * @return JCheckBoxMenuItem
   */
  private JCheckBoxMenuItem addPlayerItem(final String name) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
    item.addActionListener(
        new java.awt.event.ActionListener() {
          @Override
          public void actionPerformed(java.awt.event.ActionEvent e) {
            JMenuItem item = (JMenuItem) e.getSource();
            String player = item.getText();

            // don't do anything if the player hasn't been changed
            if (player.equals(Core.player.getName())) return;

            // save old player
            Core.player.store();

            // get new player
            Player p = new Player(player);
            Core.player = p; // default player
            item.setSelected(true);
            updateLevelMenus();

            // return to intro screen
            GameController.setTransition(GameController.TransitionState.TO_INTRO);
            Fader.setState(Fader.State.OUT);
          }
        });
    playerGroup.add(item);
    jMenuSelectPlayer.add(item);
    return item;
  }

  /**
   * Convert String to int.
   *
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
   *
   * @param args
   */
  public static void main(final String[] args) {
    /*
     * Set "Look and Feel" to system default
     */
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      /* don't care */
    }
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
    int jreVer = getInt(jreStr);

    if (jreVer < 7) {
      JOptionPane.showMessageDialog(
          null, "Run this with JVM >= 1.7", "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

    // check free memory
    long free = Runtime.getRuntime().maxMemory();
    if (free < 60 * 1024 * 1024) { // 64MB doesn't seem to work even if set with -Xmx64M
      JOptionPane.showMessageDialog(
          null, "You need at least 64MB of heap", "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

    Toolkit.getDefaultToolkit().setDynamicLayout(true);
    thisFrame = new Lemmini();
  }

  /** Set window bounds in response to user choice */
  void setScale(double scale) {
    // calculate new heights of graphicsPane
    int newHeight = (int) ((float) scale * Core.getDrawHeight());
    int newWidth = (int) ((float) scale * Core.getDrawWidth());

    // set window size
    setSize(newWidth + xMargin, newHeight + yMargin);
  }

  /** Update the level menus according to the progress of the current player. */
  private void updateLevelMenus() {
    // update level menus
    for (int lp = 1; lp < GameController.getLevelPackNum(); lp++) { // skip dummy level pack
      LevelPack lPack = GameController.getLevelPack(lp);
      String difficulties[] = lPack.getDiffLevels();
      for (int i = 0; i < difficulties.length; i++) {
        // get activated levels for this group
        GroupBitfield bf = Core.player.getBitField(lPack.getName(), difficulties[i]);
        updateLevelMenu(lPack.getName(), difficulties[i], bf);
      }
    }
  }

  /**
   * Update the level menus according to the given progress information.
   *
   * @param pack name of level pack
   * @param diff name of difficulty level
   * @param bf bitmap containing availability flags for each level
   */
  private void updateLevelMenu(final String pack, final String diff, final GroupBitfield bf) {
    ArrayList<LvlMenuItem> menuItems = diffLevelMenus.get(LevelPack.getID(pack, diff));
    for (int k = 0; k < menuItems.size(); k++) {
      // select level, e.g. "All fall down"
      JMenuItem level = menuItems.get(k);
      if (k == 0 || Core.player.isAvailable(bf, k)) level.setEnabled(true);
      else level.setEnabled(false);
    }
  }

  /** Update the sound and music menu items based on the their current status */
  private void updateSoundAndMusicMenuItems() {
    jMenuItemMusic.setSelected(GameController.isMusicOn());
    jMenuItemSound.setSelected(GameController.isSoundOn());
  }

  /**
   * Development function: patch current level x offset in the level configuration file. Works only
   * in cheat mode.
   *
   * @param lvlPath path of level configuration files
   */
  private void patchLevel(final String lvlPath) {
    try {
      ArrayList<String> lines = new ArrayList<String>();
      BufferedReader r = new BufferedReader(new FileReader(lvlPath));
      String l;
      while ((l = r.readLine()) != null) lines.add(l);
      r.close();
      FileWriter sw = new FileWriter(lvlPath);
      for (int i = 0; i < lines.size(); i++) {
        String s = lines.get(i);
        if (s.startsWith("xPos =")) {
          sw.write("xPos = " + Integer.toString(GameController.getxPos()) + "\n");
        } else sw.write(s + "\n");
      }
      sw.close();
    } catch (FileNotFoundException ex) {
    } catch (IOException ex) {
    }
  }

  /** Add a level pack taken from a folder */
  private void addLevelPack() {
    // Run a popup for user to give directory
    Path sourceDirectory = ToolBox.getFolderName(this);
    if (sourceDirectory == null) return;

    // Have we added this before?
    Path name = sourceDirectory.getName(sourceDirectory.getNameCount() - 1);
    Path targetDirectory = Paths.get(Core.resourcePath + "levels/" + name.toString());
    if (Files.exists(targetDirectory)) {
      JOptionPane.showMessageDialog(
          thisFrame,
          "Error!",
          "Target directory already exists",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    // Add the level pack to the live application
    try {
      GameController.addLevelPack(sourceDirectory.toString());
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          thisFrame,
          "Error!",
          "Failed to load level pack. Bad formatting?",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    // Add to menu
    int lp = GameController.getLevelPackNum() - 1;
    jMenuLevel.insert(makeLevelPackMenu(lp), lp - 1);

    // copy source to target using Files Class
    try {
      ToolBox.copyFolder(sourceDirectory, targetDirectory);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(
          thisFrame,
          "Error!",
          "Failed to copy pack to resource directory",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
   */
  @Override
  public void keyPressed(final KeyEvent keyevent) {
    int code = keyevent.getKeyCode();
    if (code == KeyEvent.VK_ESCAPE) {
      if (Core.isFullScreen()) {
        toggleFullScreen();
      }
    } else if (GameController.getGameState() == GameController.State.LEVEL) {
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
          if (GameController.isCheat()) gp.setDebugDraw(!gp.getDebugDraw());
          break;
        case KeyEvent.VK_W:
          if (GameController.isCheat()) {
            GameController.setNumLeft(GameController.getNumLemmingsMax());
            GameController.endLevel();
          }
          break;
        case KeyEvent.VK_L: // print current level on the console
          if (GameController.isCheat())
            System.out.println(
                GameController.getLevelPack(GameController.getCurLevelPackIdx())
                    .getInfo(GameController.getCurDiffLevel(), GameController.getCurLevelNumber())
                    .getFileName());
          break;
        case KeyEvent.VK_S: // superlemming on/off
          if (GameController.isCheat())
            GameController.setSuperLemming(!GameController.isSuperLemming());
          else {
            try {
              File file = new File(Core.resourcePath + "/level.png");
              BufferedImage tmp =
                  GameController.getLevel()
                      .createMiniMap(null, GameController.getBgImage(), 1, 1, false);
              ImageIO.write(tmp, "png", file);
            } catch (Exception ex) {
            }
          }
          break;
        case KeyEvent.VK_C:
          if (Core.player.isCheat()) {
            GameController.setCheat(!GameController.isCheat());
            if (GameController.isCheat()) GameController.setWasCheated(true);
          } else GameController.setCheat(false);
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
            patchLevel(
                GameController.getLevelPack(GameController.getCurLevelPackIdx())
                    .getInfo(GameController.getCurDiffLevel(), GameController.getCurLevelNumber())
                    .getFileName());
          break;
        case KeyEvent.VK_RIGHT /*39*/:
          {
            if (GameController.isAdvancedSelect()) gp.setCursor(LemmCursor.Type.RIGHT);
            else {
              int xOfsTemp =
                  GameController.getxPos()
                      + ((gp.isShiftPressed()) ? GraphicsPane.X_STEP_FAST : GraphicsPane.X_STEP);
              if (xOfsTemp < Level.WIDTH - getWidth()) GameController.setxPos(xOfsTemp);
              else GameController.setxPos(Level.WIDTH - getWidth());
            }
            break;
          }
        case KeyEvent.VK_LEFT /*37*/:
          {
            if (GameController.isAdvancedSelect()) gp.setCursor(LemmCursor.Type.LEFT);
            else {
              int xOfsTemp =
                  GameController.getxPos()
                      - ((gp.isShiftPressed()) ? GraphicsPane.X_STEP_FAST : GraphicsPane.X_STEP);
              if (xOfsTemp > 0) GameController.setxPos(xOfsTemp);
              else GameController.setxPos(0);
            }
            break;
          }
        case KeyEvent.VK_UP:
          {
            gp.setCursor(LemmCursor.Type.WALKER);
            break;
          }
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
        case KeyEvent.VK_R:
          if (!GameController.getLevel().isReady())
            GameController.requestChangeLevel(
                GameController.getCurLevelPackIdx(),
                GameController.getCurDiffLevel(),
                GameController.getCurLevelNumber(),
                false);
          else GameController.requestRestartLevel(false);
          break;
      }
      keyevent.consume();
    }
    // System.out.println(keyevent.getKeyCode());
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
          if (LemmCursor.getType() == LemmCursor.Type.LEFT) gp.setCursor(LemmCursor.Type.NORMAL);
          break;
        case KeyEvent.VK_RIGHT:
          if (LemmCursor.getType() == LemmCursor.Type.RIGHT) gp.setCursor(LemmCursor.Type.NORMAL);
          break;
        case KeyEvent.VK_UP:
          if (LemmCursor.getType() == LemmCursor.Type.WALKER) gp.setCursor(LemmCursor.Type.NORMAL);
          break;
      }
    }
  }

  /* (non-Javadoc)
   * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
   */
  @Override
  public void keyTyped(final KeyEvent keyevent) {}

  /** Return to the init screen */
  public void showInitScreen() {
    GameController.setTransition(GameController.TransitionState.TO_INTRO);
    Fader.setState(Fader.State.OUT);
    if (!System.getProperty("os.name").equals("Mac OS X")) {
      setTitle("Lemmini");
    }
  }

  /** Toggle in and out of fullscreen mode */
  public void toggleFullScreen() {
    if (!Core.isFullScreen()) {
      // remember some stuff
      saveWindowProps();
      int menuBarHeight = jMenuBar.getHeight();
      Core.setFullScreen(true);

      // disable menus which don't work in fullscreen
      jMenuZoom.setEnabled(false);
      jMenuItemManagePlayer.setEnabled(false);
      jMenuItemLoad.setEnabled(false);
      jMenuItemReplay.setEnabled(false);
      jMenuItemLevelCode.setEnabled(false);
      jMenuItemVolume.setEnabled(false);

      // get rid of windowed window
      dispose();
      setUndecorated(true);
      setJMenuBar(null);

      // setup layered pane
      JLayeredPane newlp = new JLayeredPane();
      setContentPane(newlp);

      // add elements
      newlp.add(jMenuBar, JLayeredPane.PALETTE_LAYER);
      newlp.add(gp, JLayeredPane.DEFAULT_LAYER);
      gp.setBounds(0, 0, screenWidth, screenHeight);
      jMenuBar.setBounds(0, 0, screenWidth, menuBarHeight);

      // set full screen
      gd.setFullScreenWindow(thisFrame);

      // hide menubar and show layeredpane
      jMenuBar.setVisible(false); // start invisible
      newlp.setVisible(true);

      jMenuItemFullscreen.setText("Exit Fullscreen");
    } else {
      dispose();
      setUndecorated(false);

      // start again
      setContentPane(gp);

      // re-enable menus
      jMenuZoom.setEnabled(true);
      jMenuItemManagePlayer.setEnabled(true);
      jMenuItemLoad.setEnabled(true);
      jMenuItemReplay.setEnabled(true);
      jMenuItemLevelCode.setEnabled(true);
      jMenuItemVolume.setEnabled(true);

      setJMenuBar(jMenuBar);

      jMenuItemFullscreen.setText("Fullscreen");

      jMenuBar.setVisible(true);
      setVisible(true);
      Core.setFullScreen(false);
    }
  }

  /** Store window properties. */
  public void saveWindowProps() {
    Core.recordWindowProps(getLocation());
    Core.programProps.save();
  }

  /** Common exit method to use in exit events. */
  private void exit() {
    if (!Core.isFullScreen()) {
      // don't record window position when in full screen
      Core.recordWindowProps(getLocation());
    }

    // music
    Core.programProps.set("music", GameController.isMusicOn());
    Core.programProps.set("sound", GameController.isSoundOn());

    // save ini file
    Core.programProps.save();

    // remember player status
    Core.savePlayerProps();

    // exit
    System.exit(0);
  }

  /** Make the Level Pack Menu */
  private JMenu makeLevelPackMenu(int lp) {
    LevelPack lPack = GameController.getLevelPack(lp);
    JMenu jMenuPack = new JMenu(lPack.getName());
    String difficulties[] = lPack.getDiffLevels();
    for (int i = 0; i < difficulties.length; i++) {
      // get activated levels for this group
      GroupBitfield bf = Core.player.getBitField(lPack.getName(), difficulties[i]);
      String names[] = lPack.getLevels(i);
      JMenu jMenuDiff = new JMenu(difficulties[i]);
      // store menus to access them later
      ArrayList<LvlMenuItem> menuItems = new ArrayList<LvlMenuItem>();
      for (int n = 0; n < names.length; n++) {
        LvlMenuItem jMenuLvl = new LvlMenuItem(names[n], lp, i, n);
        jMenuLvl.addActionListener(lvlListener);
        if (Core.player.isAvailable(bf, n)) jMenuLvl.setEnabled(true);
        else jMenuLvl.setEnabled(false);
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
   *
   * @author Volker Oth
   */
  class LevelMenuUpdateListener implements UpdateListener {
    /* (non-Javadoc)
     * @see Game.UpdateListener#update()
     */
    @Override
    public void update() {
      if (GameController.getCurLevelPackIdx() != 0) { // 0 is the dummy pack
        LevelPack lvlPack = GameController.getLevelPack(GameController.getCurLevelPackIdx());
        String pack = lvlPack.getName();
        String diff = lvlPack.getDiffLevels()[GameController.getCurDiffLevel()];
        // get next level
        int num = GameController.getCurLevelNumber() + 1;
        if (num >= lvlPack.getLevels(GameController.getCurDiffLevel()).length)
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
   *
   * @author Volker Oth
   */
  class LvlMenuItem extends JMenuItem {
    private static final long serialVersionUID = 0x01;

    // index of level pack
    int levelPack;
    // index of difficulty level
    int diffLevel;
    // level number
    int level;

    /**
     * Constructor
     *
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
