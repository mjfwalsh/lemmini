package GameUtil;

import Game.Core;
import Game.GameController;
import Game.ResourceException;
import java.io.File;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;

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
 * Used to play a number of sounds. Supports upsampling and one pitched sample.
 *
 * @author Volker Oth
 */
public class Sound {

  /** default sampling frequency */
  private static final float DEFAULT_FREQUENCY = 22050;

  /** number of pitch levels */
  private static final int NUMBER_PITCHED = 100;

  /** fade in the first n samples when calculating the pitched buffers */
  private static final int PITCH_FADE_IN = 20;

  /** maximum number of sounds played in parallel */
  private static final int MAX_SIMUL_SOUNDS = 6;

  /** line listener to be called after sample was played */
  private LineListener defaultListener;

  /** sound buffers to store the samples */
  private byte soundBuffer[][];

  /** pitch buffers to store all pitched samples */
  private byte pitchBuffers[][];

  /** audio formats for normal samples (one for each sample) */
  private AudioFormat format[];

  /** audio format for pitched samples */
  private AudioFormat pitchFormat;

  /** audio format for upsampling */
  private AudioFormat defaultFormat;

  /** line info for each sample */
  private DataLine.Info info[];

  /** line info for the pitched sample */
  private DataLine.Info pitchInfo;

  /** line info for upsampling */
  private DataLine.Info defaultInfo;

  /** gain/volume: 1.0 = 100% */
  private double gain;

  /** number of sounds currently played */
  static int simulSounds;

  /** selected mixer index */
  static int mixerIdx;

  /** array of available mixers */
  static Mixer mixers[];

  /** number of samples to be used */
  static int sampleNum;

  /**
   * Constructor.
   *
   * @param snum number of samples to use
   * @param pitchID ID of the pitched sample (-1 for none)
   * @throws ResourceException
   */
  public Sound(final int snum, final int pitchID) throws ResourceException {
    String fName = "";
    sampleNum = snum;
    soundBuffer = new byte[sampleNum][];
    format = new AudioFormat[sampleNum];
    info = new DataLine.Info[sampleNum];

    gain = 1.0;
    simulSounds = 0;
    defaultListener = new DefaultListener();

    // upsampling to default frequency (more compatible for weird sample frequencies)
    defaultFormat = new AudioFormat(DEFAULT_FREQUENCY, 16, 1, true, false);
    defaultInfo = new DataLine.Info(Clip.class, defaultFormat);

    int maxLen = 0;
    try {
      for (int i = 0; i < sampleNum; i++) {
        fName = "sound/sound_" + Integer.toString(i) + ".wav";
        File fs = Core.findResource(fName);
        AudioInputStream f = AudioSystem.getAudioInputStream(fs.toURI().toURL());
        format[i] = f.getFormat();
        info[i] = new DataLine.Info(Clip.class, format[i]);
        byte soundBuffer8[] = new byte[(int) f.getFrameLength() * format[i].getFrameSize()];
        f.read(soundBuffer8);
        f.close();

        // convert samples with frequencies < 8kHz no work around bug in JDK6
        // convert to 16bit due to bug in MacOS JRE
        if (format[i].getFrameSize() > 2)
          throw new ResourceException("Unsupported sample format for sample " + fName);
        if ((format[i].getFrameSize() == 1)
            && (format[i].getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED))
          throw new ResourceException("Unsupported sample format for sample " + fName);
        if ((format[i].getFrameSize() == 2)
            && (format[i].getEncoding() != AudioFormat.Encoding.PCM_SIGNED))
          throw new ResourceException("Unsupported sample format for sample " + fName);
        soundBuffer[i] = convertToDefault(soundBuffer8, format[i]);
        format[i] = defaultFormat;
        info[i] = defaultInfo;

        if (soundBuffer[i].length > maxLen) maxLen = soundBuffer[i].length;
      }
    } catch (Exception ex) {
      throw new ResourceException(fName);
    }

    if (pitchID >= 0) {
      // create buffers for pitching
      // note that bit size (8) and channels (1) have to be the same for all pitched buffers
      pitchFormat = new AudioFormat(44100, 16, 1, true, false);
      pitchInfo = new DataLine.Info(Clip.class, pitchFormat);
      pitchBuffers = new byte[NUMBER_PITCHED][];
      for (int i = 0; i < NUMBER_PITCHED; i++) pitchBuffers[i] = createPitched(pitchID, i);
    }

    // get all available mixers
    Mixer.Info[] mixInfo = AudioSystem.getMixerInfo();
    ArrayList<Mixer> mix = new ArrayList<Mixer>();
    for (int i = 0; i < mixInfo.length; i++) {
      Mixer mixer = AudioSystem.getMixer(mixInfo[i]);
      Line.Info info = new Line.Info(Clip.class);
      int num = mixer.getMaxLines(info);
      if (num != 0) mix.add(mixer);
    }
    mixers = new Mixer[mix.size()];
    mixers = mix.toArray(mixers);
  }

