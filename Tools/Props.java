package Tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

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
 * Property class to ease use of ini files to save/load properties.
 *
 * @author Volker Oth
 */
public class Props {

  /** extended hash to store properties */
  private final Properties hash;

  /** header string */
  private String header;

  /** file name */
  private String propertyFileName;

  /** Constructor */
  public Props() {
    hash = new Properties();
    header = new String();
  }

  /**
   * Set the property file header
   *
   * @param h String containing Header information
   */
  public void setHeader(final String h) {
    header = h;
  }

  /** Clear all properties */
  public void clear() {
    hash.clear();
  }

  /**
   * Remove key
   *
   * @param key Name of key
   */
  public void remove(final String key) {
    hash.remove(key);
  }

  /**
   * Set string property
   *
   * @param key Name of the key to set value for
   * @param value Value to set
   */
  public void set(final String key, final String value) {
    hash.setProperty(key, value);
  }

  /**
   * Set integer property
   *
   * @param key Name of the key to set value for
   * @param value Value to set
   */
  public void set(final String key, final int value) {
    hash.setProperty(key, String.valueOf(value));
  }

  /**
   * Set boolean property
   *
   * @param key Name of the key to set value for
   * @param value Value to set
   */
  public void set(final String key, final boolean value) {
    hash.setProperty(key, String.valueOf(value));
  }

  /**
   * Set double property
   *
   * @param key Name of the key to set value for
   * @param value Value to set
   */
  public void set(final String key, final double value) {
    hash.setProperty(key, String.valueOf(value));
  }

  /**
   * Get string property
   *
   * @param key Name of the key to get value for
   * @param def Default value in case key is not found
   * @return Value of key as String
   */
  public String get(final String key, final String def) {
    String s = hash.getProperty(key, def);
    return removeComment(s).trim();
  }

  /**
   * Get integer property
   *
   * @param key Name of the key to get value for
   * @param def Default value in case key is not found
   * @return Value of key as int
   */
  public int get(final String key, final int def) {
    String s = hash.getProperty(key);
    if (s == null) return def;
    s = removeComment(s);
    return parseString(s);
  }

  /**
   * Get integer array property
   *
   * @param key Name of the key to get value for
   * @param def Default value in case key is not found
   * @return Value of key as array of int
   */
  public int[] get(final String key, final int def[]) {
    String s = hash.getProperty(key);
    if (s == null) return def;
    s = removeComment(s);
    String members[] = s.split(",");
    // remove trailing and leading spaces
    for (int i = 0; i < members.length; i++) members[i] = members[i].trim();

    int ret[];
    ret = new int[members.length];
    for (int i = 0; i < members.length; i++) ret[i] = parseString(members[i]);

    return ret;
  }

  /**
   * Get string array property
   *
   * @param key Name of the key to get value for
   * @param def Default value in case key is not found
   * @return Value of key as array of string
   */
  public String[] get(final String key, final String def[]) {
    String s = hash.getProperty(key);
    if (s == null) return def;
    s = removeComment(s);
    String members[] = s.split(",");
    // remove trailing and leading spaces
    for (int i = 0; i < members.length; i++) members[i] = members[i].trim();

    return members;
  }

  /**
   * Get boolean property
   *
   * @param key Name of the key to get value for
   * @param def Default value in case key is not found
   * @return Value of key as boolean
   */
  public boolean get(final String key, final boolean def) {
    String s = hash.getProperty(key);
    if (s == null) return def;
    s = removeComment(s);
    return Boolean.valueOf(s).booleanValue();
  }

  /**
   * Get double property
   *
   * @param key Name of the key to get value for
   * @param def default value in case key is not found
   * @return value of key as double
   */
  public double get(final String key, final double def) {
    String s = hash.getProperty(key);
    if (s == null) return def;
    s = removeComment(s);
    return Double.valueOf(s).doubleValue();
  }

  /**
   * Save property file
   *
   * @param fname File name of property file
   * @return True if OK, false if exception occurred
   */
  public boolean save() {
    if (propertyFileName == null) {
      // throw new Exception("Can't save to a property file created from a URL.");
      System.out.println("Can't save to a property file created from a URL.");
      return false;
    } else {
      try {
        FileOutputStream f = new FileOutputStream(propertyFileName);
        hash.store(f, header);
        return true;
      } catch (IOException e) {
        return false;
      }
    }
  }

  /**
   * Load property file
   *
   * @param file File handle of property file
   * @return True if OK, false if exception occurred
   */
  public boolean load(final URL file) {
    propertyFileName = null;

    try {
      InputStream f = file.openStream();
      hash.load(f);
      f.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Load property file
   *
   * @param fname File name of property file
   * @return True if OK, false if exception occurred
   */
  public boolean load(final String fname) {
    propertyFileName = fname;

    try {
      FileInputStream f = new FileInputStream(fname);
      hash.load(f);
      f.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Parse hex, binary or octal number
   *
   * @param s String that contains one number
   * @return Integer value of string
   */
  private static int parseString(final String s) {
    if (s == null || s.isEmpty()) return -1;

    try {
      if (s.charAt(0) == '0') {
        if (s.length() == 1) return 0;

        switch (s.charAt(1)) {
          case 'x':
            return Integer.parseInt(s.substring(2), 16);
          case 'b':
            return Integer.parseInt(s.substring(2), 2);
          default:
            return Integer.parseInt(s, 8);
        }
      } else {
        return Integer.parseInt(s);
      }
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  /**
   * Remove comment from line. Comment character is "#". Everything behind (including "#") will be
   * removed
   *
   * @param s String to search for comment
   * @return String without comment
   */
  private String removeComment(final String s) {
    int pos = s.indexOf('#');
    if (pos != -1) return s.substring(0, pos);
    else return s;
  }
}
