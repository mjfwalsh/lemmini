package Extract;

import Tools.Props;
import Tools.ToolBox;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.Adler32;

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
 * Extraction of resources.
 *
 * @author Volker Oth
 */
public class Extract extends Thread {

  /** file name of extraction configuration */
  private static final String iniName = "extract.ini";

  /** file name of patching configuration */
  private static final String patchIniName = "patch.ini";

  /** file name of resource CRCs (WINLEMM) */
  private static final String crcIniName = "crc.ini";

  /** array of extensions to be ignored - read from ini */
  private String ignoreExt[];

  /** output dialog */
  private OutputDialog outputDiag;

  /** source path (WINLEMM) for extraction */
  private File sourcePath;

  /** destination path (Lemmini resource) for extraction */
  private File destinationPath;

  /** path of the DIF files */
  private static final String patchPath = "patch/";

  /** success flag */
  private boolean extractedSuccessfully = false;

  /** reference to class loader */
  private static ClassLoader loader = Extract.class.getClassLoader();

  /* (non-Javadoc)
   * @see java.lang.Thread#run()
   *
   * Extraction running in a Thread.
   */
  @Override
  public void run() {
    try {
      // read ini file
      Props props = new Props();
      URL fn = findFile(iniName);
      if (fn == null || !props.load(fn))
        throw new ExtractException("File " + iniName + " not found or error while reading");

      ignoreExt = props.get("ignore_ext", ignoreExt);

      // prolog_ check CRC
      print("\nValidating WINLEMM");
      URL fncrc = findFile(crcIniName);
      Props cprops = new Props();
      if (fncrc == null || !cprops.load(fncrc))
        throw new ExtractException("File " + crcIniName + " not found or error while reading");
      for (int i = 0; true; i++) {
        String crcbuf[] = {null, null, null};
        // 0: name, 1:size, 2: crc
        crcbuf = cprops.get("crc_" + Integer.toString(i), crcbuf);
        if (crcbuf[0] == null) break;
        print(crcbuf[0]);

        File f = new File(sourcePath, crcbuf[0]);
        if (!f.exists())
          throw new ExtractException("File not found: " + f.getAbsolutePath() + ".\n");

        if (f.length() != Long.parseLong(crcbuf[1]))
          throw new ExtractException("CRC error for file " + f.getAbsolutePath() + ".\n");

        byte src[] = readFile(f);
        Adler32 crc32 = new Adler32();
        crc32.update(src);
        if (Long.toHexString(crc32.getValue()).compareToIgnoreCase(crcbuf[2].substring(2)) != 0)
          throw new ExtractException("CRC error for file " + f.getAbsolutePath() + ".\n");

        if (outputDiag.isCancelled()) return;
      }

      // step one: extract the levels
      print("\nExtracting levels");
      for (int i = 0; true; i++) {
        String lvls[] = {null, null};
        // 0: srcPath, 1: destPath
        lvls = props.get("level_" + Integer.toString(i), lvls);
        if (lvls[0] == null) break;
        extractLevels(new File(sourcePath, lvls[0]), new File(destinationPath, lvls[1]));
        if (outputDiag.isCancelled()) return;
      }

      // step two: extract the styles
      print("\nExtracting styles");
      ExtractSPR sprite = new ExtractSPR();
      for (int i = 0; true; i++) {
        String styles[] = {null, null, null, null};
        // 0:SPR, 1:PAL, 2:path, 3:fname
        styles = props.get("style_" + Integer.toString(i), styles);
        if (styles[0] == null) break;
        print(styles[3]);
        File dest = new File(destinationPath, styles[2]);
        dest.mkdirs();
        // load palette and sprite
        sprite.loadPalette(sourcePath, styles[1]);
        sprite.loadSPR(sourcePath, styles[0]);
        sprite.saveAll(dest, styles[3], false);

        if (outputDiag.isCancelled()) return;
      }

      // step three: extract the objects
      print("\nExtracting objects");
      for (int i = 0; true; i++) {
        String object[] = {null, null, null, null};
        // 0:SPR, 1:PAL, 2:resource, 3:path
        object = props.get("objects_" + Integer.toString(i), object);
        if (object[0] == null) break;
        print(object[0]);
        File dest = new File(destinationPath, object[3]);
        dest.mkdirs();
        // load palette and sprite
        sprite.loadPalette(sourcePath, object[1]);
        sprite.loadSPR(sourcePath, object[0]);
        for (int j = 0; true; j++) {
          String member[] = {null, null, null};
          // 0:idx, 1:frames, 2:name
          member = props.get(object[2] + "_" + Integer.toString(j), member);
          if (member[0] == null) break;
          // save object
          sprite.saveAnim(
              new File(dest, member[2]), Integer.parseInt(member[0]), Integer.parseInt(member[1]));
          if (outputDiag.isCancelled()) return;
        }
      }

      // if (false) { // debug only

      // step four: create directories
      print("\nCreate directories");
      for (int i = 0; true; i++) {
        // 0: path
        String path = props.get("mkdir_" + Integer.toString(i), "");
        if (path.length() == 0) break;
        print(path);
        File dest = new File(destinationPath, path);
        dest.mkdirs();
        if (outputDiag.isCancelled()) return;
      }

      // step five: copy stuff
      print("\nCopy files");
      for (int i = 0; true; i++) {
        String copy[] = {null, null};
        // 0: srcName, 1: destName
        copy = props.get("copy_" + Integer.toString(i), copy);
        if (copy[0] == null) break;
        copyFile(new File(sourcePath, copy[0]), new File(destinationPath, copy[1]));
        if (outputDiag.isCancelled()) return;
      }

      // step five: clone files inside destination dir
      print("\nClone files");
      for (int i = 0; true; i++) {
        String clone[] = {null, null};
        // 0: srcName, 1: destName
        clone = props.get("clone_" + Integer.toString(i), clone);
        if (clone[0] == null) break;
        copyFile(new File(destinationPath, clone[0]), new File(destinationPath, clone[1]));
        if (outputDiag.isCancelled()) return;
      }

      // step eight: use patch.ini to extract/patch all files
      // read patch.ini file
      Props pprops = new Props();
      URL fnp = findFile(patchPath + patchIniName); // if it's in the JAR or local directory
      if (!pprops.load(fnp))
        throw new ExtractException("File " + patchIniName + " not found or error while reading");
      // copy
      print("\nExtract files");
      for (int i = 0; true; i++) {
        String copy[] = {null, null};
        // 0: name 1: crc
        copy = pprops.get("extract_" + Integer.toString(i), copy);
        if (copy[0] == null) break;
        print(copy[0]);
        String fnDecorated = copy[0].replace('/', '@');
        URL fnc = findFile(patchPath + fnDecorated);
        copyFile(fnc, new File(destinationPath, copy[0]));
        if (outputDiag.isCancelled()) return;
      }
      // patch
      print("\nPatch files");
      for (int i = 0; true; i++) {
        String ppath[] = {null, null};
        // 0: name 1: crc
        ppath = pprops.get("patch_" + Integer.toString(i), ppath);
        if (ppath[0] == null) break;
        print(ppath[0]);
        String fnDif = ppath[0].replace('/', '@');
        int pos = fnDif.toLowerCase().lastIndexOf('.');
        if (pos == -1) pos = fnDif.length();
        fnDif = fnDif.substring(0, pos) + ".dif";

        URL urlDif = findFile(patchPath + fnDif);
        File fileSrc = new File(destinationPath, ppath[0]);

        if (urlDif == null)
          throw new ExtractException(
              "Patching of file " + fileSrc.getAbsolutePath() + " failed.\n");
        byte dif[] = readFile(urlDif);
        byte src[] = readFile(fileSrc);

        try {
          byte trg[] = Diff.patchbuffers(src, dif);
          // write new file
          writeFile(new File(destinationPath, ppath[0]), trg);
        } catch (DiffException ex) {
          throw new ExtractException(
              "Patching of file "
                  + destinationPath.getAbsolutePath()
                  + "/"
                  + ppath[0]
                  + " failed.\n"
                  + ex.getMessage());
        }
        if (outputDiag.isCancelled()) return;
      }

      // finished
      print("\nSuccessfully finished!");
      extractedSuccessfully = true;
    } catch (ExtractException ex) {
      print(ex.getMessage());
    } catch (Exception | Error ex) {
      ToolBox.showException(ex);
    }
    outputDiag.enableOk();
  }

