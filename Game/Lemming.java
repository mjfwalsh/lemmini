package Game;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import Tools.Props;
import Tools.ToolBox;

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
 * Implements a Lemming.
 *
 * @author Volker Oth
 */
public class Lemming {
	/** name of the configuration file */
	private final static String LEMM_INI_STR = "misc/lemming.ini";
	/** number of resources (animations/names) */
	private final static int NUM_RESOURCES = 17;

	/** Lemming skill type */
	public static enum Type {
		/** the typical Lemming */
		WALKER,
		/** a falling Lemming */
		FALLER,
		/** a climbing Lemming */
		CLIMBER,
		/** a climbing Lemming returning to the ground */
		CLIMBER_TO_WALKER,
		/** a Lemming on a parachute */
		FLOATER,
		/** a Lemming dieing from a fall */
		SPLAT,
		/** a Lemming blocking the way for the other Lemmings */
		STOPPER,
		/** a Lemming drowning in the water */
		DROWNING,
		/** a Lemming killed by a trap */
		TRAPPED,
		/** a Lemming existing the level */
		EXITING,
		/** a Lemming blowing itself up */
		BOMBER,
		/** a Lemming building stairs */
		BUILDER,
		/** a builder Lemmings with no more steps in his backpack */
		BUILDER_END,
		/** a Lemming digging a hole in the ground */
		DIGGER,
		/** a Lemming bashing the ground before it */
		BASHER,
		/** a Lemming digging a mine with a pick */
		MINER,
		/** a Lemming jumping over a small obstacle */
		JUMPER,
		/* types without a separate animation */
		/** a Lemming that is nuked */
		NUKE,
		/** a stopper that is told to explode */
		BOMBER_STOPPER,
		/** a floater before the parachute opened completely */
		FLOATER_START,
		/** undefined */
		UNDEFINED;

		private static final Map<Integer,Type> lookup = new HashMap<Integer,Type>();

		static {
			for(Type s : EnumSet.allOf(Type.class))
				lookup.put(s.ordinal(), s);
		}

		/**
		 * Reverse lookup implemented via hashtable.
		 * @param val Ordinal value
		 * @return Parameter with ordinal value val
		 */
		public static Type get(final int val) {
			return lookup.get(val);
		}
	}

	/** Lemming heading */
	static enum Direction {
		RIGHT,
		LEFT,
		NONE;

		private static final Map<Integer,Direction> lookup = new HashMap<Integer,Direction>();

		static {
			for(Direction s : EnumSet.allOf(Direction.class))
				lookup.put(s.ordinal(), s);
		}

		/**
		 * Reverse lookup implemented via hashtable.
		 * @param val Ordinal value
		 * @return Parameter with ordinal value val
		 */
		public static Direction get(final int val) {
			return lookup.get(val);
		}

	}

	/** animation type */
	static enum Animation {
		NONE,
		LOOP,
		ONCE
	}

	/** display string for skills/types. order must be the same as in the enum! */
	private final static String LEMM_NAMES[] = {
		"WALKER", "FALLER", "CLIMBER", "CLIMBER",
		"FLOATER", "", "BLOCKER", "DROWNING",
		"", "", "BOMBER", "BUILDER",
		"BUILDER", "DIGGER", "BASHER", "MINER", "WALKER"
	};

	/** a walker walks one pixel per frame */
	private final static int WALKER_STEP = 1;
	/** a climber climbs up 1 pixel every 2nd frame */
	private final static int CLIMBER_STEP = 1;
	/** at this height a walker will turn around */
	private final static int WALKER_OBSTACLE_HEIGHT = 14;
	/** check N pixels above the lemming's feet */
	private final static int BASHER_CHECK_STEP = 12;
	/** from this on a basher will become a faller */
	private final static int BASHER_FALL_DISTANCE = 6;
	/** from this on a miner will become a faller */
	private final static int MINER_FALL_DISTANCE = 4;
	/** a faller falls down three pixels per frame */
	private final static int FALLER_STEP = 3;
	/** a floater falls down two pixels per frame */
	private final static int FLOATER_STEP = 2;
	/** a jumper moves up two pixels per frame */
	private final static int JUMPER_STEP = 2;
	/** if a walker jumps up 6 pixels, it becomes a jumper */
	private final static int JUMPER_JUMP = 4;
	/** pixels a floater falls before the parachute begins to open */
	private final static int FALL_DISTANCE_FLOAT = 32;
	/** number of free pixels below needed to convert a lemming to a faller */
	private final static int FALL_DISTANCE_FALL  = 8;
	/** used as "free below" value to convert most skills into a faller */
	private final static int FALL_DISTANCE_FORCE_FALL = 2*FALL_DISTANCE_FALL;
	/** number of steps a builder can build */
	private final static int STEPS_MAX = 12;
	/** number of steps before the warning sound is played */
	private final static int STEPS_WARNING = 9;
	/** Lemmini runs with 50fps instead of 25fps */
	private final static int TIME_SCALE = 2;
	/** explosion counter is decreased every second */
	private final static int MAX_EXPLODE_CTR = 1000000/GameController.MICROSEC_PER_FRAME;

	/** resource (animation etc.) for the current Lemming */
	private LemmingResource lemRes;
	/** animation frame */
	private int frameIdx;
	/** x coordinate of foot in pixels */
	private int x;
	/** y coordinate of foot in pixels */
	private int y;
	/** x coordinate for mask in pixels */
	private int maskX;
	/** y coordinate for mask in pixels */
	private int maskY;
	/** Lemming's heading */
	private Direction dir;
	/** Lemming's skill/type */
	private Type type;
	/** counter used for internal state changes */
	private int counter;
	/** another counter used for internal state changes */
	private int counter2;
	/** explosion counter when nuked */
	private int explodeNumCtr;
	/** Lemming can float */
	private boolean canFloat;
	/** Lemming can climb */
	private boolean canClimb;
	/** Lemming can change its skill */
	private boolean canChangeSkill;
	/** Lemming is to be nuked */
	private boolean nuke;
	/** Lemming has died */
	private boolean hasDied;
	/** Lemming has left the level */
	private boolean hasLeft;
	/** counter used to manage the explosion */
	private int explodeCtr;
	/** counter used to display the select image in replay mode */
	private int selectCtr;

