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
  private ArrayList<String> diffLevels = new ArrayList<String>();

  /** array of array of level info - [difficulty][level number] */
  private ArrayList<ArrayList<LevelInfo>> lvlInfo = new ArrayList<ArrayList<LevelInfo>>();

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

    diffLevels = new ArrayList<String>(1);
    diffLevels.add("test");

    LevelInfo linfo = new LevelInfo();
    linfo.setMusic("tim1.mod");
    linfo.setName("test");
    linfo.setFileName("");

    ArrayList<LevelInfo> arr = new ArrayList<LevelInfo>();
    arr.add(linfo);
    lvlInfo.add(arr);
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
    int idx = 0;
    String diffLevel;
    do {
      diffLevel = props.get("level_" + Integer.toString(idx++), "");
      if (diffLevel.length() > 0) diffLevels.add(diffLevel);
      else break;
    } while (true);

    // read music files
    ArrayList<String> music = new ArrayList<String>(); // <String>
    String track;
    idx = 0;
    do {
      track = props.get("music_" + Integer.toString(idx++), "");
      if (track.length() > 0) music.add(track);
    } while (track.length() > 0);
    // read levels
    String levelStr[];
    String def[] = {""};
    for (int diff = 0; diff < diffLevels.size(); diff++) {
      idx = 0;
      ArrayList<LevelInfo> levels = new ArrayList<LevelInfo>(); // <LevelInfo>
      diffLevel = diffLevels.get(diff);
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
      lvlInfo.add(levels);
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
  public ArrayList<String> getDiffLevels() {
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
    return lvlInfo.get(diffLvl).get(level);
  }

  /**
   * Get level info for a certain level.
   *
   * @param diffLvl difficulty level
   * @param level level number
   * @return LevelInfo for the given level
   */
  public int getLevelCount(final int diffLvl) {
    return lvlInfo.get(diffLvl).size();
  }

  /**
   * Return all levels for a given difficulty
   *
   * @param diffLevel number of difficulty level
   * @return level names as string array
   */
  public ArrayList<String> getLevels(final int diffLevel) {
    ArrayList<String> names = new ArrayList<String>(lvlInfo.get(diffLevel).size());
    for (var e : lvlInfo.get(diffLevel)) names.add(e.getName());
    return names;
  }
}
