package Game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import micromod.Micromod;

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
 * Class to play MOD music.
 *
 * @author Volker Oth
 */
public class ModMusic implements Runnable {
  /** sample frequency */
  private static final int SAMPLE_RATE = 44100;

  /** object to play MODs */
  private Micromod micromod;

  /** flag: loop the song */
  private boolean songloop;

  /** flag: currently playing */
  private boolean play;

  /** thread for playing */
  private Thread mmThread;

  /** data line used to play samples */
  private SourceDataLine line;

  /**
   * Load MOD file, initialize player.
   *
   * @param fn file name
   * @throws ResourceException
   */
  public void load(final String fn) throws ResourceException {
    if (mmThread != null) close();
    String fName = Core.findResource(fn);
    int datalen = (int) (new File(fName).length());
    if (datalen < 0) throw new ResourceException(fName);
    try {
      FileInputStream f = new FileInputStream(fName);
      byte[] songdata = new byte[datalen];
      f.read(songdata);
      f.close();
      micromod = new Micromod(songdata, SAMPLE_RATE);
      setloop(true);
    } catch (FileNotFoundException ex) {
      throw new ResourceException(fName);
    } catch (IOException ex) {
      throw new ResourceException(fName + " (IO exception)");
    }
    mmThread = new Thread(this);
    mmThread.start();
  }

  /**
   * Set whether the song is to loop continuously or not. The default is to loop.
   *
   * @param loop true: loop, false: playe only once
   */
  public void setloop(final boolean loop) {
    songloop = loop;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   *
   * 	Begin playback.
   *	This method will return once the song has finished, or stop has been called.
   */
  @Override
  public void run() {
    int buflen = 2048;
    int[] lbuf = new int[buflen];
    int[] rbuf = new int[buflen];
    byte[] obuf = new byte[buflen << 2];
    try {
      AudioFormat af = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
      DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, af);
      // SourceDataLine line = (SourceDataLine)AudioSystem.getLine(lineInfo);
      line = (SourceDataLine) GameController.sound.getLine(lineInfo);
      line.open();
      line.start();
      setGain(Music.getGain());
      int songlen = micromod.getlen();
      int remain = songlen;
      while (remain > 0 && Thread.currentThread() == mmThread) {
        if (play) {
          int count = buflen;
          if (count > remain) count = remain;
          micromod.mix(lbuf, rbuf, 0, count);
          for (int ix = 0; ix < count; ix++) {
            int ox = ix << 2;
            obuf[ox] = (byte) (lbuf[ix] & 0xFF);
            obuf[ox + 1] = (byte) (lbuf[ix] >> 8);
            obuf[ox + 2] = (byte) (rbuf[ix] & 0xFF);
            obuf[ox + 3] = (byte) (rbuf[ix] >> 8);
            lbuf[ix] = rbuf[ix] = 0;
          }
          line.write(obuf, 0, count << 2);
          remain -= count;
          if (remain == 0 && songloop) remain = songlen;
          Thread.yield();
        } else {
          try {
            line.flush();
            Thread.sleep(40);
          } catch (InterruptedException ex) {
          }
        }
      }
      line.flush();
      line.close();
    } catch (LineUnavailableException e) {
      e.printStackTrace();
      return;
    }
  }

  /** Instruct the run() method to finish playing and return. */
  public void stop() {
    if (mmThread != null) mmThread.interrupt();
    play = false;
  }

  /** Instruct the run() method to resume playing. */
  public void play() {
    if (mmThread != null) mmThread.interrupt();
    play = true;
  }

  /** Kills the thread. */
  public void close() {
    Thread moribund = mmThread;
    mmThread = null;
    try {
      moribund.interrupt();
      moribund.join();
    } catch (InterruptedException ex) {
    }
  }

  /**
   * Set gain (volume) of MOD output
   *
   * @param gn gain factor: 0.0 (off) .. 1.0 (full volume)
   */
  public void setGain(final double gn) {
    double gain;
    if (gn > 1.0) gain = 1.0;
    else if (gn < 0) gain = 0;
    else gain = gn;
    if (line != null) GameController.sound.setLineGain(line, gain);
  }
}