  /**
   * Get an array of available mixer names.
   *
   * @return array of available mixer names
   */
  public String[] getMixers() {
    if (mixers == null) return null;
    String s[] = new String[mixers.length];
    for (int i = 0; i < mixers.length; i++) s[i] = mixers[i].getMixerInfo().getName();
    return s;
  }

  /**
   * Set mixer to be used for sound output.
   *
   * @param idx index of mixer
   */
  public void setMixer(final int idx) {
    if (idx > mixers.length) mixerIdx = 0;
    else mixerIdx = idx;
  }

  /**
   * Return a data line to play a sample.
   *
   * @param info line info with requirements
   * @return data line to play a sample
   */
  public Line getLine(final DataLine.Info info) {
    try {
      return mixers[mixerIdx].getLine(info);
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * Play a given sound.
   *
   * @param idx index of the sound to be played
   */
  public synchronized void play(final int idx) {
    if (!GameController.isSoundOn() || simulSounds >= MAX_SIMUL_SOUNDS /*|| clips==null*/) return;

    try {
      Clip c = (Clip) mixers[mixerIdx].getLine(info[idx]);
      // Add a listener for line events
      c.addLineListener(defaultListener);
      c.open(format[idx], soundBuffer[idx], 0, soundBuffer[idx].length);
      setLineGain(c, gain);
      c.start();
      simulSounds++;
    } catch (Exception ex) {
    }
  }

  /**
   * Convert sampling rate to default sampling rate.
   *
   * @param buffer byte array containing source sample
   * @param format AudioFormat of source sample (only unsigned 8bit PCM or signed 16bit PCM
   *     supported)
   * @return sample converted to default format (16bit signed PCM 22050Hz) stored in byte array
   */
  public synchronized byte[] convertToDefault(final byte buffer[], final AudioFormat format) {
    // check unsupported formats

    // check if the default format is already OK
    if ((format.getFrameRate() == DEFAULT_FREQUENCY)
        && (format.getFrameSize() == 2)
        && !format.isBigEndian()
        && (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED)) return buffer;

    boolean from8bit = (format.getFrameSize() == 1);
    boolean convertEndian = ((format.getFrameSize() == 2) && format.isBigEndian());

    // sample up low frequency files to a DEFAULT_FREQUENCY to work around sound bug in JDK6
    double scale = DEFAULT_FREQUENCY / format.getSampleRate();
    int sampleNumSrc = buffer.length / (from8bit ? 1 : 2);
    int sampleNumTrg =
        (int) (buffer.length * scale) / (from8bit ? 1 : 2); // length of target buffer in samples
    byte buf[] = new byte[sampleNumTrg * (from8bit ? 2 : 1)];

    // create scaled buffer
    for (int i = 0; i < sampleNumTrg; i++) {
      int pos = (int) (i / scale);
      double ofs = i / scale - pos;
      if (pos >= sampleNumSrc) pos = sampleNumSrc - 1;
      int val, val2;

      if (from8bit) {
        if (ofs < 0.1 || pos == sampleNumSrc - 1) val = buffer[pos] & 0xff;
        else {
          // interpolate between sample points
          val = (int) ((buffer[pos] & 0xff) * (1.0 - ofs) + (buffer[pos + 1] & 0xff) * ofs);
        }
        // byte order is little endian
        val = (val - 0x80) << 8;
      } else {
        if (convertEndian) val = (buffer[2 * pos + 1] & 0xff) | (buffer[2 * pos] << 8);
        else val = (buffer[2 * pos] & 0xff) | (buffer[2 * pos + 1] << 8);
        if (ofs >= 0.1 && pos < sampleNumSrc - 1) {
          // interpolate between sample points
          if (convertEndian) val2 = (buffer[2 * pos + 3] & 0xff) | (buffer[2 * pos + 2] << 8);
          else val2 = (buffer[2 * pos + 2] & 0xff) | (buffer[2 * pos + 3] << 8);
          val = (int) (val * (1.0 - ofs) + val2 * ofs);
        }
      }
      buf[i * 2] = (byte) val;
      buf[i * 2 + 1] = (byte) (val >> 8);
    }
    return buf;
  }

  /**
   * Create a pitched version of a sample.
   *
   * @param idx index of the sample to be pitched
   * @param pitch pitch value as percent (0..100)
   * @return pitched sample as array of byte
   */
  public synchronized byte[] createPitched(final int idx, final int pitch) {
    // the idea is to sample up to 44KHz
    // then increase the sample rate virtually by creating a buffer which contains only
    // every Nth sample

    if (format[idx].getFrameSize() != 2) // only 16bit supported
    return null;
    double scale = pitchFormat.getSampleRate() / format[idx].getSampleRate();
    double dpitch = (1.0 + ((pitch - 1) * 0.0204));
    if (dpitch < 1.0) dpitch = 1.0;
    double fact = dpitch / scale;
    int len = (int) (soundBuffer[idx].length / (2 * fact)); // length of target buffer in samples
    byte buf[] = new byte[len * 2];
    // create scaled buffer
    for (int i = 0; i < len; i++) {
      int pos = (int) (i * fact + 0.5) * 2;
      if (pos >= soundBuffer[idx].length - 1) pos = soundBuffer[idx].length - 2;

      double val = (soundBuffer[idx][pos] & 0xff) | (soundBuffer[idx][pos + 1] << 8);
      // fade in
      if (i < PITCH_FADE_IN) val *= 1.0 - (PITCH_FADE_IN - 1 - i) / 10.0;
      // byte order is little endian
      int ival = (int) val;
      buf[i * 2] = (byte) ival;
      buf[i * 2 + 1] = (byte) (ival >> 8);
    }
    return buf;
  }

  /**
   * Play the pitched sample.
   *
   * @param pitch pitch value 0..99
   */
  public synchronized void playPitched(final int pitch) {
    if (!GameController.isSoundOn() || simulSounds >= MAX_SIMUL_SOUNDS) return;

    try {
      Clip c = (Clip) mixers[mixerIdx].getLine(pitchInfo);
      // Add a listener for line events
      c.addLineListener(defaultListener);
      c.open(pitchFormat, pitchBuffers[pitch], 0, pitchBuffers[pitch].length);
      setLineGain(c, gain);
      c.start();
      simulSounds++;
    } catch (Exception ex) {
    }
  }

  /**
   * Set gain of a line.
   *
   * @param line line
   * @param gn gain (1.0 = 100%)
   */
  public void setLineGain(final Line line, double gn) {
    if (line != null) {
      try {
        double g;
        FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        double maxGain = Math.pow(10, control.getMaximum() / 20);
        if (gn == 0) g = 0.001;
        else g = gn;
        float fgain = 20 * (float) Math.log10(g * maxGain);
        control.setValue(fgain);
      } catch (IllegalArgumentException ex) {
      }
    }
  }

  /**
   * Get gain.
   *
   * @return gain (1.0 == 100%)
   */
  public double getGain() {
    return gain;
  }

  /**
   * Set gain.
   *
   * @param gn gain (1.0 == 100%)
   */
  public void setGain(final double gn) {
    if (gn > 1.0) gain = 1.0;
    else if (gn < 0) gain = 0;
    else gain = gn;
    Core.programProps.set("soundGain", gain);
  }
}

/**
 * Default Line Listener. Called after sample was played.
 *
 * @author Volker Oth
 */
class DefaultListener implements LineListener {
  /* (non-Javadoc)
   * @see javax.sound.sampled.LineListener#update(javax.sound.sampled.LineEvent)
   */
  @Override
  public synchronized void update(final LineEvent event) {
    if (event.getType().equals(LineEvent.Type.STOP)) {
      Clip c = (Clip) event.getLine();
      if (c.isOpen()) {
        c.close();
        if (--Sound.simulSounds < 0) Sound.simulSounds = 0;
      }
    }
  }
}
