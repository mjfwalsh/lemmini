package Tools;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.swing.JFileChooser;
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
 * Selection of utility functions.
 *
 * @author Volker Oth
 */
public class ToolBox {

  private static GraphicsConfiguration gc =
      GraphicsEnvironment.getLocalGraphicsEnvironment()
          .getDefaultScreenDevice()
          .getDefaultConfiguration();

  /**
   * Create a compatible buffered image.
   *
   * @param width width of image in pixels
   * @param height height of image in pixels
   * @param transparency {@link java.awt.Transparency}
   * @return compatible buffered image
   */
  public static BufferedImage createImage(
      final int width, final int height, final int transparency) {
    BufferedImage b = gc.createCompatibleImage(width, height, transparency);
    return b;
  }

  /**
   * Create a compatible buffered image from an image.
   *
   * @param img existing {@link java.awt.Image}
   * @param transparency {@link java.awt.Transparency}
   * @return compatible buffered image
   */
  public static BufferedImage ImageToBuffered(final Image img, final int transparency) {
    BufferedImage bImg = createImage(img.getWidth(null), img.getHeight(null), transparency);
    Graphics2D g = bImg.createGraphics();
    g.drawImage(img, 0, 0, null);
    g.dispose();
    return bImg;
  }

  /**
   * Return an array of buffered images which contain an animation.
   *
   * @param img image containing all the frames one above each other
   * @param frames number of frames
   * @param transparency {@link java.awt.Transparency}
   * @return an array of buffered images which contain an animation
   */
  public static ArrayList<BufferedImage> getAnimation(
      final Image img, final int frames, final int transparency) {
    int width = img.getWidth(null);
    return getAnimation(img, frames, transparency, width);
  }

  /**
   * Return an array of buffered images which contain an animation.
   *
   * @param img image containing all the frames one above each other
   * @param frames number of frames
   * @param transparency {@link java.awt.Transparency}
   * @param width image width
   * @return an array of buffered images which contain an animation
   */
  public static ArrayList<BufferedImage> getAnimation(
      final Image img, final int frames, final int transparency, final int width) {
    int height = img.getHeight(null) / frames;
    // characters stored one above the other - now separate them into single images
    ArrayList<BufferedImage> arrImg = new ArrayList<BufferedImage>(frames);
    int y0 = 0;
    for (int i = 0; i < frames; i++, y0 += height) {
      BufferedImage frame = createImage(width, height, transparency);
      Graphics2D g = frame.createGraphics();
      g.drawImage(img, 0, 0, width, height, 0, y0, width, y0 + height, null);
      arrImg.add(frame);
      g.dispose();
    }
    return arrImg;
  }

  /**
   * Flip image in X direction.
   *
   * @param img image to flip
   * @return flipped image
   */
  public static BufferedImage flipImageX(final BufferedImage img) {
    BufferedImage trg =
        createImage(img.getWidth(), img.getHeight(), img.getColorModel().getTransparency());
    // affine transform for flipping
    AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
    tx.translate(-img.getWidth(), 0);
    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    return op.filter(img, trg);
  }

  /**
   * Returns the extension (".XXX") of a filename without the dot.
   *
   * @param path String containing file name
   * @return String containing only the extension (without the dot) or null (if no extension found)
   */
  public static String getExtension(final File file) {
    String filename = file.getName();
    int i = filename.lastIndexOf('.');
    if (i > 0 && i < filename.length() - 1) {
      return filename.substring(i + 1).toLowerCase();
    }
    return null;
  }

  /**
   * Returns the first few bytes of a file to check its type.
   *
   * @param fname Filename of the file
   * @param num Number of bytes to return
   * @return Array of bytes (size num) from the beginning of the file
   */
  public static byte[] getFileID(final String fname, final int num) {
    byte buf[] = new byte[num];
    File f = new File(fname);
    if (f.length() < num) return null;
    try {
      FileInputStream fi = new FileInputStream(fname);
      fi.read(buf);
      fi.close();
    } catch (Exception ex) {
      return null;
    }
    return buf;
  }

  /**
   * Show exception message box.
   *
   * @param ex exception
   */
  public static void showException(final Throwable ex) {
    String m = "<html><body>";
    m += ex.getClass().getName() + "<br>";
    if (ex.getMessage() != null) m += ex.getMessage() + "<br>";
    for (StackTraceElement ste : ex.getStackTrace()) m += ste.toString() + "<br>";
    m += "</body></html>";
    ex.printStackTrace();
    JOptionPane.showMessageDialog(null, m, "Error", JOptionPane.ERROR_MESSAGE);
    ex.printStackTrace();
  }

  /**
   * Open folder dialog.
   *
   * @param parent parent frame
   * @return absolute file path of selected folder or null
   */
  public static Path getFolderName(final Component parent) {
    JFileChooser jf = new JFileChooser();
    jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int returnVal = jf.showDialog(parent, null);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File f = jf.getSelectedFile();
      if (f != null) return f.getAbsoluteFile().toPath();
    }
    return null;
  }
}
