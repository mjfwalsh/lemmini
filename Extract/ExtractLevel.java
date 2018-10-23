package Extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

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
 * Convert binary "Lemmings for Win95" level files into text format.
 */
public class ExtractLevel {
	private final static int scale = 2; // Scale (to convert lowres levels into hires levels)
	/** names for defautl styles */
	private final static String styles[] = {"dirt", "fire", "marble", "pillar",
		"crystal", "brick", "rock", "snow", "bubble" };

	/** release rate : 0 is slowest, 0x0FA (250) is fastest */
	private static int releaseRate;
	/** number of Lemmings in this level (maximum 0x0072 in original LVL format) */
	private static int numLemmings;
	/** number of Lemmings to rescue : should be less than or equal to number of Lemmings */
	private static int numToRescue;
	static int timeLimit;      // Time Limit		: max 0x00FF, 0x0001 to 0x0009 works best
	/** number of climbers in this level : max 0xfa (250) */
	private static int numClimbers;
	/** number of floaters in this level : max 0xfa (250) */
	private static int numFloaters;
	/** number of bombers in this level : max 0xfa (250) */
	private static int numBombers;
	/** number of blockers in this level : max 0xfa (250) */
	private static int numBlockers;
	/** number of builders in this level : max 0xfa (250) */
	private static int numBuilders;
	/** number of bashers in this level : max 0xfa (250) */
	private static int numBashers;
	/** number of miners in this level : max 0xfa (250) */
	private static int numMiners;
	/** number of diggers in this level : max 0xfa (250) */
	private static int numDiggers;
	/** start screen x pos : 0 - 0x04f0 (1264) rounded to modulo 8 */
	private static int xPos;
	/**
	 * 0x0000 is dirt,  <br>0x0001 is fire,   <br>0x0002 is squasher,<br>
	 * 0x0003 is pillar,<br>0x0004 is crystal,<br>0x0005 is brick,   <br>
	 * 0x0006 is rock,  <br>0x0007 is snow,   <br>0x0008 is bubble
	 */
	private static int style;
	/** extended style: no used in windows version ? */
	static int extStyle;
	/** placeholder ? */
	static int dummy;
	/** objects like doors - 32 objects each consists of 8 bytes */
	static ArrayList<LvlObject> objects;
	/** terrain the Lemmings walk on etc. - 400 tiles, 4 bytes each */
	static ArrayList<Terrain> terrain;
	/** steel areas which are indestructible - 32 objects, 4 bytes each */
	static ArrayList<Steel> steel;
	/** 32 byte level name - filled with whitespaces */
	static String lvlName;

