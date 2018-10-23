package GUI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import Game.Core;

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
 * Dialog for managing players.
 * @author Volker Oth
 */
public class PlayerDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JScrollPane jScrollPane = null;

	@SuppressWarnings("rawtypes")
	private JList jList = null;

	private JButton jButtonNew = null;

	private JButton jButtonDelete = null;

	private JButton jButtonOK = null;

	private JButton jButtonCancel = null;

	// own stuff
	private Vector<String> players;


	/**
	 * Get list of players.
	 * @return list of players.
	 */
	public List<String> getPlayers() {
		return players;
	}

	/**
	 * Get selected list index.
	 * @return selected list index
	 */
	public int getSelection() {
		return jList.getSelectedIndex();
	}

	/**
	 * Initialize manually generated resources.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void init() {
		players = new Vector<String>();
		for (int i=0; i<Core.getPlayerNum(); i++)
			players.add(Core.getPlayer(i));
		jList = new JList(players);
		jScrollPane.setViewportView(jList);
	}

	/**
	 * Constructor for modal dialog in parent frame.
	 * @param frame parent frame
	 * @param modal create modal dialog?
	 */
	public PlayerDialog(final JFrame frame, final boolean modal) {
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
		this.setSize(442, 199);
		this.setTitle("Manage Players");
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagButtonNew = new GridBagConstraints();
			gridBagButtonNew.fill = GridBagConstraints.HORIZONTAL;
			gridBagButtonNew.insets = new Insets(0, 0, 0, 0);
			GridBagConstraints gridBagButtonCancel = new GridBagConstraints();
			gridBagButtonCancel.gridx = 0;
			gridBagButtonCancel.insets = new Insets(0, 0, 0, 0);
			gridBagButtonCancel.anchor = GridBagConstraints.WEST;
			gridBagButtonCancel.gridy = 5;
			GridBagConstraints gridBagButtonOk = new GridBagConstraints();
			gridBagButtonOk.gridx = 1;
			gridBagButtonOk.fill = GridBagConstraints.HORIZONTAL;
			gridBagButtonOk.insets = new Insets(0, 0, 0, 0);
			gridBagButtonOk.gridy = 5;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 1;
			gridBagConstraints2.insets = new Insets(0, 2, 2, 2);
			gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints2.anchor = GridBagConstraints.NORTH;
			gridBagConstraints2.gridy = 3;
			GridBagConstraints gridBagButtonDelete = new GridBagConstraints();
			gridBagButtonDelete.gridx = 1;
			gridBagButtonDelete.insets = new Insets(0, 0, 0, 0);
			gridBagButtonDelete.fill = GridBagConstraints.HORIZONTAL;
			gridBagButtonDelete.gridy = 2;
			GridBagConstraints gridBagScrollPane = new GridBagConstraints();
			gridBagScrollPane.fill = GridBagConstraints.BOTH;
			gridBagScrollPane.gridy = 0;
			gridBagScrollPane.weightx = 1.0;
			gridBagScrollPane.weighty = 1.0;
			gridBagScrollPane.gridheight = 4;
			gridBagScrollPane.insets = new Insets(0, 0, 0, 0);
			gridBagScrollPane.gridx = 0;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getJScrollPane(), gridBagScrollPane);
			jContentPane.add(getJButtonNew(), gridBagButtonNew);
			jContentPane.add(getJButtonDelete(), gridBagButtonDelete);
			jContentPane.add(getJButtonOK(), gridBagButtonOk);
			jContentPane.add(getJButtonCancel(), gridBagButtonCancel);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jScrollPane
	 *
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getJList());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes jList
	 *
	 * @return javax.swing.JList
	 */
	@SuppressWarnings("rawtypes")
	private JList getJList() {
		if (jList == null) {
			jList = new JList();
		}
		return jList;
	}

	/**
	 * This method initializes jButtonNew
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonNew() {
		if (jButtonNew == null) {
			jButtonNew = new JButton();
			jButtonNew.setText("New Player");
			jButtonNew.addActionListener(new java.awt.event.ActionListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					String player = JOptionPane.showInputDialog(
							Core.getCmp(), "Enter Player Name", "Input", JOptionPane.QUESTION_MESSAGE);
					if (player != null) {
						// check if this player already exists
						// it it alread exists, reset the existing profile
						boolean found= false;
						for (int pli=0; pli < players.size(); pli++) {
							if ( players.get(pli).equalsIgnoreCase(player) ) {
								player = players.get(pli);
								found = true;
								break;
							}
						}
						// really a new player
						if (!found) {
							players.add(player);
							jList.setListData(players);
							int i = players.size()-1;
							if (i>=0)
								jList.setSelectedIndex(i);
						}
					}
				}
			});
		}
		return jButtonNew;
	}

	/**
	 * This method initializes jButtonDelete
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonDelete() {
		if (jButtonDelete == null) {
			jButtonDelete = new JButton();
			jButtonDelete.setText("Delete Player");
			jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					int idx =jList.getSelectedIndex();
					if (idx != -1) {
						players.remove(idx);
						jList.setListData(players);
					}
				}
			});
		}
		return jButtonDelete;
	}

	/**
	 * This method initializes jButtonOK
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonOK() {
		if (jButtonOK == null) {
			jButtonOK = new JButton();
			jButtonOK.setText("Ok");
			jButtonOK.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
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
			jButtonCancel.setText("Cancel");
			jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					players.clear();
					players = null;
					dispose();
				}
			});
		}
		return jButtonCancel;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
