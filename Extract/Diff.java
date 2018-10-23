package Extract;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;

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
 * Simple diff/patch algorithm for text or binary files
 * In contrast to common line based diff utilities, this algorithm
 * works byte based to create small binary difference files.
 * However this very simple approach is only sensible for small files.
 * It is by no way meant as rival to full featured approaches like XDelta.
 *
 * @author Volker Oth
 */
public class Diff {
	/** insert n bytes */
	private final static byte INSERT = 0;
	/** delete n bytes */
	private final static byte DELETE = 1;
	/** replace n bytes with n bytes */
	private final static byte REPLACE = 2;
	/** substitute n bytes with m bytes */
	private final static byte SUBSTITUTE = 3;

	/** magic number for header ID */
	private final static int HEADER_ID = 0xdeadbeef;
	/** magic number for data ID */
	private final static int DATA_ID = 0xfade0ff;

	/** print info to System.out */
	private static boolean verbatim = false;

	/** re-synchronization length */
	private static int resyncLength = 4;
	/** re-synchronization window length */
	private static int windowLength = 512;

	/** target CRC */
	public  static int targetCRC = 0;

	/**
	 * Set diff parameters
	 * @param winLen Length of windows to search for re-synchronization
	 * @param resyncLen Number of equal bytes needed for re-synchronization
	 */
	public static void setParameters(final int winLen, final int resyncLen) {
		resyncLength = resyncLen;
		windowLength = winLen;
	}

	/**
	 * Create diff buffer from the differences between source and target buffer
	 * @param bsrc source buffer (the file to be patched)
	 * @param btrg target buffer (the file as it should be)
	 * @return buffer of differences
	 */
	public static byte[] diffBuffers(final byte bsrc[], final byte btrg[]) {
		ArrayList<Byte> patch = new ArrayList<Byte>();
		Buffer src = new Buffer(bsrc);
		Buffer trg = new Buffer(btrg);

		// compare crcs
		Adler32 crcSrc = new Adler32();
		crcSrc.update(src.getData());
		Adler32 crcTrg = new Adler32();
		crcTrg.update(trg.getData());
		targetCRC = (int)crcTrg.getValue();
		if (crcTrg.getValue() == crcSrc.getValue())
			return null;

		// write header
		setDWord(patch,HEADER_ID);
		// write lengths to patch list
		setLen(patch,src.length());
		setLen(patch,trg.length());
		// write crcs to patch list
		setDWord(patch,(int)crcSrc.getValue());
		setDWord(patch,(int)crcTrg.getValue());
		setDWord(patch,DATA_ID);

		// examine source buffer
		int ofs = 0;
		while (src.getIndex() < src.length()) {
			// search for difference
			int s = src.getByte();
			int t = trg.getByte();
			if (s == t) {
				ofs++;
				continue;
			}
			// reset indeces
			src.setIndex(src.getIndex()-1);
			trg.setIndex(trg.getIndex()-1);
			// write offset
			setLen(patch,ofs);
			out("Offset: "+ofs);
			ofs = 0;
			// check for insert, delete, replace
			int len, leni, lend, lenr, lens[];
			int state = -1;

			leni = checkInsert(src,trg);
			lend = checkDelete(src,trg);
			lenr = checkReplace(src,trg);
			lens = checkSubstitute(src,trg);
			len = Math.min(leni,lend);
			len = Math.min(len,lenr);
			len = Math.min(len,lens[1]);
			if (len > windowLength) {
				// completely lost synchronisation
				int rs = src.length() - src.getIndex();
				int rt = trg.length() - trg.getIndex();
				if (rs==rt) {
					len = rs;
					state = REPLACE;
				} else {
					len = rt;
					state = INSERT;
				}
				break;
			}
			if (len == leni)
				state = INSERT;
			else if (len == lend)
				state = DELETE;
			else if (len == lenr)
				state = REPLACE;
			else if (len == lens[1])
				state = SUBSTITUTE;

			switch (state) {
				case INSERT :
					// insert
					out("Insert: "+len);
					patch.add(new Byte(INSERT));
					setLen(patch,len);
					for (int i = 0; i<len; i++)
						patch.add(new Byte((byte)trg.getByte()));
					break;
				case DELETE:
					// delete
					out("Delete: "+len);
					patch.add(new Byte(DELETE));
					setLen(patch,len);
					src.setIndex(src.getIndex()+len);
					break;
				case REPLACE:
					// replace
					out("Replace: "+len);
					patch.add(new Byte(REPLACE));
					setLen(patch,len);
					for (int i = 0; i<len; i++)
						patch.add(new Byte((byte)trg.getByte()));
					src.setIndex(src.getIndex()+len);
					break;
				case SUBSTITUTE:
					// replace
					out("Substitute: "+lens[0]+"/"+lens[1]);
					patch.add(new Byte(SUBSTITUTE));
					setLen(patch,lens[0]);
					setLen(patch,lens[1]);
					for (int i = 0; i<lens[1]; i++)
						patch.add(new Byte((byte)trg.getByte()));
					src.setIndex(src.getIndex()+lens[0]);
					break;
			}
		}

		// if the files end identically, the offset needs to be written
		if (ofs != 0) {
			out("Offset: "+ofs);
			setLen(patch,ofs);
		}

		// check for stuff to insert in target
		if (trg.getIndex() < trg.length()) {
			patch.add(new Byte(INSERT));
			int len = trg.length() - trg.getIndex();
			out("Insert (End): "+len);
			setLen(patch,len);
			for (int i = 0; i<len; i++)
				patch.add(new Byte((byte)trg.getByte()));
		}

		if (patch.size() == 0)
			return null;

		out("Patch length: "+patch.size());

		// convert patch list to output byte array
		byte retVal[] = new byte[patch.size()];
		for (int i =0; i<retVal.length;i++)
			retVal[i] = patch.get(i).byteValue();
		return retVal;
	}