	/** static array of resources for each Lemming skill/type */
	private static LemmingResource lemmings[];
	/** font used for the explosion counter */
	private static ExplodeFont explodeFont;

	/**
	 * Constructor: Create Lemming
	 * @param sx x coordinate of foot
	 * @param sy y coordinate of foot
	 */
	public Lemming(final int sx, final int sy) {
		frameIdx = 0;
		type = Type.FALLER; // always start with a faller
		lemRes = lemmings[getOrdinal(type)];
		counter = 0;
		explodeNumCtr = 0;
		selectCtr = 0;
		dir = Direction.RIGHT;  // always start walking to the right
		x = sx;
		y = sy;
		//insideStopper = false;
		canFloat = false; // new lemming can't float
		canClimb = false; // new lemming can't climb
		canChangeSkill = false; // a faller can not change the skill to e.g. builder
		hasDied = false;  // not yet
		hasLeft = false;  // not yet
		nuke = false;
	}

	/**
	 * Get number of Lemming type in internal resource array.
	 * @param t Type
	 * @return resource number for type
	 */
	public static int getOrdinal(final Type t) {
		switch (t) {
			case BOMBER_STOPPER:
				return Type.BOMBER.ordinal();
			case FLOATER_START:
				return Type.FLOATER.ordinal();
			default:
				return t.ordinal();
		}
	}

