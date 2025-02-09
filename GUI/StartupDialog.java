package GUI;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.event.HyperlinkEvent;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

/*
 * Copyright 2009 Volker Oth
 * (Adapted by Michael J. Walsh Copyright 2025)
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
 *
 * @author Volker Oth
 */
public class StartupDialog extends JDialog {

  private static final long serialVersionUID = 1L;
  private boolean ok = false;
  private JTextField jTextFieldSrc = new JTextField();
  private JTextField jTextFieldTrg = new JTextField();

  /**
   * Constructor for modal dialog in parent frame.
   *
   * @param srcPath folder with WinLemm
   * @param trgPath installation folder
   */
  public StartupDialog(final File srcPath, final File trgPath) throws IOException {
    super((JFrame)null, true);
    setSize(675, 480);
    setTitle("Lemmini - Disclaimer");
    setContentPane(getJContentPane());

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Point p = ge.getCenterPoint();
    p.x -= getWidth() / 2;
    p.y -= getHeight() / 2;
    setLocation(p);

    if (srcPath != null) {
      jTextFieldSrc.setText(srcPath.getAbsolutePath());
    }

    if (trgPath != null && !System.getProperty("os.name").equals("Mac OS X")) {
      jTextFieldTrg.setText(trgPath.getAbsolutePath());
    }
  }

  /**
   * This method sets up the content pane
   *
   * @return javax.swing.JPanel
   */
  private JPanel getJContentPane() throws IOException {
    boolean isMac = System.getProperty("os.name").equals("Mac OS X");

    JScrollPane webView = getJScrollPane();
    JLabel sourceLabel = new JLabel("Source Path (\"WINLEM\" directory)");
    JButton srcButton = getBrowseButton(jTextFieldSrc);
    JLabel targetLabel = new JLabel("Target Path");
    JButton trgButton = getBrowseButton(jTextFieldTrg);
    JButton buttonQuit = getJButtonQuit();
    JButton buttonExtract = getJButtonExtract();
  
    javax.swing.JPanel jContentPane = new javax.swing.JPanel();
    GroupLayout layout = new GroupLayout(jContentPane);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);
    jContentPane.setLayout(layout);

    GroupLayout.SequentialGroup v = layout.createSequentialGroup()
      .addComponent(webView)
      .addComponent(sourceLabel)
      .addGroup(layout.createParallelGroup()
        .addComponent(jTextFieldSrc)
        .addComponent(srcButton));

    if(!isMac)
      v.addComponent(targetLabel)
       .addGroup(layout.createParallelGroup()
        .addComponent(jTextFieldTrg)
        .addComponent(trgButton));

    v.addGap(10)
    .addGroup(layout.createParallelGroup()
      .addComponent(buttonQuit)
      .addComponent(buttonExtract));
    layout.setVerticalGroup(v);

    GroupLayout.ParallelGroup h = layout.createParallelGroup()
      .addComponent(webView)
      .addComponent(sourceLabel)
      .addGroup(layout.createSequentialGroup()
        .addComponent(jTextFieldSrc)
        .addComponent(srcButton));

    if(!isMac)
      h.addComponent(targetLabel)
       .addGroup(layout.createSequentialGroup()
        .addComponent(jTextFieldTrg)
        .addComponent(trgButton));

    h.addGroup(layout.createSequentialGroup()
      .addComponent(buttonQuit)
      .addGap(0, 999999, 999999)
      .addComponent(buttonExtract));
    layout.setHorizontalGroup(h);

    return jContentPane;
  }

  /**
   * This method creates a cancel button
   *
   * @return javax.swing.JButton
   */
  private JButton getJButtonQuit() {
    JButton button = new JButton();
    button.setText("Cancel");
    button.addActionListener(
        (java.awt.event.ActionEvent e) -> {
          ok = false;
          dispose();
        });
    return button;
  }
  

  /**
   * This method creates a browse button
   *
   * @return javax.swing.JButton
   */
  private JButton getBrowseButton(JTextField textField) {
    JButton button = new JButton();
    button.setText("Browse");
    button.addActionListener(
      (java.awt.event.ActionEvent e) -> {
        JFileChooser jf = new JFileChooser(textField.getText());
        jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = jf.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          textField.setText(jf.getSelectedFile().getAbsolutePath());
        }
      });
    return button;
  }


  /**
   * This creates a proceed button
   *
   * @return javax.swing.JButton
   */
  private JButton getJButtonExtract() {
    JButton jButtonOk = new JButton();
    jButtonOk.setText("Agree & Extract");
    jButtonOk.addActionListener(
        (java.awt.event.ActionEvent e) -> {
          ok = true;
          dispose();
        });
    return jButtonOk;
  }

  /**
   * This method creates a scrollable html view
   *
   * @return javax.swing.JScrollPane
   */
  private JScrollPane getJScrollPane() throws IOException {
    JScrollPane jScrollPane = new JScrollPane();
    jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    ClassLoader loader = StartupDialog.class.getClassLoader();
    URL webpage = loader.getResource("disclaimer.htm");

    JEditorPane thisEditor = new JEditorPane(webpage);
    thisEditor.setEditable(false);
    // needed to open browser via clicking on a link
    thisEditor.addHyperlinkListener(
        (HyperlinkEvent e) -> {
          URL url = e.getURL();
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
              if (url.sameFile(webpage)) thisEditor.setPage(url);
              else Desktop.getDesktop().browse(url.toURI());
            } catch (IOException | URISyntaxException ex) {
            }
          } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            if (url.sameFile(webpage)) thisEditor.setToolTipText(url.getRef());
            else thisEditor.setToolTipText(url.toExternalForm());
          } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            thisEditor.setToolTipText(null);
          }
        });
    jScrollPane.setViewportView(thisEditor); // Generated

    return jScrollPane;
  }

  /**
   * Get target (Lemmini resource) path for extraction.
   *
   * @return target (Lemmini resource) path for extraction
   */
  public File getTarget() {
    return new File(jTextFieldTrg.getText());
  }

  /**
   * Get source (WINLEMM) path for extraction.
   *
   * @return source (WINLEMM) path for extraction
   */
  public File getSource() {
    return new File(jTextFieldSrc.getText());
  }

  /**
   * Ok button was pressed.
   *
   * @return true: ok button was pressed.
   */
  public boolean isOk() {
    return ok;
  }
} //  @jve:decl-index=0:visual-constraint="10,10"
