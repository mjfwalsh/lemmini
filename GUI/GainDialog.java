package GUI;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import Game.GameController;
import Game.Music;

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
 * Dialog for volume/gain control.
 * @author Volker Oth
 */
public class GainDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JSlider jSliderMusic = null;

	private JLabel jLabelMusicGain = null;

	private JLabel jLabelSoundGain = null;

	private JSlider jSliderSound = null;

	private JButton jButtonOK = null;

	private JButton jButtonCancel = null;


	/**
	 * Constructor for modal dialog in parent frame.
	 * @param frame parent frame
	 * @param modal create modal dialog?
	 */
	public GainDialog(final JFrame frame, final boolean modal) {
		super(frame, modal);
		initialize();

		//
		Point p = frame.getLocation();
		this.setLocation(p.x+frame.getWidth()/2-getWidth()/2, p.y+frame.getHeight()/2-getHeight()/2);
		jSliderSound.setValue((int)(100*GameController.sound.getGain()));
		jSliderMusic.setValue((int)(100*Music.getGain()));
	}

	/**
	 * Automatically generated init.
	 */
	private void initialize() {
		this.setSize(300, 200);
		this.setTitle("Volume Controls");
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jLabelSoundGain = new JLabel();
			jLabelSoundGain.setBounds(new Rectangle(15, 75, 101, 14));
			jLabelSoundGain.setText("Sound Volume");
			jLabelMusicGain = new JLabel();
			jLabelMusicGain.setText("Music Volume");
			jLabelMusicGain.setBounds(new Rectangle(15, 15, 106, 14));
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			jContentPane.add(getJSliderMusic(), null);
			jContentPane.add(jLabelMusicGain, null);
			jContentPane.add(jLabelSoundGain, null);
			jContentPane.add(getJSliderSound(), null);
			jContentPane.add(getJButtonOK(), null);
			jContentPane.add(getJButtonCancel(), null);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jSliderMusic
	 *
	 * @return javax.swing.JSlider
	 */
	private JSlider getJSliderMusic() {
		if (jSliderMusic == null) {
			jSliderMusic = new JSlider();
			jSliderMusic.setBounds(new Rectangle(15, 30, 256, 25));
			jSliderMusic.setMaximum(100);
			jSliderMusic.setMinimum(0);
			jSliderMusic.setMajorTickSpacing(10);
			jSliderMusic.setPaintTicks(true);
			jSliderMusic.setValue(100);
		}
		return jSliderMusic;
	}

	/**
	 * This method initializes jSliderSound
	 *
	 * @return javax.swing.JSlider
	 */
	private JSlider getJSliderSound() {
		if (jSliderSound == null) {
			jSliderSound = new JSlider();
			jSliderSound.setBounds(new Rectangle(15, 90, 256, 25));
			jSliderSound.setMaximum(100);
			jSliderSound.setMinimum(0);
			jSliderSound.setPaintTicks(true);
			jSliderSound.setValue(100);
			jSliderSound.setMajorTickSpacing(10);
		}
		return jSliderSound;
	}

	/**
	 * This method initializes jButtonOK
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonOK() {
		if (jButtonOK == null) {
			jButtonOK = new JButton();
			jButtonOK.setBounds(new Rectangle(210, 135, 66, 25));
			jButtonOK.setText(" Ok ");
			jButtonOK.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					Music.setGain(jSliderMusic.getValue()/100.0);
					GameController.sound.setGain(jSliderSound.getValue()/100.0);
					dispose();
				}
			});
		}
		return jButtonOK;
	}

	/**
	 * This method initializes jButtonCancel
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCancel() {
		if (jButtonCancel == null) {
			jButtonCancel = new JButton();
			jButtonCancel.setBounds(new Rectangle(14, 136, 77, 23));
			jButtonCancel.setText("Cancel");
			jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					dispose();
				}
			});
		}
		return jButtonCancel;
	}
}
