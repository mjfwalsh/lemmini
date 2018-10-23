package GUI;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import Game.GameController;

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
 * Dialog for entering level codes.
 * @author Volker Oth
 */
public class LevelCodeDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JLabel jLabelLvlPack = null;

	@SuppressWarnings("rawtypes")
	private JComboBox jComboBoxLvlPack = null;

	private JLabel jLabelCode = null;

	private JTextField jTextFieldCode = null;

	private JButton jButtonOk = null;

	private JButton jButtonCancel = null;


	// own stuff
	private int levelPackIndex;
	private String code;  //  @jve:decl-index=0:

	/**
	 * Initialize manually generated resources.
	 */
	@SuppressWarnings("unchecked")
	private void init() {
		// level pack 0 is the dummy level pack -> not selectable
		for (int i=1; i<GameController.getLevelPackNum(); i++)
			jComboBoxLvlPack.addItem(GameController.getLevelPack(i).getName());
		int lpi = GameController.getCurLevelPackIdx();
		if (lpi==0)
			lpi = 1;
		jComboBoxLvlPack.setSelectedIndex(lpi-1);

		levelPackIndex = lpi;
		jTextFieldCode.setText("");
	}

	/**
	 * Get entered level code.
	 * @return entered level code.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Get selected level pack.
	 * @return selected level pack
	 */
	public int getLevelPack() {
		return levelPackIndex;
	}

	/**
	 * Constructor for modal dialog in parent frame
	 * @param frame parent frame
	 * @param modal create modal dialog?
	 */
	public LevelCodeDialog(final JFrame frame, final boolean modal) {
		super(frame, modal);
		initialize();

		// own stuff
		Point p = frame.getLocation();
		this.setLocation(p.x+frame.getWidth()/2-getWidth()/2, p.y+frame.getHeight()/2-getHeight()/2);
		init();
	}

	/**
	 * Automatically generated init.
	 */
	private void initialize() {
		this.setSize(300, 153);
		this.setTitle("Enter Level Code");
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagButtonCancel = new GridBagConstraints();
			gridBagButtonCancel.gridx = 1;
			gridBagButtonCancel.insets = new Insets(8, 24, 0, 4);
			gridBagButtonCancel.anchor = GridBagConstraints.EAST;
			gridBagButtonCancel.weightx = 1.0D;
			gridBagButtonCancel.fill = GridBagConstraints.HORIZONTAL;
			gridBagButtonCancel.gridy = 4;
			GridBagConstraints gridBagButtonOk = new GridBagConstraints();
			gridBagButtonOk.gridx = 0;
			gridBagButtonOk.insets = new Insets(8, 4, 0, 24);
			gridBagButtonOk.anchor = GridBagConstraints.WEST;
			gridBagButtonOk.weightx = 1.0D;
			gridBagButtonOk.fill = GridBagConstraints.HORIZONTAL;
			gridBagButtonOk.gridy = 4;
			GridBagConstraints gridBagTextFieldCode = new GridBagConstraints();
			gridBagTextFieldCode.fill = GridBagConstraints.BOTH;
			gridBagTextFieldCode.gridy = 3;
			gridBagTextFieldCode.weightx = 1.0;
			gridBagTextFieldCode.insets = new Insets(0, 4, 0, 4);
			gridBagTextFieldCode.gridwidth = 2;
			gridBagTextFieldCode.gridx = 0;
			GridBagConstraints gridBagLabelCode = new GridBagConstraints();
			gridBagLabelCode.gridx = 0;
			gridBagLabelCode.anchor = GridBagConstraints.WEST;
			gridBagLabelCode.insets = new Insets(8, 4, 0, 4);
			gridBagLabelCode.fill = GridBagConstraints.HORIZONTAL;
			gridBagLabelCode.gridwidth = 2;
			gridBagLabelCode.gridy = 2;
			jLabelCode = new JLabel();
			jLabelCode.setText("Enter Level Code");
			GridBagConstraints gridBagComboLvlPack = new GridBagConstraints();
			gridBagComboLvlPack.fill = GridBagConstraints.BOTH;
			gridBagComboLvlPack.gridy = 1;
			gridBagComboLvlPack.weightx = 1.0;
			gridBagComboLvlPack.anchor = GridBagConstraints.WEST;
			gridBagComboLvlPack.insets = new Insets(0, 4, 6, 4);
			gridBagComboLvlPack.gridwidth = 2;
			gridBagComboLvlPack.gridx = 0;
			GridBagConstraints gridBagLabelLvlPack = new GridBagConstraints();
			gridBagLabelLvlPack.gridx = 0;
			gridBagLabelLvlPack.anchor = GridBagConstraints.NORTHWEST;
			gridBagLabelLvlPack.insets = new Insets(4, 4, 0, 4);
			gridBagLabelLvlPack.fill = GridBagConstraints.HORIZONTAL;
			gridBagLabelLvlPack.gridwidth = 2;
			gridBagLabelLvlPack.gridy = 0;
			jLabelLvlPack = new JLabel();
			jLabelLvlPack.setText("Chose level pack");
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(jLabelLvlPack, gridBagLabelLvlPack);
			jContentPane.add(getJComboBoxLvlPack(), gridBagComboLvlPack);
			jContentPane.add(jLabelCode, gridBagLabelCode);
			jContentPane.add(getJTextFieldCode(), gridBagTextFieldCode);
			jContentPane.add(getJButtonOk(), gridBagButtonOk);
			jContentPane.add(getJButtonCancel(), gridBagButtonCancel);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jComboBoxLvlPack
	 *
	 * @return javax.swing.JComboBox
	 */
	@SuppressWarnings("rawtypes")
	private JComboBox getJComboBoxLvlPack() {
		if (jComboBoxLvlPack == null) {
			jComboBoxLvlPack = new JComboBox();
		}
		return jComboBoxLvlPack;
	}

	/**
	 * This method initializes jTextFieldCode
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldCode() {
		if (jTextFieldCode == null) {
			jTextFieldCode = new JTextField();
			jTextFieldCode.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					code = jTextFieldCode.getText();
					levelPackIndex = jComboBoxLvlPack.getSelectedIndex()+1;
					dispose();
				}
			});
		}
		return jTextFieldCode;
	}

	/**
	 * This method initializes jButtonOk
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonOk() {
		if (jButtonOk == null) {
			jButtonOk = new JButton();
			jButtonOk.setText("Ok");
			jButtonOk.setPreferredSize(new Dimension(90, 50));
			jButtonOk.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					code = jTextFieldCode.getText();
					levelPackIndex = jComboBoxLvlPack.getSelectedIndex()+1;
					dispose();
				}
			});
		}
		return jButtonOk;
	}

	/**
	 * This method initializes jButtonCancel
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCancel() {
		if (jButtonCancel == null) {
			jButtonCancel = new JButton();
			jButtonCancel.setText("Cancel");
			jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					code = null;
					levelPackIndex = -1;
					dispose();
				}
			});
		}
		return jButtonCancel;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