	/**
	 * Update animation, move Lemming, check state transitions.
	 */
	public void animate() {
		int free;
		Type oldType = type;
		Type newType = type;
		int oldX = x;
		boolean explode = false;
		// first check explode state
		if (explodeNumCtr != 0) {
			if (++explodeCtr >= MAX_EXPLODE_CTR) {
				explodeCtr -= MAX_EXPLODE_CTR;
				explodeNumCtr--;
				if (explodeNumCtr==0)
					explode = true;
			}
		}
		if (selectCtr>0)
			selectCtr--;
		flipDirBorder();
		// lemming state machine
		switch (type) {

			case CLIMBER_TO_WALKER:
				if (explode) {
					explode();
					break;
				}
				break;

			case FALLER:
				if (explode) {
					explode();
					break;
				}
				free = freeBelow(FALLER_STEP);
				if (free == FALL_DISTANCE_FORCE_FALL)
					y += FALLER_STEP;
				else
					y += free; // max: FALLER_STEP
				if (!crossedLowerBorder()) {
					counter += free; // fall counter
					// check conversion to floater
					if (canFloat && counter >= FALL_DISTANCE_FLOAT) {
						newType = Type.FLOATER_START;
						counter2 = 0; // used for parachute opening "jump" up
					} else if (free == 0) { // check ground hit
						//System.out.println(counter);
						if (counter > GameController.getLevel().getMaxFallDistance())
							newType = Type.SPLAT;
						else {
							newType = Type.WALKER;
							counter = 0;
						}
					}
				}
				break;

			case JUMPER: {
				if (explode) {
					newType = Type.BOMBER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
					break;
				}
				// check collision with stopper
				if (turnedByStopper())
					break;
				int levitation = aboveGround();
				if (levitation > JUMPER_STEP)
					y -= JUMPER_STEP;
				else {
					// conversion to walker
					y -= levitation;
					newType = Type.WALKER;
				}
				break;
			}

			case WALKER: {
				if (explode) {
					newType = Type.BOMBER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
					break;
				}
				// check collision with stopper
				if (turnedByStopper())
					break;
				if (dir == Direction.RIGHT)
					x += WALKER_STEP;
				else if (dir == Direction.LEFT)
					x -= WALKER_STEP;
				// check
				free = freeBelow(FALL_DISTANCE_FALL);
				if (free >= FALL_DISTANCE_FALL)
					y += FALLER_STEP;
				else {
					y += free;
					counter = free;
					//					if (free == 0)
					//						counter = 0; // reset fall counter
				}
				int levitation = aboveGround();
				// check for flip direction
				if (levitation < WALKER_OBSTACLE_HEIGHT && (y+lemRes.height/2)>0) {
					//y -= levitation;
					if (levitation >= JUMPER_JUMP) {
						y -= JUMPER_STEP;
						newType = Type.JUMPER;
						break;
					} else y -= levitation;
				} else {
					x = oldX;
					//y = oldY;
					if (canClimb) {
						newType = Type.CLIMBER;
						break;
					} else {
						dir = (dir==Direction.RIGHT) ? Direction.LEFT : Direction.RIGHT;
					}
				}
				if (free>0) {
					// check for conversion to faller
					counter += FALLER_STEP; // @check: is this ok? increasing counter, but using free???
					if (free >= FALL_DISTANCE_FALL)
						newType = Type.FALLER;
				}
				break; }

			case FLOATER_START:
				if (explode) {
					explode();
					break;
				}
				switch (counter2++) {
					case 0:
					case 1: // keep falling with faller speed
					case 2: y += FALLER_STEP-FLOATER_STEP;
					break;
					case 3: y -= FLOATER_STEP-1; // decelerate a little
					break;
					case 4:
					case 5:
					case 6:
					case 7: y -= FLOATER_STEP; // decelerate some more
					break;
					default:
						type = Type.FLOATER;
				}
				//$FALL-THROUGH$
			case FLOATER:
				if (explode) {
					explode();
					break;
				}
				free = freeBelow(FLOATER_STEP);
				if (free == FALL_DISTANCE_FORCE_FALL)
					y += FLOATER_STEP;
				else
					y += free; // max: FLOATER_STEP
				if (!crossedLowerBorder()) {
					counter += free; // fall counter
					// check ground hit
					if (free == 0) {
						newType = Type.WALKER;
						counter = 0;
					}
				}
				break;

			case CLIMBER:
				if (explode) {
					explode();
					break;
				}
				if ( (++counter & 1) == 1) // only every other step
					y -= CLIMBER_STEP;
				if (midY() < 0 || freeAbove(2)<2) {
					dir = (dir==Direction.RIGHT) ? Direction.LEFT : Direction.RIGHT;
					newType = Type.FALLER;
					counter = 0;
				} else if (reachedPlateau()) {
					counter = 0;
					newType = Type.CLIMBER_TO_WALKER;
				}
				break;

			case SPLAT:
				if (explode) {
					explode();
					break;
				}
				if (frameIdx ==0) // looped once
					GameController.sound.play(GameController.SND_SPLAT);
				break;

			case DIGGER:
				if (explode) {
					newType = Type.BOMBER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
					break;
				}
				break;

			case BASHER: {
				if (explode) {
					newType = Type.BOMBER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
					break;
				}
				// check for conversion to faller
				// check collision with stopper
				if (turnedByStopper()) {
					newType = Type.WALKER;
					break;
				}
				free = freeBelow(FLOATER_STEP);
				if (free == FALL_DISTANCE_FORCE_FALL)
					y += FALLER_STEP;
				else
					y += free;
				if (free != 0) {
					counter += free;
					if (counter >= BASHER_FALL_DISTANCE)
						newType = Type.FALLER;
				} else counter = 0;
				Mask m;
				int checkMask;
				int idx = frameIdx+1;
				if (idx >= lemRes.frames*TIME_SCALE)
					idx=0;
				switch (idx) {
					case 2*TIME_SCALE:
					case 3*TIME_SCALE:
					case 4*TIME_SCALE:
					case 5*TIME_SCALE: {
						//	bash mask should have the same height as the lemming
						m = lemRes.getMask(dir);
						int sx =screenX();
						int sy =screenY();
						checkMask = Stencil.MSK_STEEL|((dir==Direction.LEFT) ? Stencil.MSK_NO_DIG_LEFT : Stencil.MSK_NO_DIG_RIGHT);
						m.eraseMask(sx,sy,idx/TIME_SCALE-2, checkMask);
						// check for conversion to walker because there are indestructible pixels
						if (lemRes.getImask(dir).checkType(sx, sy, 0, checkMask)) {
							GameController.sound.play(GameController.SND_CHINK);
							newType = Type.WALKER;
						}
						if (idx == 5*TIME_SCALE)
							// check for conversion to walker because there are no bricks left
							if (!canBash()) {
								// no bricks any more
								newType = Type.WALKER;
							}
						break;}
					case 18*TIME_SCALE:
					case 19*TIME_SCALE:
					case 20*TIME_SCALE:
					case 21*TIME_SCALE: {
						//	bash mask should have the same height as the lemming
						m = lemRes.getMask(dir);;
						int sx =screenX();
						int sy =screenY();
						checkMask = Stencil.MSK_STEEL|((dir==Direction.LEFT) ? Stencil.MSK_NO_DIG_LEFT : Stencil.MSK_NO_DIG_RIGHT);
						m.eraseMask(sx,sy,idx/TIME_SCALE-18, checkMask);
						// check for conversion to walker because there are indestructible pixels
						if (lemRes.getImask(dir).checkType(sx, sy, 0, checkMask)) {
							GameController.sound.play(GameController.SND_CHINK);
							newType = Type.WALKER;
						}
						break; }
					case 10*TIME_SCALE:
					case 11*TIME_SCALE:
					case 12*TIME_SCALE:
					case 13*TIME_SCALE:
					case 14*TIME_SCALE:
					case 26*TIME_SCALE:
					case 27*TIME_SCALE:
					case 28*TIME_SCALE:
					case 29*TIME_SCALE:
					case 30*TIME_SCALE:
						if (dir == Direction.RIGHT)
							x += 2;
						else
							x -= 2;
						break;

				}
				break; }

			case MINER: {
				if (explode) {
					newType = Type.BOMBER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
					break;
				}
				// check collision with stopper
				if (turnedByStopper())
					break;
				Mask m;
				int sx;
				int sy;
				int idx = frameIdx+1;
				if (idx >= lemRes.frames*TIME_SCALE)
					idx=0;
				switch (idx) {
					case 1*TIME_SCALE:
					case 2*TIME_SCALE:
						// check for steel in mask
						m = lemRes.getMask(dir);
						sx = screenX();
						sy = screenY();
						int checkMask = Stencil.MSK_STEEL|((dir==Direction.LEFT) ? Stencil.MSK_NO_DIG_LEFT : Stencil.MSK_NO_DIG_RIGHT);
						m.eraseMask(sx,sy,idx/TIME_SCALE-1,checkMask);
						if (lemRes.getImask(dir).checkType(sx, sy, 0, checkMask)) {
							GameController.sound.play(GameController.SND_CHINK);
							newType = Type.WALKER;
						}
						break;
					case  3*TIME_SCALE:
					case 15*TIME_SCALE:
						if (dir == Direction.RIGHT)
							x += 4;
						else
							x -= 4;
						// check for conversion to faller
						free = freeBelow(MINER_FALL_DISTANCE);
						if (free >= MINER_FALL_DISTANCE) {
							if (free == FALL_DISTANCE_FORCE_FALL)
								y += FALLER_STEP;
							else
								y += free;
							newType = Type.FALLER;
							break;
						}
						if (idx == 15*TIME_SCALE)
							y += 4;
						break;
						//case 23*TIME_SCALE:
						//	break;
				}
				break; }

			case BUILDER_END:
				if (explode) {
					newType = Type.BOMBER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
				}
				break;
			case BUILDER: {
				if (explode) {
					newType = Type.BOMBER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
					break;
				}
				// check collision with stopper
				if (turnedByStopper())
					break;
				int idx = frameIdx+1;
				if (idx >= lemRes.frames*TIME_SCALE) {
					// step created -> move up
					idx=0;
					counter++; // step counter;
					if (dir == Direction.RIGHT)
						x += 4; // step forward
					else
						x -= 4;
					y-=2; // step up
					int levitation = aboveGround(); // should be 0, if not, we built into a wall -> stop
					// check for conversion to walker
					int fa = freeAbove(8); // check if builder is too close to ceiling
					if (fa < 8 || levitation > 0) {
						newType = Type.WALKER;
						// a lemming can jump through the ceiling like in Mayhem2-Boiler Room
						if (levitation >= WALKER_OBSTACLE_HEIGHT) {
							// avoid getting stuck
							x = oldX;
							y += 2;
						}
						dir = (dir==Direction.RIGHT) ? Direction.LEFT : Direction.RIGHT;
						break;
					}
					// check for last step used
					if (counter >= STEPS_MAX) {
						newType = Type.BUILDER_END;
						break;
					}
				} else if (idx == 9*TIME_SCALE) {
					// stair mask is the same heigth as a lemming
					Mask m;
					m = lemRes.getMask(dir);;
					int sx = screenX();
					int sy = screenY();
					m.paintStep(sx,sy,0,GameController.getLevel().getDebrisColor());
					if (counter >= STEPS_WARNING)
						GameController.sound.play(GameController.SND_TING);
				}
				break; }

			case STOPPER: {
				if (explode) {
					// don't erase stopper mask!
					newType = Type.BOMBER_STOPPER;
					if (!nuke)
						GameController.sound.play(GameController.SND_OHNO);
					break;
				}
				// check for conversion to faller
				free = freeBelow(FLOATER_STEP);
				if (free > 0) {
					if (free == FALL_DISTANCE_FORCE_FALL)
						y += FALLER_STEP;
					else
						y += free;
					counter += free;
					if (counter >= FALL_DISTANCE_FALL)
						newType = Type.FALLER;
					else
						newType = Type.WALKER;
					// conversion to faller or walker -> erase stopper mask
					Mask m = lemmings[getOrdinal(Type.STOPPER)].getMask(dir);
					m.clearType(maskX,maskY,0,Stencil.MSK_STOPPER);
				} else counter = 0;
				break; }

			case BOMBER_STOPPER:
				// don't erase stopper mask before stopper finally explodes or falls
				free = freeBelow(FLOATER_STEP);
				if (free>0) {
					// stopper falls -> erase mask and convert to normal stopper.
					Mask m = lemmings[getOrdinal(Type.STOPPER)].getMask(dir);
					m.clearType(maskX,maskY,0,Stencil.MSK_STOPPER);
					type = Type.BOMBER;
					// fall through
				} else break;
				//$FALL-THROUGH$
			case BOMBER:
				free = freeBelow(FLOATER_STEP);
				if (free == FALL_DISTANCE_FORCE_FALL)
					y += FALLER_STEP;
				else
					y += free;
				crossedLowerBorder();
				break;

			default:
				// all cases not explicitly checked above should at least explode
				if (explode) {
					explode();
					break;
				}
				break;

		}
		// check collision with exit and traps
		int s = stencilMid();
		switch (s & (Stencil.MSK_TRAP|Stencil.MSK_EXIT)) {
			case Stencil.MSK_TRAP_DROWN:
				if (type != Type.DROWNING) {
					newType = Type.DROWNING;
					SpriteObject spr = GameController.getLevel().getSprObject(Stencil.getObjectID(s));
					GameController.sound.play(spr.getSound());
				}
				break;
			case Stencil.MSK_TRAP_DIE:
				if (type != Type.TRAPPED) {
					SpriteObject spr = GameController.getLevel().getSprObject(Stencil.getObjectID(s));
					if (spr.canBeTriggered()) {
						if (spr.trigger()) {
							GameController.sound.play(spr.getSound());
							newType = Type.TRAPPED;
						}
					} else {
						GameController.sound.play(spr.getSound());
						newType = Type.TRAPPED;
					}
					if ( type == Type.STOPPER || type == Type.BOMBER_STOPPER) {
						// erase stopper mask
						Mask m = lemmings[getOrdinal(Type.STOPPER)].getMask(dir);
						m.clearType(maskX,maskY,0,Stencil.MSK_STOPPER);
					}
				}
				break;
			case Stencil.MSK_TRAP_REPLACE: {
				SpriteObject spr = GameController.getLevel().getSprObject(Stencil.getObjectID(s));
				if (spr.canBeTriggered()) {
					if (spr.trigger()) {
						GameController.sound.play(spr.getSound());
						hasDied = true;
					}
				} else {
					GameController.sound.play(spr.getSound());
					hasDied = true;
				}
				if ( type == Type.STOPPER || type == Type.BOMBER_STOPPER) {
					// erase stopper mask
					Mask m = lemmings[getOrdinal(Type.STOPPER)].getMask(dir);
					m.clearType(maskX,maskY,0,Stencil.MSK_STOPPER);
				}
				break; }
			case Stencil.MSK_EXIT:
				switch (type) {
					case WALKER:
					case JUMPER:
					case BASHER:
					case MINER:
					case BUILDER:
					case DIGGER:
						SpriteObject spr = GameController.getLevel().getSprObject(Stencil.getObjectID(s));
						newType = Type.EXITING;
						GameController.sound.play(spr.getSound());
						break;
				}
				break;
		}
		// animate
		if (oldType == newType) {
			boolean trigger = false;
			switch (lemRes.animMode) {
				case LOOP:
					if(++frameIdx >= lemRes.frames*TIME_SCALE)
						frameIdx = 0;
					if (lemRes.maskStep > 0 && frameIdx % (lemRes.maskStep*TIME_SCALE) == 0)
						trigger = true;
					break;
				case ONCE:
					if (frameIdx < lemRes.frames*TIME_SCALE-1)
						frameIdx++;
					else
						trigger = true;
					break;
			}
			if (trigger) {
				// Trigger condition reached?
				switch (type) {
					case BOMBER_STOPPER: {
						Mask m = lemmings[getOrdinal(Type.STOPPER)].getMask(dir);
						m.clearType(maskX,maskY,0,Stencil.MSK_STOPPER);}
					//$FALL-THROUGH$
					case BOMBER:
						explode();
						break;
					case SPLAT:
					case DROWNING:
					case TRAPPED:
						hasDied = true;
						break;
					case EXITING:
						hasLeft = true;
						GameController.increaseLeft();
						break;
					case FLOATER_START:
						type = Type.FLOATER; // should never happen
						//$FALL-THROUGH$
					case FLOATER:
						frameIdx -= 5*TIME_SCALE; // rewind 5 frames
						break;
					case CLIMBER_TO_WALKER:
						newType = Type.WALKER;
						y -= 10; // why is this needed? could be done via foot coordinates?
						break;
					case DIGGER:
						// the dig mask must be applied to the bottom of the lemming
					{Mask m = lemRes.getMask(dir);
					int sx = screenX();
					int sy = screenY();
					m.eraseMask(sx,sy,0,Stencil.MSK_STEEL);

					// check for conversion to walker when hitting steel
					if (lemRes.getImask(dir).checkType(sx, sy, 0, Stencil.MSK_STEEL)) {
						GameController.sound.play(GameController.SND_CHINK);
						newType = Type.WALKER;
					} else
						y += 2; // move down

					// check for conversion to faller
					int freeMin = Integer.MAX_VALUE;
					free = 0;
					int xOld = x;
					for (int i=-6; i<6; i++) { // should be 14 pixels, here it's more like 12
						x = xOld+i;
						if (x<0)
							x=0;
						else if (x>=Level.WIDTH)
							x=Level.WIDTH;
						if ((free = freeBelow(FLOATER_STEP)) < freeMin)
							freeMin = free;
					}
					x = xOld;
					free = freeMin;
					if (free > 0) {
						//convert to faller or walker
						if (free >= FALL_DISTANCE_FALL)
							newType = Type.FALLER;
						else
							newType = Type.FALLER;
						if (free >= FALLER_STEP)
							y += FALLER_STEP;
						else
							y += free;
					}

					break;}
					case BUILDER_END:
						newType = Type.WALKER;
						break;
				}
			}
		}
		changeType(oldType, newType);
	}

