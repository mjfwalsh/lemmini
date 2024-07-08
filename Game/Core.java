package Game;

import Extract.Extract;
import Extract.FolderDialog;
import GUI.LegalDialog;
import Tools.Props;
import Tools.ToolBox;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

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
 * Well, this started as some kind of core class to collect all global stuff Now lots of the
 * functionality moved to GameController. Would need some cleaning up, maybe remove the whole thing?
 *
 * @author Volker Oth
 */
public class Core {
  /** The revision string for resource compatibility - not necessarily the version number */
  private static final String REVISION = "0.80";

  /** name of the ini file */
  private static final String INI_NAME = "lemmings.ini";

  /** extensions accepted for level files in file dialog */
  public static final String[] LEVEL_EXTENSIONS = {"ini", "lvl"};

  /** extensions accepted for replay files in file dialog */
  public static final String[] REPLAY_EXTENSIONS = {"rpl"};

  /** program properties */
  public static Props programProps;

  /** path of (extracted) resources */
  public static String resourcePath;

  /** current player */
  public static Player player;

  /** parent component (main frame) */
  private static JFrame cmp;

  /** player properties */
  private static Props playerProps;

  /** list of all players */
  private static ArrayList<String> players;

  /**
   * Initialize some core elements.
   *
   * @param frame parent frame
   * @throws LemmException
   */
  public static synchronized void init(final JFrame frame) throws LemmException {
    // alias for object
    cmp = frame;

    // get ini path
    String programPropsFileStr = "";

    if (System.getProperty("os.name").equals("Mac OS X")) {
      // resourcePath and programPropsFileStr are in fixed places on Mac
      resourcePath = System.getProperty("user.home") + "/Library/Application Support/Lemmini/";

      // ini path
      programPropsFileStr = resourcePath + INI_NAME;
    } else {
      String s = frame.getClass().getName().replace('.', '/') + ".class";
      URL url = frame.getClass().getClassLoader().getResource(s);
      int pos;
      try {
        programPropsFileStr = URLDecoder.decode(url.getPath(), "UTF-8");
      } catch (UnsupportedEncodingException ex) {
      }

      // special handling for JAR
      if (((pos = programPropsFileStr.toLowerCase().indexOf("file:")) != -1))
        programPropsFileStr = programPropsFileStr.substring(pos + 5);
      if ((pos = programPropsFileStr.toLowerCase().indexOf(s.toLowerCase())) != -1)
        programPropsFileStr = programPropsFileStr.substring(0, pos);

      // @todo doesn't work if JAR is renamed...
      // Maybe it would be a better idea to search only for ".JAR" and then
      // for the first path separator...

      s = (frame.getClass().getName().replace('.', '/') + ".jar").toLowerCase();
      if ((pos = programPropsFileStr.toLowerCase().indexOf(s)) != -1)
        programPropsFileStr = programPropsFileStr.substring(0, pos);
      programPropsFileStr += INI_NAME;
    }

    // read main ini file
    programProps = new Props();
    if (!programProps.load(programPropsFileStr)) { // might exist or not - if not, it's created
      LegalDialog ld = new LegalDialog(null, true);
      ld.setVisible(true);
      if (!ld.isOk()) System.exit(0);
    }

    if (!System.getProperty("os.name").equals("Mac OS X")) {
      resourcePath = getFolder("resourcePath");
    }
    String sourcePath = getFolder("sourcePath");
    String rev = programProps.get("revision", "");

    if (sourcePath.length() < 1 || resourcePath.length() < 1 || !REVISION.equalsIgnoreCase(rev)) {

      // start dialog to prompt user for resource and source paths
      FolderDialog fDiag = new FolderDialog();
      fDiag.setParameters(sourcePath, resourcePath);
      fDiag.setVisible(true);
      if (!fDiag.getSuccess()) System.exit(0);

      // save them
      if (!System.getProperty("os.name").equals("Mac OS X")) {
        resourcePath = saveFolder("resourcePath", fDiag.getTarget());
      }
      sourcePath = saveFolder("sourcePath", fDiag.getSource());

      // extract resources
      Extract extract = new Extract(sourcePath, resourcePath);

      if (extract.extractionSuccessful()) {
        programProps.set("revision", REVISION);
      } else {
        programProps.set("revision", "invalid");
        System.exit(1);
      }
    } else {
      // A little hacky but this saves user from having to a full extract operation for one new file
      Extract.extractSingleFile(
          "patch/misc@lemmfontscaled.gif", resourcePath + "misc/lemmfontscaled.gif");
    }

    // load misc settings
    GameController.setMusicOn(programProps.get("music", false));
    GameController.setSoundOn(programProps.get("sound", true));
    GameController.setMusicGain(programProps.get("musicGain", 1.0));
    GameController.setSoundGain(programProps.get("soundGain", 1.0));
    GameController.setAdvancedSelect(programProps.get("advancedSelect", true));

    System.gc(); // force garbage collection here before the game starts

    // read player names
    playerProps = new Props();
    playerProps.load(resourcePath + "players.ini");
    String defaultPlayer = playerProps.get("defaultPlayer", "default");
    players = new ArrayList<String>();
    for (int idx = 0; true; idx++) {
      String p = playerProps.get("player_" + Integer.toString(idx), "");
      if (p.length() == 0) break;
      players.add(p);
    }
    if (players.size() == 0) {
      // no players yet, establish default player
      players.add("default");
      playerProps.set("player_0", "default");
    }
    player = new Player(defaultPlayer);
  }

