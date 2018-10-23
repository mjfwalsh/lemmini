package Game;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

import GameUtil.Sprite;
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
 * Load a level, paint level, create background stencil.
 *
 * @author Volker Oth
 */
public class Level {
	/** maximum width of level */
	public final static int WIDTH = 1664*2;
	/** maximum height of level */
	public final static int HEIGHT = 160*2;
	/** array of default ARGB colors for particle effects */
	public final static int DEFAULT_PARTICLE_COLORS[] = { 0xff00ff00, 0xff0000ff, 0xffffffff, 0xffffffff, 0xffff0000 };

	/** array of default styles */
	private final static String STYLES[] = { "dirt", "fire", "marble", "pillar",
		"crystal", "brick", "rock", "snow", "Bubble", "special" };
	/** template color to be replaced with debris color */
	private final static int TEMPLATE_COLOR = 0xffff00ff;

	/** array of normal sprite objects - no transparency, drawn behind background image */
	private SpriteObject sprObjBehind[];
	/** array of special sprite objects - with transparency, drawn above background image */
	private SpriteObject sprObjFront[];
	/** array of all sprite objects (in front and behind) */
	private SpriteObject sprObjects[];
	/** array of level entries */
	private Entry entries[];
	/** release rate : 0 is slowest, 0x0FA (250) is fastest */
	private int releaseRate;
	/** number of Lemmings in this level (maximum 0x0072 in original LVL format) */
	private int numLemmings;
	/** number of Lemmings to rescue : should be less than or equal to number of Lemmings */
	private int numToRescue;
	/** time limit in seconds */
	private int timeLimitSeconds;
	/** number of climbers in this level : max 0xfa (250) */
	private int numClimbers;
	/** number of floaters in this level : max 0xfa (250) */
	private int numFloaters;
	/** number of bombers in this level : max 0xfa (250) */
	private int numBombers;
	/** number of blockers in this level : max 0xfa (250) */
	private int numBlockers;
	/** number of builders in this level : max 0xfa (250) */
	private int numBuilders;
	/** number of bashers in this level : max 0xfa (250) */
	private int numBashers;
	/** number of miners in this level : max 0xfa (250) */
	private int numMiners;
	/** number of diggers in this level : max 0xfa (250) */
	private int numDiggers;
	/** start screen x pos : 0 - 0x04f0 (1264) rounded to modulo 8 */
	private int xPos;
	/** background color as ARGB */
	private int bgCol;
	/** background color */
	private Color bgColor;
	/** color used for steps and debris */
	private int debrisCol;
	/** array of ARGB colors used for particle effects */
	private int particleCol[];
	/** maximum safe fall distance */
	private int maxFallDistance;
	/** this level is a SuperLemming level (runs faster) */
	private boolean superlemming;
	/** level is  completely loaded */
	private boolean ready = false;
	/** objects like doors - originally 32 objects where each consists of 8 bytes */
	private ArrayList<LvlObject> objects;
	/** background tiles - every pixel in them is interpreted as brick in the stencil */
	private Image tiles[];
	/** sprite objects of all sprite objects available in this style */
	private SpriteObject sprObjAvailable[];
	/** terrain the Lemmings walk on etc. - originally 400 tiles, 4 bytes each */
	private ArrayList<Terrain> terrain;
	/** steel areas which are indestructible - originally 32 objects, 4 bytes each */
	private ArrayList<Steel> steel; //
	/** level name - originally 32 bytes ASCII - filled with whitespaces */
	private String lvlName;
	/** used to read in the configuration file */
	private Props props;