  /**
   * Get source path (WINLEMM) for extraction.
   *
   * @return source path (WINLEMM) for extraction
   */
  public boolean extractionSuccessful() {
    return extractedSuccessfully;
  }

  /**
   * Extract all resources
   *
   * @param srcPath WINLEMM directory
   * @param dstPath target (installation) directory. May also be a relative path inside JAR
   */
  public Extract(final File srcPath, final File dstPath) {
    sourcePath = srcPath;
    destinationPath = dstPath;

    // open output dialog
    outputDiag = new OutputDialog();

    // start thread
    start();

    outputDiag.setVisible(true);
    try {
      join();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }

  /**
   * Extract the level INI files from LVL files
   *
   * @param r name of root folder (source of LVL files)
   * @param dest destination folder for extraction (resource folder)
   * @throws ExtractException
   */
  private void extractLevels(final File root, final File dest) throws ExtractException {
    // first extract the levels
    FilenameFilter ff = new LvlFilter();
    dest.mkdirs();

    File[] levels = root.listFiles(ff);
    if (levels == null)
      throw new ExtractException("Path " + root + " doesn't exist or IO error occured.");
    for (File level : levels) {
      String n = level.getName();
      int pos = n.length() - 4; // file MUST end with ".lvl" because of file filter
      n = n.substring(0, pos).toLowerCase() + ".ini";
      File fOut = new File(dest, n);

      try {
        print(level.getName());
        ExtractLevel.convertLevel(level, fOut);
      } catch (Exception ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.length() > 0) print(ex.getMessage());
        else print(ex.toString());
        throw new ExtractException(msg);
      }
    }
  }