	/**
	 * Convert one binary LVL file into text file
	 * @param fnIn Name of binary LVL file
	 * @param fnOut Name of target text file
	 * @throws Exception
	 */
	static public void convertLevel(final String fnIn, final String fnOut) throws Exception {
		// read file into buffer
		LevelBuffer b;
		try {
			File f = new File(fnIn);
			if (f.length() != 2048)
				throw new Exception("Lemmings level files must be 2048 bytes in size!");
			FileInputStream fi = new FileInputStream(fnIn);
			byte buffer[] = new byte[(int)f.length()];
			fi.read(buffer);
			b = new LevelBuffer(buffer);
			fi.close();
		} catch(FileNotFoundException e) {
			throw new Exception("File "+fnIn+" not found");
		}
		catch(IOException e) {
			throw new Exception("I/O error while reading "+fnIn);
		}
		// output file
		FileWriter fo = new FileWriter(fnOut);
		// add only file name without the path in the first line
		int p1 = fnIn.lastIndexOf("/");
		int p2 = fnIn.lastIndexOf("\\");
		if (p2 > p1)
			p1 = p2;
		if (p1 < 0)
			p1 = 0;
		else
			p1++;
		String fn = fnIn.substring(p1);
		// analyze buffer
		fo.write("# LVL extracted by Lemmini # "+fn+"\n");
		// read configuration in big endian word
		releaseRate = b.getWord();
		fo.write("releaseRate = "+releaseRate+"\n");
		numLemmings = b.getWord();
		fo.write("numLemmings = "+numLemmings+"\n");
		numToRescue  = b.getWord();
		fo.write("numToRescue = "+numToRescue+"\n");
		timeLimit   = b.getWord();
		fo.write("timeLimit = "+timeLimit+"\n");
		numClimbers = b.getWord();
		fo.write("numClimbers = "+numClimbers+"\n");
		numFloaters = b.getWord();
		fo.write("numFloaters = "+numFloaters+"\n");
		numBombers  = b.getWord();
		fo.write("numBombers = "+numBombers+"\n");
		numBlockers = b.getWord();
		fo.write("numBlockers = "+numBlockers+"\n");
		numBuilders = b.getWord();
		fo.write("numBuilders = "+numBuilders+"\n");
		numBashers  = b.getWord();
		fo.write("numBashers = "+numBashers+"\n");
		numMiners   = b.getWord();
		fo.write("numMiners = "+numMiners+"\n");
		numDiggers  = b.getWord();
		fo.write("numDiggers = "+numDiggers+"\n");
		xPos        = b.getWord();
		// bugfix: in some levels, the position is negative (?)
		if (xPos<0)
			xPos = -xPos;
		xPos *= scale;
		fo.write("xPos = "+xPos+"\n");
		style       = b.getWord();
		fo.write("style = "+styles[style]+"\n");
		extStyle    = b.getWord();
		dummy       = b.getWord();
		// read objects
		fo.write("\n# Objects"+"\n");
		fo.write("# id, xpos, ypos, paint mode (), upside down (0,1)"+"\n");
		fo.write("# paint modes: 8=VIS_ON_TERRAIN, 4=NO_OVERWRITE, 0=FULL (only one value possible)\n");
		byte by[] = new byte[8];
		objects = new ArrayList<LvlObject>();
		int idx = 0;
		for (int i=0; i<32; i++) {
			int sum = 0;
			for (int j=0; j<8; j++) {
				by[j] = b.getByte();
				sum += by[j] & 0xff;
			}
			if (sum != 0) {
				LvlObject obj = new LvlObject(by, scale);
				objects.add(obj);
				fo.write("object_"+idx+" = "+obj.id+", "+obj.xPos+", "+obj.yPos+", "+obj.paintMode+", "+(obj.upsideDown?1:0)+"\n");
				idx++;
			}
		}
		// read terrain
		fo.write("\n# Terrain"+"\n");
		fo.write("# id, xpos, ypos, modifier"+"\n");
		fo.write("# modifier: 8=NO_OVERWRITE, 4=UPSIDE_DOWN, 2=REMOVE (combining allowed, 0=FULL)\n");
		terrain = new ArrayList<Terrain>();
		idx = 0;
		for (int i=0; i<400; i++) {
			int mask = 0xff;
			for (int j=0; j<4; j++) {
				by[j] = b.getByte();
				mask &= by[j];
			}
			if (mask != 0xff) {
				Terrain ter = new Terrain(by, scale);
				terrain.add(ter);
				fo.write("terrain_"+idx+" = "+ter.id+", "+ter.xPos+", "+ter.yPos+", "+ter.modifier+"\n");
				idx++;
			}
		}
		// read steel blocks
		fo.write("\n#Steel"+"\n");
		fo.write("# id, xpos, ypos, width, height"+"\n");
		steel = new ArrayList<Steel>();
		idx =0;
		for (int i=0; i<32; i++) {
			int sum = 0;
			for (int j=0; j<4; j++) {
				by[j] = b.getByte();
				sum += by[j] & 0xff;
			}
			if (sum != 0) {
				Steel stl = new Steel(by, scale);
				steel.add(stl);
				fo.write("steel_"+idx+" = "+stl.xPos+", "+stl.yPos+", "+stl.width+", "+stl.height+"\n");
				idx++;
			}
		}
		// read name
		fo.write("\n#Name"+"\n");
		char cName[] = new char[32];
		for (int j=0; j<32; j++) {
			// replace wrong apostrophes
			char c = (char)(b.getByte() & 0xff);
			if (c == '´' || c == '`')
				c = '\'';
			cName[j] = c;
		}

		//	int pos = fnIn.lastIndexOf("\\");
		//	if (pos == -1)
		//		pos = fnIn.lastIndexOf("/");
		//	if (pos > -1)
		//		fnIn = fnIn.substring(pos+1);
		lvlName = String.valueOf(cName);
		fo.write("name = "+lvlName+"\n");
		fo.close();
	}
}

/**
 * Storage class for level objects.
 * @author Volker Oth
 */
class LvlObject {
	/** paint mode: only visible on a terrain pixel */
	private static final int MODE_VIS_ON_TERRAIN = 8;
	/** paint mode: don't overwrite terrain pixel in the original background image */
	private static final int MODE_NO_OVERWRITE = 4;
	/** paint mode: paint without any further checks */
	private static final int MODE_FULL = 0;

	/** x position in pixels */
	int xPos;
	/** y position in pixels */
	int yPos;
	/** identifier */
	int id;
	/** paint mode */
	int paintMode;
	/** flag: paint object upsdie down */
	boolean upsideDown;