	/**
	 * Check if a Lemming is to be turned by a stopper/blocker.
	 * @return true if Lemming is to be turned, false otherwise
	 */
	private boolean turnedByStopper() {
		int s = (stencilMid()&Stencil.MSK_STOPPER);

		if (s == Stencil.MSK_STOPPER_LEFT && dir==Direction.RIGHT) {
			dir = Direction.LEFT;
			return true;
		}
		if (s == Stencil.MSK_STOPPER_RIGHT && dir==Direction.LEFT) {
			dir = Direction.RIGHT;
			return true;
		}
		return false;
	}

	/**
	 * Change skill/type.
	 * @param oldType old skill/type of Lemming
	 * @param newType new skill/type of Lemming
	 */
	private void changeType(final Type oldType, final Type newType) {
		if (oldType != newType) {
			type = newType;
			lemRes = lemmings[getOrdinal(type)];
			if (newType == Type.DIGGER)
				frameIdx = lemRes.frames*TIME_SCALE-1; // start digging immediately
			else
				frameIdx = 0;

			// some types can't change the skill - check this
			switch (newType) {
				case WALKER:
					//insideStopper = (stencilMid()&Stencil.MSK_STOPPER) != 0;
				case BASHER:
				case BUILDER:
				case BUILDER_END:
				case DIGGER:
				case MINER:
					canChangeSkill = true;
					break;
				default:
					canChangeSkill = false;
			}
		}
	}