  /**
   * Get parent component (main frame).
   *
   * @return parent component
   */
  public static synchronized JFrame getCmp() {
    return cmp;
  }

  /**
   * Get String to resource in resource path.
   *
   * @param fname file name (without path)
   * @return absolute path to resource
   */
  public static synchronized String findResource(final String fname) {
    return resourcePath + fname;
  }

  /** Save properties */
  public static synchronized void saveProps() {
    // music
    programProps.set("music", GameController.isMusicOn());
    programProps.set("sound", GameController.isSoundOn());

    // save ini file
    programProps.save();

    // Store player properties.
    playerProps.set("defaultPlayer", player.getName());
    playerProps.save();
    player.store();

    System.out.println("Saving on exit");
  }

  /**
   * Output error message box in case of a missing resource.
   *
   * @param rsrc name missing of resource.
   */
  public static synchronized void resourceError(final String rsrc) {
    String out =
        "The resource " + rsrc + " is missing\n" + "Please restart to extract all resources.";
    JOptionPane.showMessageDialog(null, out, "Error", JOptionPane.ERROR_MESSAGE);
    // invalidate resources
    programProps.set("revision", "invalid");
    programProps.save();
    System.exit(1);
  }

  /**
   * Load an image from the resource path.
   *
   * @param tracker media tracker
   * @param fName file name
   * @return Image
   * @throws ResourceException
   */
  public static synchronized Image loadImage(final MediaTracker tracker, final String fName)
      throws ResourceException {
    String fileLoc = findResource(fName);
    if (fileLoc == null) return null;
    return loadImage(tracker, fileLoc, false);
  }

  /**
   * Load an image from either the resource path or from inside the JAR (or the directory of the
   * main class).
   *
   * @param tracker media tracker
   * @param fName file name
   * @param jar true: load from the jar/class path, false: load from resource path
   * @return Image
   * @throws ResourceException
   */
  private static synchronized Image loadImage(
      final MediaTracker tracker, final String fName, final boolean jar) throws ResourceException {
    Image image;
    if (jar) {
      ClassLoader loader = Core.class.getClassLoader();
      URL url = loader.getResource(fName);
      image = Toolkit.getDefaultToolkit().createImage(url);
    } else {
      image = Toolkit.getDefaultToolkit().createImage(fName);
    }

    if (image != null) {
      tracker.addImage(image, 0);
      try {
        tracker.waitForID(0);
        if (tracker.isErrorAny()) {
          image = null;
        }
      } catch (Exception ex) {
        image = null;
      }
    }
    if (image == null) throw new ResourceException(fName);
    return image;
  }

  /**
   * Load an image from the resource path.
   *
   * @param fname file name
   * @return Image
   * @throws ResourceException
   */
  public static synchronized Image loadImage(final String fname) throws ResourceException {
    MediaTracker tracker = new MediaTracker(cmp);
    Image img = loadImage(tracker, fname);
    if (img == null) throw new ResourceException(fname);
    return img;
  }

  /**
   * Load an image from inside the JAR or the directory of the main class.
   *
   * @param fname
   * @return Image
   * @throws ResourceException
   */
  public static synchronized Image loadImageJar(final String fname) throws ResourceException {
    MediaTracker tracker = new MediaTracker(cmp);
    Image img = loadImage(tracker, fname, true);
    if (img == null) throw new ResourceException(fname);
    return img;
  }

  /**
   * Get player name via index.
   *
   * @param idx player index
   * @return player name
   */
  public static synchronized String getPlayer(final int idx) {
    return players.get(idx);
  }

  /**
   * Get number of players.
   *
   * @return number of player.
   */
  public static synchronized int getPlayerNum() {
    if (players == null) return 0;
    return players.size();
  }

  /** Reset list of players. */
  public static synchronized void clearPlayers() {
    players.clear();
    playerProps.clear();
  }

  /**
   * Add player.
   *
   * @param name player name
   */
  public static synchronized void addPlayer(final String name) {
    players.add(name);
    playerProps.set("player_" + (players.size() - 1), name);
  }

  /**
   * Convert path to unix path and add a trailing slash
   *
   * @param string folder path
   */
  private static String getFolder(String name) {
    return ToolBox.exchangeSeparators(ToolBox.addSeparator(programProps.get(name, "")));
  }

  /**
   * Convert path to unix path and add a trailing slash
   *
   * @param string folder path
   */
  private static String saveFolder(String name, String folder) {
    String path = ToolBox.exchangeSeparators(ToolBox.addSeparator(folder));
    programProps.set(name, path);
    return path;
  }
}
