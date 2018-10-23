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
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/*
 * Copyright 2009 Volker Oth
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

	private static GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

	/**
	 * Create a compatible buffered image.
	 * @param width width of image in pixels
	 * @param height height of image in pixels
	 * @param transparency {@link java.awt.Transparency}
	 * @return compatible buffered image
	 */
	public static BufferedImage createImage(final int width, final int height, final int transparency) {
		BufferedImage b = gc.createCompatibleImage(width, height, transparency);
		return b;
	}

	/**
	 * Create a compatible buffered image from an image.
	 * @param img existing {@link java.awt.Image}
	 * @param transparency {@link java.awt.Transparency}
	 * @return compatible buffered image
	 */	public static BufferedImage ImageToBuffered(final Image img, final int transparency) {
		 BufferedImage bImg = createImage(img.getWidth(null), img.getHeight(null), transparency);
		 Graphics2D g = bImg.createGraphics();
		 g.drawImage(img, 0, 0, null);
		 g.dispose();
		 return bImg;
	 }

	 /**
	  * Return an array of buffered images which contain an animation.
	  * @param img image containing all the frames one above each other
	  * @param frames number of frames
	  * @param transparency {@link java.awt.Transparency}
	  * @return an array of buffered images which contain an animation
	  */
	 public static BufferedImage[] getAnimation(final Image img, final int frames, final int transparency) {
		 int width = img.getWidth(null);
		 return getAnimation(img, frames, transparency, width);
	 }

	 /**
	  * Return an array of buffered images which contain an animation.
	  * @param img image containing all the frames one above each other
	  * @param frames number of frames
	  * @param transparency {@link java.awt.Transparency}
	  * @param width image width
	  * @return an array of buffered images which contain an animation
	  */
	 public static BufferedImage[] getAnimation(final Image img, final int frames, final int transparency, final int width) {
		 int height = img.getHeight(null)/frames;
		 // characters stored one above the other - now separate them into single images
		 ArrayList<BufferedImage> arrImg = new ArrayList<BufferedImage>(frames);
		 int y0 = 0;
		 for (int i=0; i<frames; i++, y0+=height) {
			 BufferedImage frame = createImage(width, height, transparency);
			 Graphics2D g = frame.createGraphics();
			 g.drawImage(img, 0, 0, width, height, 0, y0, width, y0+height, null);
			 arrImg.add(frame);
			 g.dispose();
		 }
		 BufferedImage images[] = new BufferedImage[arrImg.size()];
		 return arrImg.toArray(images);
	 }

	 /**
	  * Flip image in X direction.
	  * @param img image to flip
	  * @return flipped image
	  */
	 public static BufferedImage flipImageX(final BufferedImage img) {
		 BufferedImage trg = createImage(img.getWidth(), img.getHeight(), img.getColorModel().getTransparency());
		 // affine transform for flipping
		 AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
		 tx.translate(-img.getWidth(), 0);
		 AffineTransformOp op = new AffineTransformOp(tx,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		 return op.filter(img, trg);
	 }

	 /**
	  * Use the Loader to find a file.
	  * @param fname file name
	  * @return URL of the file
	  */
	 public static URL findFile(final String fname) {
		 ClassLoader loader = ToolBox.class.getClassLoader();
		 return loader.getResource(fname);
	 }

	 /**
	  * Add (system default) path separator to string (if there isn't one already).
	  * @param fName String containing path name
	  * @return String that ends with the (system default) path separator for sure
	  */
	 public static String addSeparator(final String fName) {
		 int pos = fName.lastIndexOf(File.separator);
		 if (pos != fName.length()-1)
			 pos = fName.lastIndexOf("/");
		 if (pos != fName.length()-1)
			 return fName + "/";
		 else return fName;
	 }

	 /**
	  * Exchange any DOS style path separator ("\") with a Unix style separator ("/").
	  * @param fName String containing file/path name
	  * @return String with only Unix style path separators
	  */
	 public static String exchangeSeparators(final String fName) {
		 int pos;
		 StringBuffer sb = new StringBuffer(fName);
		 while ( (pos = sb.indexOf("\\")) != -1 )
			 sb.setCharAt(pos,'/');
		 return sb.toString();
	 }

	 /**
	  * Return file name from path.
	  * @param path String of a path with a file name
	  * @return String containing only the file name
	  */
	 public static String getFileName(final String path) {
		 int p1 = path.lastIndexOf("/");
		 int p2 = path.lastIndexOf("\\");
		 if (p2 > p1)
			 p1 = p2;
		 if (p1 < 0)
			 p1 = 0;
		 else
			 p1++;
		 return path.substring(p1);
	 }

	 /**
	  * Returns the extension (".XXX") of a filename without the dot.
	  * @param path String containing file name
	  * @return String containing only the extension (without the dot) or null (if no extension found)
	  */
	 public static String getExtension(final String path) {
		 int p1 = path.lastIndexOf("/");
		 int p2 = path.lastIndexOf("\\");
		 int p = path.lastIndexOf(".");
		 if (p==-1 || p<p1 || p<p2)
			 return null;
		 return path.substring(p+1);
	 }


	 /**
	  * Returns the first few bytes of a file to check its type.
	  * @param fname Filename of the file
	  * @param num Number of bytes to return
	  * @return Array of bytes (size num) from the beginning of the file
	  */
	 public static byte[] getFileID(final String fname, final int num) {
		 byte buf[] = new byte[num];
		 File f = new File(fname);
		 if (f.length() < num)
			 return null;
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
	  * Get path name from absolute file name.
	  * @param path absolute file name
	  * @return path name without the separator
	  */
	 public static String getPathName(final String path) {
		 int p1 = path.lastIndexOf("/");
		 int p2 = path.lastIndexOf("\\");
		 if (p2 > p1)
			 p1 = p2;
		 if (p1 < 0)
			 p1 = 0;
		 return path.substring(0,p1);
	 }

	 /**
	  * Show exception message box.
	  * @param ex exception
	  */
	 public static void showException(final Throwable ex) {
		 String m;
		 m = "<html>";
		 m += ex.getClass().getName()+"<p>";
		 if (ex.getMessage() != null)
			 m += ex.getMessage() +"<p>";
		 StackTraceElement ste[] = ex.getStackTrace();
		 for (int i=0; i<ste.length; i++)
			 m += ste[i].toString()+"<p>";
		 m += "</html>";
		 ex.printStackTrace();
		 JOptionPane.showMessageDialog( null, m, "Error", JOptionPane.ERROR_MESSAGE );
		 ex.printStackTrace();
	 }

	 /**
	  * Open file dialog.
	  * @param parent parent frame
	  * @param path default file name
	  * @param ext array of allowed extensions
	  * @param load true: load, false: save
	  * @return absolute file name of selected file or null
	  */
	 public static String getFileName(final Component parent, final String path, final String ext[], final boolean load) {
		 String p = path;
		 if (p.length() == 0)
			 p = ".";
		 JFileChooser jf = new JFileChooser(p);
		 if (ext != null) {
			 JFileFilter filter = new JFileFilter();
			 for (int i=0; i<ext.length; i++)
				 filter.addExtension(ext[i]);
			 jf.setFileFilter(filter);
		 }
		 jf.setFileSelectionMode(JFileChooser.FILES_ONLY );
		 if (!load)
			 jf.setDialogType(JFileChooser.SAVE_DIALOG);
		 int returnVal = jf.showDialog(parent,null);
		 if(returnVal == JFileChooser.APPROVE_OPTION) {
			 File f = jf.getSelectedFile();
			 if (f != null)
				 return f.getAbsolutePath();
		 }
		 return null;
	 }
}
