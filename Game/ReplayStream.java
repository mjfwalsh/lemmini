package Game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
 * Handle replays.
 * @author Volker Oth
 */
public class ReplayStream {
	// event types
	final static int ASSIGN_SKILL = 0;
	final static int MOVE_XPOS = 1;
	final static int SELECT_SKILL = 2;
	final static int SET_RELEASE_RATE = 3;
	final static int NUKE = 4;

	private ArrayList<ReplayEvent> events;
	private int replayIndex;

	/**
	 * Constructor.
	 */
	public ReplayStream() {
		events = new ArrayList<ReplayEvent>(); // <events>
		replayIndex = 0;
	}

	/**
	 * Rewind replay to start position.
	 */
	public void rewind() {
		replayIndex = 0;
	}

	/**
	 * Get next replay event
	 * @param ctr frame counter
	 * @return replay event
	 */
	public ReplayEvent getNext(final int ctr) {
		if (replayIndex >= events.size())
			return null;
		ReplayEvent r = events.get(replayIndex);
		/* Note: there can be multiple replay events for one frame.
		 * return the next stored event if was stored for a frame
		 * smaller or equal to the given frame counter.
		 */
		if (ctr >= r.frameCtr) {
			replayIndex++;
			return r;
		}
		return null; /* no more events for this frame */
	}

	/**
	 * Clear the replay buffer.
	 */
	public void clear() {
		events.clear();
	}

	/**
	 * Clear the replay buffer from a certain frame counter.
	 * @param ctr frame counter
	 */
	public void clearFrom(final int ctr) {
		/* Note: there can be multiple replay events for one frame. */
		for (int i=events.size()-1; i>0; i--) {
			ReplayEvent r = events.get(i);
			if (r.frameCtr > ctr // clearly behind ctr -> erase
					|| r.frameCtr == ctr && i > replayIndex) // equal to ctr, but after replayIndex -> erase
				events.remove(i);
			else break;
		}
		replayIndex = 0;
	}

