package GUI;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

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
 * Dialog with legal information.
 * @author Volker Oth
 */
public class LegalDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JButton jButtonCancel = null;

	private JButton jButtonOk = null;

	private JScrollPane jScrollPane = null;

	private JEditorPane thisEditor = null;

	// own stuff
	private boolean ok = false;

	private URL thisURL;

	/**
	 * Initialize manually generated resources.
	 */
	private void init() {
		ClassLoader loader = LegalDialog.class.getClassLoader();
		thisURL = loader.getResource("disclaimer.htm");
		try {
			thisEditor = new JEditorPane( thisURL );
			thisEditor.setEditable( false );
			// needed to open browser via clicking on a link
			thisEditor.addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					URL url = e.getURL();
					if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						try {
							if (url.sameFile(thisURL))
								thisEditor.setPage(url);
							else
								Desktop.getDesktop().browse(url.toURI());
						} catch (IOException ex) {
						} catch (URISyntaxException ex) {
						}
					} else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
						if (url.sameFile(thisURL))
							thisEditor.setToolTipText(url.getRef());
						else
							thisEditor.setToolTipText(url.toExternalForm());
					} else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
						thisEditor.setToolTipText(null);
					}
				}
			});
			jScrollPane.setViewportView(thisEditor);  // Generated
		} catch (IOException ex) {ex.printStackTrace();};
	}


	/**
	 * Constructor for modal dialog in parent frame.
	 * @param frame parent frame
	 * @param modal create modal dialog?
	 */
	public LegalDialog(final JFrame frame, final boolean modal) {
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
		init();
	}

	/**
	 * Automatically generated init.
	 */
	private void initialize() {
		this.setSize(640, 450);
		this.setTitle("Lemmini - Disclaimer");
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagScrollPane = new GridBagConstraints();
			gridBagScrollPane.fill = GridBagConstraints.BOTH;
			gridBagScrollPane.gridy = 0;
			gridBagScrollPane.weightx = 1.0;
			gridBagScrollPane.weighty = 1.0;
			gridBagScrollPane.gridwidth = 2;
			gridBagScrollPane.gridx = 0;
			GridBagConstraints gridBagButtonOk = new GridBagConstraints();
			gridBagButtonOk.gridx = 1;
			gridBagButtonOk.anchor = GridBagConstraints.EAST;
			gridBagButtonOk.insets = new Insets(5, 0, 5, 5);
			gridBagButtonOk.gridy = 1;
			GridBagConstraints gridBagButtonCancel = new GridBagConstraints();
			gridBagButtonCancel.gridx = 0;
			gridBagButtonCancel.insets = new Insets(5, 5, 5, 0);
			gridBagButtonCancel.gridy = 1;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getJButtonCancel(), gridBagButtonCancel);
			jContentPane.add(getJButtonOk(), gridBagButtonOk);
			jContentPane.add(getJScrollPane(), gridBagScrollPane);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jButtonCancel
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCancel() {
		if (jButtonCancel == null) {
			jButtonCancel = new JButton();
			jButtonCancel.setText("I disagree");
			jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					ok = false;
					dispose();
				}
			});
		}
		return jButtonCancel;
	}

	/**
	 * This method initializes jButtonOk
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonOk() {
		if (jButtonOk == null) {
			jButtonOk = new JButton();
			jButtonOk.setText("I agree");
			jButtonOk.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					ok = true;
					dispose();
				}
			});
		}
		return jButtonOk;
	}

	/**
	 * This method initializes jScrollPane
	 *
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		}
		return jScrollPane;
	}

	/**
	 * Ok button was pressed.
	 * @return true: ok button was pressed.
	 */
	public boolean isOk() {
		return ok;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
