package Extract;

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

import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Dialog to enter source and target paths for resource extraction.
 *
 * @author Volker Oth
 */
public class FolderDialog extends JDialog {

	private javax.swing.JPanel jContentPane = null;
	private JLabel jLabelTrg = null;
	private JTextField jTextFieldTrg = null;
	private JLabel jLabelSrc = null;
	private JTextField jTextFieldSrc = null;
	private JButton jButtonSrc = null;
	private JButton jButtonTrg = null;
	private JButton jButtonQuit = null;
	private JButton jButtonExtract = null;
	private JLabel jLabelHeader = null;

	// own stuff
	private final static long serialVersionUID = 0x01;

	/** target (Lemmini resource) path for extraction */
	private String targetPath;
	/** source (WINLEMM) path for extraction */
	private String sourcePath;  //  @jve:decl-index=0:
	/** self reference to this dialog */
	private JDialog thisDialog;
	/** flag that tells whether to extract or not */
	private boolean doExtract;


	/**
	 * Constructor for modal dialog in parent frame
	 * @param frame parent frame
	 * @param modal create modal dialog?
	 */
	public FolderDialog(final JFrame frame, final boolean modal) {
		super(frame, modal);
		initialize();

		// own stuff
		thisDialog = this;
		doExtract = false;
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
	 * Set parameters for text edit boxes.
	 * @param srcPath source (WINLEMM) path for extraction
	 * @param trgPath target (Lemmini resource) path for extraction
	 */
	public void setParameters(final String srcPath, final String trgPath) {
		jTextFieldSrc.setText( srcPath );
		sourcePath = srcPath;
		jTextFieldTrg.setText( trgPath );
		targetPath = trgPath;
	}

	/**
	 * Get target (Lemmini resource) path for extraction.
	 * @return target (Lemmini resource) path for extraction
	 */
	public String getTarget() {
		if (targetPath != null)
			return targetPath;
		else
			return "";
	}

	/**
	 * Get source (WINLEMM) path for extraction.
	 * @return source (WINLEMM) path for extraction
	 */
	public String getSource() {
		if (sourcePath != null)
			return sourcePath;
		else
			return "";
	}

	/**
	 * Get extraction selection status.
	 * @return true if extraction was chosen, false otherwise
	 */
	public boolean getSuccess() {
		return doExtract;
	}

	/**
	 * Initialize manually generated resources.
	 */
	private void initialize() {
		this.setSize(457, 208);
		this.setContentPane(getJContentPane());
		this.setTitle("Lemmini Resource Extractor");
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getJContentPane() {
		if(jContentPane == null) {
			GridBagConstraints gridBagLabelHeader = new GridBagConstraints();
			gridBagLabelHeader.gridx = 0;
			gridBagLabelHeader.gridwidth = 2;
			gridBagLabelHeader.anchor = GridBagConstraints.NORTHWEST;
			gridBagLabelHeader.insets = new Insets(10, 10, 10, 0);
			gridBagLabelHeader.gridy = 0;
			jLabelHeader = new JLabel();
			jLabelHeader.setText("Extract the resources from Lemmings for Windows");
			GridBagConstraints gridBagLabelTrg = new GridBagConstraints();
			gridBagLabelTrg.gridx = 0;
			gridBagLabelTrg.anchor = GridBagConstraints.WEST;
			gridBagLabelTrg.insets = new Insets(10, 10, 0, 0);
			gridBagLabelTrg.gridy = 5;
			GridBagConstraints gridBagButtonTrg = new GridBagConstraints();
			gridBagButtonTrg.gridx = 1;
			gridBagButtonTrg.anchor = GridBagConstraints.EAST;
			gridBagButtonTrg.insets = new Insets(0, 0, 0, 10);
			gridBagButtonTrg.gridy = 6;
			GridBagConstraints gridBagButtonSrc = new GridBagConstraints();
			gridBagButtonSrc.gridx = 1;
			gridBagButtonSrc.anchor = GridBagConstraints.EAST;
			gridBagButtonSrc.insets = new Insets(0, 0, 0, 10);
			gridBagButtonSrc.gridy = 4;
			GridBagConstraints gridBagTextFieldSrc = new GridBagConstraints();
			gridBagTextFieldSrc.fill = GridBagConstraints.BOTH;
			gridBagTextFieldSrc.gridy = 4;
			gridBagTextFieldSrc.weightx = 1.0;
			gridBagTextFieldSrc.anchor = GridBagConstraints.WEST;
			gridBagTextFieldSrc.insets = new Insets(0, 10, 0, 10);
			gridBagTextFieldSrc.gridx = 0;
			GridBagConstraints gridBagTextFieldTrg = new GridBagConstraints();
			gridBagTextFieldTrg.fill = GridBagConstraints.BOTH;
			gridBagTextFieldTrg.gridy = 6;
			gridBagTextFieldTrg.weightx = 1.0;
			gridBagTextFieldTrg.anchor = GridBagConstraints.WEST;
			gridBagTextFieldTrg.insets = new Insets(0, 10, 0, 10);
			gridBagTextFieldTrg.gridx = 0;
			GridBagConstraints gridBagLabelSrc = new GridBagConstraints();
			gridBagLabelSrc.gridx = 0;
			gridBagLabelSrc.anchor = GridBagConstraints.WEST;
			gridBagLabelSrc.insets = new Insets(0, 10, 0, 0);
			gridBagLabelSrc.gridy = 3;
			GridBagConstraints gridBagButtonExtract = new GridBagConstraints();
			gridBagButtonExtract.gridx = 1;
			gridBagButtonExtract.insets = new Insets(20, 0, 0, 10);
			gridBagButtonExtract.anchor = GridBagConstraints.EAST;
			gridBagButtonExtract.gridy = 7;
			GridBagConstraints gridBagButtonQuit = new GridBagConstraints();
			gridBagButtonQuit.gridx = 0;
			gridBagButtonQuit.anchor = GridBagConstraints.WEST;
			gridBagButtonQuit.insets = new Insets(20, 10, 0, 0);
			gridBagButtonQuit.gridy = 7;
			jLabelSrc = new JLabel();
			jLabelTrg = new JLabel();
			jContentPane = new javax.swing.JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jLabelTrg.setText("Target Path");
			jLabelSrc.setText("Source Path (\"WINLEMM\" directory)");
			jLabelSrc.setComponentOrientation(java.awt.ComponentOrientation.UNKNOWN);
			jContentPane.setComponentOrientation(java.awt.ComponentOrientation.UNKNOWN);
			jContentPane.add(jLabelTrg, gridBagLabelTrg);
			jContentPane.add(getJTextFieldTrg(), gridBagTextFieldTrg);
			jContentPane.add(jLabelSrc, gridBagLabelSrc);
			jContentPane.add(getJTextFieldSrc(), gridBagTextFieldSrc);
			jContentPane.add(getJButtonSrc(), gridBagButtonSrc);
			jContentPane.add(getJButtonTrg(), gridBagButtonTrg);
			jContentPane.add(getJButtonQuit(), gridBagButtonQuit);
			jContentPane.add(getJButtonExtract(), gridBagButtonExtract);
			jContentPane.add(jLabelHeader, gridBagLabelHeader);
		}
		return jContentPane;
	}
	/**
	 * This method initializes jTextFieldTrg
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldTrg() {
		if (jTextFieldTrg == null) {
			jTextFieldTrg = new JTextField();
			jTextFieldTrg.setPreferredSize(new java.awt.Dimension(100,19));
			jTextFieldTrg.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					targetPath = jTextFieldTrg.getText();
				}
			});
		}
		return jTextFieldTrg;
	}
	/**
	 * This method initializes jTextFieldSrc
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldSrc() {
		if (jTextFieldSrc == null) {
			jTextFieldSrc = new JTextField();
			jTextFieldSrc.setPreferredSize(new java.awt.Dimension(100,19));
			jTextFieldSrc.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					sourcePath = jTextFieldSrc.getText();
				}
			});
		}
		return jTextFieldSrc;
	}
	/**
	 * This method initializes jButtonSrc
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonSrc() {
		if (jButtonSrc == null) {
			jButtonSrc = new JButton();
			jButtonSrc.setText("Browse");
			jButtonSrc.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					JFileChooser jf = new JFileChooser(sourcePath);
					jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
					int returnVal = jf.showOpenDialog(thisDialog);
					if(returnVal == JFileChooser.APPROVE_OPTION) {
						sourcePath = jf.getSelectedFile().getAbsolutePath();
						jTextFieldSrc.setText(sourcePath);
					}
				}
			});
		}
		return jButtonSrc;
	}

	/**
	 * This method initializes jButtonTrg
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonTrg() {
		if (jButtonTrg == null) {
			jButtonTrg = new JButton();
			jButtonTrg.setText("Browse");
			jButtonTrg.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					JFileChooser jf = new JFileChooser(targetPath);
					jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
					int returnVal = jf.showOpenDialog(thisDialog);
					if(returnVal == JFileChooser.APPROVE_OPTION) {
						targetPath = jf.getSelectedFile().getAbsolutePath();
						jTextFieldTrg.setText(targetPath);
					}
				}
			});
		}
		return jButtonTrg;
	}
	/**
	 * This method initializes jButtonQuit
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonQuit() {
		if (jButtonQuit == null) {
			jButtonQuit = new JButton();
			jButtonQuit.setText("Quit");
			jButtonQuit.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					sourcePath = jTextFieldSrc.getText();
					targetPath = jTextFieldTrg.getText();
					dispose();
				}
			});
		}
		return jButtonQuit;
	}
	/**
	 * This method initializes jButtonExtract
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonExtract() {
		if (jButtonExtract == null) {
			jButtonExtract = new JButton();
			jButtonExtract.setText("Extract");
			jButtonExtract.addActionListener(new java.awt.event.ActionListener() {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {
					sourcePath = jTextFieldSrc.getText();
					targetPath = jTextFieldTrg.getText();
					doExtract = true;
					dispose();
				}
			});
		}
		return jButtonExtract;
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
