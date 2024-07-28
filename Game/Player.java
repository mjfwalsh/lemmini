package Game;

import Tools.Props;
import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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
 * Stores player progress.
 *
 * @author Volker Oth
 */
public class Player {

  /** property class to store player settings persistently */
  private Props props;

  /** used to store level progress */
  private HashMap<String, BigInteger> lvlGroup;

  /** cheat mode enabled? */
  private boolean cheat;

  /** player's name */
  private String name;

  /** last difficulty level played */
  private String[] curDifLevel;

  /**
   * Constructor.
   *
   * @param n player's name
   */
  public Player(final String n) {
    name = n;
    lvlGroup = new HashMap<String, BigInteger>();
    // read main ini file
    props = new Props();
    // create players directory if it doesn't exist
    File dest = Core.findResource("players");
    dest.mkdirs();

    // might exist or not - if not, it's created
    if (props.load(Core.findResource("players/" + name + ".ini"))) {
      // file existed, now extract entries
      String sdef[] = {null, null};
      for (int idx = 0; true; idx++) {
        String s[] = props.get("group" + Integer.toString(idx), sdef);
        if (s == null || s.length != 2 || s[0] == null) break;
        // first string is the level group key identifier
        // second string is a BigInteger used as bitfield to store won levels
        lvlGroup.put(s[0], new BigInteger(s[1]));
      }
    }

    // get the difficulty level the player is on
    String cdl[] = {null, null};
    curDifLevel = props.get("curDifLevel", cdl);

    // cheat mode
    cheat = false;
  }

  /** Enable cheat mode for this player. */
  public void enableCheatMode() {
    cheat = true;
  }

  /** Store player's progress. */
  public void store() {
    Set<String> k = lvlGroup.keySet();
    Iterator<String> it = k.iterator();
    int idx = 0;
    while (it.hasNext()) {
      String s = it.next();
      BigInteger bf = lvlGroup.get(s);
      String sout = s + ", " + bf.toString();
      props.set("group" + Integer.toString(idx++), sout);
    }
    props.save();
  }

  /**
   * Allow a level to be played.
   *
   * @param pack level pack
   * @param diff difficulty level
   * @param num level number
   * @return updated bitfield
   */
  public BigInteger setAvailable(final String pack, final String diff, final int num) {
    // get current bitfield
    String id = LevelPack.getID(pack, diff);
    BigInteger bf = lvlGroup.get(id);
    if (bf == null) bf = BigInteger.ONE; // first level is always available
    bf = bf.setBit(num); // set bit in bitfield (just overwrite existing bit)
    // store new value
    lvlGroup.put(id, bf);
    return bf;
  }

  /**
   * Check if player is allowed to play a level.
   *
   * @param pack level pack
   * @param diff difficulty level
   * @param num level number
   * @return true if allowed, false if not
   */
  public boolean isAvailable(final String pack, final String diff, final int num) {
    if (isCheat()) return true;
    // get current bitfield
    String id = LevelPack.getID(pack, diff);
    BigInteger bf = lvlGroup.get(id);
    if (bf == null) bf = BigInteger.ONE; // first level is always available
    return (bf.testBit(num));
  }

  /**
   * Check if player is allowed to play a level.
   *
   * @param bf bitfield containing the approval information for all levels of this pack/difficulty
   * @param num number of level
   * @return true if allowed, false if not
   */
  public boolean isAvailable(final BigInteger bf, final int num) {
    if (isCheat()) return true;
    return (bf.testBit(num));
  }

  /**
   * Get bitfield containing the approval information for all levels of this pack/difficulty.
   *
   * @param pack level pack
   * @param diff difficulty level
   * @return bitfield containing the approval information for all levels of this pack/difficulty
   */
  public BigInteger getBitField(final String pack, final String diff) {
    if (isCheat())
      return new BigInteger(
          "18446744073709551615"); // 0xffffffffffffffff (8 bytes with all bits set)

    String id = LevelPack.getID(pack, diff);
    BigInteger bf = lvlGroup.get(id);
    if (bf == null) return BigInteger.ONE;
    return bf;
  }

  /**
   * Get int of the hightest level to be completed in a level of difficulty.
   *
   * @param pack level pack
   * @param diff difficulty level
   * @return bitfield containing the approval information for all levels of this pack/difficulty
   */
  public int getCompletedLevelNum(final String pack, final String diff) {
    BigInteger bf = getBitField(pack, diff);
    return bf.bitLength() - 1;
  }

  /**
   * Get player's name.
   *
   * @return player's name
   */
  public String getName() {
    return name;
  }

  /**
   * Get cheat state.
   *
   * @return true if cheat is enabled
   */
  public boolean isCheat() {
    return cheat;
  }

  /**
   * Set last successful level played
   *
   * @param pack level pack
   * @param diff difficulty level
   * @param num level number
   */
  public void setCurDifLevel(String lp, String dl) {
    curDifLevel[0] = lp;
    curDifLevel[1] = dl;
    props.set("curDifLevel", lp.toLowerCase() + "," + dl.toLowerCase());
  }

  /**
   * Get last successful level played
   *
   * @return {int, int, int}
   */
  public String[] getCurDifLevel() {
    return curDifLevel;
  }
}
