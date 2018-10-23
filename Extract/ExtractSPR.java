package Extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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
 * Extract graphics from "Lemming for Win95" SPR data files.
 * @author Volker Oth
 */
public class ExtractSPR {
	/** palette index of transparent color */
	private final static int transparentIndex = 0;

	/** array of GIF images to store to disk */
	private GIFImage images[];
	/** color palette */
	private Palette palette = null;
	/** buffer used to compress the palette (remove double entries) to work around issues on MacOS */
	private int lookupBuffer[];

	/**
	 * Load palette.
	 * @param fname Name of palette file
	 * @return ColorModel representation of Palette
	 * @throws ExtractException
	 */
	Palette loadPalette(final String fname) throws ExtractException {
		byte buffer[];

		// read file into buffer
		int paletteSize = 0;
		try {
			File f = new File(fname);
			FileInputStream fi = new FileInputStream(fname);
			buffer = new byte[(int)f.length()];
			fi.read(buffer);
			fi.close();
		} catch(FileNotFoundException e) {
			throw new ExtractException("File "+fname+" not found");
		}
		catch(IOException e) {
			throw new ExtractException("I/O error while reading "+fname);
		}
		// check header
		if (buffer[0] != 0x20 || buffer[1] != 0x4c || buffer[2] != 0x41 || buffer[3] != 0x50 )
			throw new ExtractException("File "+fname+" ist not a lemmings palette file");

		paletteSize = unsigned(buffer[4]) + unsigned(buffer[5])*256;   // number of palette entries

		byte[] r = new byte[paletteSize];
		byte[] g = new byte[paletteSize];
		byte[] b = new byte[paletteSize];

		int ofs = 6; // skip two bytes which contain number of palettes (?)
		for (int idx = 0; idx < paletteSize; idx++) {
			r[idx] = buffer[ofs++];
			g[idx] = buffer[ofs++];
			b[idx] = buffer[ofs++];
			ofs++;
		}

		// search for double entries, create
		// new palette without double entries
		// and lookup table to fix the pixel values
		byte compressedR[] = new byte[paletteSize];
		byte compressedG[] = new byte[paletteSize];
		byte compressedB[] = new byte[paletteSize];
		lookupBuffer = new int[paletteSize];

		Arrays.fill(lookupBuffer,-1); // mark all entries invalid
		Arrays.fill(compressedR, (byte)0);
		Arrays.fill(compressedG, (byte)0);
		Arrays.fill(compressedB, (byte)0);

		int compressedIndex = 0;
		for (int i=0; i<paletteSize; i++) {

			if (lookupBuffer[i] == -1) {             // if -1, this value is no doublette of a lower index
				compressedR[compressedIndex] = r[i]; // copy value to compressed buffer
				compressedG[compressedIndex] = g[i];
				compressedB[compressedIndex] = b[i];
				if ( i != transparentIndex ) {       // don't search doublettes of transparent color
					// search for doublettes at higher indeces
					for (int j=i+1; j<paletteSize; j++) {
						if ( j == transparentIndex ) // transparent color can't be a doublette of another color
							continue;
						if ( (r[i] == r[j]) && (g[i] == g[j]) && (b[i] == b[j]) )
							lookupBuffer[j] = compressedIndex; // mark double entry in lookupBuffer
					}
				}
				lookupBuffer[i] = compressedIndex++;
			}
		}

		if (paletteSize != compressedIndex) {
			paletteSize = compressedIndex;
			r = compressedR;
			g = compressedG;
			b = compressedB;
		}

		palette = new Palette(r, g, b);
		return palette;
	}

	/**
	 * Convert byte in unsigned int
	 * @param b Byte to convert
	 * @return Unsigned value of byte
	 */
	private static int unsigned(final byte b) {
		return b & 0xFF;
	}

