package Game;

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
 * Storage class to store level info.
 *
 * @author Volker Oth
 */
public class LevelInfo {
	/** level name */
	private String name;
	/** name of music for this level */
	private String music;
	/** file name of the INI file containing the level information */
	private String fileName;

	/**
	 * Set the file name
	 * @param fileName file name
	 */
	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Get the file name.
	 * @return file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Set name of music.
	 * @param music name of music
	 */
	public void setMusic(final String music) {
		this.music = music;
	}

	/**
	 * Get name of music.
	 * @return name of music.
	 */
	public String getMusic() {
		return music;
	}

	/**
	 * Set level name.
	 * @param name level name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Get level name.
	 * @return level name
	 */
	public String getName() {
		return name;
	}
}
