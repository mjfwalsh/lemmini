package Extract;

import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;
import java.awt.Point;

import javax.swing.JDialog;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import java.awt.Insets;

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
 * Dialog used to output extraction progress information.
 * @author Volker Oth
 */
public class OutputDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JButton jButtonOk = null;

	private JScrollPane jScrollPaneOut = null;

	private JButton jButtonCancel = null;

	private JTextArea jTextAreaOut = null;

	// own stuff
	/** Extraction canceled? */
	private boolean cancel = false;

	/**
	 * Constructor for modal dialog in parent frame.
	 * @param frame parent frame
	 * @param modal create modal dialog?
	 */
	public OutputDialog(final JFrame frame, final boolean modal) {
		super(frame, modal);
		initialize();

		// own stuff
		if (frame != null) {
			Point p = frame.getLocation();
			this.setLocation(p.x+frame.getWidth()/2-getWidth()/2, p.y+frame.getHeight()/2-getHeight()/2);
		} else {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			Point p = ge.getCenterPoint();
			p.x -= this.getWidth()/2;
			p.y -= this.getHeight()/2;
			this.setLocation(p);
		}
	}

	/**
	 * Print text to output console.
	 * @param txt text to print
	 */
	public void print(final String txt) {
		jTextAreaOut.insert(txt, jTextAreaOut.getDocument().getLength());
		jTextAreaOut.setCaretPosition(jTextAreaOut.getDocument().getLength());
	}

	/**
	 * Return cancel state of extraction process.
	 * @return true if extraction was cancelled, else false
	 */
	public boolean isCancelled() {
		return cancel;
	}

	/**
	 * Enable the Ok button (if extraction was done successfully)
	 */
	public void enableOk() {
		jButtonOk.setEnabled(true);
	}

	/**
	 * Initialize manually generated resources.
	 */
	private void initialize() {
		this.setSize(500, 352);
		this.setTitle("Lemmini Resource Extractor");
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
			gridBagButtonCancel.gridx = 0;
			gridBagButtonCancel.anchor = GridBagConstraints.WEST;
			gridBagButtonCancel.insets = new Insets(5, 5, 5, 10);
			gridBagButtonCancel.gridy = 1;
			GridBagConstraints gridBagScrollPane = new GridBagConstraints();
			gridBagScrollPane.fill = GridBagConstraints.BOTH;
			gridBagScrollPane.gridy = 0;
			gridBagScrollPane.weightx = 1.0;
			gridBagScrollPane.weighty = 1.0;
			gridBagScrollPane.gridwidth = 2;
			gridBagScrollPane.gridx = 0;
			GridBagConstraints gridBagButtonOk = new GridBagConstraints();
			gridBagButtonOk.gridx = 1;
			gridBagButtonOk.insets = new Insets(5, 5, 5, 5);
			gridBagButtonOk.anchor = GridBagConstraints.EAST;
			gridBagButtonOk.gridy = 1;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getJButtonOk(), gridBagButtonOk);
			jContentPane.add(getJScrollPaneOut(), gridBagScrollPane);
			jContentPane.add(getJButtonCancel(), gridBagButtonCancel);
		}
		return jContentPane;
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
			jButtonOk.setEnabled(false);
			jButtonOk.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					dispose();
				}
			});
		}
		return jButtonOk;
	}

	/**
	 * This method initializes jScrollPaneOut
	 *
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPaneOut() {
		if (jScrollPaneOut == null) {
			jScrollPaneOut = new JScrollPane();
			jScrollPaneOut.setViewportView(getJTextAreaOut());
		}
		return jScrollPaneOut;
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
					cancel =true;
					dispose();
				}
			});
		}
		return jButtonCancel;
	}

	/**
	 * This method initializes jTextAreaOut
	 *
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getJTextAreaOut() {
		if (jTextAreaOut == null) {
			jTextAreaOut = new JTextArea();
			jTextAreaOut.setEditable(false);
		}
		return jTextAreaOut;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
