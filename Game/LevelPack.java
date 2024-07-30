package Game;

import Tools.Props;
import java.io.File;
import java.util.ArrayList;

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
 * Handling of a level pack.
 *
 * @author Volker Oth
 */
public class LevelPack {

  /** name of the level pack */
  private String name;

  /** seed used to generate the level codes */
  private String codeSeed;

  /** array containing names of difficulty levels (easiest first, hardest last) */
  private String diffLevels[];

  /** array of array of level info - [difficulty][level number] */
  private LevelInfo lvlInfo[][];

  /** path of level pack - where the INI files for the level are located */
  private File path;

  /** maximum number of pixels a Lemming can fall before he dies */
  private int maxFallDistance;

  /** offset to apply in level code algorithm */
  private int codeOffset;

  /** Constructor for dummy level pack. Needed for loading single levels. */
  public LevelPack() {
    name = "test";
    path = new File("");
    codeSeed = "AAAAAAAAAA";
    maxFallDistance = 126;
    codeOffset = 0;

    diffLevels = new String[1];
    diffLevels[0] = "test";

    lvlInfo = new LevelInfo[1][1];
    lvlInfo[0][0] = new LevelInfo();
    // lvlInfo[0][0].code = "..........";
    lvlInfo[0][0].setMusic("tim1.mod");
    lvlInfo[0][0].setName("test");
    lvlInfo[0][0].setFileName("");
  }

  /**
   * Constructor for loading a level pack.
   *
   * @param file File of level pack ini
   * @throws ResourceException
   */
  public LevelPack(final File file) throws ResourceException {
    // extract path from descriptor file
    path = file.getParentFile();
    // load the descriptor file
    Props props = new Props();
    if (!props.load(file)) {
      name = "empty";
      return;
    }

    // read name
    name = props.get("name", "");
    // read code seed
    codeSeed = props.get("codeSeed", "").trim().toUpperCase();
    // read code level offset
    codeOffset = props.get("codeOffset", 0);
    // read max falling distance
    maxFallDistance = props.get("maxFallDistance", 126);
    // read levels of difficulty
    ArrayList<String> difficulty = new ArrayList<String>(); // <String>
    int idx = 0;
    String diffLevel;
    do {
      diffLevel = props.get("level_" + Integer.toString(idx++), "");
      if (diffLevel.length() > 0) difficulty.add(diffLevel);
    } while (diffLevel.length() > 0);
    diffLevels = new String[difficulty.size()];
    diffLevels = difficulty.toArray(diffLevels);
    // read music files
    ArrayList<String> music = new ArrayList<String>(); // <String>
    String track;
    idx = 0;
    do {
      track = props.get("music_" + Integer.toString(idx++), "");
      if (track.length() > 0) music.add(track);
    } while (track.length() > 0);
    // read levels
    lvlInfo = new LevelInfo[difficulty.size()][];
    String levelStr[];
    String def[] = {""};
    for (int diff = 0; diff < difficulty.size(); diff++) {
      idx = 0;
      ArrayList<LevelInfo> levels = new ArrayList<LevelInfo>(); // <LevelInfo>
      diffLevel = difficulty.get(diff);
      do {
        levelStr = props.get(diffLevel.toLowerCase() + "_" + Integer.toString(idx), def);
        // filename, music number
        if (levelStr.length == 2) {
          File iniFile = new File(path, levelStr[0]);

          // get name from ini file
          Props lvlProps = new Props();
          lvlProps.load(iniFile);
          // Now put everything together
          LevelInfo info = new LevelInfo();
          info.setFileName(iniFile.getAbsolutePath());
          info.setMusic(music.get(Integer.parseInt(levelStr[1])));
          info.setName(lvlProps.get("name", "")); // only used in menu
          levels.add(info);
        }
        idx++;
      } while (levelStr.length == 2);
      lvlInfo[diff] = new LevelInfo[levels.size()];
      lvlInfo[diff] = levels.toArray(lvlInfo[diff]);
    }
  }

  /**
   * Assemble level pack and difficulty level to string.
   *
   * @param pack level pack
   * @param diff name of difficulty level
   * @return String formed from level pack and difficulty level
   */
  public static String getID(final String pack, final String diff) {
    return pack.toLowerCase() + "-" + diff.toLowerCase();
  }

  /**
   * Return levels of difficulty as string array.
   *
   * @return levels of difficulty as string array
   */
  public String[] getDiffLevels() {
    return diffLevels;
  }

  /**
   * Get name of level pack.
   *
   * @return name of level pack
   */
  public String getName() {
    return name;
  }

  /**
   * Get code seed.
   *
   * @return code seed.
   */
  public String getCodeSeed() {
    return codeSeed;
  }

  /**
   * Get maximum fall distance.
   *
   * @return maximum fall distance
   */
  public int getMaxFallDistance() {
    return maxFallDistance;
  }

  /**
   * Get offset to apply in level code algorithm.
   *
   * @return offset to apply in level code algorithm
   */
  public int getCodeOffset() {
    return codeOffset;
  }

  /**
   * Get level info for a certain level.
   *
   * @param diffLvl difficulty level
   * @param level level number
   * @return LevelInfo for the given level
   */
  public LevelInfo getInfo(final int diffLvl, final int level) {
    return lvlInfo[diffLvl][level];
  }

  /**
   * Get level info for a certain level.
   *
   * @param diffLvl difficulty level
   * @param level level number
   * @return LevelInfo for the given level
   */
  public int getLevelCount(final int diffLvl) {
    return lvlInfo[diffLvl].length;
  }

  /**
   * Return all levels for a given difficulty
   *
   * @param diffLevel number of difficulty level
   * @return level names as string array
   */
  public String[] getLevels(final int diffLevel) {
    String names[] = new String[lvlInfo[diffLevel].length];
    for (int i = 0; i < lvlInfo[diffLevel].length; i++) names[i] = lvlInfo[diffLevel][i].getName();
    return names;
  }
}