	/**
	 * Let the Lemming explode.
	 */
	private void explode () {
		GameController.sound.play(GameController.SND_EXPLODE);
		// create particle explosion
		GameController.addExplosion(midX(), midY());
		hasDied = true;
		changeType(type, Type.BOMBER);
		// consider height difference between lemming and mask
		Mask m = lemRes.getMask(Direction.RIGHT);
		// check if lemming is standing on steel
		int sy = y+1;
		if (x > 0 && x < Level.WIDTH && sy > 0 && sy < Level.HEIGHT)
			m.eraseMask(x-m.getWidth()/2,midY()-m.getHeight()/2+3,0,Stencil.MSK_STEEL);
		//			if ((Core.stencil.get(x + sy*Level.width) & Stencil.MSK_STEEL) == 0)
		//				m.eraseMask(x-m.width/2,midY()-m.height/2,0,0);
	}


	/**
	 * Get stencil value from the middle of the lemming
	 * @return stencil value from the middle of the lemming
	 */
	private int stencilMid() {
		int xm = x;
		int ym = y-lemRes.size;
		int retval;
		if (xm>0 && xm<Level.WIDTH && ym > 0 && ym < Level.HEIGHT)
			retval = GameController.getStencil().get(xm+Level.WIDTH*ym);
		else
			retval = Stencil.MSK_EMPTY;
		return retval;
	}

	/**
	 * Check if bashing is possible.
	 * @return true if bashing is possible, false otherwise.
	 */
	private boolean canBash() {
		int xm = midX();
		int ypos = Level.WIDTH*(y-BASHER_CHECK_STEP) ;
		int xb;
		int bricks = 0;
		for (int i=16; i<25; i++) {
			if (dir == Direction.RIGHT)
				xb = xm+i;
			else
				xb = xm-i;
			int sval = GameController.getStencil().get(xb+ypos);
			if ((sval & Stencil.MSK_NO_DIG_LEFT) != 0 && dir == Direction.LEFT)
				return false;
			if ((sval & Stencil.MSK_NO_DIG_RIGHT) != 0 && dir == Direction.RIGHT)
				return false;
			if ((sval & Stencil.MSK_STEEL) != 0)
				return false;
			if ((sval & Stencil.MSK_WALK_ON) == Stencil.MSK_BRICK)
				bricks++;
		}
		if (bricks>0)
			return true;
		else
			return false;
	}

	/**
	 * Check if digging is possible.
	 * @return true if digging is possible, false otherwise.
	 */
	private boolean canDig() {
		int ypos = Level.WIDTH*(y+1);
		int xm = x;
		int sval = GameController.getStencil().get(xm+ypos);
		if ((sval & Stencil.MSK_WALK_ON) == Stencil.MSK_BRICK)
			return true;
		return false;
	}

	/**
	 * Check if mining is possible.
	 * @return true if mining is possible, false otherwise.
	 */
	private boolean canMine() {
		int ypos = Level.WIDTH*(y+1);
		int bricks= 0;
		int xMin;
		int xMax;
		if (dir == Direction.RIGHT) {
			xMin = x;
			xMax = x-lemRes.footX+lemRes.width;
		} else {
			xMin = x-lemRes.footX;
			xMax = x;
		}
		for (int xb = xMin; xb < xMax; xb++) {
			int sval = GameController.getStencil().get(xb+ypos);
			if ((sval & Stencil.MSK_NO_DIG_LEFT) != 0 && dir == Direction.LEFT)
				return false;
			if ((sval & Stencil.MSK_NO_DIG_RIGHT) != 0 && dir == Direction.RIGHT)
				return false;
			if ((sval & Stencil.MSK_STEEL) != 0)
				return false;
			if ((sval & Stencil.MSK_WALK_ON) == Stencil.MSK_BRICK)
				bricks++;
		}
		if (bricks>0)
			return true;
		else
			return false;
	}