	/**
	 * Load SPR file. Load palette first!
	 * @param fname Name of SPR file
	 * @return Array of Images representing all images stored in the SPR file
	 * @throws ExtractException
	 */
	GIFImage[] loadSPR(final String fname) throws ExtractException {
		byte buffer[];

		if (palette == null)
			throw new ExtractException("Load Palette first!");

		// read file into buffer
		try {
			File f = new File(fname);
			FileInputStream fi = new FileInputStream(fname);
			buffer = new byte[(int)f.length()];
			fi.read(buffer);
			fi.close();
		} catch(FileNotFoundException e) {
			throw new ExtractException("File "+fname+" not found");
		}
		catch(IOException e) {
			throw new ExtractException("I/O error while reading "+fname);
		}
		// check header
		if (buffer[0] != 0x53 || buffer[1] != 0x52 || buffer[2] != 0x4c || buffer[3] != 0x45 )
			throw new ExtractException("File "+fname+" ist not a lemmings sprite file");
		// get number of frames
		int frames = unsigned(buffer[4]) + unsigned(buffer[5])*256;
		int ofs = unsigned(buffer[6]) + unsigned(buffer[7])*256;

		images = new GIFImage[frames];
		byte b;
		int lineOfs;

		for (int frame=0; frame < frames; frame++) {
			// get header info
			int xOfs = unsigned(buffer[ofs++]) + unsigned(buffer[ofs++])*256;   // x offset of data in output image
			int yOfs = unsigned(buffer[ofs++]) + unsigned(buffer[ofs++])*256;   // y offset of data in output image
			int maxLen = unsigned(buffer[ofs++]) + unsigned(buffer[ofs++])*256; // maximum length of a data line
			int lines = unsigned(buffer[ofs++]) + unsigned(buffer[ofs++])*256;  // number of data lines
			int width = unsigned(buffer[ofs++]) + unsigned(buffer[ofs++])*256;  // width of output image
			int height = unsigned(buffer[ofs++]) + unsigned(buffer[ofs++])*256; // height of output image

			byte pixels[] = new byte[width*height];

			for (int i=0; i<pixels.length; i++)
				pixels[i] = transparentIndex;

			int y = yOfs*width;

			int pxOffset = 0; // additional offset for lines broken in several packets

			for (int line = 0; line < lines; ) {
				// read line
				b = buffer[ofs++]; // start character including length (>= 0x80) or line offset (<0x80)
				lineOfs = 0;
				while (b == 0x7f) {   // special line offset for large sprites
					lineOfs += 0x7f;
					b = buffer[ofs++];
				}
				if (!( (b & 0x80) == 0x80)) {
					// additional line offset
					lineOfs += (b & 0x7f);
					b = buffer[ofs++]; // start character
				}
				// get line length
				int len = (b & 0xff) - 0x80;
				if (len < 0 || len > 0x7f || len > maxLen)
					throw new ExtractException(
							"Maximum data line length exceeded in line "+line+" of frame "+frame+" of "+fname+" (ofs:"+ofs+")");
				if (len > 0) {
					try {
						for (int pixel = 0; pixel < len; pixel++) {
							// none of the extracted images uses more than 128 colors (indeed much less)
							// but some use higher indeces. Instead of mirroring the palette, just and every
							// entry with 0x7f.
							// The lookup table is needed to get new index in compresse palette
							byte pixelVal = (byte)(lookupBuffer[buffer[ofs++] & 0x7f] & 0xff);
							pixels[y+xOfs+lineOfs+pixel+pxOffset] = pixelVal;
						}
					} catch (ArrayIndexOutOfBoundsException ex) {
						throw new ExtractException(
								"Index out of bounds in line "+line+" of frame "+frame+" of "+fname+" (ofs:"+ofs+")");
					}
					b = buffer[ofs++]; // end character must be 0x80
					if ( (b & 0xff) != 0x80) {
						// if this is not the end character, the line is continued after an offset
						pxOffset += (lineOfs + len);
						ofs--;
						continue;
					}
				}
				pxOffset = 0;
				line++;
				y += width;
			}
			// convert byte array into BufferedImage
			images[frame] = new GIFImage(width, height, pixels, palette);
		}

		return images;
	}

	/**
	 * Save all images of currently loaded SPR file
	 * @param fname Filename of GIF files to export. "_N.gif" will be appended with N being the image number.
	 * @param keepAnims If true, consequently stored imaged with same size will be stored inside one GIF (one beneath the other)
	 * @return Array of all the filenames stored
	 * @throws ExtractException
	 */
	String[] saveAll(final String fname, final boolean keepAnims) throws ExtractException {
		int width = images[0].getWidth();
		int height = images[0].getHeight();
		int startIdx = 0;
		int animNum = 0;
		ArrayList<String> files = new ArrayList<String>();

		for (int idx=1; idx <= images.length; idx++) {
			// search for first image with different size
			if (keepAnims)
				if (idx < images.length)
					if (images[idx].getWidth() == width && images[idx].getHeight() == height)
						continue;
			// now save all the images in one: one above the other
			int num;
			if (keepAnims)
				num = idx - startIdx;
			else num = 1;
			byte pixels[] = new byte[width*num*height];
			GIFImage anim = new GIFImage(width, num*height, pixels, palette);
			for (int n = 0; n<num; n++)
				System.arraycopy(images[startIdx+n].getPixels(), 0, pixels, n*height*width, images[startIdx+n].getPixels().length);

			startIdx = idx;
			// construct filename
			String fn = fname+"_"+Integer.toString(animNum++)+".gif";
			// save gif
			saveGif(anim, fn);
			files.add(fn.toLowerCase());
			// remember new size
			if (idx < images.length) {
				width = images[idx].getWidth();
				height = images[idx].getHeight();
			}
		}
		String[] fileArray = new String[files.size()];
		return files.toArray(fileArray);
	}

