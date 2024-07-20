package Tools;

/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All  Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT
 * OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN
 * IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

/*
 * @(#)JFileFilter.java	1.14 03/01/23
 */

import java.io.File;
import java.util.HashSet;
import javax.swing.filechooser.FileFilter;

/**
 * A convenience implementation of FileFilter that extensions out all files except for those type
 * extensions that it knows about.
 *
 * <p>Extensions are of the type ".foo", which is typically found on Windows and Unix boxes, but not
 * on Macinthosh. Case is ignored.
 *
 * <p>Example - create a new filter that filerts out all files but gif and jpg image files:
 *
 * <p><code>JFileChooser chooser = new JFileChooser();
 *     JFileFilter filter = new JFileFilter(
 *                   new String{"gif", "jpg"}, "JPEG & GIF Images")
 *     chooser.addChoosableFileFilter(filter);
 *     chooser.showOpenDialog(this);</code>
 *
 * @version 1.14 01/23/03
 * @author Jeff Dinkins
 */
public class JFileFilter extends FileFilter {

  private HashSet<String> extensions = new HashSet<String>();
  private String description = null;

  /**
   * Creates a file filter from the given string array and description. Example: new
   * JFileFilter(String {"gif", "jpg"}, "Gif and JPG Images");
   *
   * <p>Note that the "." before the extension is not needed and will be ignored.
   *
   * @param description string containing file description
   * @param extensions string array containing extensions
   * @see #addExtension(String)
   */
  public JFileFilter(final String desc, final String... exts) {
    for (String ext : exts) {
      extensions.add(ext);
    }
    description = desc;
  }

  /**
   * Return true if this file should be shown in the directory pane, false if it shouldn't.
   *
   * <p>Files that begin with "." are ignored.
   *
   * @see #getExtension(File)
   * @see #accept(File)
   */
  @Override
  public boolean accept(final File f) {
    if (f == null) return false;
    if (f.isDirectory()) return true;

    String extension = ToolBox.getExtension(f);
    return extension != null && extensions.contains(extension);
  }

  /**
   * Returns the human readable description of this filter. For example: "JPEG and GIF Image Files
   * (*.jpg, *.gif)"
   *
   * @see #setDescription(String)
   * @see #setExtensionListInDescription(boolean)
   * @see #isExtensionListInDescription()
   * @see #getDescription()
   */
  @Override
  public String getDescription() {
    String fullDescription = description;

    // build the description from the extension list
    if (!extensions.isEmpty()) fullDescription += " (." + String.join(", .", extensions) + ")";

    return fullDescription;
  }
}