	/**
	 * Load replay buffer from file.
	 * @param fname file name
	 * @return replay information
	 */
	public ReplayLevelInfo load(final String fname) {
		try {
			ArrayList<ReplayEvent> ev = new ArrayList<ReplayEvent>();
			BufferedReader f = new BufferedReader(new FileReader(fname));
			String line = f.readLine();
			if (!line.equals("#REPLAY")) {
				f.close();
				return null;
			}
			// read level info
			line = f.readLine();
			String e[] = line.split(",");
			for (int j=0; j<e.length; j++)
				e[j] = e[j].trim();
			ReplayLevelInfo rli = new ReplayLevelInfo();
			if (e[0].charAt(0) != '#') {
				f.close();
				return null;
			}
			rli.setLevelPack(e[0].substring(1));
			rli.setDiffLevel(Integer.parseInt(e[1]));
			rli.setLvlNumber(Integer.parseInt(e[2]));
			// read events
			while ( (line=f.readLine()) != null) {
				e = line.split(",");
				int i[] = new int[e.length];
				for (int j=0; j<e.length; j++)
					i[j] = Integer.parseInt(e[j].trim());

				switch (i[1] /* type*/) {
					case ASSIGN_SKILL:
						ev.add(new ReplayAssignSkillEvent(i[0], Lemming.Type.get(i[2]), i[3]));
						break;
					case MOVE_XPOS:
						ev.add(new ReplayMoveXPosEvent(i[0], i[2]));
						break;
					case SELECT_SKILL:
						ev.add(new ReplaySelectSkillEvent(i[0], Lemming.Type.get(i[2])));
						break;
					case SET_RELEASE_RATE:
						ev.add(new ReplayReleaseRateEvent(i[0], i[2]));
						break;
					case NUKE:
						ev.add(new ReplayEvent(i[0], NUKE));
						break;
					default:
						return null;
				}
			}
			f.close();
			events = ev;
			return rli;
		} catch(FileNotFoundException e) {
			return null;
		}
		catch(IOException e) {
			return null;
		}
		catch(NumberFormatException e) {
			return null;
		}
		catch(ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Store replay info in a file.
	 * @param fname file name
	 * @return true if save ok, false otherwise
	 */
	public boolean save(final String fname) {
		try {
			FileWriter f = new FileWriter(new File(fname));
			f.write("#REPLAY\n");
			LevelPack lp = GameController.getCurLevelPack();
			f.write("#"+lp.getName()+", "+GameController.getCurDiffLevel()+", "+GameController.getCurLevelNumber()+"\n");
			for (int i=0; i < events.size(); i++) {
				ReplayEvent r = events.get(i);
				f.write(r.toString()+"\n"); // will use toString of the correct child object
			}
			f.close();

			return true;
		} catch(FileNotFoundException e) {
			return false;
		}
		catch(IOException e) {
			return false;
		}
	}

	/**
	 * Add a NUKE event (all lemmings nuked).
	 * @param ctr frame counter
	 */
	public void addNukeEvent(final int ctr) {
		ReplayEvent event = new ReplayEvent(ctr, NUKE);
		events.add(event);
	}

	/**
	 * Add ASSIGN_SKILL event (one lemming was assigned a skill).
	 * @param ctr frame counter
	 * @param skill skill assigned
	 * @param lemming Lemming the skill was assigned to
	 */
	public void addAssignSkillEvent(final int ctr, final Lemming.Type skill, final int lemming ) {
		ReplayAssignSkillEvent event = new ReplayAssignSkillEvent(ctr, skill, lemming);
		events.add(event);
	}

	/**
	 * Add SELECT_SKILL event (skill selection button was pressed).
	 * @param ctr frame counter
	 * @param skill skill selected
	 */
	public void addSelectSkillEvent(final int ctr, final Lemming.Type skill) {

		ReplaySelectSkillEvent event = new ReplaySelectSkillEvent(ctr, skill);
		events.add(event);
	}

	/**
	 * Add MOVE_XPOS event (screen moved left/right).
	 * @param ctr frame counter
	 * @param xPos new screen position
	 */
	public void addXPosEvent(final int ctr, final int xPos ) {
		ReplayMoveXPosEvent event = new ReplayMoveXPosEvent(ctr, xPos);
		events.add(event);
	}

	/**
	 * Add SET_RELEASE_RATE event (release rate was changed).
	 * @param ctr frame counter
	 * @param releaserate new release rate
	 */
	public void addReleaseRateEvent(final int ctr, final int releaserate ) {
		ReplayReleaseRateEvent event = new ReplayReleaseRateEvent(ctr, releaserate);
		events.add(event);
	}
}

/**
 * Storage class for one replay event.
 * @author Volker Oth
 */
class ReplayEvent {
	/** frame counter */
	int frameCtr;
	/** event type */
	int type;

	/**
	 * Constructor
	 * @param ctr frame counter
	 * @param t type
	 */
	public ReplayEvent(final int ctr, final int t) {
		frameCtr = ctr;
		type = t;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ""+frameCtr+", "+type;
	}
}

/**
 * Storage class for ASSIGN_SKILL event
 * @author Volker Oth
 */
class ReplayAssignSkillEvent extends ReplayEvent {
	/** skill */
	Lemming.Type skill;
	/** Lemming */
	int lemming;

	/**
	 * Skill assigned
	 * @param ctr Frame counter
	 * @param s skill selected
	 * @param lem lemming no. that the skill was assigned
	 */
	public ReplayAssignSkillEvent(final int ctr, final Lemming.Type s, final int lem) {
		super(ctr, ReplayStream.ASSIGN_SKILL);
		skill = s;
		lemming = lem;
	}

	/* (non-Javadoc)
	 * @see Game.ReplayEvent#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+", "+skill.ordinal()+", "+lemming;
	}
}

/**
 * Storage class for SELECT_SKILL event.
 * @author Volker Oth
 */
class ReplaySelectSkillEvent extends ReplayEvent {
	Lemming.Type skill;

	/**
	 * Skill selected
	 * @param ctr Frame counter
	 * @param s skill selected
	 */
	public ReplaySelectSkillEvent(final int ctr, final Lemming.Type s) {
		super(ctr, ReplayStream.SELECT_SKILL);
		skill = s;
	}

	/* (non-Javadoc)
	 * @see Game.ReplayEvent#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+", "+skill.ordinal();
	}
}

/**
 * Storage class for MOVE_XPOS event.
 * @author Volker Oth
 */
class ReplayMoveXPosEvent extends ReplayEvent {
	/** screen x position */
	int xPos;

	/**
	 * Screen X position changed event
	 * @param ctr Frame counter
	 * @param x release x position
	 */
	public ReplayMoveXPosEvent(final int ctr, final int x) {
		super(ctr, ReplayStream.MOVE_XPOS);
		xPos = x;
	}

	/* (non-Javadoc)
	 * @see Game.ReplayEvent#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+", "+xPos;
	}
}

/**
 * Storage class for SET_RELEASE_RATE event.
 * @author Volker Oth
 */
class ReplayReleaseRateEvent extends ReplayEvent {
	int releaseRate;

	/**
	 * Release Rate changed event
	 * @param ctr Frame counter
	 * @param rate release rate value
	 */
	public ReplayReleaseRateEvent(final int ctr, final int rate) {
		super(ctr, ReplayStream.SET_RELEASE_RATE);
		releaseRate = rate;
	}

	/* (non-Javadoc)
	 * @see Game.ReplayEvent#toString()
	 */
	@Override
	public String toString() {
		return super.toString()+", "+releaseRate;
	}
}