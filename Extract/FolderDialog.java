package Extract;

/*
 * Copyright 2009 Volker Oth
 * (Minor changes by Michael J. Walsh Copyright 2018)
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
import java.awt.Point;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.GroupLayout.Alignment;

/**
 * Dialog to enter source and target paths for resource extraction.
 *
 * @author Volker Oth
 */
public class FolderDialog extends JDialog {

	private javax.swing.JPanel jContentPane = null;
	private JTextField jTextFieldTrg = null;
	private JTextField jTextFieldSrc = null;
	private JButton jButtonSrc = null;
	private JButton jButtonTrg = null;
	private JButton jButtonQuit = null;
	private JButton jButtonExtract = null;

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

		/*IF-NOT-MAC
		jTextFieldTrg.setText( trgPath );
		targetPath = trgPath;
		//END-NOT-MAC*/
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
		this.setContentPane(getJContentPane());
		this.setTitle("Lemmini Resource Extractor");
		this.setResizable(false);
		this.pack();
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private javax.swing.JPanel getJContentPane() {
		if(jContentPane == null) {
			jContentPane = new javax.swing.JPanel();
			GroupLayout gl = new GroupLayout(jContentPane);
			gl.setAutoCreateGaps(true);
			gl.setAutoCreateContainerGaps(true);
			jContentPane.setLayout(gl);

			GroupLayout.SequentialGroup	h = gl.createSequentialGroup();
			GroupLayout.SequentialGroup v = gl.createSequentialGroup();
			GroupLayout.ParallelGroup leftAlign = gl.createParallelGroup();
			GroupLayout.ParallelGroup rightAlign = gl.createParallelGroup();

			JLabel title = new JLabel("Extract the resources from Lemmings for Windows");
			leftAlign.addComponent(title);
			v.addComponent(title);

			v.addPreferredGap(ComponentPlacement.UNRELATED);

			JLabel sourceLabel = new JLabel("Source Path (\"WINLEM\" directory)");
			leftAlign.addComponent(sourceLabel);
			v.addComponent(sourceLabel);

			v.addPreferredGap(ComponentPlacement.RELATED);

			leftAlign.addComponent(getJTextFieldSrc());
			rightAlign.addComponent(getJButtonSrc());
			v.addGroup(gl.createParallelGroup(Alignment.BASELINE).addComponent(getJTextFieldSrc()).addComponent(getJButtonSrc()));

			v.addPreferredGap(ComponentPlacement.UNRELATED);

			/*IF-NOT-MAC

			JLabel targetLabel = new JLabel("Target Path");
			leftAlign.addComponent(targetLabel);
			v.addComponent(targetLabel);

			v.addPreferredGap(ComponentPlacement.RELATED);

			leftAlign.addComponent(getJTextFieldTrg());
			rightAlign.addComponent(getJButtonTrg());
			v.addGroup(gl.createParallelGroup(Alignment.BASELINE).addComponent(getJTextFieldTrg()).addComponent(getJButtonTrg()));

			v.addPreferredGap(ComponentPlacement.UNRELATED);

			//END-NOT-MAC*/

			leftAlign.addComponent(getJButtonQuit());
			rightAlign.addComponent(getJButtonExtract());
			v.addGroup(gl.createParallelGroup(Alignment.BASELINE).addComponent(getJButtonQuit()).addComponent(getJButtonExtract()));

			h.addGroup(leftAlign);
			h.addGroup(rightAlign);
			gl.setHorizontalGroup(h);
			gl.setVerticalGroup(v);
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
					System.exit(0);
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
					/*IF-NOT-MAC
					targetPath = jTextFieldTrg.getText();
					//END-NOT-MAC*/
					doExtract = true;
					dispose();
				}
			});
		}
		return jButtonExtract;
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