	/**
	 * Get number of free pixels below the lemming (max of step is checked).
	 * @return number of free pixels below the lemming
	 */
	private int freeBelow(final int step) {
		if (x<0 || x >= Level.WIDTH)
			return 0;
		int free = 0;
		int pos = x;
		Stencil stencil = GameController.getStencil();
		int yb = y+1;
		pos = x+yb*Level.WIDTH; // line below the lemming
		for (int i=0; i<step; i++) {
			if (yb+i >= Level.HEIGHT)
				return FALL_DISTANCE_FORCE_FALL; // convert most skill to faller
			int s = stencil.get(pos);
			if ((s & Stencil.MSK_WALK_ON) == Stencil.MSK_EMPTY)
				free++;
			else break;
			pos += Level.WIDTH;
		}
		return free;
	}

	/**
	 * Check if Lemming reached the left or right border of the level and was turned.
	 * @return true if lemming was turned, false otherwise.
	 */
	private boolean flipDirBorder() {
		boolean flip = false;
		if (lemRes.dirs >1) {
			if (x<0) {
				x=0;
				flip = true;
			} else if (x >= Level.WIDTH) {
				x = Level.WIDTH-1;
				flip = true;
			}
		}
		if (flip)
			dir = (dir==Direction.RIGHT) ? Direction.LEFT : Direction.RIGHT;
		return flip;
	}

	/**
	 * Get number of free pixels above the lemming (max of step is checked).
	 * @return number of free pixels above the lemming
	 */
	private int freeAbove(final int step) {
		if (x<0 || x >= Level.WIDTH)
			return 0;

		int free = 0;
		int pos;
		int ym = midY();
		Stencil stencil = GameController.getStencil();
		pos = x + ym*Level.WIDTH;
		for (int i=0; i<step; i++) {
			if (ym-i <= 0)
				return -1; // splat
			if ((stencil.get(pos) & Stencil.MSK_WALK_ON) == Stencil.MSK_EMPTY
			/*|| (stencil.get(pos+1) & Stencil.MSK_WALK_ON) == Stencil.MSK_EMPTY  */ )
				free++;
			else break;
			pos -= Level.WIDTH;
		}
		return free;
	}

	/**
	 * Check if Lemming has fallen to/through the bottom of the level.
	 * @return true if Lemming has fallen to/through the bottom of the level, false otherwise
	 */
	private boolean crossedLowerBorder() {
		if (y >= Level.HEIGHT) {
			hasDied = true;
			GameController.sound.play(GameController.SND_DIE);
			return true;
		}
		return false;
	}

	/**
	 * Get the number of pixels of walkable ground above the Lemmings foot.
	 * @return number of pixels of walkable ground above the Lemmings foot.
	 */
	private int aboveGround() {
		if (x<0 || x >= Level.WIDTH)
			return Level.HEIGHT-1;

		int ym = y;
		if (ym >= Level.HEIGHT)
			return Level.HEIGHT-1;
		int pos = x;
		Stencil stencil = GameController.getStencil();
		pos += ym*Level.WIDTH;
		int levitation;
		for (levitation=0; levitation<WALKER_OBSTACLE_HEIGHT; levitation++, pos -= Level.WIDTH, ym--) {
			if (ym<0)
				return WALKER_OBSTACLE_HEIGHT+1; // forbid leaving level to the top
			if ( (stencil.get(pos) & Stencil.MSK_WALK_ON) == Stencil.MSK_EMPTY)
				break;
		}
		return levitation;
	}

	/**
	 * Check if climber reached a plateau he can walk on.
	 * @return true if climber reached a plateau he can walk on, false otherwise
	 */
	private boolean reachedPlateau() {
		if (x<2 || x >= Level.WIDTH-2)
			return false;
		int ym = midY();
		if (ym>= Level.HEIGHT || ym<0)
			return false;
		int pos = x;
		if (dir == Direction.LEFT)
			pos -= 2;
		else
			pos += 2;
		pos += ym*Level.WIDTH;
		if ((GameController.getStencil().get(pos) & Stencil.MSK_WALK_ON) == Stencil.MSK_EMPTY)
			return true;
		else
			return false;
	}

	/**
	 * Replace a color in the animation frame with another color.
	 * Used to patch the color of debris from pink color to a level specific color.
	 * @param findCol color to find
	 * @param replaceCol color to replace with
	 */
	public static void patchColors(final int findCol, final int replaceCol) {
		for (int l = 0; l<NUM_RESOURCES; l++) { 	 // go through all the lemmings
			LemmingResource lr = lemmings[l];
			for (int f = 0; f<lr.frames; f++)    // go through all frames
				for (int d=0; d<lr.dirs; d++)    // go though all dirs
					for (int xp=0; xp<lr.width; xp++)
						for (int yp=0; yp<lr.height; yp++) {
							BufferedImage i = lr.getImage(Direction.get(d),f);
							if (i.getRGB(xp,yp) == findCol)
								i.setRGB(xp,yp,replaceCol);
						}
		}
	}