	/**
	 * Create a target buffer from a source buffer and a buffer of differences
	 * @param bsrc source buffer
	 * @param bpatch buffer containing differences
	 * @return target buffer created from a source buffer and a buffer of differences
	 * @throws DiffException
	 */
	public static byte[] patchbuffers(final byte bsrc[], final byte bpatch[]) throws DiffException {
		Buffer src = new Buffer(bsrc);
		Buffer patch = new Buffer(bpatch);
		// calculate src crc
		Adler32 crc = new Adler32();
		crc.update(src.getData());
		// analyze header
		if (patch.getDWord() != Diff.HEADER_ID)
			throw new DiffException("No header id found in patch");
		int lenSrc = getLen(patch);
		if (lenSrc != src.length())
			throw new DiffException("Size of source differs from that in patch header");
		int lenTrg = getLen(patch);
		int crcPatchSrc = patch.getDWord();
		if (crcPatchSrc != (int)crc.getValue())
			throw new DiffException("CRC of source (0x"+Integer.toHexString((int)crc.getValue())+
					") differs from that in patch header (0x"+Integer.toHexString(crcPatchSrc)+")");
		int crcTrg = patch.getDWord();
		if (patch.getDWord() != Diff.DATA_ID)
			throw new DiffException("No data id found in patch header");

		Buffer trg = new Buffer(lenTrg);

		// step through patch buffer
		try {
			while (patch.getIndex()<patch.length()) {
				int ofs = getLen(patch);
				out("Offset: "+ofs);
				// copy bytes from source buffer
				for (int i=0; i<ofs; i++)
					trg.setByte((byte)src.getByte());
				// check for patch buffer empty
				if (patch.getIndex()==patch.length())
					break;
				// now there must follow a command followed by a
				int cmdIdx = patch.getIndex(); // just for exception
				int cmd = patch.getByte();
				int len = getLen(patch);
				switch (cmd) {
					case Diff.DELETE:
						out("Delete: "+len);
						src.setIndex(src.getIndex()+len);
						break;
					case Diff.REPLACE:
						out("Replace/");
						src.setIndex(src.getIndex()+len);
						//$FALL-THROUGH$
					case Diff.INSERT:
						out("Insert: "+len);
						for (int r=0; r<len;r++)
							trg.setByte((byte)patch.getByte());
						break;
					case Diff.SUBSTITUTE: {
						int lenT = getLen(patch);
						out("Substitute: "+len+"/"+lenT);
						src.setIndex(src.getIndex()+len);
						for (int r=0; r<lenT;r++)
							trg.setByte((byte)patch.getByte());
						break; }
					default:
						throw new DiffException("Unknown command "+cmd+" at patch offset "+cmdIdx);
				}
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new DiffException("Array index exceeds bounds. Patch file corrupt...");
		}

		// check length
		if (trg.getIndex() != lenTrg)
			throw new DiffException("Size of target differs from that in patch header");

		// compare crc
		crc.reset();
		crc.update(trg.getData());
		if (crcTrg != (int)crc.getValue())
			throw new DiffException("CRC of target differs from that in patch");

		return trg.getData();
	}

	/**
	 * Lengths/Offset are stored as 7bit values. The 8th bit is used as marker if the number
	 * is continued in the next byte.
	 * @param b Buffer from which to read the length/offset
	 * @return integer value of length/offset
	 * @throws ArrayIndexOutOfBoundsException
	 */
	private static int getLen(final Buffer b) throws ArrayIndexOutOfBoundsException {
		int val = 0;
		int v;
		int shift = 0;
		do {
			v = b.getByte();
			if ((v & 0x80) == 0) {
				// no continue bit set
				val += (v<<shift);
				break;
			}
			// erase contine marker bit
			v &= 0x7f;
			val += (v<<shift);
			shift += 7;
		} while (true);
		return val;
	}

	/**
	 * Store length/offset information in 7bit encoding. A set 8th bit means: continued in next byte
	 * So 127 is stored as 0x7f, but 128 is stored as 0x80 0x01 (where 0x80 means 0, highest bit is marker)
	 * @param l Patch list to add length/offset in 7bit encoding
	 * @param value Value to add in 7bit encoding
	 */
	private static void setLen(final List<Byte> l, final int value) {
		int val = value;
		while ( val > 0x7f) {
			l.add(new Byte((byte)(val & 0x7f | 0x80)));
			val >>>= 7;
		}
		l.add(new Byte((byte)val));
	}

	/**
	 * Check for "insert" difference
	 * @param src source buffer
	 * @param trg target buffer
	 * @return number of bytes inserted
	 * @throws ArrayIndexOutOfBoundsException
	 */
	private static int checkInsert(final Buffer src, final Buffer trg) throws ArrayIndexOutOfBoundsException {
		byte[] bs = src.getData();
		int is = src.getIndex();
		byte[] bt = trg.getData();
		int it = trg.getIndex();
		int len = windowLength;
		if (is+len+resyncLength >= bs.length)
			len = bs.length - is - resyncLength;
		if (it+len+resyncLength >= bt.length)
			len = bt.length - it - resyncLength;
		for (int w=1; w<len; w++) {
			int r;
			for (r = 0; r<resyncLength; r++)
				if (bs[is+r] != bt[it+w+r])
					break;
			if (r == resyncLength)
				return w;
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * Check for "delete" difference
	 * @param src source buffer
	 * @param trg target buffer
	 * @return number of bytes deleted
	 * @throws ArrayIndexOutOfBoundsException
	 */
	private static int checkDelete(final Buffer src, final Buffer trg) throws ArrayIndexOutOfBoundsException {
		byte[] bs = src.getData();
		int is = src.getIndex();
		byte[] bt = trg.getData();
		int it = trg.getIndex();
		int len = windowLength;
		if (is+len+resyncLength >= bs.length)
			len = bs.length - is - resyncLength;
		if (it+len+resyncLength >= bt.length)
			len = bt.length - it - resyncLength;
		for (int w=1; w<len; w++) {
			int r;
			for (r = 0; r<resyncLength; r++)
				if (bs[is+w+r] != bt[it+r])
					break;
			if (r == resyncLength)
				return w;
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * Check for "replace" difference
	 * @param src source buffer
	 * @param trg target buffer
	 * @return number of bytes replaced
	 * @throws ArrayIndexOutOfBoundsException
	 */
	private static int checkReplace(final Buffer src, final Buffer trg) throws ArrayIndexOutOfBoundsException {
		byte[] bs = src.getData();
		int is = src.getIndex();
		byte[] bt = trg.getData();
		int it = trg.getIndex();
		int len = windowLength;
		if (is+len+resyncLength >= bs.length)
			len = bs.length - is - resyncLength;
		if (it+len+resyncLength >= bt.length)
			len = bt.length - it - resyncLength;
		for (int w=1; w<len; w++) {
			int r;
			for (r = 0; r<resyncLength; r++)
				if (bs[is+w+r] != bt[it+w+r])
					break;
			if (r == resyncLength)
				return w;
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * Check for "substitute" difference
	 * @param src source buffer
	 * @param trg target buffer
	 * @return integer array: [0]: number of bytes to delete in source, [1]: number of bytes to insert in target
	 * @throws ArrayIndexOutOfBoundsException

	 */
	private static int[] checkSubstitute(final Buffer src, final Buffer trg) throws ArrayIndexOutOfBoundsException {
		byte[] bs = src.getData();
		int is = src.getIndex();
		byte[] bt = trg.getData();
		int it = trg.getIndex();
		int len = windowLength;
		if (is+len+resyncLength >= bs.length)
			len = bs.length - is - resyncLength;
		if (it+len+resyncLength >= bt.length)
			len = bt.length - it - resyncLength;

		ArrayList<int[]> solutions = new ArrayList<int[]>();

		for (int ws=1; ws<len; ws++) {
			for (int wt=1; wt<len; wt++) {
				int r;
				for (r = 0; r<resyncLength; r++)
					if (bs[is+ws+r] != bt[it+wt+r])
						break;
				if (r == resyncLength) {
					int retVal[] = new int[2];
					retVal[0] = ws;
					retVal[1] = wt;
					solutions.add(retVal);
				}
			}
		}

		if (solutions.size() == 0) {
			// nothing found
			int retVal[] = new int[2];
			retVal[0] = Integer.MAX_VALUE;
			retVal[1] = Integer.MAX_VALUE;
			return retVal;
		}

		// search best solution
		int sMinIdx = 0;
		for (int i=1; i<solutions.size(); i++) {
			int s[] = solutions.get(i);
			int sMin[] = solutions.get(sMinIdx);
			if (s[0]+s[1] < sMin[0]+sMin[1])
				sMinIdx = i;
		}
		return solutions.get(sMinIdx);
	}

	/**
	 * Write DWord to difference list
	 * @param l difference list
	 * @param val DWord value
	 */
	private static void setDWord(final List<Byte> l, final int val) {
		l.add(new Byte((byte)val));
		l.add(new Byte((byte)(val>>8)));
		l.add(new Byte((byte)(val>>16)));
		l.add(new Byte((byte)(val>>24)));
	}

	private static void out(final String s) {
		if (verbatim)
			System.out.println(s);
	}
}


/**
 * Buffer class that manages reading/writing from/to a byte buffer
 *
 * @author Volker Oth
 */
class Buffer {
	/** array of byte which defines the data buffer */
	private byte buffer[];
	/** byte index in buffer */
	private int  index;

	/**
	 * Constructor.
	 * @param size buffer size in bytes
	 */
	Buffer(final int size) {
		index = 0;
		buffer = new byte[size];
	}

	/**
	 * Constructor.
	 * @param b array of byte to use as buffer
	 */
	Buffer(final byte b[]) {
		index = 0;
		buffer = b;
	}

	/**
	 * Get size of buffer.
	 * @return size of buffer in bytes
	 */
	int length() {
		return buffer.length;
	}

	/**
	 * Get current byte index.
	 * @return current byte index
	 */
	int getIndex() {
		return index;
	}

	/**
	 * Get data buffer.
	 * @return data buffer
	 */
	byte[] getData() {
		return buffer;
	}

	/**
	 * Set index to new byte position.
	 * @param idx index to new byte position
	 */
	void setIndex(final int idx) {
		index = idx;
	}

	/**
	 * Get byte at current position.
	 * @return byte at current position
	 * @throws ArrayIndexOutOfBoundsException
	 */
	int getByte() throws ArrayIndexOutOfBoundsException {
		return buffer[index++] & 0xff;
	}

	/**
	 * Set byte at current position, increase index by 1.
	 * @param val byte value to write
	 * @throws ArrayIndexOutOfBoundsException
	 */
	void setByte(final byte val) throws ArrayIndexOutOfBoundsException {
		buffer[index++] = val;
	}

	/**
	 * Get word (2 bytes, little endian) at current position.
	 * @return word at current position
	 * @throws ArrayIndexOutOfBoundsException
	 */
	int getWord() throws ArrayIndexOutOfBoundsException {
		return getByte() | (getByte()<<8);
	}

	/**
	 * Set word (2 bytes, little endian) at current position, increase index by 2.
	 * @param val word to write at current position
	 * @throws ArrayIndexOutOfBoundsException
	 */
	void setWord(final int val) throws ArrayIndexOutOfBoundsException {
		setByte((byte)val);
		setByte((byte)(val>>8));
	}

	/**
	 * Get double word (4 bytes, little endian) at current position.
	 * @return dword at current position
	 * @throws ArrayIndexOutOfBoundsException
	 */
	int getDWord() throws ArrayIndexOutOfBoundsException {
		return getByte() | (getByte()<<8) | (getByte()<<16) | (getByte()<<24);
	}

	/**
	 * Set double word (4 bytes, little endian) at current position, increase index by 4.
	 * @param val dword to write at current position
	 * @throws ArrayIndexOutOfBoundsException
	 */
	void setDWord(final int val) throws ArrayIndexOutOfBoundsException {
		setByte((byte)val);
		setByte((byte)(val>>8));
		setByte((byte)(val>>16));
		setByte((byte)(val>>24));
	}
}

/**
 * Generic Exception for Diff.
 * @author Volker Oth
 */
class DiffException extends Exception {
	private final static long serialVersionUID = 0x000000001;

	/**
	 * Constructor.
	 */
	public DiffException() {
		super();
	}

	/**
	 * Constructor.
	 * @param s Exception string
	 */
	public DiffException(String s) {
		super(s);
	}
}