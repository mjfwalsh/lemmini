package GameUtil;

import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

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
 * Simple sprite class.
 *
 * @author Volker Oth
 */
public class Sprite {

	/**
	 * Animation style.
	 */
	public static enum Animation {
		/** not animated */
		NONE,
		/** endless animation loop */
		LOOP,
		/** triggered animation */
		TRIGGERED,
		/** one animation cycle */
		ONCE
	};

	/** sprite width in pixels */
	protected int width;
	/** sprite height in pixels */
	protected int height;
	/** number of animation frames */
	private int numframes;
	/** index of current animation frame */
	private int frameIdx;
	/** animation mode */
	private Animation animMode;
	/** index for a sound */
	private int sound;
	/** boolean flag: animation was triggered */
	private boolean triggered;
	/** array of animation frames */
	private BufferedImage frames[];

	/**
	 * Constructor.
	 * @param sourceImg Image containing animation frames one above each other.
	 * @param animFrames number of frames.
	 */
	public Sprite(final Image sourceImg, final int animFrames) {
		init(sourceImg, animFrames);
	}

	/**
	 * Constructor. Create Sprite from other Sprite.
	 * @param src Sprite to clone.
	 */
	public Sprite(final Sprite src) {
		copyFrom(src);
	}

	/**
	 * Copy all class attributes from another Sprite to this one.
	 * @param src Sprite to copy from.
	 */
	private void copyFrom(final Sprite src) {
		numframes = src.numframes;
		width = src.width;
		height = src.height;
		frameIdx = src.frameIdx;
		animMode = src.animMode;
		sound = src.sound;
		triggered = false;
		frames = src.frames.clone();
	}

	/**
	 * Initialized sprite with new animation.
	 * @param sourceImg Image containing animation frames one above each other.
	 * @param animFrames number of frames.
	 */
	private void init(final Image sourceImg, final int animFrames) {
		numframes = animFrames;
		width = sourceImg.getWidth(null);
		height = sourceImg.getHeight(null)/numframes;
		frameIdx = 0;
		animMode = Animation.NONE;
		triggered = false;
		// animation frames stored one above the other - now separate them into single images
		frames = ToolBox.getAnimation(sourceImg, animFrames, Transparency.BITMASK);
	}

	/**
	 * Get given animation frame.
	 * @param idx index of animation frame
	 * @return animation frame at position idx.
	 */
	public BufferedImage getImage(final int idx) {
		return frames[idx];
	}

	/**
	 * Get current animation frame.
	 * @return current animation frame.
	 */
	public BufferedImage getImage() {
		return frames[frameIdx];
	}

	/**
	 * Replace one animation frame.
	 * Note: replacing with an image of different size will create problems.
	 * @param idx index of frame to replace
	 * @param img image to use for this animation frame
	 */
	public void setImage(final int idx, final BufferedImage img) {
		frames[idx] = img;
	}

	/**
	 * Get current animation frame and animate.
	 * @return current animation frame (before increasing the animation step).
	 */
	public BufferedImage getImageAnim() {
		BufferedImage i = frames[frameIdx];
		switch (animMode) {
			case LOOP:
				if(++frameIdx >= numframes)
					frameIdx = 0;
				break;
			case ONCE:
				if (frameIdx < numframes-1)
					frameIdx++;
				break;
			case TRIGGERED:
				if (triggered) {
					if (frameIdx < numframes-1)
						frameIdx++;
					else {
						triggered = false;
						frameIdx = 0;
					}
				} else frameIdx = 0;
				break;
		}
		return i;
	}

	/**
	 * Set Pixel in all animation frames.
	 * @param x x position
	 * @param y y position
	 * @param color color as TYPE_INT_ARGB
	 */
	public void setPixel(final int x, final int y, final int color) {
		if (x>=0 && x<width && y>=0 && y<height)
			for (int i=0; i<numframes; i++)
				frames[i].setRGB(x,y,color);
	}

	/**
	 * Get animation mode.
	 * @return animation mode.
	 */
	public Animation getAnimMode() {
		return animMode;
	}

	/**
	 * Set animation mode.
	 * @param mode animation mode
	 */
	public void setAnimMode(final Animation mode) {
		animMode = mode;
	}

	/**
	 * Check if the animation is a triggered type.
	 * @return true if the animation is Animation.TRIGGERED.
	 */
	public boolean canBeTriggered() {
		return (animMode == Animation.TRIGGERED);
	}

	/**
	 * Trigger a triggered animation
	 * @return true if not yet triggered, false if already triggered
	 */
	public boolean trigger() {
		if (triggered == true)
			return false;
		triggered = true;
		frameIdx = 1;
		return true;
	}

	/**
	 * Get current animation frame index.
	 * @return current animation frame index
	 */
	public int getFrameIdx() {
		return frameIdx;
	}

	/** Set current animation frame index.
	 * @param frameIdx current animation frame index
	 */
	public void setFrameIdx(final int frameIdx) {
		this.frameIdx = frameIdx;
	}

	/**
	 * Get number of animation frames.
	 * @return number of animation frames
	 */
	public int getNumFrames() {
		return numframes;
	}

	/**
	 * Get sound index.
	 * @return sound index
	 */
	public int getSound() {
		return sound;
	}

	/**
	 * Set sound index.
	 * @param s sound index
	 */
	public void setSound(final int s) {
		sound = s;
	}

	/**
	 * Get width of Sprite in pixels.
	 * @return width of Sprite in pixels
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Get height of Sprite in pixels.
	 * @return height of Sprite in pixels
	 */
	public int getHeight() {
		return height;
	}

}
