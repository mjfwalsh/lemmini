package GUI;

import java.awt.Point;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

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

	private JButton jButtonDone = null;

	private JButton jButtonCancel = null;

	private JCheckBox jCheckSoundMute = null;

	private JCheckBox jCheckMusicMute = null;


	/**
	 * Constructor for modal dialog in parent frame.
	 * @param frame parent frame
	 * @param modal create modal dialog?
	 */
	public GainDialog(final JFrame frame, final boolean modal) {
		super(frame, modal);
		initialize();

		// dialogue window position
		Point p = frame.getLocation();
		this.setLocation(p.x+frame.getWidth()/2-getWidth()/2, p.y+frame.getHeight()/2-getHeight()/2);

		// start values
		jSliderSound.setValue((int)(100*GameController.sound.getGain()));
		jSliderMusic.setValue((int)(100*Music.getGain()));
		jCheckMusicMute.setSelected(!GameController.isMusicOn());
		jCheckSoundMute.setSelected(!GameController.isSoundOn());
		jSliderMusic.setEnabled(GameController.isMusicOn());
		jSliderSound.setEnabled(GameController.isSoundOn());

		// finish window
		this.pack();
	}

	/**
	 * Automatically generated init.
	 */
	private void initialize() {
		this.setMinimumSize(new Dimension(300, 10));
		this.setResizable(false);
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
			jContentPane = new JPanel();

			//labels
			jLabelSoundGain = new JLabel("Sound Volume");
			jLabelMusicGain = new JLabel("Music Volume");

			GroupLayout gl = new GroupLayout(jContentPane);
			gl.setHorizontalGroup(
				gl.createParallelGroup()
					.addComponent(jLabelMusicGain, Alignment.LEADING)
					.addComponent(getJSliderMusic(), Alignment.LEADING)
					.addComponent(jLabelSoundGain, Alignment.LEADING)
					.addComponent(getJSliderSound(), Alignment.LEADING)
					.addComponent(getJCheckMusicMute(), Alignment.TRAILING)
					.addComponent(getJCheckSoundMute(), Alignment.TRAILING)
					.addComponent(getJButtonDone(), Alignment.TRAILING)
			);
			gl.setVerticalGroup(
				gl.createParallelGroup()
					.addGroup(gl.createSequentialGroup()
						.addGroup(gl.createParallelGroup()
							.addComponent(jLabelMusicGain)
							.addComponent(getJCheckMusicMute()))
						.addComponent(getJSliderMusic())
						.addGap(30)
						.addGroup(gl.createParallelGroup()
							.addComponent(jLabelSoundGain)
							.addComponent(getJCheckSoundMute()))
						.addComponent(getJSliderSound())
						.addGap(20)
						.addComponent(getJButtonDone()))
			);
			gl.setAutoCreateGaps(false);
			gl.setAutoCreateContainerGaps(true);
			jContentPane.setLayout(gl);
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
			jSliderMusic.setMaximum(100);
			jSliderMusic.setMinimum(0);
			jSliderMusic.setMajorTickSpacing(10);
			jSliderMusic.setPaintTicks(true);
			jSliderMusic.setValue(100);
			jSliderMusic.addChangeListener(new javax.swing.event.ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					Music.setGain(jSliderMusic.getValue()/100.0);
				}
			});
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
			jSliderSound.setMaximum(100);
			jSliderSound.setMinimum(0);
			jSliderSound.setPaintTicks(true);
			jSliderSound.setValue(100);
			jSliderSound.setMajorTickSpacing(10);
			jSliderSound.addChangeListener(new javax.swing.event.ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					GameController.sound.setGain(jSliderSound.getValue()/100.0);
				}
			});
		}
		return jSliderSound;
	}

	/**
	 * This method initializes jButtonCancel
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonDone() {
		if (jButtonDone == null) {
			jButtonDone = new JButton();
			jButtonDone.setText("Done");
			jButtonDone.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					dispose();
				}
			});
		}
		return jButtonDone;
	}

	/**
	 * This method initializes jCheckMusicMute
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckMusicMute() {
		if (jCheckMusicMute == null) {
			jCheckMusicMute = new JCheckBox("Mute");
			jCheckMusicMute.addChangeListener(new javax.swing.event.ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					boolean selected = jCheckMusicMute.isSelected();

					GameController.setMusicOn(!selected);
					jSliderMusic.setEnabled(!selected);

					if (GameController.getLevel() != null) {
						if (!selected) {
							Music.play();
						} else {
							Music.stop();
						}
					}
				}
			});
		}
		return jCheckMusicMute;
	}

	/**
	 * This method initializes jCheckSoundMute
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckSoundMute() {
		if (jCheckSoundMute == null) {
			jCheckSoundMute = new JCheckBox("Mute");
			jCheckSoundMute.addChangeListener(new javax.swing.event.ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					boolean selected = jCheckSoundMute.isSelected();
					GameController.setSoundOn(!selected);
					jSliderSound.setEnabled(!selected);
				}
			});
		}
		return jCheckSoundMute;
	}
}