	/**
	 * Load images used for Lemming animations.
	 * @param cmp parent component
	 * @throws ResourceException
	 */
	public static void loadLemmings(final Component cmp) throws ResourceException {
		explodeFont = new ExplodeFont(cmp);
		MediaTracker tracker = new MediaTracker(cmp);
		// read lemmings definition file
		String fn = Core.findResource(LEMM_INI_STR);
		Props p = new Props();
		if (!p.load(fn))
			throw new ResourceException(LEMM_INI_STR);
		lemmings = new LemmingResource[NUM_RESOURCES];
		// read lemmings
		int def[] = {-1};
		for (int i = 0; true; i++) {
			int[] val = p.get("lemm_"+i,def);
			int type;
			if (val.length == 3) {
				// frames, directions, animation type
				type = i;
				if (lemmings[type] == null) {
					BufferedImage sourceImg = ToolBox.ImageToBuffered(
							Core.loadImage(tracker, "misc/lemm_"+i+".gif"), Transparency.BITMASK);
					try {
						tracker.waitForAll();
					} catch (InterruptedException ex) {}
					lemmings[type] = new LemmingResource(sourceImg, val[0], val[1]);
					lemmings[type].animMode = (val[2]==0) ? Animation.LOOP : Animation.ONCE;
				}
			} else break;
			// read mask
			val = p.get("mask_"+i,def);
			if (val.length == 3) {
				// mask_Y: frames, directions, step
				type = i;
				Image sourceImg = Core.loadImage(tracker, "misc/mask_"+i+".gif");
				Mask mask = new Mask(ToolBox.ImageToBuffered(sourceImg, Transparency.BITMASK), val[0]);
				lemmings[type].setMask(Direction.RIGHT, mask);
				int dirs = val[1];
				if (dirs > 1) {
					Mask maskLeft = new Mask(ToolBox.flipImageX(ToolBox.ImageToBuffered(sourceImg,Transparency.BITMASK)), val[0]);
					lemmings[type].setMask(Direction.LEFT, maskLeft);
				}
				lemmings[type].maskStep = val[2];
			}
			// read indestructible mask
			val = p.get("imask_"+i,def);
			if (val.length == 2) {
				// mask_Y: type, frames, directions, step
				type = i;
				Image sourceImg = Core.loadImage(tracker, "misc/imask_"+i+".gif");
				Mask mask = new Mask(ToolBox.ImageToBuffered(sourceImg, Transparency.BITMASK), val[0]);
				lemmings[type].setImask(Direction.RIGHT, mask);
				int dirs = val[1];
				if (dirs > 1) {
					Mask maskLeft = new Mask(ToolBox.flipImageX(ToolBox.ImageToBuffered(sourceImg,Transparency.BITMASK)), val[0]);
					lemmings[type].setImask(Direction.LEFT, maskLeft);
				}
			}
			// read foot position and size
			val = p.get("pos_"+i,def);
			if (val.length == 3) {
				lemmings[type].footX = val[0];
				lemmings[type].footY = val[1];
				lemmings[type].size  = val[2];
			} else break;
		}
	}

	/**
	 * Get display name of this Lemming.
	 * @return display name of this Lemming
	 */
	public String getName() {
		Type t;
		switch (type) {
			case BOMBER_STOPPER:
				t = Type.BOMBER;
				break;
			case FLOATER_START:
				t = Type.FLOATER;
				break;
			default:
				t = type;
		}
		String n = LEMM_NAMES[getOrdinal(t)];
		if (n.length()>0) {
			if (canFloat) {
				if (canClimb)
					n += "(A)";
				else if (t != Type.FLOATER)
					n += "(F)";
			} else {
				if (canClimb && t != Type.CLIMBER)
					n += "(C)";
			}
		}
		return n;
	}

	/**
	 * Get current skill/type of this Lemming.
	 * @return current skill/type of this Lemming
	 */
	public Type getSkill() {
		return type;
	}

	/**
	 * Set new skill/type of this Lemming.
	 * @param skill new skill/type
	 * @return true if a change was possible, false otherwise
	 */
	public boolean setSkill(final Type skill) {
		if (skill == type || hasDied)
			return false;
		// check types which can't even get an additional skill anymore
		switch (type) {
			case DROWNING:
			case EXITING:
			case SPLAT:
			case TRAPPED:
			case BOMBER:
				if (skill == Type.NUKE) {
					if (nuke)
						return false;
					nuke = true;
					if (explodeNumCtr==0) {
						explodeNumCtr = 5;
						explodeCtr = 0;
						return true;
					} else return false;
				}
				return false;
		}
		// check additional skills
		switch (skill) {
			case CLIMBER:
				if (canClimb)
					return false;
				canClimb = true;
				return true;
			case FLOATER:
				if (canFloat)
					return false;
				canFloat = true;
				return true;
			case NUKE: // special case:  nuke request
				if (nuke)
					return false;
				nuke = true;
				//$FALL-THROUGH$
			case BOMBER:
				if (explodeNumCtr==0) {
					explodeNumCtr = 5;
					explodeCtr = 0;
					return true;
				} else return false;
		}
		// check main skills
		if (canChangeSkill) {
			switch (skill) {
				case DIGGER:
					if (canDig()) {
						//y += DIGGER_GND_OFFSET;
						changeType(type, skill);
						counter = 0;
						return true;
					} else return false;
				case MINER:
					if (canMine()) {
						//y += 2;
						changeType(type, skill);
						counter = 0;
						return true;
					} else return false;
				case BASHER:
					//if (canBash(true)) {
					changeType(type, skill);
					counter = 0;
					return true;
					//} else return false;
				case BUILDER: {
					//int fa = freeAbove(4);
					int fb = freeBelow(FALLER_STEP);
					if (/*fa <= 0 ||*/ fb != 0)
						return false;
					//x = x & ~1; // start building at even positions
					changeType(type, skill);
					counter = 0;
					return true; }
				case STOPPER: {
					Mask m = Lemming.getResource(Type.STOPPER).getMask(Direction.LEFT);
					maskX = screenX();
					maskY = screenY();
					if (m.checkType(maskX, maskY, 0, Stencil.MSK_STOPPER))
						return false; // overlaps existing stopper
					changeType(type, skill);
					counter = 0;
					// set stopper mask
					m.setStopperMask(maskX,maskY,x);
					return true;}
			}
		}
		return false;
	}

	/**
	 * Get width of animation frame in pixels.
	 * @return width of animation frame in pixels
	 */
	public int width() {
		return lemRes.width;
	}

	/**
	 * Get height of animation frame in pixels.
	 * @return height of animation frame in pixels
	 */
	public int height() {
		return lemRes.height;
	}

	/**
	 * Get static resource for a skill/type
	 * @param type skill/type
	 * @return static resource for this skill/type
	 */
	private static LemmingResource getResource(final Type type) {
		return lemmings[getOrdinal(type)];
	}

	/**
	 * Get X coordinate of upper left corner of animation frame.
	 * @return X coordinate of upper left corner of animation frame
	 */
	public int screenX() {
		if (lemRes.dirs == 1 || dir == Direction.RIGHT) {
			return x - lemRes.footX;
		} else {
			return x - lemRes.width + lemRes.footX;
		}
	}

	/**
	 * Get Y coordinate of upper left corner of animation frame
	 * @return Y coordinate of upper left corner of animation frame
	 */
	public int screenY() {
		return y - lemRes.footY;
	}

	/**
	 * Get X coordinate of collision position in pixels.
	 * @return X coordinate of collision position in pixels.
	 */
	public int midX() {
		return x;
	}