	/**
	 * Constructor.
	 * @param b buffer
	 * @param scale Scale (to convert lowres levels into hires levels)
	 */
	LvlObject (final byte b[], final int scale) {
		// x pos  : min 0xFFF8, max 0x0638.  0xFFF8 = -24, 0x0000 = -16, 0x0008 = -8
		// 0x0010 = 0, 0x0018 = 8, ... , 0x0638 = 1576    note: should be multiples of 8
		xPos = (short)(((b[0] & 0xff)<<8) + (b[1] & 0xff)) - 16;
		xPos *= scale;
		// y pos  : min 0xFFD7, max 0x009F.  0xFFD7 = -41, 0xFFF8 = -8, 0xFFFF = -1
		// 0x0000 = 0, ... , 0x009F = 159.  note: can be any value in the specified range
		yPos = (short)(((b[2] & 0xff)<<8) + (b[3] & 0xff));
		yPos *= scale;
		// obj id : min 0x0000, max 0x000F.  the object id is different in each
		// graphics set, however 0x0000 is always an exit and 0x0001 is always a start.
		id = ((b[4] & 0xff)<<8) + (b[5] & 0xff);
		// modifier : first byte can be 80 (do not overwrite existing terrain) or 40
		// (must have terrain underneath to be visible). 00 specifies always draw full graphic.
		// second byte can be 8F (display graphic upside-down) or 0F (display graphic normally)
		switch (b[6] & 0xff) {
			case 0x80:
				paintMode = MODE_NO_OVERWRITE;
				break;
			case 0x40:
			case 0xc0: // bug in original level 36: overwrite AND visible on terrain: impossible
				paintMode = MODE_VIS_ON_TERRAIN;
				break;
			default:
				paintMode = MODE_FULL;
				break;
		}
		upsideDown = ((b[7] & 0xff) == 0x8f);
	}
}

/**
 * Storage class for terrain tiles.
 * @author Volker Oth
 */
class Terrain {
	/** identifier */
	int id;
	/** x position in pixels */
	int xPos;
	/** y position in pixels */
	int yPos;
	/** modifier - must be one of the above MODEs */
	int modifier;

	/**
	 * Constructor.
	 * @param b buffer
	 * @param scale Scale (to convert lowres levels into hires levels)
	 */
	Terrain (final byte b[], final int scale) {
		// xpos: 0x0000..0x063F.  0x0000 = -16, 0x0008 = -8, 0x0010 = 0, 0x063f = 1583.
		// note: the xpos also contains modifiers.  the first nibble can be
		// 8 (do no overwrite existing terrain), 4 (display upside-down), or
		// 2 (remove terrain instead of add it). you can add them together.
		// 0 indicates normal.
		// eg: 0xC011 means draw at xpos=1, do not overwrite, upside-down.
		modifier = (b[0] & 0xf0)>>4;
		xPos = ((b[0] & 0x0f)<<8)+(b[1] & 0xff) - 16;
		xPos *= scale;
		// y pos : 9-bit value. min 0xEF0, max 0x518.  0xEF0 = -38, 0xEF8 = -37,
		// 0x020 = 0, 0x028 = 1, 0x030 = 2, 0x038 = 3, ... , 0x518 = 159
		// note: the ypos value bleeds into the next value since it is 9bits.
		yPos = (((b[2] & 0xff)<<1) + ((b[3]&0x80)>>7));
		if ((yPos & 256) != 0) // highest bit set -> negative
			yPos -= 512;
		yPos -= 4;
		yPos *= scale;
		// terrain id: min 0x00, max 0x3F.  not all graphic sets have all 64 graphics.
		id = b[3] & 0x3f;
	}
}

/**
 *
 * Storage class for steel areas.
 * @author Volker Oth
 */
class Steel {
	/** x position in pixels */
	int xPos;
	/** y position in pixels */
	int yPos;
	/** width in pixels */
	int width;
	/** height in pixels */
	int height;

	/**
	 * Constructor.
	 * @param b buffer
	 * @param scale Scale (to convert lowres levels into hires levels)
	 */
	Steel (final byte b[], final int scale) { // note: last byte is always 0
		// xpos: 9-bit value: 0x000..0x178).  0x000 = -16, 0x178 = 1580
		xPos = (((b[0] & 0xff)<<1) + ((b[1]&0x80)>>7))*4 - 16;
		xPos *= scale;
		// ypos: 0x00..0x27. 0x00 = 0, 0x27 = 156 - each hex value represents 4 pixels
		yPos = (b[1] & 0x7f) * 4;
		yPos *= scale;
		// area: 0x00..max 0xFF.  first nibble is the x-size, from 0..F (represents 4 pixels)
		// second nibble is the y-size. 0x00 = (4,4), 0x11 = (8,8), 0x7F = (32,64)
		width = ((b[2] & 0xf0) >>4) * 4 + 4;
		width *= scale;
		height = (b[2] & 0xf) * 4 + 4;
		height *= scale;
	}
}

/**
 * Abstraction layer for binary level in byte buffer.
 * @author Volker Oth
 */
class LevelBuffer {
	/** data buffer */
	private byte buffer[];
	/** byte offset */
	private int ofs;

	/**
	 * Constructor.
	 * @param b array of byte to use as buffer
	 */
	LevelBuffer(final byte b[]) {
		buffer = b;
		ofs = 0;
	}

	/**
	 * Get word (2 bytes, little endian) at current position.
	 * @return word at current position
	 * @throws ArrayIndexOutOfBoundsException
	 */
	int getWord() throws ArrayIndexOutOfBoundsException {
		return ((buffer[ofs++]&0xff)<<8) + buffer[ofs++];
	}

	/**
	 * Get byte at current position.
	 * @return byte at current position
	 * @throws ArrayIndexOutOfBoundsException
	 */
	byte getByte() throws ArrayIndexOutOfBoundsException {
		return buffer[ofs++];
	}
}