  /**
   * Copy a file.
   *
   * @param source URL of source file
   * @param destination full destination file name including path
   * @throws FileNotFoundException
   * @throws IOException
   */
  private static void copyFile(final URL source, final File destination) throws ExtractException {
    try {
      InputStream fSrc = source.openStream();
      FileOutputStream fDest = new FileOutputStream(destination);
      byte buffer[] = new byte[4096];
      int len;

      while ((len = fSrc.read(buffer)) != -1) fDest.write(buffer, 0, len);
      fSrc.close();
      fDest.close();
    } catch (Exception ex) {
      throw new ExtractException(
          "Copying " + source.toString() + " to " + destination.getAbsolutePath() + " failed");
    }
  }

  /**
   * Copy a file.
   *
   * @param source full source file name including path
   * @param destination full destination file name including path
   * @throws FileNotFoundException
   * @throws IOException
   */
  private static void copyFile(final File source, final File destination) throws ExtractException {
    try {
      FileInputStream fSrc = new FileInputStream(source);
      FileOutputStream fDest = new FileOutputStream(destination);
      byte buffer[] = new byte[4096];
      int len;

      while ((len = fSrc.read(buffer)) != -1) fDest.write(buffer, 0, len);
      fSrc.close();
      fDest.close();
    } catch (Exception ex) {
      throw new ExtractException(
          "Copying "
              + source.getAbsolutePath()
              + " to "
              + destination.getAbsolutePath()
              + " failed");
    }
  }

  /**
   * Read file into an array of byte.
   *
   * @param fname file name
   * @return array of byte
   * @throws ExtractException
   */
  private static byte[] readFile(final File file) throws ExtractException {
    byte buf[] = null;
    try {
      int len = (int) file.length();
      FileInputStream f = new FileInputStream(file);
      buf = new byte[len];
      f.read(buf);
      f.close();
      return buf;
    } catch (FileNotFoundException ex) {
      throw new ExtractException("File " + file.getAbsolutePath() + " not found");
    } catch (IOException ex) {
      throw new ExtractException("IO exception while reading file " + file.getAbsolutePath());
    }
  }

  /**
   * Read file into an array of byte.
   *
   * @param fname file name as URL
   * @return array of byte
   * @throws ExtractException
   */
  private static byte[] readFile(final URL fname) throws ExtractException {
    byte buf[] = null;
    try {
      InputStream f = fname.openStream();
      byte buffer[] = new byte[4096];
      // URLs/InputStreams suck: we can't read a length
      int len;
      ArrayList<Byte> lbuf = new ArrayList<Byte>();

      while ((len = f.read(buffer)) != -1) {
        for (int i = 0; i < len; i++) lbuf.add(Byte.valueOf(buffer[i]));
      }
      f.close();

      // reconstruct byte array from ArrayList
      buf = new byte[lbuf.size()];
      for (int i = 0; i < lbuf.size(); i++) buf[i] = lbuf.get(i).byteValue();

      return buf;
    } catch (FileNotFoundException ex) {
      throw new ExtractException("File " + fname + " not found");
    } catch (IOException ex) {
      throw new ExtractException("IO exception while reading file " + fname);
    }
  }

  /**
   * Write array of byte to file.
   *
   * @param fname file name
   * @param buf array of byte
   * @throws ExtractException
   */
  private static void writeFile(final File fname, final byte buf[]) throws ExtractException {
    try {
      FileOutputStream f = new FileOutputStream(fname);
      f.write(buf);
      f.close();
    } catch (IOException ex) {
      throw new ExtractException("IO exception while writing file " + fname.getAbsolutePath());
    }
  }

  /**
   * Find a file.
   *
   * @param fname File name (without absolute path)
   * @return URL to file
   */
  private static URL findFile(final String fname) {
    URL retval = loader.getResource(fname);
    try {
      if (retval == null) retval = new File(fname).toURI().toURL();
      return retval;
    } catch (MalformedURLException ex) {
    }
    return null;
  }

  /**
   * Print string to output dialog.
   *
   * @param s string to print
   */
  private void print(final String s) {
    // System.out.println(s);
    if (outputDiag != null) outputDiag.print(s + "\n");
  }

  /** Extract a single file from the jar where necessary */
  public static void extractSingleFile(String from, File newFontFile) {
    if (newFontFile.exists()) return;

    try {
      URL fontFile = findFile(from);
      copyFile(fontFile, newFontFile);
    } catch (Exception ex) {
    }
  }
}

/**
 * File name filter for level files.
 *
 * @author Volker Oth
 */
class LvlFilter implements FilenameFilter {

  /* (non-Javadoc)
   * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
   */
  @Override
  public boolean accept(final File dir, final String name) {
    if (name.toLowerCase().endsWith(".lvl")) return true;
    else return false;
  }
}