	/**
	 * Collision position
	 * @return Position inside lemming which is used for collisions
	 */
	public int midY() {
		return y - lemRes.size;
	}

	/**
	 * Get heading of Lemming.
	 * @return heading of Lemming
	 */
	public Direction getDirection() {
		return dir;
	}

	/**
	 * Get current animation frame for this Lemming.
	 * @return current animation frame for this Lemming
	 */
	public BufferedImage getImage() {
		return lemRes.getImage(dir,frameIdx/TIME_SCALE);
	}

	/**
	 * Get image for explosion countdown.
	 * @return image for explosion countdown (or null if no explosion countdown)
	 */
	public BufferedImage getCountdown() {
		if (explodeNumCtr==0)
			return null;
		else return explodeFont.getImage(explodeNumCtr-1);
	}

	/**
	 * Used for replay: start to display the selection image.
	 */
	public void setSelected() {
		selectCtr = 20;
	}

	/**
	 * Get the selection image for replay.
	 * @return the selection image (or null if no selection displayed)
	 */
	public BufferedImage getSelectImg() {
		if (selectCtr==0)
			return null;
		else return MiscGfx.getImage(MiscGfx.Index.SELECT);
	}

	/**
	 * Get: Lemming has died.
	 * @return true if Lemming has died, false otherwise
	 */
	public boolean hasDied() {
		return hasDied;
	}

	/**
	 * Get: Lemming has left the level.
	 * @return true if Lemming has left the level, false otherwise
	 */
	public boolean hasLeft() {
		return hasLeft;
	}

	/**
	 * Get: Lemming is to be nuked.
	 * @return true if Lemming is to be nuked, false otherwise
	 */
	public boolean nuke() {
		return nuke;
	}

	/**
	 * Get: Lemming can float.
	 * @return true if Lemming can float, false otherwise
	 */
	public boolean canFloat() {
		return canFloat;
	}

	/**
	 * Get: Lemming can climb.
	 * @return true if Lemming can climb, false otherwise
	 */
	public boolean canClimb() {
		return canClimb;
	}

	/**
	 * Get: Lemming can get a new skill.
	 * @return true if Lemming can get a new skill, false otherwise
	 */
	public boolean canChangeSkill() {
		return canChangeSkill;
	}

}

/**
 * Storage class for a Lemming.
 * @author Volker Oth
 */
class LemmingResource {
	/** relative foot X position in pixels inside bitmap */
	int footX;
	/** relative foot Y position in pixels inside bitmap */
	int footY;
	/** mask collision ("mid") position above foot in pixels */
	int size;
	/** width of image in pixels */
	int width;
	/** height of image in pixels */
	int height;
	/** number of animation frames */
	int frames;
	/** animation mode */
	Lemming.Animation animMode;
	/** number of directions (1 or 2) */
	int dirs;
	int maskStep;
	/** array of images to store the animation [Direction][AnimationFrame] */
	private BufferedImage img[][];
	/** array of removal masks used for digging/bashing/mining/explosions etc. [Direction] */
	private Mask mask[];
	/** array of check masks for indestructible pixels [Direction] */
	private Mask iMask[];

	/**
	 * Constructor.
	 * @param sourceImg  image containing animation frames (one above the other)
	 * @param animFrames number of animation frames.
	 * @param directions number of directions (1 or 2)
	 */
	LemmingResource(final BufferedImage sourceImg, final int animFrames, final int directions) {
		img = new BufferedImage[directions][];
		mask = new Mask[directions];
		iMask = new Mask[directions];
		frames = animFrames;
		width = sourceImg.getWidth(null);
		height = sourceImg.getHeight(null)/animFrames;
		dirs = directions;
		animMode = Lemming.Animation.NONE;
		img[Lemming.Direction.RIGHT.ordinal()] = ToolBox.getAnimation(sourceImg, animFrames, Transparency.BITMASK);
		if (dirs>1)
			img[Lemming.Direction.LEFT.ordinal()] = ToolBox.getAnimation(ToolBox.flipImageX(sourceImg), animFrames, Transparency.BITMASK);
	}

	/**
	 * Get the mask for stencil manipulation.
	 * @param dir Direction
	 * @return mask for stencil manipulation
	 */
	Mask getMask(final Lemming.Direction dir) {
		if (dirs > 1)
			return mask[dir.ordinal()];
		else
			return mask[0];
	}

	/**
	 * Set the mask for stencil manipulation.
	 * @param dir Direction
	 * @param m mask for stencil manipulation
	 */
	void setMask(final Lemming.Direction dir, final Mask m) {
		if (dirs > 1)
			mask[dir.ordinal()] = m;
		else
			mask[0] = m;
	}

	/**
	 * Get the mask for checking of indestructible pixels.
	 * @param dir Direction
	 * @return mask for checking of indestructible pixels
	 */
	Mask getImask(final Lemming.Direction dir) {
		if (dirs > 1)
			return iMask[dir.ordinal()];
		else
			return iMask[0];
	}

	/**
	 * Set the mask for checking of indestructible pixels
	 * @param dir Direction
	 * @param m mask for checking of indestructible pixels
	 */
	void setImask(final Lemming.Direction dir, final Mask m) {
		if (dirs > 1)
			iMask[dir.ordinal()] = m;
		else
			iMask[0] = m;
	}

	/**
	 * Get specific animation frame.
	 * @param dir Direction.
	 * @param frame Index of animation frame.
	 * @return specific animation frame
	 */
	BufferedImage getImage(final Lemming.Direction dir, final int frame) {
		if (dirs > 1)
			return img[dir.ordinal()][frame];
		else
			return img[0][frame];
	}
}


/**
 * Used to manage the font for the explosion counter.
 * @author Volker Oth
 */
class ExplodeFont {

	/**
	 * Constructor.
	 * @param cmp parent component
	 * @throws ResourceException
	 */
	ExplodeFont(final Component cmp) throws ResourceException {
		Image sourceImg = Core.loadImage("misc/countdown.gif");
		img = ToolBox.getAnimation(sourceImg,5,Transparency.BITMASK);
	}

	/**
	 * Get image for a counter value (0..9)
	 * @param num counter value (0..9)
	 * @return
	 */
	BufferedImage getImage(final int num) {
		return img[num];
	}

	/** array of images for each counter value */
	private BufferedImage img[];
}