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
import java.io.File;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

/**
 * Dialog to enter source and target paths for resource extraction.
 *
 * @author Volker Oth
 */
public class FolderDialog extends JDialog {
  private JTextField jTextFieldTrg = null;
  private JTextField jTextFieldSrc = null;
  private JButton jButtonSrc = null;
  private JButton jButtonTrg = null;
  private JButton jButtonQuit = null;
  private JButton jButtonExtract = null;

  // own stuff
  private static final long serialVersionUID = 0x01;

  /** target (Lemmini resource) path for extraction */
  private File targetPath;

  /** source (WINLEMM) path for extraction */
  private File sourcePath; //  @jve:decl-index=0:

  /** flag that tells whether to extract or not */
  private boolean doExtract = false;

  /**
   * Constructor for modal dialog in parent frame
   *
   * @param srcPath source (WINLEMM) path for extraction
   * @param trgPath target (Lemmini resource) path for extraction
   */
  public FolderDialog(final File srcPath, final File trgPath) {
    super((JFrame) null, true);

    initContentPane();
    setTitle("Lemmini Resource Extractor");
    setResizable(false);
    pack();

    // centre windows
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Point p = ge.getCenterPoint();
    p.x -= getWidth() / 2;
    p.y -= getHeight() / 2;
    setLocation(p);

    if (srcPath != null) jTextFieldSrc.setText(srcPath.getAbsolutePath());
    sourcePath = srcPath;

    if (!System.getProperty("os.name").equals("Mac OS X")) {
      if (trgPath != null) jTextFieldTrg.setText(trgPath.getAbsolutePath());
      targetPath = trgPath;
    }
  }

  /**
   * Get target (Lemmini resource) path for extraction.
   *
   * @return target (Lemmini resource) path for extraction
   */
  public File getTarget() {
    return targetPath;
  }

  /**
   * Get source (WINLEMM) path for extraction.
   *
   * @return source (WINLEMM) path for extraction
   */
  public File getSource() {
    return sourcePath;
  }

  /**
   * Get extraction selection status.
   *
   * @return true if extraction was chosen, false otherwise
   */
  public boolean getSuccess() {
    return doExtract;
  }

  /**
   * This method initializes jContentPane
   *
   * @return javax.swing.JPanel
   */
  private void initContentPane() {
    javax.swing.JPanel jContentPane = new javax.swing.JPanel();
    GroupLayout gl = new GroupLayout(jContentPane);
    gl.setAutoCreateGaps(true);
    gl.setAutoCreateContainerGaps(true);
    jContentPane.setLayout(gl);

    GroupLayout.SequentialGroup h = gl.createSequentialGroup();
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
    v.addGroup(
        gl.createParallelGroup(Alignment.BASELINE)
            .addComponent(getJTextFieldSrc())
            .addComponent(getJButtonSrc()));

    v.addPreferredGap(ComponentPlacement.UNRELATED);

    if (!System.getProperty("os.name").equals("Mac OS X")) {
      JLabel targetLabel = new JLabel("Target Path");
      leftAlign.addComponent(targetLabel);
      v.addComponent(targetLabel);

      v.addPreferredGap(ComponentPlacement.RELATED);

      leftAlign.addComponent(getJTextFieldTrg());
      rightAlign.addComponent(getJButtonTrg());
      v.addGroup(
          gl.createParallelGroup(Alignment.BASELINE)
              .addComponent(getJTextFieldTrg())
              .addComponent(getJButtonTrg()));

      v.addPreferredGap(ComponentPlacement.UNRELATED);
    }

    leftAlign.addComponent(getJButtonQuit());
    rightAlign.addComponent(getJButtonExtract());
    v.addGroup(
        gl.createParallelGroup(Alignment.BASELINE)
            .addComponent(getJButtonQuit())
            .addComponent(getJButtonExtract()));

    h.addGroup(leftAlign);
    h.addGroup(rightAlign);
    gl.setHorizontalGroup(h);
    gl.setVerticalGroup(v);
    super.setContentPane(jContentPane);
  }

  /**
   * This method initializes jTextFieldTrg
   *
   * @return javax.swing.JTextField
   */
  private JTextField getJTextFieldTrg() {
    if (jTextFieldTrg == null) {
      jTextFieldTrg = new JTextField();
      jTextFieldTrg.addActionListener(
          (java.awt.event.ActionEvent e) -> {
            targetPath = new File(jTextFieldTrg.getText());
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
      jTextFieldSrc.addActionListener(
          (java.awt.event.ActionEvent e) -> {
            sourcePath = new File(jTextFieldSrc.getText());
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
      jButtonSrc.addActionListener(
          (java.awt.event.ActionEvent e) -> {
            JFileChooser jf = new JFileChooser(sourcePath);
            jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = jf.showOpenDialog(FolderDialog.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
              sourcePath = jf.getSelectedFile().getAbsoluteFile();
              jTextFieldSrc.setText(sourcePath.getAbsolutePath());
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
      jButtonTrg.addActionListener(
          (java.awt.event.ActionEvent e) -> {
            JFileChooser jf = new JFileChooser(targetPath);
            jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = jf.showOpenDialog(FolderDialog.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
              targetPath = jf.getSelectedFile().getAbsoluteFile();
              jTextFieldTrg.setText(targetPath.getAbsolutePath());
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
      jButtonQuit.addActionListener(
          (java.awt.event.ActionEvent e) -> {
            dispose();
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
      jButtonExtract.addActionListener(
          (java.awt.event.ActionEvent e) -> {
            sourcePath = new File(jTextFieldSrc.getText());
            if (!System.getProperty("os.name").equals("Mac OS X")) {
              targetPath = new File(jTextFieldTrg.getText());
            }

            // check if source path exists
            if (!sourcePath.exists()) {
              JOptionPane.showMessageDialog(
                  FolderDialog.this,
                  sourcePath.getAbsolutePath() + " doesn't exist!",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
            } else {
              doExtract = true;
              dispose();
            }
          });
    }
    return jButtonExtract;
  }
} //  @jve:decl-index=0:visual-constraint="10,10"
