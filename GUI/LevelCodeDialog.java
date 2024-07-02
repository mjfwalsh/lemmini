package GUI;

import Game.GameController;
import java.awt.Point;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/*
 * Copyright 2009 Volker Oth
 * (With changes by Michael J. Walsh Copyright 2019)
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
 *
 * @author Volker Oth
 */
public class LevelCodeDialog extends JDialog {

  private static final long serialVersionUID = 1L;

  private JPanel jContentPane = null;

  private JComboBox<String> jComboBoxLvlPack = null;

  private JTextField jTextFieldCode = null;

  private JButton jButtonOk = null;

  private JButton jButtonCancel = null;

  // own stuff
  private int levelPackIndex;
  private String code; //  @jve:decl-index=0:

  /** Initialize manually generated resources. */
  private void init() {
    // level pack 0 is the dummy level pack -> not selectable
    for (int i = 1; i < GameController.getLevelPackNum(); i++)
      jComboBoxLvlPack.addItem(GameController.getLevelPack(i).getName());
    int lpi = GameController.getCurLevelPackIdx();
    if (lpi == 0) lpi = 1;
    jComboBoxLvlPack.setSelectedIndex(lpi - 1);

    levelPackIndex = lpi;
  }

  /**
   * Get entered level code.
   *
   * @return entered level code.
   */
  public String getCode() {
    return code;
  }

  /**
   * Get selected level pack.
   *
   * @return selected level pack
   */
  public int getLevelPack() {
    return levelPackIndex;
  }

  /**
   * Constructor for modal dialog in parent frame
   *
   * @param frame parent frame
   * @param modal create modal dialog?
   */
  public LevelCodeDialog(final JFrame frame, final boolean modal) {
    super(frame, modal);
    initialize();

    // own stuff
    Point p = frame.getLocation();
    this.setLocation(
        p.x + frame.getWidth() / 2 - getWidth() / 2, p.y + frame.getHeight() / 2 - getHeight() / 2);
    init();

    this.pack();
  }

  /** Automatically generated init. */
  private void initialize() {
    this.setTitle("Enter Level Code");
    this.setContentPane(getJContentPane());
    this.setResizable(false);
  }

  /**
   * This method initializes jContentPane
   *
   * @return javax.swing.JPanel
   */
  private JPanel getJContentPane() {
    if (jContentPane == null) {
      jContentPane = new JPanel();
      JLabel jLabelLvlPack = new JLabel("Chose level pack");
      JLabel jLabelCode = new JLabel("Enter Level Code");

      GroupLayout gl = new GroupLayout(jContentPane);
      gl.setHorizontalGroup(
          gl.createParallelGroup()
              .addComponent(jLabelLvlPack, Alignment.LEADING)
              .addComponent(getJComboBoxLvlPack(), Alignment.LEADING)
              .addComponent(jLabelCode, Alignment.LEADING)
              .addComponent(getJTextFieldCode(), Alignment.LEADING)
              .addComponent(getJButtonOk(), Alignment.LEADING)
              .addComponent(getJButtonCancel(), Alignment.TRAILING));
      gl.setVerticalGroup(
          gl.createSequentialGroup()
              .addComponent(jLabelLvlPack)
              .addComponent(getJComboBoxLvlPack())
              .addComponent(jLabelCode)
              .addComponent(getJTextFieldCode())
              .addGroup(
                  gl.createParallelGroup()
                      .addComponent(getJButtonOk())
                      .addComponent(getJButtonCancel())));
      gl.setAutoCreateGaps(false);
      gl.setAutoCreateContainerGaps(true);
      jContentPane.setLayout(gl);
    }
    return jContentPane;
  }

  /**
   * This method initializes jComboBoxLvlPack
   *
   * @return javax.swing.JComboBox
   */
  private JComboBox<String> getJComboBoxLvlPack() {
    if (jComboBoxLvlPack == null) {
      jComboBoxLvlPack = new JComboBox<>();
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
      jTextFieldCode.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
              code = jTextFieldCode.getText();
              levelPackIndex = jComboBoxLvlPack.getSelectedIndex() + 1;
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
      jButtonOk = new JButton("Ok");
      jButtonOk.addActionListener(
          new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
              code = jTextFieldCode.getText();
              levelPackIndex = jComboBoxLvlPack.getSelectedIndex() + 1;
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
      jButtonCancel = new JButton("Cancel");
      jButtonCancel.addActionListener(
          new java.awt.event.ActionListener() {
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
} //  @jve:decl-index=0:visual-constraint="10,10"