	/**
	 * Load a level and all level resources.
	 * @param fname file name
	 * @throws ResourceException
	 * @throws LemmException
	 */
	void loadLevel(final String fname) throws ResourceException, LemmException {
		ready = false;
		// read level properties from file
		Props p = new Props();
		if (!p.load(fname))
			throw new ResourceException(fname);

		// read name
		lvlName = p.get("name","");
		//out(fname + " - " + lvlName);
		maxFallDistance = p.get("maxFallDistance", GameController.getCurLevelPack().getMaxFallDistance());
		// read configuration in big endian word
		releaseRate = p.get("releaseRate",-1);
		//out("releaseRate = " + releaseRate);
		numLemmings = p.get("numLemmings",-1);
		//out("numLemmings = " + numLemmings);
		numToRescue = p.get("numToRescue",-1);
		//out("numToRescue = " + numToRescue);
		timeLimitSeconds = p.get("timeLimitSeconds",-1);
		if (timeLimitSeconds == -1) {
			int timeLimit = p.get("timeLimit",-1);
			timeLimitSeconds = timeLimit*60;
		}

		//out("timeLimit = " + timeLimit);
		numClimbers = p.get("numClimbers",-1);
		//out("numClimbers = " + numClimbers);
		numFloaters = p.get("numFloaters",-1);
		//out("numFloaters = " + numFloaters);
		numBombers = p.get("numBombers",-1);
		//out("numBombers = " + numBombers);
		numBlockers = p.get("numBlockers",-1);
		//out("numBlockers = " + numBlockers);
		numBuilders = p.get("numBuilders",-1);
		//out("numBuilders = " + numBuilders);
		numBashers = p.get("numBashers",-1);
		//out("numBashers = " + numBashers);
		numMiners = p.get("numMiners",-1);
		//out("numMiners = " + numMiners);
		numDiggers = p.get("numDiggers",-1);
		//out("numDiggers = " + numDiggers);
		xPos = p.get("xPos",-1);
		//out("xPos = " + xPos);
		String strStyle = p.get("style","");
		int style;
		style = -1;
		for (int i=0; i<STYLES.length; i++)
			if (strStyle.equalsIgnoreCase(STYLES[i])) {
				style = i;
				break;
			}
		//out("style = " + styles[style]);
		superlemming = p.get("superlemming", false);

		// read objects
		//out("\n[Objects]");
		objects = new ArrayList<LvlObject>();
		int def[] = {-1};
		for (int i = 0; true /*i < 32*/; i++) {
			int[] val = p.get("object_"+i,def);
			if (val.length == 5) {
				LvlObject obj = new LvlObject(val);
				objects.add(obj);
				//out("" + obj.id + ", " + obj.xPos + ", " + obj.yPos + ", "+ obj.paintMode + ", " + obj.upsideDown);
			} else break;
		}
		// read terrain
		//out("\n[Terrain]");
		terrain = new ArrayList<Terrain>();
		for (int i = 0; true /*i < 400*/; i++) {
			int[] val = p.get("terrain_"+i,def);
			if (val.length == 4) {
				Terrain ter = new Terrain(val);
				terrain.add(ter);
				//out("" + ter.id + ", " + ter.xPos + ", " + ter.yPos + ", " + ter.modifier);
			} else break;
		}
		// read steel blocks
		//out("\n[Steel]");
		steel = new ArrayList<Steel>();
		for (int i = 0; true/*i < 32*/; i++) {
			int[] val = p.get("steel_"+i,def);
			if (val.length == 4) {
				Steel stl = new Steel(val);
				steel.add(stl);
				//out("" + stl.xPos + ", " + stl.yPos + ", " + stl.width + ", " + stl.height);
			} else break;
		}
		// load objects
		sprObjAvailable = null;
		// first load the data from object descriptor file xxx.ini
		String fnames = Core.findResource("styles/"+strStyle + "/" + strStyle + ".ini");
		props = new Props();
		if (!props.load(fnames)) {
			if (style != -1)
				throw new ResourceException(fnames);
			else
				throw new LemmException("Style "+strStyle+ " not existing.");
		}
		// load blockset
		tiles = loadTileSet(strStyle, Core.getCmp());
		sprObjAvailable = loadObjects(strStyle, Core.getCmp());
		ready = true;
	}

