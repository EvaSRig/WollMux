/*
* Dateiname: Common.java
* Projekt  : WollMux
* Funktion : Enth�lt von den Dialogen gemeinsam genutzten Code.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 22.11.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * Enth�lt von den Dialogen gemeinsam genutzten Code.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Common
{
  /**
   * Setzt das System Look and Feel, falls es nicht MetalLookAndFeel ist. Ansonsten setzt es GTKLookAndFeel falls m�glich.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void setLookAndFeel()
  {
    String lafName = UIManager.getSystemLookAndFeelClassName();
    if (lafName.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
      lafName = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
    try{UIManager.setLookAndFeel(lafName);}catch(Exception x){};
    JFrame.setDefaultLookAndFeelDecorated(true);
  }
}
