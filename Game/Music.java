package Game;
import java.io.File;
import java.io.FileFilter;

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
 * Play background music. Abstraction layer for ModMusic and MidiMusic.
 *
 * @author Volker Oth
 */
public class Music {

	/** music type */
	private static enum Type {
		/** no type */
		NONE,
		/** midi music */
		MIDI,
		/** MOD music */
		MOD
	}

	/** music type */
	private static Type type;
	/** currently playing? */
	private static boolean playing;
	/** MOD music object */
	private static ModMusic modMusic;
	/** Midi music object */
	private static MidiMusic midiMusic;
	/** music gain */
	private static double gain = 1.0;
	/** array of file names */
	private static String musicFiles[];


	/**
	 * Initialization.
	 */
	public static void init() {
		type = Type.NONE;
		modMusic = new ModMusic();

		// read available musicfiles for random mode
		File dir = new File(Core.resourcePath+"music");
		File files[] = dir.listFiles(new MusicFileFilter());
		musicFiles = new String[files.length];
		for (int i=0; i<files.length; i++)
			musicFiles[i] = files[i].getName();
	}

	/**
	 * Load music file.
	 * @param fName file name
	 * @throws ResourceException
	 * @throws LemmException
	 */
	public static void load(final String fName) throws ResourceException, LemmException {
		if (fName.toLowerCase().indexOf(".mid") != -1) {
			// MIDI
			midiMusic = new MidiMusic(fName);
			if (type == Type.MOD)
				modMusic.close();
			type = Type.MIDI;
		} else if (fName.toLowerCase().indexOf(".mod") != -1) {
			// MOD
			modMusic.load(fName);
			if (type == Type.MIDI)
				midiMusic.close();
			type = Type.MOD;
		}
		playing = false;
	}

	/**
	 * Get file name of a random track.
	 * @return file name of a random track
	 */
	public static String getRandomTrack() {
		double r = Math.random()*musicFiles.length;
		return musicFiles[(int)r];
	}

	/**
	 * Play music.
	 */
	public static void play() {
		switch (type) {
			case MIDI:
				midiMusic.play();
				playing = true;
				break;
			case MOD:
				modMusic.play();
				playing = true;
				break;
		}
	}

	/**
	 * Stop music.
	 */
	public static void stop() {
		switch (type) {
			case MIDI:
				midiMusic.stop();
				playing = false;
				break;
			case MOD:
				modMusic.stop();
				playing = false;
				break;
		}
	}

	/**
	 * Close music.
	 */
	public static void close() {
		switch (type) {
			case MIDI:
				midiMusic.close();
				playing = false;
				break;
			case MOD:
				modMusic.close();
				playing = false;
				break;
		}
	}

	/**
	 * Check if music is currently playing
	 * @return true if music is currently playing, else false
	 */
	public static boolean isPlaying() {
		return playing;
	}

	/**
	 * Get current music gain (1.0=100%)
	 * @return current music gain (1.0=100%)
	 */
	public static double getGain() {
		return gain;
	}

	/**
	 * Set music gain
	 * @param gn gain (1.0=100%)
	 */
	public static void setGain(final double gn) {
		if (gn > 1.0)
			gain = 1.0;
		else if (gn < 0)
			gain = 0;
		else
			gain = gn;
		switch (type) {
			case MIDI:
				midiMusic.setGain(gain);
				break;
			case MOD:
				modMusic.setGain(gain);
				break;
		}
		Core.programProps.set("musicGain", gain);
	}

	/**
	 * Get current music type.
	 * @return music type
	 */
	public static Type getType() {
		return type;
	}
}

/**
 * File filter for music files.
 * @author Volker Oth
 */
class MusicFileFilter implements FileFilter {
	/* (non-Javadoc)
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(final File f) {
		if (!f.isFile())
			return false;
		if (f.getName().toLowerCase().indexOf(".mid") != -1)
			return true;
		if (f.getName().toLowerCase().indexOf(".mod") != -1)
			return true;
		return false;
	}
}