	/**
	 * Paint a level.
	 * @param bgImage background image to draw into
	 * @param cmp parent component
	 * @param s stencil to reuse
	 * @return stencil of this level
	 */
	Stencil paintLevel(final BufferedImage bgImage, final Component cmp, final Stencil s) {
		// flush all resources
		sprObjFront = null;
		sprObjBehind = null;
		sprObjects = null;
		entries = null;
		System.gc();
		// the screenBuffer should be big enough to hold the level
		// returns stencil buffer;
		int bgWidth = bgImage.getWidth();
		int bgHeight = bgImage.getHeight();
		// try to reuse old stencil
		Stencil stencil;
		if (s!= null && s.getWidth() == bgWidth && s.getHeight() == bgImage.getHeight()) {
			s.clear();
			stencil = s;
		} else
			stencil = new Stencil(bgWidth,bgImage.getHeight());
		// paint terrain
		for (int n = 0; n < terrain.size(); n++) {
			Terrain t = terrain.get(n);
			Image i = tiles[t.id];
			int width = i.getWidth(null);
			int height = i.getHeight(null);

			int source[] = new int[width * height];
			PixelGrabber pixelgrabber = new PixelGrabber(i, 0, 0, width,
					height, source, 0, width);
			try {
				pixelgrabber.grabPixels();
			} catch (InterruptedException interruptedexception) {}
			int tx = t.xPos;
			int ty = t.yPos;
			boolean upsideDown = (t.modifier & Terrain.MODE_UPSIDE_DOWN) != 0;
			boolean overwrite = (t.modifier & Terrain.MODE_NO_OVERWRITE) == 0;
			boolean remove = (t.modifier & Terrain.MODE_REMOVE) != 0;
			try {
				for (int y = 0; y < height; y++) {
					if (y + ty < 0 || y + ty >= bgHeight)
						continue;
					int yLineStencil = (y + ty) * bgWidth;
					int yLine;
					if (upsideDown)
						yLine = (height - y - 1) * width;
					else
						yLine = y * width;
					for (int x = 0; x < width; x++) {
						if (x + tx < 0 || x + tx >= bgWidth)
							continue;
						int col = source[yLine + x];
						// ignore transparent pixels
						if ((col & 0xff000000) == 0)
							continue;
						boolean paint = false;
						if (!overwrite) {
							// don't overwrite -> only paint if background is transparent
							if (stencil.get(yLineStencil + tx + x) == Stencil.MSK_EMPTY)
								paint = true;
						} else if (remove) {
							bgImage.setRGB(x + tx, y + ty, 0 /*bgCol*/);
							stencil.set(yLineStencil + tx + x, Stencil.MSK_EMPTY);
						} else
							paint = true;
						if (paint) {
							bgImage.setRGB(x + tx, y + ty, col);
							stencil.set(yLineStencil + tx + x,Stencil.MSK_BRICK);
						}
					}
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
			}
		}

		// now for the animated objects
		ArrayList<SpriteObject> oCombined = new ArrayList<SpriteObject>(64);
		ArrayList<SpriteObject> oBehind = new ArrayList<SpriteObject>(64);
		ArrayList<SpriteObject> oFront = new ArrayList<SpriteObject>(4);
		ArrayList<Entry> entry = new ArrayList<Entry>(4);
		AffineTransform tx;
		for (int n = 0; n < objects.size(); n++) {
			try {
				LvlObject o = objects.get(n);
				//if (sprObjAvailable[o.id].animMode != Sprite.ANIM_NONE) {
				SpriteObject spr = new SpriteObject(sprObjAvailable[o.id]);
				spr.setX(o.xPos);
				spr.setY(o.yPos);
				// affine transform for flipping
				tx = AffineTransform.getScaleInstance(1, -1);
				tx.translate(0, -spr.getHeight());
				AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
				BufferedImage imgSpr;
				// check for entries (ignore upside down entries)
				if (spr.getType() == SpriteObject.Type.ENTRY && !o.upsideDown) {
					Entry e = new Entry(o.xPos+spr.getWidth()/2, o.yPos);
					e.id = oCombined.size();
					entry.add(e);
					spr.setAnimMode(Sprite.Animation.NONE);
				}
				// animated
				boolean drawOnVis = o.paintMode == LvlObject.MODE_VIS_ON_TERRAIN;
				boolean noOverwrite = o.paintMode == LvlObject.MODE_NO_OVERWRITE;
				boolean inFront = (drawOnVis || !noOverwrite);
				boolean drawFull = o.paintMode == LvlObject.MODE_FULL;

				if (inFront)
					oFront.add(spr);
				else
					oBehind.add(spr);
				oCombined.add(spr);

				// draw stencil (only for objects that are not upside down)
				if (!o.upsideDown) {
					for (int y = 0; y < spr.getHeight(); y++) {
						if (y + spr.getY() < 0 || y + spr.getY() >= bgHeight)
							continue;
						int yLineStencil = (y + spr.getY()) * bgWidth;
						for (int x = 0; x < spr.getWidth(); x++) {
							//boolean pixOverdraw = false;
							if (x + spr.getX() < 0 || x + spr.getX() >= bgWidth)
								continue;
							// manage collision mask
							// now read stencil
							int stencilVal;
							stencilVal = stencil.get(yLineStencil + spr.getX()+ x);
							// store object type in mask and idx in higher byte
							if (/*paint &&*/ spr.getType() != SpriteObject.Type.ENTRY && spr.getType() != SpriteObject.Type.PASSIVE)
								if ((spr.getMask(x,y) & 0xff000000) != 0) { // not transparent
									// avoid two objects on the same stencil position
									// overlap makes it impossible to delete pixels in objects (mask operations)
									if (Stencil.getObjectID(stencilVal) == 0) {
										stencil.or(yLineStencil + spr.getX() + x, spr.getMaskType() | Stencil.createObjectID(n));
									} // else: overlap - erased later in object instance
								}
						}
					}
				}
				// remove invisible pixels from all object frames that are "in front"
				// for upside down objects, just create the upside down copy
				if (o.upsideDown || inFront) {
					for (int frame = 0; frame < spr.getNumFrames(); frame++) {
						imgSpr = ToolBox.createImage(spr.getWidth(),spr.getHeight(), Transparency.BITMASK);
						// get flipped or normal version
						if (o.upsideDown) {
							// flip the image vertically
							imgSpr = op.filter(spr.getImage(frame), imgSpr);
						} else {
							WritableRaster rImgSpr = imgSpr.getRaster();
							rImgSpr.setRect(spr.getImage(frame).getRaster()); // just copy
						}
						// for "in front" objects the really drawn pixels have to be determined
						if (inFront) {
							for (int y = 0; y < spr.getHeight(); y++) {
								if (y + spr.getY() < 0 || y + spr.getY() >= bgHeight)
									continue;
								int yLineStencil = (y + spr.getY()) * bgWidth;
								for (int x = 0; x < spr.getWidth(); x++) {
									if (x + spr.getX() < 0 || x + spr.getX() >= bgWidth)
										continue;
									// now read stencil
									int stencilVal = stencil.get(yLineStencil + spr.getX()+ x);
									int stencilValMasked = stencilVal & Stencil.MSK_WALK_ON;
									boolean paint =
										drawFull || (stencilValMasked != 0 && drawOnVis) || (stencilValMasked == 0 && noOverwrite);
									// hack for overlap:
									int id = Stencil.getObjectID(stencilVal);
									// check if a different interactive object was already entered at this pixel position
									// however: exits must always be painted
									// also: passive objects will always be painted
									if ( inFront && spr.getType() != SpriteObject.Type.PASSIVE && spr.getType() != SpriteObject.Type.EXIT && id!=0 && id != n)
										paint = false;
									// sprite screenBuffer  pixel
									int imgCol = imgSpr.getRGB(x,y);
									if ((imgCol & 0xff000000) == 0)
										continue;
									if (!paint)
										imgSpr.setRGB(x,y,imgCol&0xffffff); // set transparent
								}
							}
						}
						//spr.img[frame].flush(); // will be overwritten -> flush data
						spr.setImage(frame,imgSpr);
					}
				}
			} catch (ArrayIndexOutOfBoundsException ex) {
				//System.out.println("Array out of bounds");
			}
		}

		entries = new Entry[entry.size()];
		entries = entry.toArray(entries);

		// paint steel tiles into stencil
		for (int n = 0; n < steel.size(); n++) {
			Steel stl = steel.get(n);
			int sx = stl.xPos;
			int sy = stl.yPos;
			for (int y = 0; y < stl.height; y++) {
				if (y + sy < 0 || y + sy >= bgHeight)
					continue;
				int yLineStencil = (y + sy) * bgWidth;
				for (int x = 0; x < stl.width; x++) {
					if (x + sx < 0 || x + sx >= bgWidth)
						continue;
					int stencilVal = stencil.get(yLineStencil + x + sx);
					// only allow steel on brick
					if ((stencilVal & Stencil.MSK_BRICK) != 0) {
						stencilVal &= ~Stencil.MSK_BRICK;
						stencilVal |= Stencil.MSK_STEEL;
						stencil.set(yLineStencil + x + sx, stencilVal);
					}
				}
			}
		}
		// flush tiles
		//		if (tiles != null)
		//			for (int i=0; i < tiles.length; i++)
		//				tiles[i].flush();

		sprObjects    = new SpriteObject[oCombined.size()];
		sprObjects = oCombined.toArray(sprObjects);
		sprObjFront = new SpriteObject[oFront.size()];
		sprObjFront = oFront.toArray(sprObjFront);
		sprObjBehind = new SpriteObject[oBehind.size()];
		sprObjBehind = oBehind.toArray(sprObjBehind);
		System.gc();

		return stencil;
	}

	/**
	 * Draw opaque objects behind background image.
	 * @param g graphics object to draw on
	 * @param width width of screen
	 * @param xOfs level offset position
	 */
	public void drawBehindObjects(final Graphics2D g, final int width, final int xOfs) {
		// draw "behind" objects
		if (sprObjBehind != null) {
			for (int n=0; n<sprObjBehind.length; n++) {
				try {
					SpriteObject spr = sprObjBehind[n];
					BufferedImage img = spr.getImage();
					if (spr.getX()+spr.getWidth() > xOfs && spr.getX() < xOfs+width)
						g.drawImage(img,spr.getX()-xOfs,spr.getY(),null);
					//spr.drawHidden(offImg,xOfsTemp);
				} catch (ArrayIndexOutOfBoundsException ex) {}
			}
		}
	}

	/**
	 * Draw transparent objects in front of background image.
	 * @param g graphics object to draw on
	 * @param width width of screen
	 * @param xOfs level offset position
	 */
	public void drawInFrontObjects(final Graphics2D g, final int width, final int xOfs) {
		// draw "in front" objects
		if (sprObjFront != null) {
			for (int n=0; n<sprObjFront.length; n++) {
				try {
					SpriteObject spr = sprObjFront[n];
					BufferedImage img = spr.getImage();
					if (spr.getX()+spr.getWidth() > xOfs && spr.getX() < xOfs+width)
						g.drawImage(img,spr.getX()-xOfs,spr.getY(),null);
				} catch (ArrayIndexOutOfBoundsException ex) {}
			}
		}
	}

	//	/**
	//	 * Debug output.
	//	 * @param o string to print
	//	 */
	//	private static void out(final String o) {
	//		System.out.println(o);
	//	}

	/**
	 * Load tile set from a styles folder.
	 * @param set name of the style
	 * @param cmp parent component
	 * @return array of images where each image contains one tile
	 * @throws ResourceException
	 */
	private Image[] loadTileSet(final String set, final Component cmp) throws ResourceException {
		ArrayList<Image> images = new ArrayList<Image>(64);
		MediaTracker tracker = new MediaTracker(cmp);
		int tiles = props.get("tiles", 64);
		for (int n = 0; n < tiles; n++) {
			String fName = "styles/" + set + "/" + set + "_" + Integer.toString(n) + ".gif";
			Image img = Core.loadImage(tracker, fName);
			images.add(img);
		}
		try {
			tracker.waitForAll();
		} catch (InterruptedException ex) {}
		Image ret[] = new Image[images.size()];
		ret = images.toArray(ret);
		images = null;
		return ret;
	}


	/**
	 * Load level sprite objects.
	 * @param set name of the style
	 * @param cmp parent component
	 * @return array of images where each image contains one tile
	 * @throws ResourceException
	 */
	private SpriteObject[] loadObjects(final String set, final Component cmp) throws ResourceException {
		//URLClassLoader urlLoader = (URLClassLoader) this.getClass().getClassLoader();
		MediaTracker tracker = new MediaTracker(cmp);
		// first some global settings
		bgCol = props.get("bgColor",0x000000) | 0xff000000;
		bgColor = new Color(bgCol);
		debrisCol = props.get("debrisColor",0xffffff) | 0xff000000;
		// replace pink color with debris color
		Lemming.patchColors(TEMPLATE_COLOR,debrisCol);
		particleCol = props.get("particleColor", DEFAULT_PARTICLE_COLORS);
		for (int i=0; i<particleCol.length; i++)
			particleCol[i] |= 0xff000000;
		// go through all the entries (shouldn't be more than 64)
		ArrayList<SpriteObject> sprites = new ArrayList<SpriteObject>(64);
		int idx;
		for (idx = 0; true; idx++) {
			// get number of animations
			String sIdx = Integer.toString(idx);
			int frames = props.get("frames_" + sIdx, -1);
			if (frames < 0)
				break;
			// load screenBuffer
			String fName = "styles/"+set + "/" + set + "o_" + Integer.toString(idx)+ ".gif";
			Image img = Core.loadImage(tracker, fName);
			try {
				tracker.waitForAll();
			} catch (InterruptedException ex) {}
			// get animation mode
			int anim = props.get("anim_" + sIdx, -1);
			if (anim < 0)
				break;
			SpriteObject sprite = new SpriteObject(img, frames);
			switch (anim) {
				case 0: // dont' animate
					sprite.setAnimMode(Sprite.Animation.NONE);
					break;
				case 1: // loop mode
					sprite.setAnimMode(Sprite.Animation.LOOP);
					break;
				case 2: // triggered animation - for the moment handle like loop
					sprite.setAnimMode(Sprite.Animation.TRIGGERED);
					break;
				case 3: // entry animation
					sprite.setAnimMode(Sprite.Animation.ONCE);
					break;
			}
			// get object type
			int type = props.get("type_" + sIdx, -1);
			if (type < 0)
				break;
			sprite.setType(SpriteObject.getType(type));
			switch (sprite.getType()) {
				case EXIT:
				case NO_DIG_LEFT:
				case NO_DIG_RIGHT:
				case TRAP_DIE:
				case TRAP_REPLACE:
				case TRAP_DROWN:
					// load mask
					fName = "styles/"+set + "/" + set + "om_" + Integer.toString(idx)+ ".gif";
					img = Core.loadImage(tracker, fName);
					sprite.setMask(img);
					break;
			}
			// get sound
			int sound = props.get("sound_" + sIdx, -1);
			sprite.setSound(sound);

			sprites.add(sprite);
		}
		SpriteObject ret[] = new SpriteObject[sprites.size()];
		ret = sprites.toArray(ret);
		sprites = null;
		return ret;
	}

	/**
	 * Create a mini map for this level.
	 * @param image image to re-use (if null or wrong size, it will be recreated)
	 * @param bgImage background image used as source for the mini map
	 * @param scaleX integer X scaling factor (2 -> half width)
	 * @param scaleY integer Y scaling factor (2 -> half height)
	 * @param tint apply a greenish color tint
	 * @return image with mini map
	 */
	public BufferedImage createMiniMap(final BufferedImage image, final BufferedImage bgImage, final int scaleX, final int scaleY, final boolean tint) {
		Level level = GameController.getLevel();
		int bgCol;
		int width = bgImage.getWidth()/scaleX;
		int height = bgImage.getHeight()/scaleY;
		BufferedImage img;

		if (image == null || image.getWidth() != width || image.getHeight() != height)
			img = ToolBox.createImage(width,height,Transparency.OPAQUE);
		else
			img = image;
		Graphics2D gx = img.createGraphics();
		// clear background
		gx.setBackground(bgColor);
		gx.clearRect(0, 0, width, height);
		// read back background color to avoid problems with 16bit mode
		// (bgColor written can be slightly different from the one read)
		bgCol = img.getRGB(0,0);
		// draw "behind" objects
		if (level != null && level.sprObjBehind != null) {
			for (int n=0; n<level.sprObjBehind.length; n++) {
				try {
					SpriteObject spr = level.sprObjBehind[n];
					BufferedImage sprImg = spr.getImage();
					gx.drawImage(sprImg,spr.getX()/scaleX,spr.getY()/scaleY,
							spr.getWidth()/scaleX, spr.getHeight()/scaleY, null);
				} catch (ArrayIndexOutOfBoundsException ex) {}
			}
		}
		gx.drawImage(bgImage,0,0,width,height,0,0,bgImage.getWidth(),bgImage.getHeight(),null);
		// draw "in front" objects
		if (level != null && level.sprObjFront != null) {
			for (int n=0; n<level.sprObjFront.length; n++) {
				try {
					SpriteObject spr = level.sprObjFront[n];
					BufferedImage sprImg = spr.getImage();
					gx.drawImage(sprImg,spr.getX()/scaleX,spr.getY()/scaleY,
							spr.getWidth()/scaleX, spr.getHeight()/scaleY, null);
				} catch (ArrayIndexOutOfBoundsException ex) {}
			}
		}
		gx.dispose();
		// now tint in green
		if (tint) {
			for (int y=0; y<img.getHeight(); y++)
				for (int x=0; x<img.getWidth(); x++) {
					int c = img.getRGB(x,y);
					if (c == bgCol)
						c = 0xff000000; // make backgroud black instead of dark green (easier mask operations)
					else {
						int sum = 0;
						for (int i =0; i<3; i++,c>>=8)
							sum += (c&0xff);
						sum /= 3; // mean value
						if (sum != 0)
							sum += 0x60;
						//sum *= 3; // make lighter
						if (sum > 0xff)
							sum = 0xff;
						c = 0xff000000 + /*((sum<<16)&0xff0000)*/ + ((sum<<8)&0xff00) /*+ sum*/;
					}
					img.setRGB(x,y,c);
				}
		}
		return img;
	}

	/**
	 * Get level sprite object via index.
	 * @param idx index
	 * @return level sprite object
	 */
	public SpriteObject getSprObject(final int idx) {
		return sprObjects[idx];
	}

	/**
	 * Get number of level sprite objects.
	 * @return number of level sprite objects
	 */
	public int getSprObjectNum() {
		if (sprObjects == null)
			return 0;
		return sprObjects.length;
	}

	/**
	 * Get level Entry via idx.
	 * @param idx index
	 * @return level Entry
	 */
	public Entry getEntry(final int idx) {
		return entries[idx];
	}

	/**
	 * Get number of entries for this level.
	 * @return number of entries.
	 */
	public int getEntryNum() {
		if (entries == null)
			return 0;
		return entries.length;
	}

	/**
	 * Get background color.
	 * @return background color.
	 */
	public Color getBgColor() {
		return bgColor;
	}

	/**
	 * Get ready state of level.
	 * @return true if level is completely loaded.
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * Get maximum safe fall distance.
	 * @return maximum safe fall distance
	 */
	public int getMaxFallDistance() {
		return maxFallDistance;
	}

	/**
	 * Get array of ARGB colors used for particle effects.
	 * @return array of ARGB colors used for particle effects
	 */
	public int[] getParticleCol() {
		return particleCol;
	}

	/**
	 * Get start screen x position : 0 - 0x04f0 (1264) rounded to modulo 8,
	 * @return start screen x position
	 */
	public int getXpos() {
		return xPos;
	}

	/**
	 * Get number of climbers in this level : max 0xfa (250).
	 * @return number of climbers in this level
	 */
	public int getNumClimbers () {
		return numClimbers;
	}

	/**
	 * Get number of floaters in this level : max 0xfa (250).
	 * @return number of floaters in this level
	 */
	public int getNumFloaters () {
		return numFloaters;
	}

	/**
	 * Get number of bombers in this level : max 0xfa (250).
	 * @return number of bombers in this level
	 */
	public int getNumBombers () {
		return numBombers;
	}

	/**
	 * Get number of blockers in this level : max 0xfa (250).
	 * @return number of blockers in this level
	 */
	public int getNumBlockers () {
		return numBlockers;
	}

	/**
	 * Get number of builders in this level : max 0xfa (250).
	 * @return number of builders in this level
	 */
	public int getNumBuilders () {
		return numBuilders;
	}

	/**
	 * Get number of bashers in this level : max 0xfa (250).
	 * @return number of bashers in this level
	 */
	public int getNumBashers () {
		return numBashers;
	}

	/**
	 * Get number of miners in this level : max 0xfa (250).
	 * @return number of miners in this level
	 */
	public int getNumMiners () {
		return numMiners;
	}


	/**
	 * Get number of diggers in this level : max 0xfa (250).
	 * @return number of diggers in this level
	 */
	public int getMumDiggers () {
		return numDiggers;
	}

	/**
	 * Get time limit in seconds
	 * @return time limit in seconds
	 */
	public int getTimeLimitSeconds() {
		return timeLimitSeconds;
	}

	/**
	 * Get number of Lemmings to rescue : should be less than or equal to number of Lemmings.
	 * @return number of Lemmings to rescue
	 */
	public int getNumToRescue() {
		return numToRescue;
	}

	/**
	 * Get number of Lemmings in this level (maximum 0x0072 = 114 in original LVL format).
	 * @return number of Lemmings in this level
	 */
	public int getNumLemmings() {
		return numLemmings;
	}

	/**
	 * Get color of debris pixels (to be replaced with level color).
	 * @return color of debris pixels as ARGB
	 */
	public int getDebrisColor() {
		return debrisCol;
	}

	/**
	 * Get release rate : 0 is slowest, 0x0FA (250) is fastest
	 * @return release rate : 0 is slowest, 0x0FA (250) is fastest
	 */
	public int getReleaseRate() {
		return releaseRate;
	}

	/**
	 * Check if this is a SuperLemming level (runs faster).
	 * @return true if this is a SuperLemming level, false otherwise
	 */
	public boolean isSuperLemming() {
		return superlemming;
	}

	/**
	 * Get level name.
	 * @return level name
	 */
	public String getLevelName() {
		return lvlName;
	}
}

/**
 * Storage class for a level object.
 * @author Volker Oth
 */
class LvlObject {
	/** paint mode: only visible on a terrain pixel */
	static final int MODE_VIS_ON_TERRAIN = 8;
	/** paint mode: don't overwrite terrain pixel in the original background image */
	static final int MODE_NO_OVERWRITE = 4;
	/** paint mode: don't overwrite terrain pixel in the current (!) background image.
		special NO_OVERWRITE case for objects hidden behind terrain. */
	static final int MODE_HIDDEN = 5;
	/** paint mode: paint without any further checks */
	static final int MODE_FULL = 0;