	/**
	 * Save a number of images of currently loaded SPR file into one GIF (one image beneath the other)
	 * @param fname Name of GIF file to create (".gif" will NOT be appended)
	 * @param startIdx Index of first image to store
	 * @param frames Number of frames to store
	 * @throws ExtractException
	 */
	void saveAnim(final String fname, final int startIdx, final int frames) throws ExtractException {
		int width = images[startIdx].getWidth();
		int height = images[startIdx].getHeight();

		byte pixels[] = new byte[width*frames*height];
		GIFImage anim = new GIFImage(width, frames*height, pixels, palette);
		for (int n = 0; n<frames; n++)
			System.arraycopy(images[startIdx+n].getPixels(), 0, pixels, n*height*width, images[startIdx+n].getPixels().length);
		// save gif
		saveGif(anim, fname);
	}

	/**
	 * Save one image as GIF
	 * @param img Image object to save
	 * @param fname Name of GIF file to create (".gif" will NOT be appended)
	 * @throws ExtractException
	 */
	public static void saveGif(final GIFImage img, final String fname) throws ExtractException {
		GifEncoder gifEnc = new GifEncoder(img.getWidth(), img.getHeight(), img.getPixels(),
				img.palette.getRed(), img.palette.getGreen(), img.getPalette().getBlue());
		try {
			FileOutputStream f = new FileOutputStream(fname);
			gifEnc.setTransparentPixel(transparentIndex);
			gifEnc.write(f);
			f.close();
		} catch(FileNotFoundException ex) {
			throw new ExtractException("Can't open file "+fname+" for writing." );
		}
		catch(IOException ex) {
			throw new ExtractException("I/O error while writing file "+fname);
		}
	}


	/**
	 * Storage class for palettes.
	 * @author Volker Oth
	 */
	class Palette {
		/** byte array of red components */
		private byte[] red;
		/** byte array of green components */
		private byte[] green;
		/** byte array of blue components */
		private byte[] blue;

		/**
		 * Create palette from array of color components
		 * @param r byte array of red components
		 * @param g byte array of green components
		 * @param b byte array of blue components
		 */
		Palette(final byte[] r, final byte[] g, final byte[] b) {
			red = r;
			green = g;
			blue = b;
		}

		/**
		 * Get blue components.
		 * @return byte array of blue components
		 */
		public byte[] getBlue() {
			return blue;
		}

		/**
		 * Get green components.
		 * @return byte array of green components
		 */
		public byte[] getGreen() {
			return green;
		}

		/**
		 * Get red components.
		 * @return byte array of red components
		 */
		public byte[] getRed() {
			return red;
		}
	}

	/**
	 * Stores GIF Image in RAM.
	 */
	class GIFImage {
		/** width in pixels */
		private int width;
		/** height in pixels */
		private int height;
		/** pixel data */
		private byte[] pixels;
		/** color palette */
		private Palette palette;

		/**
		 * Constructor.
		 * @param w width in pixels.
		 * @param h height in pixels.
		 * @param buf pixel data
		 * @param p color palette
		 */
		GIFImage(final int w, final int h, final byte[] buf, final Palette p) {
			width = w;
			height = h;
			pixels = buf;
			palette = p;
		}

		/**
		 * Get pixel data.
		 * @return pixel data as array of bytes
		 */
		public byte[] getPixels() {
			return pixels;
		}

		/**
		 * Get width in pixels.
		 * @return width in pixels
		 */
		public int getWidth() {
			return width;
		}

		/**
		 * Get height in pixels.
		 * @return height in pixels
		 */
		public int getHeight() {
			return height;
		}

		/**
		 * Get color palette.
		 * @return color palette
		 */
		public Palette getPalette() {
			return palette;
		}
	}
}