	/** identifier */
	int id;
	/** x position in pixels */
	int xPos;
	/** y position in pixels */
	int yPos;
	/** paint mode - must be one of the MODEs above */
	int paintMode;
	/** flag: paint the object upside down */
	boolean upsideDown;

	/**
	 * Constructor
	 * @param val three values as array [identifier, x position, y position]
	 */
	public LvlObject(final int[] val) {
		id   = val[0];
		xPos = val[1];
		yPos = val[2];
		paintMode = val[3];
		upsideDown = val[4] != 0;
	}
}

/**
 * Storage class for a terrain/background tiles.
 * @author Volker Oth
 */
class Terrain {
	/** paint mode: don't overwrite existing terrain pixel */
	static final int MODE_NO_OVERWRITE = 8;
	/** paint mode: upside down */
	static final int MODE_UPSIDE_DOWN = 4;
	/** paint mode: remove existing terrain pixels instead of overdrawing them */
	static final int MODE_REMOVE = 2;

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
	 * @param val three values as array [identifier, x position, y position]
	 */
	public Terrain(final int[] val) {
		id   = val[0];
		xPos = val[1];
		yPos = val[2];
		modifier = val[3];
	}
}

/**
 * Storage class for steel tiles.
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
	 * @param val four values as array [x position, y position, width, height]
	 */
	public Steel(final int[] val) {
		xPos = val[0];
		yPos = val[1];
		width = val[2];
		height = val[3];
	}
}

/**
 * Storage class for level Entries.
 * @author Volker Oth
 */
class Entry {
	/** identifier */
	int id;
	/** x position in pixels */
	int xPos;
	/** y position in pixels */
	int yPos;


	/**
	 * Constructor.
	 * @param x x position in pixels
	 * @param y y position in pixels
	 */
	Entry(final int x, final int y) {
		xPos = x;
		yPos = y;
	}

}
